package org.shiyuliu.gowhen.quickfix

import org.shiyuliu.gowhen.chain.ChainIssue

object FixAppender {
    fun buildExhaustiveInsertion(
        text: String,
        issue: ChainIssue,
    ): FixInsertion {
        val layout = detectChainLayout(
            text = text,
            issue = issue,
        )

        val insertText = when (layout) {
            ChainLayout.SINGLE_LINE -> ".Exhaustive()"
            ChainLayout.MULTI_LINE -> buildMultiLineExhaustiveText(
                text = text,
                issue = issue,
            )
        }

        return FixInsertion(
            offset = issue.insertOffset,
            text = insertText,
        )
    }

    private fun detectChainLayout(
        text: String,
        issue: ChainIssue,
    ): ChainLayout {
        val lastAction = issue.chain.matchers
            .lastOrNull()
            ?.action
            ?: return ChainLayout.SINGLE_LINE

        val betweenDotAndActionName = text.substring(
            lastAction.dotOffset,
            lastAction.nameStartOffset,
        )

        return if (betweenDotAndActionName.contains('\n')) {
            ChainLayout.MULTI_LINE
        } else {
            ChainLayout.SINGLE_LINE
        }
    }

    private fun buildMultiLineExhaustiveText(
        text: String,
        issue: ChainIssue,
    ): String {
        val lastAction = issue.chain.matchers
            .lastOrNull()
            ?.action
            ?: return ".\n\tExhaustive()"

        val indent = readIndentBeforeOffset(
            text = text,
            offset = lastAction.nameStartOffset,
        )

        return ".\n${indent}Exhaustive()"
    }

    private fun readIndentBeforeOffset(
        text: String,
        offset: Int,
    ): String {
        var lineStart = offset

        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        return text.substring(lineStart, offset)
            .takeWhile { ch -> ch == ' ' || ch == '\t' }
    }
}

package org.shiyuliu.gowhen.quickfix

import org.shiyuliu.gowhen.chain.model.ChainIssue

object FixAppender {
    fun buildTerminalInsertion(
        text: String,
        issue: ChainIssue,
        kind: TerminalFixKind,
    ): FixInsertion {
        val layout = detectChainLayout(
            text = text,
            issue = issue,
        )

        val insertText = when (layout) {
            ChainLayout.SINGLE_LINE -> kind.singleLineText
            ChainLayout.MULTI_LINE -> buildMultiLineTerminalText(
                text = text,
                issue = issue,
                kind = kind,
            )
        }

        return FixInsertion(
            offset = issue.chain.endOffset,
            text = insertText,
        )
    }

    private fun buildMultiLineTerminalText(
        text: String,
        issue: ChainIssue,
        kind: TerminalFixKind,
    ): String {
        val lastAction = issue.chain.matchers
            .lastOrNull()
            ?.action
            ?: return ".\n\t${kind.multiLineCallText}"

        val indent = readIndentBeforeOffset(
            text = text,
            offset = lastAction.nameStartOffset,
        )

        return ".\n${indent}${kind.multiLineCallText}"
    }

    private fun detectChainLayout(
        text: String,
        issue: ChainIssue,
    ): ChainLayout {
        val chain = issue.chain
        val lastAction = chain.matchers.lastOrNull()?.action
            ?: return ChainLayout.SINGLE_LINE

        val rootLine = lineNumberAt(
            text = text,
            offset = chain.root.startOffset,
        )

        val lastActionLine = lineNumberAt(
            text = text,
            offset = lastAction.startOffset,
        )

        return if (lastActionLine == rootLine) {
            ChainLayout.SINGLE_LINE
        } else {
            ChainLayout.MULTI_LINE
        }
    }

    private fun lineNumberAt(
        text: String,
        offset: Int,
    ): Int {
        var line = 0
        var i = 0

        while (i < offset && i < text.length) {
            if (text[i] == '\n') {
                line++
            }

            i++
        }

        return line
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

package org.shiyuliu.gowhen.chain

import com.goide.psi.GoFile
import com.intellij.psi.PsiFile
import org.shiyuliu.gowhen.chain.analyzer.ChainIssueAnalyzer
import org.shiyuliu.gowhen.chain.model.Chain
import org.shiyuliu.gowhen.chain.model.ChainIssue
import org.shiyuliu.gowhen.chain.constants.ScannerConstants
import org.shiyuliu.gowhen.imports.model.ImportedPackage
import org.shiyuliu.gowhen.imports.ImportsParser

object ChainScanner {
    fun scan(file: PsiFile): List<ChainIssue> {
        val goFile = file as? GoFile ?: return emptyList()
        val importedPackage = ImportsParser.parse(goFile) ?: return emptyList()

        val chains = scanChains(
            text = file.text,
            importedPackage = importedPackage,
        )

        return chains.flatMap { chain ->
            ChainIssueAnalyzer.analyze(chain)
        }
    }

    fun scanChains(
        text: String,
        importedPackage: ImportedPackage,
    ): List<Chain> {
        val chains = mutableListOf<Chain>()
        val alias = importedPackage.alias

        var searchFrom = 0

        while (searchFrom < text.length) {
            val aliasStartOffset = text.indexOf(alias, searchFrom)
            if (aliasStartOffset < 0) {
                break
            }

            val aliasEndOffset = aliasStartOffset + alias.length

            if (!hasAliasBoundary(
                    text = text,
                    aliasStartOffset = aliasStartOffset,
                    aliasEndOffset = aliasEndOffset,
                )
            ) {
                searchFrom = aliasStartOffset + 1
                continue
            }

            val dotOffset = aliasEndOffset
            if (dotOffset >= text.length || text[dotOffset] != '.') {
                searchFrom = aliasStartOffset + 1
                continue
            }

            val rootNameStartOffset = dotOffset + 1
            val rootNameEndOffset = readIdentifierEnd(text, rootNameStartOffset)
            if (rootNameEndOffset == rootNameStartOffset) {
                searchFrom = aliasStartOffset + 1
                continue
            }

            val rootFuncName = text.substring(rootNameStartOffset, rootNameEndOffset)
            if (rootFuncName !in ScannerConstants.ROOT_METHODS) {
                searchFrom = aliasStartOffset + 1
                continue
            }

            val chain = ChainParser.parse(
                text = text,
                importedPackage = importedPackage,
                rootFuncName = rootFuncName,
                rootStartOffset = aliasStartOffset,
            )

            if (chain == null) {
                searchFrom = rootNameEndOffset
                continue
            }

            chains.add(chain)

            // Skip the whole parsed chain to avoid re-scanning inside the same chain.
            // This is intentionally faster, but it also means nested go-when chains inside
            // arguments/lambdas of this chain are not scanned as independent chains.
//            searchFrom = maxOf(chain.endOffset, aliasStartOffset + 1)
            searchFrom = aliasStartOffset + alias.length + 1
        }

        return chains.sortedBy { it.startOffset }
    }

    private fun hasAliasBoundary(
        text: String,
        aliasStartOffset: Int,
        aliasEndOffset: Int,
    ): Boolean {
        val beforeOk = aliasStartOffset == 0 || !isIdentifierChar(text[aliasStartOffset - 1])
        val afterOk = aliasEndOffset < text.length && text[aliasEndOffset] == '.'

        return beforeOk && afterOk
    }

    private fun readIdentifierEnd(
        text: String,
        start: Int,
    ): Int {
        var i = start

        while (i < text.length) {
            val ch = text[i]
            if (isIdentifierChar(ch)) {
                i++
            } else {
                break
            }
        }

        return i
    }

    private fun isIdentifierChar(ch: Char): Boolean {
        return ch == '_' || ch.isLetterOrDigit()
    }
}

package org.shiyuliu.gowhen.chain

import org.shiyuliu.gowhen.constant.ScannerConstants
import org.shiyuliu.gowhen.import.ImportedPackage

object ChainParser {
    fun parse(
        text: String,
        importedPackage: ImportedPackage,
        rootFuncName: String,
        rootStartOffset: Int,
    ): Chain? {
        return parse(
            text = text,
            alias = importedPackage.alias,
            rootFuncName = rootFuncName,
            rootStartOffset = rootStartOffset,
        )
    }

    fun parse(
        text: String,
        alias: String,
        rootFuncName: String,
        rootStartOffset: Int,
    ): Chain? {
        val matcherType = ScannerConstants.matcherTypeOfRootMethod(rootFuncName)
            ?: return null

        val rootPrefix = "$alias.$rootFuncName"
        if (!text.startsWith(rootPrefix, rootStartOffset)) {
            return null
        }

        val rootNameStartOffset = rootStartOffset + alias.length + 1
        val rootNameEndOffset = rootNameStartOffset + rootFuncName.length

        var pos = rootStartOffset + rootPrefix.length
        pos = skipWhitespaceAndComments(text, pos)

        // Supports generic roots:
        // when.MatchAs[string](value)
        // when.MatchAnyAs[Result](value)
        // when.Err[Response](err)
        if (pos < text.length && text[pos] == '[') {
            val closeBracketOffset = findMatching(
                text = text,
                openOffset = pos,
                open = '[',
                close = ']',
            )

            if (closeBracketOffset < 0) {
                return null
            }

            pos = closeBracketOffset + 1
            pos = skipWhitespaceAndComments(text, pos)
        }

        if (pos >= text.length || text[pos] != '(') {
            return null
        }

        val rootCloseParenOffset = findMatching(
            text = text,
            openOffset = pos,
            open = '(',
            close = ')',
        )

        if (rootCloseParenOffset < 0) {
            return null
        }

        val rootEndOffset = rootCloseParenOffset + 1

        val root = ChainRoot(
            type = matcherType,
            alias = alias,
            name = rootFuncName,
            startOffset = rootStartOffset,
            nameStartOffset = rootNameStartOffset,
            nameEndOffset = rootNameEndOffset,
            endOffset = rootEndOffset,
        )

        val rootModifiers = mutableListOf<ChainRootModifier>()
        val matchers = mutableListOf<ChainMatcher>()
        val terminals = mutableListOf<ChainTerminal>()

        pos = rootEndOffset
        var chainEndOffset = rootEndOffset

        // Root modifiers must appear immediately after the root call:
        //
        // when.MatchAs[Resp](code).
        //     WithErr().
        //     Case(400).ThenErr(resp, err).
        //     ElseErr(resp, err)
        //
        // Keep them separate from matchers and terminals so ChainRoot.endOffset
        // continues to represent only the root call.
        while (true) {
            val call = parseCall(text, pos) ?: break

            if (call.name !in ScannerConstants.ROOT_MODIFIER_METHODS) {
                break
            }

            rootModifiers.add(
                ChainRootModifier(
                    name = call.name,
                    dotOffset = call.dotOffset,
                    nameStartOffset = call.nameStartOffset,
                    nameEndOffset = call.nameEndOffset,
                    startOffset = call.startOffset,
                    endOffset = call.endOffset,
                )
            )

            chainEndOffset = call.endOffset
            pos = call.endOffset
        }

        while (true) {
            val call = parseCall(text, pos) ?: break

            when {
                call.name in ScannerConstants.CONDITION_METHODS -> {
                    val condition = MatcherCondition(
                        name = call.name,
                        dotOffset = call.dotOffset,
                        nameStartOffset = call.nameStartOffset,
                        nameEndOffset = call.nameEndOffset,
                        openParenOffset = call.openParenOffset,
                        closeParenOffset = call.closeParenOffset,
                        argsStartOffset = call.argsStartOffset,
                        argsEndOffset = call.argsEndOffset,
                        startOffset = call.startOffset,
                        endOffset = call.endOffset,
                    )

                    val action = parseActionAfterCondition(
                        text = text,
                        start = call.endOffset,
                    )

                    if (action == null) {
                        matchers.add(
                            ChainMatcher(
                                condition = condition,
                                action = null,
                                startOffset = condition.startOffset,
                                endOffset = condition.endOffset,
                            )
                        )

                        chainEndOffset = condition.endOffset
                        pos = condition.endOffset
                        continue
                    }

                    matchers.add(
                        ChainMatcher(
                            condition = condition,
                            action = action,
                            startOffset = condition.startOffset,
                            endOffset = action.endOffset,
                        )
                    )

                    chainEndOffset = action.endOffset
                    pos = action.endOffset
                }

                call.name in ScannerConstants.TERMINAL_METHODS -> {
                    terminals.add(
                        ChainTerminal(
                            name = call.name,
                            startOffset = call.startOffset,
                            nameStartOffset = call.nameStartOffset,
                            nameEndOffset = call.nameEndOffset,
                            endOffset = call.endOffset,
                        )
                    )

                    chainEndOffset = call.endOffset
                    pos = call.endOffset

                    // Do not break here. Keep parsing so the analyzer can detect
                    // duplicate terminals or matchers that appear after a terminal.
                }

                call.name in ScannerConstants.ACTION_METHODS -> {
                    // Orphan Then / ThenDo / ThenErr / ThenDoE. Parser keeps the
                    // valid prefix and lets Go type checking or future analyzer
                    // rules report the error.
                    break
                }

                else -> {
                    break
                }
            }
        }

        val numericIntervals = NumericConditionExtractor.extract(
            text = text,
            alias = alias,
            matchers = matchers,
        )

        return Chain(
            root = root,
            rootModifiers = rootModifiers,
            matchers = matchers,
            terminals = terminals,
            numericIntervals = numericIntervals,
            startOffset = rootStartOffset,
            endOffset = chainEndOffset,
        )
    }

    private fun parseActionAfterCondition(
        text: String,
        start: Int,
    ): MatcherAction? {
        val call = parseCall(text, start) ?: return null

        if (call.name !in ScannerConstants.ACTION_METHODS) {
            return null
        }

        return MatcherAction(
            name = call.name,
            dotOffset = call.dotOffset,
            nameStartOffset = call.nameStartOffset,
            nameEndOffset = call.nameEndOffset,
            startOffset = call.startOffset,
            endOffset = call.endOffset,
        )
    }

    private fun parseCall(
        text: String,
        start: Int,
    ): ParsedCall? {
        var pos = skipWhitespaceAndComments(text, start)

        if (pos >= text.length || text[pos] != '.') {
            return null
        }

        val dotOffset = pos

        pos++
        pos = skipWhitespaceAndComments(text, pos)

        val nameStartOffset = pos
        val nameEndOffset = readIdentifierEnd(text, nameStartOffset)

        if (nameEndOffset == nameStartOffset) {
            return null
        }

        val name = text.substring(nameStartOffset, nameEndOffset)

        pos = skipWhitespaceAndComments(text, nameEndOffset)

        if (pos >= text.length || text[pos] != '(') {
            return null
        }

        val closeParenOffset = findMatching(
            text = text,
            openOffset = pos,
            open = '(',
            close = ')',
        )

        if (closeParenOffset < 0) {
            return null
        }

        return ParsedCall(
            name = name,
            dotOffset = dotOffset,
            nameStartOffset = nameStartOffset,
            nameEndOffset = nameEndOffset,
            openParenOffset = pos,
            closeParenOffset = closeParenOffset,
            argsStartOffset = pos + 1,
            argsEndOffset = closeParenOffset,
            startOffset = dotOffset,
            endOffset = closeParenOffset + 1,
        )
    }

    private data class ParsedCall(
        val name: String,
        val dotOffset: Int,
        val nameStartOffset: Int,
        val nameEndOffset: Int,
        val openParenOffset: Int,
        val closeParenOffset: Int,
        val argsStartOffset: Int,
        val argsEndOffset: Int,
        val startOffset: Int,
        val endOffset: Int,
    )

    private fun skipWhitespaceAndComments(text: String, start: Int): Int {
        var i = start

        while (i < text.length) {
            while (i < text.length && text[i].isWhitespace()) {
                i++
            }

            if (i + 1 < text.length && text[i] == '/' && text[i + 1] == '/') {
                i += 2
                while (i < text.length && text[i] != '\n') {
                    i++
                }
                continue
            }

            if (i + 1 < text.length && text[i] == '/' && text[i + 1] == '*') {
                i += 2
                while (i + 1 < text.length && !(text[i] == '*' && text[i + 1] == '/')) {
                    i++
                }

                if (i + 1 < text.length) {
                    i += 2
                }

                continue
            }

            break
        }

        return i
    }

    private fun readIdentifierEnd(text: String, start: Int): Int {
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

    private fun findMatching(
        text: String,
        openOffset: Int,
        open: Char,
        close: Char,
    ): Int {
        var depth = 0
        var i = openOffset

        var inLineComment = false
        var inBlockComment = false
        var inRawString = false
        var inString = false
        var inRune = false
        var escaped = false

        while (i < text.length) {
            val ch = text[i]
            val next = text.getOrNull(i + 1)

            if (inLineComment) {
                if (ch == '\n') {
                    inLineComment = false
                }

                i++
                continue
            }

            if (inBlockComment) {
                if (ch == '*' && next == '/') {
                    inBlockComment = false
                    i += 2
                } else {
                    i++
                }

                continue
            }

            if (inRawString) {
                if (ch == '`') {
                    inRawString = false
                }

                i++
                continue
            }

            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inString = false
                }

                i++
                continue
            }

            if (inRune) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '\'') {
                    inRune = false
                }

                i++
                continue
            }

            if (ch == '/' && next == '/') {
                inLineComment = true
                i += 2
                continue
            }

            if (ch == '/' && next == '*') {
                inBlockComment = true
                i += 2
                continue
            }

            when (ch) {
                '`' -> inRawString = true
                '"' -> inString = true
                '\'' -> inRune = true

                open -> depth++

                close -> {
                    depth--

                    if (depth == 0) {
                        return i
                    }
                }
            }

            i++
        }

        return -1
    }
}

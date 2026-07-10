package org.shiyuliu.gowhen.chain.extractor

import org.shiyuliu.gowhen.chain.model.ChainNumericInterval
import org.shiyuliu.gowhen.chain.model.NumericIntervalSourceKind
import org.shiyuliu.gowhen.chain.model.RangeBound
import org.shiyuliu.gowhen.chain.model.ChainMatcher
import org.shiyuliu.gowhen.chain.model.MatcherAction
import org.shiyuliu.gowhen.chain.model.MatcherCondition

object NumericConditionExtractor {
    fun extract(
        text: String,
        alias: String,
        matchers: List<ChainMatcher>,
    ): List<ChainNumericInterval> {
        val numericIntervals = mutableListOf<ChainNumericInterval>()

        for ((matcherIndex, matcher) in matchers.withIndex()) {
            val condition = matcher.condition

            when (condition.name) {
                "Case" -> {
                    numericIntervals.addAll(
                        extractIntervalsFromCaseCondition(
                            text = text,
                            matcherIndex = matcherIndex,
                            condition = condition,
                            action = matcher.action,
                        )
                    )
                }

                "Range" -> {
                    numericIntervals.addAll(
                        extractIntervalsFromRangeCondition(
                            text = text,
                            alias = alias,
                            matcherIndex = matcherIndex,
                            condition = condition,
                            action = matcher.action,
                        )
                    )
                }
            }
        }

        return numericIntervals.sortedBy { interval ->
            interval.startOffset
        }
    }

    private fun extractIntervalsFromCaseCondition(
        text: String,
        matcherIndex: Int,
        condition: MatcherCondition,
        action: MatcherAction?,
    ): List<ChainNumericInterval> {
        val intervals = mutableListOf<ChainNumericInterval>()

        val args = splitTopLevelArgs(
            text = text,
            start = condition.argsStartOffset,
            end = condition.argsEndOffset,
        )

        for (arg in args) {
            val bound = parseNumericBound(
                text = text,
                range = arg,
            ) ?: continue

            intervals.add(
                ChainNumericInterval(
                    sourceKind = NumericIntervalSourceKind.CASE,
                    matcherIndex = matcherIndex,
                    condition = condition,
                    action = action,
                    lower = bound,
                    upper = bound,
                    lowerInclusive = true,
                    upperInclusive = true,
                    startOffset = bound.sourceStartOffset,
                    endOffset = bound.sourceEndOffset,
                )
            )
        }

        return intervals
    }

    private fun extractIntervalsFromRangeCondition(
        text: String,
        alias: String,
        matcherIndex: Int,
        condition: MatcherCondition,
        action: MatcherAction?,
    ): List<ChainNumericInterval> {
        val intervals = mutableListOf<ChainNumericInterval>()

        var pos = condition.argsStartOffset
        val end = condition.argsEndOffset

        while (pos < end) {
            val call = parseQualifiedCall(
                text = text,
                alias = alias,
                start = pos,
                end = end,
            )

            if (call == null) {
                pos++
                continue
            }

            val interval = parseRangeHelperCall(
                text = text,
                matcherIndex = matcherIndex,
                condition = condition,
                action = action,
                call = call,
            )

            if (interval != null) {
                intervals.add(interval)
            }

            pos = maxOf(call.endOffset, pos + 1)
        }

        return intervals
    }

    private fun parseQualifiedCall(
        text: String,
        alias: String,
        start: Int,
        end: Int,
    ): ParsedQualifiedCall? {
        var aliasStartOffset = text.indexOf(alias, start)

        while (aliasStartOffset >= 0 && aliasStartOffset < end) {
            val aliasEndOffset = aliasStartOffset + alias.length

            if (!hasIdentifierBoundary(text, aliasStartOffset, aliasEndOffset)) {
                aliasStartOffset = text.indexOf(alias, aliasStartOffset + 1)
                continue
            }

            var pos = aliasEndOffset

            if (pos >= end || text[pos] != '.') {
                aliasStartOffset = text.indexOf(alias, aliasStartOffset + 1)
                continue
            }

            val dotOffset = pos
            pos++

            val nameStartOffset = pos
            val nameEndOffset = readIdentifierEnd(text, nameStartOffset)

            if (nameEndOffset == nameStartOffset) {
                aliasStartOffset = text.indexOf(alias, aliasStartOffset + 1)
                continue
            }

            val name = text.substring(nameStartOffset, nameEndOffset)

            pos = skipWhitespaceAndComments(text, nameEndOffset)

            if (pos >= end || text[pos] != '(') {
                aliasStartOffset = text.indexOf(alias, aliasStartOffset + 1)
                continue
            }

            val openParenOffset = pos
            val closeParenOffset = findMatching(
                text = text,
                openOffset = openParenOffset,
                open = '(',
                close = ')',
            )

            if (closeParenOffset < 0 || closeParenOffset >= end) {
                aliasStartOffset = text.indexOf(alias, aliasStartOffset + 1)
                continue
            }

            return ParsedQualifiedCall(
                alias = alias,
                name = name,
                dotOffset = dotOffset,
                nameStartOffset = nameStartOffset,
                nameEndOffset = nameEndOffset,
                openParenOffset = openParenOffset,
                closeParenOffset = closeParenOffset,
                argsStartOffset = openParenOffset + 1,
                argsEndOffset = closeParenOffset,
                startOffset = aliasStartOffset,
                endOffset = closeParenOffset + 1,
            )
        }

        return null
    }

    private fun parseRangeHelperCall(
        text: String,
        matcherIndex: Int,
        condition: MatcherCondition,
        action: MatcherAction?,
        call: ParsedQualifiedCall,
    ): ChainNumericInterval? {
        return when (call.name) {
            "Range" -> parseTwoBoundRange(
                text = text,
                matcherIndex = matcherIndex,
                condition = condition,
                action = action,
                call = call,
                lowerInclusive = true,
                upperInclusive = false,
            )

            "Closed" -> parseTwoBoundRange(
                text = text,
                matcherIndex = matcherIndex,
                condition = condition,
                action = action,
                call = call,
                lowerInclusive = true,
                upperInclusive = true,
            )

            "From" -> parseFromRange(
                text = text,
                matcherIndex = matcherIndex,
                condition = condition,
                action = action,
                call = call,
            )

            "Until" -> parseUntilRange(
                text = text,
                matcherIndex = matcherIndex,
                condition = condition,
                action = action,
                call = call,
            )

            else -> null
        }
    }

    private fun parseTwoBoundRange(
        text: String,
        matcherIndex: Int,
        condition: MatcherCondition,
        action: MatcherAction?,
        call: ParsedQualifiedCall,
        lowerInclusive: Boolean,
        upperInclusive: Boolean,
    ): ChainNumericInterval? {
        val args = splitTopLevelArgs(
            text = text,
            start = call.argsStartOffset,
            end = call.argsEndOffset,
        )

        if (args.size != 2) {
            return null
        }

        val lower = parseNumericBound(text, args[0]) ?: return null
        val upper = parseNumericBound(text, args[1]) ?: return null

        return ChainNumericInterval(
            sourceKind = NumericIntervalSourceKind.RANGE,
            matcherIndex = matcherIndex,
            condition = condition,
            action = action,
            lower = lower,
            upper = upper,
            lowerInclusive = lowerInclusive,
            upperInclusive = upperInclusive,
            startOffset = call.startOffset,
            endOffset = call.endOffset,
        )
    }

    private fun parseFromRange(
        text: String,
        matcherIndex: Int,
        condition: MatcherCondition,
        action: MatcherAction?,
        call: ParsedQualifiedCall,
    ): ChainNumericInterval? {
        val args = splitTopLevelArgs(
            text = text,
            start = call.argsStartOffset,
            end = call.argsEndOffset,
        )

        if (args.size != 1) {
            return null
        }

        val lower = parseNumericBound(text, args[0]) ?: return null

        return ChainNumericInterval(
            sourceKind = NumericIntervalSourceKind.RANGE,
            matcherIndex = matcherIndex,
            condition = condition,
            action = action,
            lower = lower,
            upper = null,
            lowerInclusive = true,
            upperInclusive = false,
            startOffset = call.startOffset,
            endOffset = call.endOffset,
        )
    }

    private fun parseUntilRange(
        text: String,
        matcherIndex: Int,
        condition: MatcherCondition,
        action: MatcherAction?,
        call: ParsedQualifiedCall,
    ): ChainNumericInterval? {
        val args = splitTopLevelArgs(
            text = text,
            start = call.argsStartOffset,
            end = call.argsEndOffset,
        )

        if (args.size != 1) {
            return null
        }

        val upper = parseNumericBound(text, args[0]) ?: return null

        return ChainNumericInterval(
            sourceKind = NumericIntervalSourceKind.RANGE,
            matcherIndex = matcherIndex,
            condition = condition,
            action = action,
            lower = null,
            upper = upper,
            lowerInclusive = false,
            upperInclusive = false,
            startOffset = call.startOffset,
            endOffset = call.endOffset,
        )
    }

    private fun parseNumericBound(
        text: String,
        range: SourceRange,
    ): RangeBound? {
        val trimmed = trimWhitespaceAndComments(
            text = text,
            range = range,
        )

        if (trimmed.startOffset >= trimmed.endOffset) {
            return null
        }

        val raw = text.substring(
            trimmed.startOffset,
            trimmed.endOffset,
        )

        if (!NUMERIC_LITERAL_REGEX.matches(raw)) {
            return null
        }

        val value = raw.toDoubleOrNull() ?: return null

        return RangeBound(
            value = value,
            sourceStartOffset = trimmed.startOffset,
            sourceEndOffset = trimmed.endOffset,
        )
    }

    private fun splitTopLevelArgs(
        text: String,
        start: Int,
        end: Int,
    ): List<SourceRange> {
        val args = mutableListOf<SourceRange>()

        var argStart = start
        var pos = start

        var parenDepth = 0
        var bracketDepth = 0
        var braceDepth = 0

        var inLineComment = false
        var inBlockComment = false
        var inRawString = false
        var inString = false
        var inRune = false
        var escaped = false

        while (pos < end) {
            val ch = text[pos]
            val next = text.getOrNull(pos + 1)

            if (inLineComment) {
                if (ch == '\n') {
                    inLineComment = false
                }

                pos++
                continue
            }

            if (inBlockComment) {
                if (ch == '*' && next == '/') {
                    inBlockComment = false
                    pos += 2
                } else {
                    pos++
                }

                continue
            }

            if (inRawString) {
                if (ch == '`') {
                    inRawString = false
                }

                pos++
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

                pos++
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

                pos++
                continue
            }

            if (ch == '/' && next == '/') {
                inLineComment = true
                pos += 2
                continue
            }

            if (ch == '/' && next == '*') {
                inBlockComment = true
                pos += 2
                continue
            }

            when (ch) {
                '`' -> inRawString = true
                '"' -> inString = true
                '\'' -> inRune = true

                '(' -> parenDepth++
                ')' -> parenDepth--

                '[' -> bracketDepth++
                ']' -> bracketDepth--

                '{' -> braceDepth++
                '}' -> braceDepth--

                ',' -> {
                    if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                        args.add(
                            SourceRange(
                                startOffset = argStart,
                                endOffset = pos,
                            )
                        )

                        argStart = pos + 1
                    }
                }
            }

            pos++
        }

        args.add(
            SourceRange(
                startOffset = argStart,
                endOffset = end,
            )
        )

        return args
    }

    private fun trimWhitespaceAndComments(
        text: String,
        range: SourceRange,
    ): SourceRange {
        var start = range.startOffset
        var end = range.endOffset

        start = skipWhitespaceAndComments(text, start)

        while (end > start && text[end - 1].isWhitespace()) {
            end--
        }

        return SourceRange(
            startOffset = start,
            endOffset = end,
        )
    }

    private fun skipWhitespaceAndComments(
        text: String,
        start: Int,
    ): Int {
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

    private fun hasIdentifierBoundary(
        text: String,
        start: Int,
        end: Int,
    ): Boolean {
        val beforeOk = start == 0 || !isIdentifierChar(text[start - 1])
        val afterOk = end >= text.length || !isIdentifierChar(text[end])

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

    private data class ParsedQualifiedCall(
        val alias: String,
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

    private data class SourceRange(
        val startOffset: Int,
        val endOffset: Int,
    )

    private val NUMERIC_LITERAL_REGEX = Regex(
        pattern = """[-+]?(?:\d+(?:\.\d*)?|\.\d+)"""
    )
}

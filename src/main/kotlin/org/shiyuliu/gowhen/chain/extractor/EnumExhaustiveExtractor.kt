package org.shiyuliu.gowhen.chain.extractor

import org.shiyuliu.gowhen.chain.model.ChainEnumCase
import org.shiyuliu.gowhen.chain.model.ChainEnumFacts
import org.shiyuliu.gowhen.chain.model.ChainEnumKind
import org.shiyuliu.gowhen.chain.model.ChainMatcher
import org.shiyuliu.gowhen.chain.model.ChainRoot
import org.shiyuliu.gowhen.chain.model.ChainTerminal
import org.shiyuliu.gowhen.chain.model.MatcherType

object EnumExhaustiveExtractor {
    fun extract(
        text: String,
        root: ChainRoot,
        matchers: List<ChainMatcher>,
        terminals: List<ChainTerminal>,
    ): ChainEnumFacts? {
        if (root.type != MatcherType.MATCHER) {
            return null
        }

        if (terminals.none { terminal -> terminal.name == "Exhaustive" }) {
            return null
        }

        val enumKindsByType = extractEnumTypeDeclarations(text)
        if (enumKindsByType.isEmpty()) {
            return null
        }

        val declaredCasesByType = extractEnumConstCases(
            text = text,
            enumKindsByType = enumKindsByType,
        )

        if (declaredCasesByType.isEmpty()) {
            return null
        }

        val coveredCases = extractCoveredCaseRefs(
            text = text,
            matchers = matchers,
        )

        if (coveredCases.isEmpty()) {
            return null
        }

        val enumTypeByCaseName = declaredCasesByType
            .flatMap { (enumTypeName, cases) ->
                cases.map { enumCase -> enumCase.name to enumTypeName }
            }
            .toMap()

        val candidateEnumTypes = coveredCases
            .mapNotNull { coveredCase -> enumTypeByCaseName[coveredCase.name] }
            .distinct()

        // v1 only supports the common case where the chain's Case(...) refs
        // clearly point to exactly one enum type in the current file.
        if (candidateEnumTypes.size != 1) {
            return null
        }

        val enumTypeName = candidateEnumTypes.single()
        val enumKind = enumKindsByType[enumTypeName] ?: return null
        val declaredCases = declaredCasesByType[enumTypeName].orEmpty()

        if (declaredCases.isEmpty()) {
            return null
        }

        val declaredCaseNames = declaredCases
            .map { enumCase -> enumCase.name }
            .toSet()

        val coveredEnumCases = coveredCases
            .filter { coveredCase -> coveredCase.name in declaredCaseNames }

        if (coveredEnumCases.isEmpty()) {
            return null
        }

        return ChainEnumFacts(
            enumTypeName = enumTypeName,
            enumKind = enumKind,
            declaredCases = declaredCases,
            coveredCases = coveredEnumCases,
        )
    }

    private fun extractEnumTypeDeclarations(
        text: String,
    ): Map<String, ChainEnumKind> {
        val enumKindsByType = linkedMapOf<String, ChainEnumKind>()

        for (match in TYPE_DECL_REGEX.findAll(text)) {
            val typeName = match.groupValues[1]
            val underlying = match.groupValues[2]

            val enumKind = when (underlying) {
                "int" -> ChainEnumKind.INT
                "string" -> ChainEnumKind.STRING
                else -> continue
            }

            enumKindsByType[typeName] = enumKind
        }

        return enumKindsByType
    }

    private fun extractEnumConstCases(
        text: String,
        enumKindsByType: Map<String, ChainEnumKind>,
    ): Map<String, List<ChainEnumCase>> {
        val casesByType = linkedMapOf<String, MutableList<ChainEnumCase>>()

        extractEnumConstGroups(
            text = text,
            enumKindsByType = enumKindsByType,
            casesByType = casesByType,
        )

        extractSingleEnumConsts(
            text = text,
            enumKindsByType = enumKindsByType,
            casesByType = casesByType,
        )

        return casesByType
            .mapValues { (_, cases) ->
                cases.distinctBy { enumCase -> enumCase.name }
            }
    }

    private fun extractEnumConstGroups(
        text: String,
        enumKindsByType: Map<String, ChainEnumKind>,
        casesByType: MutableMap<String, MutableList<ChainEnumCase>>,
    ) {
        for (match in CONST_GROUP_REGEX.findAll(text)) {
            val groupStartOffset = match.range.first
            val groupText = match.value

            val openParenOffset = groupText.indexOf('(')
            val closeParenOffset = groupText.lastIndexOf(')')

            if (openParenOffset < 0 || closeParenOffset <= openParenOffset) {
                continue
            }

            val bodyStartOffset = groupStartOffset + openParenOffset + 1
            val body = groupText.substring(openParenOffset + 1, closeParenOffset)

            var currentEnumTypeName: String? = null

            var lineStartInBody = 0
            for (rawLine in body.lines()) {
                val lineStartOffset = bodyStartOffset + lineStartInBody

                val parsed = parseConstLine(
                    rawLine = rawLine,
                    lineStartOffset = lineStartOffset,
                    enumKindsByType = enumKindsByType,
                    inheritedEnumTypeName = currentEnumTypeName,
                )

                if (parsed != null) {
                    currentEnumTypeName = parsed.enumTypeName

                    casesByType
                        .getOrPut(parsed.enumTypeName) { mutableListOf() }
                        .add(parsed.enumCase)
                }

                lineStartInBody += rawLine.length + 1
            }
        }
    }

    private fun extractSingleEnumConsts(
        text: String,
        enumKindsByType: Map<String, ChainEnumKind>,
        casesByType: MutableMap<String, MutableList<ChainEnumCase>>,
    ) {
        for (match in SINGLE_CONST_REGEX.findAll(text)) {
            val caseName = match.groupValues[1]
            val enumTypeName = match.groupValues[2]

            if (enumTypeName !in enumKindsByType) {
                continue
            }

            val caseStartOffset = match.range.first + match.value.indexOf(caseName)
            val caseEndOffset = caseStartOffset + caseName.length

            val valueText = extractValueTextFromConstLine(match.value)

            casesByType
                .getOrPut(enumTypeName) { mutableListOf() }
                .add(
                    ChainEnumCase(
                        name = caseName,
                        valueText = valueText,
                        startOffset = caseStartOffset,
                        endOffset = caseEndOffset,
                    )
                )
        }
    }

    private fun parseConstLine(
        rawLine: String,
        lineStartOffset: Int,
        enumKindsByType: Map<String, ChainEnumKind>,
        inheritedEnumTypeName: String?,
    ): ParsedEnumConstLine? {
        val withoutLineComment = rawLine.substringBefore("//")
        val trimmedStart = withoutLineComment.indexOfFirst { ch -> !ch.isWhitespace() }

        if (trimmedStart < 0) {
            return null
        }

        val effectiveLine = withoutLineComment.substring(trimmedStart)

        // Skip local block comments or unsupported forms in v1.
        if (effectiveLine.startsWith("/*") || effectiveLine.startsWith("*")) {
            return null
        }

        val nameEndInEffectiveLine = readIdentifierEnd(
            text = effectiveLine,
            start = 0,
        )

        if (nameEndInEffectiveLine == 0) {
            return null
        }

        val caseName = effectiveLine.substring(0, nameEndInEffectiveLine)

        // v1 only handles one enum case name per const spec.
        // Example skipped:
        //   A, B Phase = iota, iota
        val afterName = effectiveLine.substring(nameEndInEffectiveLine)
        if (afterName.trimStart().startsWith(",")) {
            return null
        }

        val explicitEnumTypeName = firstIdentifier(afterName)
            ?.takeIf { typeName -> typeName in enumKindsByType }

        val enumTypeName = explicitEnumTypeName ?: inheritedEnumTypeName
            ?: return null

        if (enumTypeName !in enumKindsByType) {
            return null
        }

        val caseStartOffset = lineStartOffset + trimmedStart
        val caseEndOffset = caseStartOffset + caseName.length

        return ParsedEnumConstLine(
            enumTypeName = enumTypeName,
            enumCase = ChainEnumCase(
                name = caseName,
                valueText = extractValueTextFromConstLine(effectiveLine),
                startOffset = caseStartOffset,
                endOffset = caseEndOffset,
            ),
        )
    }

    private fun extractCoveredCaseRefs(
        text: String,
        matchers: List<ChainMatcher>,
    ): List<ChainEnumCase> {
        val coveredCases = mutableListOf<ChainEnumCase>()

        for (matcher in matchers) {
            val condition = matcher.condition

            if (condition.name != "Case") {
                continue
            }

            val args = splitTopLevelArgs(
                text = text,
                start = condition.argsStartOffset,
                end = condition.argsEndOffset,
            )

            for (arg in args) {
                val trimmed = trimWhitespaceAndComments(
                    text = text,
                    range = arg,
                )

                if (trimmed.startOffset >= trimmed.endOffset) {
                    continue
                }

                val raw = text.substring(
                    trimmed.startOffset,
                    trimmed.endOffset,
                )

                // v1 only supports bare enum const identifiers:
                //   Case(Pending)
                //   Case(Pending, Running)
                //
                // Skipped:
                //   Case(pkg.Pending)
                //   Case(Phase(1))
                //   Case("active")
                if (!IDENTIFIER_REGEX.matches(raw)) {
                    continue
                }

                coveredCases.add(
                    ChainEnumCase(
                        name = raw,
                        valueText = null,
                        startOffset = trimmed.startOffset,
                        endOffset = trimmed.endOffset,
                    )
                )
            }
        }

        return coveredCases.distinctBy { enumCase -> enumCase.name }
    }

    private fun extractValueTextFromConstLine(
        line: String,
    ): String? {
        val equalOffset = line.indexOf('=')

        if (equalOffset < 0) {
            return null
        }

        val raw = line.substring(equalOffset + 1)
            .substringBefore("//")
            .trim()

        if (raw.isEmpty()) {
            return null
        }

        return raw
    }

    private fun firstIdentifier(
        text: String,
    ): String? {
        val start = text.indexOfFirst { ch ->
            ch == '_' || ch.isLetter()
        }

        if (start < 0) {
            return null
        }

        val end = readIdentifierEnd(text, start)

        if (end == start) {
            return null
        }

        return text.substring(start, end)
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

    private fun readIdentifierEnd(
        text: String,
        start: Int,
    ): Int {
        var i = start

        while (i < text.length) {
            val ch = text[i]

            if (ch == '_' || ch.isLetterOrDigit()) {
                i++
            } else {
                break
            }
        }

        return i
    }

    private data class ParsedEnumConstLine(
        val enumTypeName: String,
        val enumCase: ChainEnumCase,
    )

    private data class SourceRange(
        val startOffset: Int,
        val endOffset: Int,
    )

    private val TYPE_DECL_REGEX = Regex(
        pattern = """\btype\s+([A-Za-z_]\w*)\s+(int|string)\b"""
    )

    private val CONST_GROUP_REGEX = Regex(
        pattern = """(?s)\bconst\s*\((.*?)\)"""
    )

    private val SINGLE_CONST_REGEX = Regex(
        pattern = """(?m)^\s*const\s+([A-Za-z_]\w*)\s+([A-Za-z_]\w*)\b(.*)$"""
    )

    private val IDENTIFIER_REGEX = Regex(
        pattern = """[A-Za-z_]\w*"""
    )
}

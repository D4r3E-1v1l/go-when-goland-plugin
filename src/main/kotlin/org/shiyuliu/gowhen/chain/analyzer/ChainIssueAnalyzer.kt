package org.shiyuliu.gowhen.chain.analyzer

import org.shiyuliu.gowhen.chain.model.ChainNumericInterval
import org.shiyuliu.gowhen.chain.model.RangeBound
import org.shiyuliu.gowhen.chain.model.Chain
import org.shiyuliu.gowhen.chain.model.ChainIssue
import org.shiyuliu.gowhen.chain.model.ChainIssueType
import org.shiyuliu.gowhen.chain.model.MatcherType
import org.shiyuliu.gowhen.chain.constants.ScannerConstants

object ChainIssueAnalyzer {
    fun analyze(chain: Chain): List<ChainIssue> {
        for (stage in STAGES) {
            val issues = stage.analyze(chain)

            if (issues.isNotEmpty()) {
                return issues
            }
        }

        return emptyList()
    }

    private val STAGES = listOf(
        ChainIssueStage(
            name = "chain-structure",
            rules = listOf(
                ::rootOnly,
                ::incompleteMatchers,
            ),
        ),
        ChainIssueStage(
            name = "matcher-type-compatibility",
            rules = listOf(
                ::invalidConditionsForMatcherType,
                ::invalidActionsForMatcherType,
                ::invalidTerminalsForMatcherType,
            ),
        ),
        ChainIssueStage(
            name = "terminal-structure",
            rules = listOf(
                ::missingTerminal,
                ::duplicateTerminals,
                ::terminalNotLast,
            ),
        ),
        ChainIssueStage(
            name = "numeric-semantic",
            rules = listOf(
                ::numericSemanticIssues,
            ),
        ),
        ChainIssueStage(
            name = "enum-exhaustive",
            rules = listOf(
                ::enumExhaustiveIssues,
            ),
        ),
    )

    private fun rootOnly(chain: Chain): List<ChainIssue> {
        if (chain.matchers.isNotEmpty() || chain.terminals.isNotEmpty()) {
            return emptyList()
        }

        return listOf(
            ChainIssue(
                type = ChainIssueType.ROOT_ONLY,
                chain = chain,
                startOffset = chain.root.startOffset,
                endOffset = chain.root.endOffset,
            )
        )
    }

    private fun incompleteMatchers(chain: Chain): List<ChainIssue> {
        return chain.matchers
            .filter { matcher -> matcher.action == null }
            .map { matcher ->
                ChainIssue(
                    type = ChainIssueType.INCOMPLETE_MATCHER,
                    chain = chain,
                    startOffset = matcher.condition.startOffset,
                    endOffset = matcher.condition.endOffset,
                )
            }
    }

    private fun invalidConditionsForMatcherType(chain: Chain): List<ChainIssue> {
        val matcherType = effectiveMatcherType(chain)

        return chain.matchers
            .filter { matcher ->
                !ScannerConstants.isConditionMethodAllowed(
                    matcherType = matcherType,
                    conditionMethodName = matcher.condition.name,
                )
            }
            .map { matcher ->
                ChainIssue(
                    type = ChainIssueType.INVALID_CONDITION_FOR_MATCHER_TYPE,
                    chain = chain,
                    startOffset = matcher.condition.nameStartOffset,
                    endOffset = matcher.condition.nameEndOffset,
                )
            }
    }

    private fun invalidActionsForMatcherType(chain: Chain): List<ChainIssue> {
        val matcherType = effectiveMatcherType(chain)

        return chain.matchers
            .mapNotNull { matcher -> matcher.action }
            .filter { action ->
                !ScannerConstants.isActionMethodAllowed(
                    matcherType = matcherType,
                    actionMethodName = action.name,
                )
            }
            .map { action ->
                ChainIssue(
                    type = ChainIssueType.INVALID_ACTION_FOR_MATCHER_TYPE,
                    chain = chain,
                    startOffset = action.nameStartOffset,
                    endOffset = action.nameEndOffset,
                )
            }
    }

    private fun invalidTerminalsForMatcherType(chain: Chain): List<ChainIssue> {
        val matcherType = effectiveMatcherType(chain)

        return chain.terminals
            .filter { terminal ->
                !ScannerConstants.isTerminalMethodAllowed(
                    matcherType = matcherType,
                    terminalMethodName = terminal.name,
                )
            }
            .map { terminal ->
                ChainIssue(
                    type = ChainIssueType.INVALID_TERMINAL_FOR_MATCHER_TYPE,
                    chain = chain,
                    startOffset = terminal.nameStartOffset,
                    endOffset = terminal.nameEndOffset,
                )
            }
    }

    private fun missingTerminal(chain: Chain): List<ChainIssue> {
        val hasCompleteMatcher = chain.matchers.any { matcher ->
            matcher.action != null
        }

        if (!hasCompleteMatcher) {
            return emptyList()
        }

        if (chain.terminals.isNotEmpty()) {
            return emptyList()
        }

        return listOf(
            ChainIssue(
                type = ChainIssueType.MISSING_TERMINAL,
                chain = chain,
                startOffset = chain.startOffset,
                endOffset = chain.endOffset,
            )
        )
    }

    private fun duplicateTerminals(chain: Chain): List<ChainIssue> {
        if (chain.terminals.size <= 1) {
            return emptyList()
        }

        return chain.terminals
            .drop(1)
            .map { terminal ->
                ChainIssue(
                    type = ChainIssueType.DUPLICATE_TERMINAL,
                    chain = chain,
                    startOffset = terminal.startOffset,
                    endOffset = terminal.endOffset,
                )
            }
    }

    private fun terminalNotLast(chain: Chain): List<ChainIssue> {
        val firstTerminal = chain.terminals.minByOrNull { terminal ->
            terminal.startOffset
        } ?: return emptyList()

        val matcherAfterTerminal = chain.matchers.firstOrNull { matcher ->
            matcher.startOffset > firstTerminal.startOffset
        }

        if (matcherAfterTerminal == null) {
            return emptyList()
        }

        return listOf(
            ChainIssue(
                type = ChainIssueType.TERMINAL_NOT_LAST,
                chain = chain,
                startOffset = firstTerminal.startOffset,
                endOffset = firstTerminal.endOffset,
            )
        )
    }

    private fun effectiveMatcherType(chain: Chain): MatcherType {
        return ScannerConstants.effectiveMatcherType(
            rootMatcherType = chain.root.type,
            withErr = chain.rootModifiers.any { rootModifier ->
                rootModifier.name == "WithErr"
            },
        )
    }

    private fun numericSemanticIssues(chain: Chain): List<ChainIssue> {
        if (chain.numericIntervals.isEmpty()) {
            return emptyList()
        }

        return NumericSemanticRules.analyze(chain)
    }

    private fun enumExhaustiveIssues(chain: Chain): List<ChainIssue> {
        val enumFacts = chain.enumFacts
            ?: return emptyList()

        val exhaustiveTerminal = chain.terminals.firstOrNull { terminal ->
            terminal.name == "Exhaustive"
        } ?: return emptyList()

        val coveredNames = enumFacts.coveredCases
            .map { enumCase -> enumCase.name }
            .toSet()

        val missingCases = enumFacts.declaredCases
            .filter { enumCase -> enumCase.name !in coveredNames }
            .map { enumCase -> enumCase.name }

        if (missingCases.isEmpty()) {
            return emptyList()
        }

        return listOf(
            ChainIssue(
                type = ChainIssueType.MISSING_ENUM_CASES,
                chain = chain,
                startOffset = exhaustiveTerminal.startOffset,
                endOffset = exhaustiveTerminal.endOffset,
                details = mapOf(
                    "enumType" to enumFacts.enumTypeName,
                    "missingCases" to missingCases.joinToString(", "),
                ),
            )
        )
    }
}

private object NumericSemanticRules {
    fun analyze(chain: Chain): List<ChainIssue> {
        val issues = mutableListOf<ChainIssue>()
        val previousIntervals = mutableListOf<ChainNumericInterval>()

        val intervalsInOrder = chain.numericIntervals
            .sortedBy { interval -> interval.startOffset }

        for (interval in intervalsInOrder) {
            val overlappingPrevious = previousIntervals.filter { previous ->
                overlaps(
                    left = previous,
                    right = interval,
                )
            }

            if (overlappingPrevious.isNotEmpty()) {
                val fullyCovered = overlappingPrevious.any { previous ->
                    contains(
                        container = previous,
                        candidate = interval,
                    )
                }

                val issueType = if (fullyCovered) {
                    ChainIssueType.UNREACHABLE_NUMERIC_CONDITION
                } else {
                    ChainIssueType.NUMERIC_OVERLAP
                }

                issues.add(
                    ChainIssue(
                        type = issueType,
                        chain = chain,
                        startOffset = interval.startOffset,
                        endOffset = interval.endOffset,
                    )
                )
            }

            previousIntervals.add(interval)
        }

        return issues
    }

    private fun overlaps(
        left: ChainNumericInterval,
        right: ChainNumericInterval,
    ): Boolean {
        return lowerLessThanUpper(
            lower = left.lower,
            lowerInclusive = left.lowerInclusive,
            upper = right.upper,
            upperInclusive = right.upperInclusive,
        ) &&
            lowerLessThanUpper(
                lower = right.lower,
                lowerInclusive = right.lowerInclusive,
                upper = left.upper,
                upperInclusive = left.upperInclusive,
            )
    }

    private fun contains(
        container: ChainNumericInterval,
        candidate: ChainNumericInterval,
    ): Boolean {
        return lowerCovers(
            containerLower = container.lower,
            containerLowerInclusive = container.lowerInclusive,
            candidateLower = candidate.lower,
            candidateLowerInclusive = candidate.lowerInclusive,
        ) &&
            upperCovers(
                containerUpper = container.upper,
                containerUpperInclusive = container.upperInclusive,
                candidateUpper = candidate.upper,
                candidateUpperInclusive = candidate.upperInclusive,
            )
    }

    private fun lowerLessThanUpper(
        lower: RangeBound?,
        lowerInclusive: Boolean,
        upper: RangeBound?,
        upperInclusive: Boolean,
    ): Boolean {
        if (lower == null) {
            return true
        }

        if (upper == null) {
            return true
        }

        return when {
            lower.value < upper.value -> true
            lower.value > upper.value -> false
            else -> lowerInclusive && upperInclusive
        }
    }

    private fun lowerCovers(
        containerLower: RangeBound?,
        containerLowerInclusive: Boolean,
        candidateLower: RangeBound?,
        candidateLowerInclusive: Boolean,
    ): Boolean {
        // Container starts from -infinity, so it covers any candidate lower bound.
        if (containerLower == null) {
            return true
        }

        // Candidate starts from -infinity, but container does not.
        if (candidateLower == null) {
            return false
        }

        return when {
            containerLower.value < candidateLower.value -> true
            containerLower.value > candidateLower.value -> false

            // Same lower bound.
            //
            // If candidate includes the boundary, container must include it too.
            // If candidate excludes the boundary, either inclusive or exclusive
            // container still covers candidate's actual value set.
            else -> containerLowerInclusive || !candidateLowerInclusive
        }
    }

    private fun upperCovers(
        containerUpper: RangeBound?,
        containerUpperInclusive: Boolean,
        candidateUpper: RangeBound?,
        candidateUpperInclusive: Boolean,
    ): Boolean {
        // Container goes to +infinity, so it covers any candidate upper bound.
        if (containerUpper == null) {
            return true
        }

        // Candidate goes to +infinity, but container does not.
        if (candidateUpper == null) {
            return false
        }

        return when {
            containerUpper.value > candidateUpper.value -> true
            containerUpper.value < candidateUpper.value -> false

            // Same upper bound.
            //
            // If candidate includes the boundary, container must include it too.
            // If candidate excludes the boundary, either inclusive or exclusive
            // container still covers candidate's actual value set.
            else -> containerUpperInclusive || !candidateUpperInclusive
        }
    }
}

private data class ChainIssueStage(
    val name: String,
    val rules: List<(Chain) -> List<ChainIssue>>,
) {
    fun analyze(chain: Chain): List<ChainIssue> {
        return rules.flatMap { rule ->
            rule(chain)
        }
    }
}

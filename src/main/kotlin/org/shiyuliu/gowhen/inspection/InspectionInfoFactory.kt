package org.shiyuliu.gowhen.inspection

import com.intellij.codeInspection.ProblemHighlightType
import org.shiyuliu.gowhen.chain.model.ChainIssue
import org.shiyuliu.gowhen.chain.model.ChainIssueType
import org.shiyuliu.gowhen.chain.model.MatcherType
import org.shiyuliu.gowhen.chain.constants.ScannerConstants

object InspectionInfoFactory {
    fun from(issue: ChainIssue): InspectionInfo {
        return when (issue.type) {
            ChainIssueType.ROOT_ONLY -> InspectionInfo(
                message = "go-when matcher root must be followed by at least one matcher and one terminal method",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.INVALID_ROOT_MODIFIER -> InspectionInfo(
                message = "${issue.displayCallName()} is not a valid go-when root modifier",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.ROOT_MODIFIER_NOT_ALLOWED -> InspectionInfo(
                message = "${issue.displayCallName()} is not valid for ${issue.displayRootMatcherType()}",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.DUPLICATE_ROOT_MODIFIER -> InspectionInfo(
                message = "duplicate go-when root modifier: ${issue.displayCallName()}",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.INCOMPLETE_MATCHER -> InspectionInfo(
                message = "go-when matcher condition must be followed by ${issue.displayAllowedActionMethods()}",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.INVALID_CONDITION_FOR_MATCHER_TYPE -> InspectionInfo(
                message = "${issue.displayCallName()} is not valid for ${issue.displayEffectiveMatcherType()}",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.INVALID_ACTION_FOR_MATCHER_TYPE -> InspectionInfo(
                message = "${issue.displayCallName()} is not valid for ${issue.displayEffectiveMatcherType()}",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.INVALID_TERMINAL_FOR_MATCHER_TYPE -> InspectionInfo(
                message = "${issue.displayCallName()} is not valid for ${issue.displayEffectiveMatcherType()}",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.MISSING_TERMINAL -> InspectionInfo(
                message = "go-when matcher chain must end with ${issue.displayAllowedTerminalMethods()}",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.DUPLICATE_TERMINAL -> InspectionInfo(
                message = "go-when matcher chain must have exactly one terminal method",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.TERMINAL_NOT_LAST -> InspectionInfo(
                message = "go-when terminal method must be the last call in the chain",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.NUMERIC_OVERLAP -> InspectionInfo(
                message = "go-when range overlaps with a previous range",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.UNREACHABLE_NUMERIC_CONDITION -> InspectionInfo(
                message = "go-when range is unreachable because it is fully covered by previous ranges",
                highlightType = ProblemHighlightType.GENERIC_ERROR,
            )

            ChainIssueType.MISSING_ENUM_CASES -> InspectionInfo(
                message = issue.enumMissingCasesMessage(),
                highlightType = ProblemHighlightType.WARNING,
            )
        }
    }

    private fun ChainIssue.displayCallName(): String {
        for (rootModifier in chain.rootModifiers) {
            if (issueRangeInsideTarget(rootModifier.nameStartOffset, rootModifier.nameEndOffset)) {
                return rootModifier.name
            }
        }

        for (matcher in chain.matchers) {
            val condition = matcher.condition
            if (issueRangeInsideTarget(condition.nameStartOffset, condition.nameEndOffset)) {
                return condition.name
            }

            val action = matcher.action
            if (action != null && issueRangeInsideTarget(action.nameStartOffset, action.nameEndOffset)) {
                return action.name
            }
        }

        for (terminal in chain.terminals) {
            if (issueRangeInsideTarget(terminal.nameStartOffset, terminal.nameEndOffset)) {
                return terminal.name
            }
        }

        return "This call"
    }

    private fun ChainIssue.displayAllowedActionMethods(): String {
        return when (effectiveMatcherType()) {
            MatcherType.FALLIBLE_MATCHER,
            MatcherType.FALLIBLE_ANY_MATCHER -> "Then, ThenErr, ThenDo, or ThenDoE"

            else -> "Then or ThenDo"
        }
    }

    private fun ChainIssue.displayAllowedTerminalMethods(): String {
        return when (effectiveMatcherType()) {
            MatcherType.FALLIBLE_MATCHER,
            MatcherType.FALLIBLE_ANY_MATCHER -> "Else, ElseErr, ElseDo, ElseDoE, or Exhaustive"

            else -> "Else, ElseDo, or Exhaustive"
        }
    }

    private fun ChainIssue.displayRootMatcherType(): String {
        return displayMatcherType(chain.root.type)
    }

    private fun ChainIssue.displayEffectiveMatcherType(): String {
        return displayMatcherType(effectiveMatcherType())
    }

    private fun ChainIssue.effectiveMatcherType(): MatcherType {
        return ScannerConstants.effectiveMatcherType(
            rootMatcherType = chain.root.type,
            withErr = chain.rootModifiers.any { rootModifier ->
                rootModifier.name == "WithErr"
            },
        )
    }

    private fun displayMatcherType(matcherType: MatcherType): String {
        return when (matcherType) {
            MatcherType.MATCHER -> "Matcher"
            MatcherType.ANY_MATCHER -> "AnyMatcher"
            MatcherType.ERR_MATCHER -> "ErrMatcher"
            MatcherType.FALLIBLE_MATCHER -> "FallibleMatcher"
            MatcherType.FALLIBLE_ANY_MATCHER -> "FallibleAnyMatcher"
        }
    }

    private fun ChainIssue.issueRangeInsideTarget(
        targetStart: Int,
        targetEnd: Int,
    ): Boolean {
        return startOffset >= targetStart && endOffset <= targetEnd
    }

    private fun ChainIssue.enumMissingCasesMessage(): String {
        val enumType = details["enumType"]
        val missingCases = details["missingCases"]

        if (!enumType.isNullOrBlank() && !missingCases.isNullOrBlank()) {
            return "go-when enum exhaustive check failed for $enumType, missing cases: $missingCases"
        }

        if (!missingCases.isNullOrBlank()) {
            return "go-when enum exhaustive check failed, missing cases: $missingCases"
        }

        return "go-when enum exhaustive check failed: some enum cases are not covered"
    }
}

package org.shiyuliu.gowhen.inspection

import com.intellij.codeInspection.LocalQuickFix
import org.shiyuliu.gowhen.chain.ChainIssue
import org.shiyuliu.gowhen.chain.ChainIssueType
import org.shiyuliu.gowhen.quickfix.AddExhaustiveQuickFix

object InspectionQuickFixFactory {
    fun from(issue: ChainIssue): Array<LocalQuickFix> {
        return when (issue.type) {
            ChainIssueType.MISSING_TERMINAL -> arrayOf(
                AddExhaustiveQuickFix(issue),
            )

            ChainIssueType.ROOT_ONLY,
            ChainIssueType.INVALID_ROOT_MODIFIER,
            ChainIssueType.ROOT_MODIFIER_NOT_ALLOWED,
            ChainIssueType.DUPLICATE_ROOT_MODIFIER,
            ChainIssueType.INCOMPLETE_MATCHER,
            ChainIssueType.INVALID_CONDITION_FOR_MATCHER_TYPE,
            ChainIssueType.INVALID_ACTION_FOR_MATCHER_TYPE,
            ChainIssueType.INVALID_TERMINAL_FOR_MATCHER_TYPE,
            ChainIssueType.DUPLICATE_TERMINAL,
            ChainIssueType.TERMINAL_NOT_LAST,
            ChainIssueType.NUMERIC_OVERLAP,
            ChainIssueType.UNREACHABLE_NUMERIC_CONDITION,
            ChainIssueType.MISSING_ENUM_CASE -> emptyArray()
        }
    }
}

package org.shiyuliu.gowhen.chain.model

enum class ChainIssueType {
    // A. Root / modifier structure
    ROOT_ONLY,
    INVALID_ROOT_MODIFIER,
    ROOT_MODIFIER_NOT_ALLOWED,
    DUPLICATE_ROOT_MODIFIER,
    // B. Matcher structure
    INCOMPLETE_MATCHER,
    // C. Matcher type compatibility
    INVALID_CONDITION_FOR_MATCHER_TYPE,
    INVALID_ACTION_FOR_MATCHER_TYPE,
    INVALID_TERMINAL_FOR_MATCHER_TYPE,
    // D. Terminal structure
    MISSING_TERMINAL,
    DUPLICATE_TERMINAL,
    TERMINAL_NOT_LAST,
    // E. Range semantic
    NUMERIC_OVERLAP,
    UNREACHABLE_NUMERIC_CONDITION,
    // F. Enum exhaustive
    MISSING_ENUM_CASES,
}

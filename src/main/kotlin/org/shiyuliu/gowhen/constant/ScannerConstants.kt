package org.shiyuliu.gowhen.constant

import org.shiyuliu.gowhen.chain.MatcherType

object ScannerConstants {
    val MATCHER_ROOT_METHODS = setOf(
        "Match",
        "MatchAs",
    )

    val ANY_MATCHER_ROOT_METHODS = setOf(
        "MatchAny",
        "MatchAnyAs",
    )

    val ERR_MATCHER_ROOT_METHODS = setOf(
        "Err",
    )

    val ROOT_METHODS =
        MATCHER_ROOT_METHODS +
                ANY_MATCHER_ROOT_METHODS +
                ERR_MATCHER_ROOT_METHODS

    val ROOT_MODIFIER_METHODS = setOf(
        "WithErr",
    )

    private val ROOT_METHOD_TO_MATCHER_TYPE = mapOf(
        "Match" to MatcherType.MATCHER,
        "MatchAs" to MatcherType.MATCHER,

        "MatchAny" to MatcherType.ANY_MATCHER,
        "MatchAnyAs" to MatcherType.ANY_MATCHER,

        "Err" to MatcherType.ERR_MATCHER,
    )

    val MATCHER_CONDITION_METHODS = setOf(
        "Case",
        "Range",
        "When",
        "Pattern",
    )

    val ANY_MATCHER_CONDITION_METHODS = setOf(
        "Range",
        "When",
        "Pattern",
    )

    val ERR_MATCHER_CONDITION_METHODS = setOf(
        "Nil",
        "NotNil",
        "Is",
        "As",
        "Contains",
        "When",
        "Pattern",
    )

    private val CONDITION_METHODS_BY_MATCHER_TYPE = mapOf(
        MatcherType.MATCHER to MATCHER_CONDITION_METHODS,
        MatcherType.ANY_MATCHER to ANY_MATCHER_CONDITION_METHODS,
        MatcherType.ERR_MATCHER to ERR_MATCHER_CONDITION_METHODS,

        MatcherType.FALLIBLE_MATCHER to MATCHER_CONDITION_METHODS,
        MatcherType.FALLIBLE_ANY_MATCHER to ANY_MATCHER_CONDITION_METHODS,
    )

    val CONDITION_METHODS =
        MATCHER_CONDITION_METHODS +
                ANY_MATCHER_CONDITION_METHODS +
                ERR_MATCHER_CONDITION_METHODS

    val NORMAL_ACTION_METHODS = setOf(
        "Then",
        "ThenDo",
    )

    val FALLIBLE_ACTION_METHODS = setOf(
        "Then",
        "ThenErr",
        "ThenDo",
        "ThenDoE",
    )

    private val ACTION_METHODS_BY_MATCHER_TYPE = mapOf(
        MatcherType.MATCHER to NORMAL_ACTION_METHODS,
        MatcherType.ANY_MATCHER to NORMAL_ACTION_METHODS,
        MatcherType.ERR_MATCHER to NORMAL_ACTION_METHODS,

        MatcherType.FALLIBLE_MATCHER to FALLIBLE_ACTION_METHODS,
        MatcherType.FALLIBLE_ANY_MATCHER to FALLIBLE_ACTION_METHODS,
    )

    val ACTION_METHODS =
        NORMAL_ACTION_METHODS +
                FALLIBLE_ACTION_METHODS

    val NORMAL_TERMINAL_METHODS = setOf(
        "Else",
        "ElseDo",
        "Exhaustive",
    )

    val FALLIBLE_TERMINAL_METHODS = setOf(
        "Else",
        "ElseErr",
        "ElseDo",
        "ElseDoE",
        "Exhaustive",
    )

    private val TERMINAL_METHODS_BY_MATCHER_TYPE = mapOf(
        MatcherType.MATCHER to NORMAL_TERMINAL_METHODS,
        MatcherType.ANY_MATCHER to NORMAL_TERMINAL_METHODS,
        MatcherType.ERR_MATCHER to NORMAL_TERMINAL_METHODS,

        MatcherType.FALLIBLE_MATCHER to FALLIBLE_TERMINAL_METHODS,
        MatcherType.FALLIBLE_ANY_MATCHER to FALLIBLE_TERMINAL_METHODS,
    )

    val TERMINAL_METHODS =
        NORMAL_TERMINAL_METHODS +
                FALLIBLE_TERMINAL_METHODS

    val ALL_CHAIN_STEP_METHODS =
        CONDITION_METHODS +
                ACTION_METHODS +
                TERMINAL_METHODS

    fun matcherTypeOfRootMethod(rootMethodName: String): MatcherType? {
        return ROOT_METHOD_TO_MATCHER_TYPE[rootMethodName]
    }

    fun effectiveMatcherType(
        rootMatcherType: MatcherType,
        withErr: Boolean,
    ): MatcherType {
        if (!withErr) {
            return rootMatcherType
        }

        return when (rootMatcherType) {
            MatcherType.MATCHER -> MatcherType.FALLIBLE_MATCHER
            MatcherType.ANY_MATCHER -> MatcherType.FALLIBLE_ANY_MATCHER

            // 当前不支持 Err(...).WithErr()
            // 如果 parser 不解析 Err 后面的 WithErr，这里不会走到。
            // 这里先保守返回原类型，避免 analyzer 误判成 fallible err。
            MatcherType.ERR_MATCHER -> MatcherType.ERR_MATCHER

            MatcherType.FALLIBLE_MATCHER -> MatcherType.FALLIBLE_MATCHER
            MatcherType.FALLIBLE_ANY_MATCHER -> MatcherType.FALLIBLE_ANY_MATCHER
        }
    }

    fun conditionMethodsOf(matcherType: MatcherType): Set<String> {
        return CONDITION_METHODS_BY_MATCHER_TYPE[matcherType].orEmpty()
    }

    fun actionMethodsOf(matcherType: MatcherType): Set<String> {
        return ACTION_METHODS_BY_MATCHER_TYPE[matcherType].orEmpty()
    }

    fun terminalMethodsOf(matcherType: MatcherType): Set<String> {
        return TERMINAL_METHODS_BY_MATCHER_TYPE[matcherType].orEmpty()
    }

    fun isRootMethod(methodName: String): Boolean {
        return methodName in ROOT_METHODS
    }

    fun isRootModifierMethod(methodName: String): Boolean {
        return methodName in ROOT_MODIFIER_METHODS
    }

    fun isRootModifierAllowed(
        rootMatcherType: MatcherType,
        modifierMethodName: String,
    ): Boolean {
        if (modifierMethodName != "WithErr") {
            return false
        }

        return rootMatcherType == MatcherType.MATCHER ||
                rootMatcherType == MatcherType.ANY_MATCHER
    }

    fun isConditionMethod(methodName: String): Boolean {
        return methodName in CONDITION_METHODS
    }

    fun isActionMethod(methodName: String): Boolean {
        return methodName in ACTION_METHODS
    }

    fun isTerminalMethod(methodName: String): Boolean {
        return methodName in TERMINAL_METHODS
    }

    fun isConditionMethodAllowed(
        matcherType: MatcherType,
        conditionMethodName: String,
    ): Boolean {
        return conditionMethodName in conditionMethodsOf(matcherType)
    }

    fun isActionMethodAllowed(
        matcherType: MatcherType,
        actionMethodName: String,
    ): Boolean {
        return actionMethodName in actionMethodsOf(matcherType)
    }

    fun isTerminalMethodAllowed(
        matcherType: MatcherType,
        terminalMethodName: String,
    ): Boolean {
        return terminalMethodName in terminalMethodsOf(matcherType)
    }
}
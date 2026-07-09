package org.shiyuliu.gowhen.chain

data class ChainNumericInterval(
    val sourceKind: NumericIntervalSourceKind,
    val matcherIndex: Int,
    val condition: MatcherCondition,
    val action: MatcherAction?,
    val lower: RangeBound?,
    val upper: RangeBound?,
    val lowerInclusive: Boolean,
    val upperInclusive: Boolean,
    val startOffset: Int,
    val endOffset: Int,
)
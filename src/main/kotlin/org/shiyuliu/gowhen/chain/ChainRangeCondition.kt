package org.shiyuliu.gowhen.chain

data class ChainRangeCondition(
    val matcherIndex: Int,
    val condition: MatcherCondition,
    val action: MatcherAction?,
    val intervals: List<ChainNumericInterval>,
    val startOffset: Int,
    val endOffset: Int,
)
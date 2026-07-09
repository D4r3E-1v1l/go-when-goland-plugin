package org.shiyuliu.gowhen.chain

data class ChainMatcher(
    val condition: MatcherCondition,
    val action: MatcherAction?,
    val startOffset: Int,
    val endOffset: Int,
)

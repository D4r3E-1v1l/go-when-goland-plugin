package org.shiyuliu.gowhen.chain.model

data class ChainMatcher(
    val condition: MatcherCondition,
    val action: MatcherAction?,
    val startOffset: Int,
    val endOffset: Int,
)
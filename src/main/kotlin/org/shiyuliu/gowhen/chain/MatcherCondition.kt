package org.shiyuliu.gowhen.chain

data class MatcherCondition(
    val name: String,
    val dotOffset: Int,
    val nameStartOffset: Int,
    val nameEndOffset: Int,
    val openParenOffset: Int,
    val closeParenOffset: Int,
    val argsStartOffset: Int,
    val argsEndOffset: Int,
    val startOffset: Int,
    val endOffset: Int,
)
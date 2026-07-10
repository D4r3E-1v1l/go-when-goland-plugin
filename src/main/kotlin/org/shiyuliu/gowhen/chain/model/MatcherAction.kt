package org.shiyuliu.gowhen.chain.model

data class MatcherAction (
    val name: String,
    val dotOffset: Int,
    val nameStartOffset: Int,
    val nameEndOffset: Int,
    val startOffset: Int,
    val endOffset: Int,
)
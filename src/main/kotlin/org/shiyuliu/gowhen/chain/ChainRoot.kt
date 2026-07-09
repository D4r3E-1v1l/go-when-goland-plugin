package org.shiyuliu.gowhen.chain

data class ChainRoot(
    val alias: String,
    val name: String,
    val type: MatcherType,
    val nameStartOffset: Int,
    val nameEndOffset: Int,
    val startOffset: Int,
    val endOffset: Int,
)

package org.shiyuliu.gowhen.chain

data class ChainTerminal(
    val name: String,
    val startOffset: Int,
    val nameStartOffset: Int,
    val nameEndOffset: Int,
    val endOffset: Int,
)
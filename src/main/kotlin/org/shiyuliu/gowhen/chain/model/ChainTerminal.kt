package org.shiyuliu.gowhen.chain.model

data class ChainTerminal(
    val name: String,
    val startOffset: Int,
    val nameStartOffset: Int,
    val nameEndOffset: Int,
    val endOffset: Int,
)
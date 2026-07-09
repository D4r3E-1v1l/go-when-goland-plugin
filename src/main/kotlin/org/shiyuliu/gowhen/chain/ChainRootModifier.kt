package org.shiyuliu.gowhen.chain

data class ChainRootModifier(
    val name: String,
    val dotOffset: Int,
    val nameStartOffset: Int,
    val nameEndOffset: Int,
    val startOffset: Int,
    val endOffset: Int,
)
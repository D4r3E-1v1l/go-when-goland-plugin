package org.shiyuliu.gowhen.chain.model

data class ChainEnumCase(
    val name: String,
    val valueText: String?,
    val startOffset: Int,
    val endOffset: Int,
)
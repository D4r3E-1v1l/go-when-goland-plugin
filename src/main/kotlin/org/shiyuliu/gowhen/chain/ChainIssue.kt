package org.shiyuliu.gowhen.chain

data class ChainIssue(
    val type: ChainIssueType,
    val chain: Chain,
    val startOffset: Int,
    val endOffset: Int,
    val insertOffset: Int,
)
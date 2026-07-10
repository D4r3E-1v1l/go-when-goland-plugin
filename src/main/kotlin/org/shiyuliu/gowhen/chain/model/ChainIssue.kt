package org.shiyuliu.gowhen.chain.model

data class ChainIssue(
    val type: ChainIssueType,
    val chain: Chain,
    val startOffset: Int,
    val endOffset: Int,
    val details: Map<String, String> = emptyMap(),
)
package org.shiyuliu.gowhen.chain.model

data class ChainEnumFacts(
    val enumTypeName: String,
    val enumKind: ChainEnumKind,
    val declaredCases: List<ChainEnumCase>,
    val coveredCases: List<ChainEnumCase>,
)
package org.shiyuliu.gowhen.chain.model

import org.shiyuliu.gowhen.chain.constants.ScannerConstants

data class Chain(
    val root: ChainRoot,
    val rootModifiers: List<ChainRootModifier>,
    val matchers: List<ChainMatcher>,
    val terminals: List<ChainTerminal>,

    // Direct semantic index, not an issue result.
    val numericIntervals: List<ChainNumericInterval>,

    // Direct enum facts for this chain, not missing-case result.
    val enumFacts: ChainEnumFacts?,

    val startOffset: Int,
    val endOffset: Int,
) {
    val name: String
        get() = root.name

    val lastCall: ChainMatcher?
        get() = matchers.lastOrNull()

    fun hasTerminal(): Boolean {
        val lengthValid = this.terminals.size == 1
        val first = this.terminals.firstOrNull()

        return lengthValid && first?.name.let { name -> ScannerConstants.TERMINAL_METHODS.contains(name) }
    }
}
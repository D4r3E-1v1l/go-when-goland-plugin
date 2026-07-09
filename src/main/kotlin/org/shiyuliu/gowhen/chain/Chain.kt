package org.shiyuliu.gowhen.chain

import org.shiyuliu.gowhen.constant.ScannerConstants.TERMINAL_METHODS

data class Chain(
    val root: ChainRoot,
    val rootModifiers: List<ChainRootModifier>,
    val matchers: List<ChainMatcher>,
    val terminals: List<ChainTerminal>,
    val numericIntervals: List<ChainNumericInterval>,
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

        return lengthValid && first?.name.let { name -> TERMINAL_METHODS.contains(name) }
    }
}


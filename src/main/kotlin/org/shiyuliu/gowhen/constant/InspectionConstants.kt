package org.shiyuliu.gowhen.constant

object InspectionConstants {
    const val SHORT_NAME = "GoWhenMissingTerminal"

    const val GROUP_NAME = "Go When"

    const val DISPLAY_NAME =
        "go-when matcher chain must end with a terminal method"

    const val MISSING_TERMINAL_MESSAGE =
        "go-when matcher chain has no terminal method; add Else, ElseDo, or Exhaustive"

    const val MISSING_TERMINAL_TOOLTIP =
        "go-when matcher chain must end with Else(...), ElseDo(...), or Exhaustive()"
}
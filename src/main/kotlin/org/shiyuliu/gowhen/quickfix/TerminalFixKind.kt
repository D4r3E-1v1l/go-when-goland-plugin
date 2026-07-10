package org.shiyuliu.gowhen.quickfix

enum class TerminalFixKind(
    val familyName: String,
    val singleLineText: String,
    val multiLineCallText: String,
) {
    EXHAUSTIVE(
        familyName = "Add .Exhaustive()",
        singleLineText = ".Exhaustive()",
        multiLineCallText = "Exhaustive()",
    ),

    ELSE(
        familyName = "Add .Else(...)",
        singleLineText = ".Else(/* TODO */)",
        multiLineCallText = "Else(/* TODO */)",
    ),

    ELSE_DO(
        familyName = "Add .ElseDo(...)",
        singleLineText = ".ElseDo(/* TODO */)",
        multiLineCallText = "ElseDo(/* TODO */)",
    ),
}
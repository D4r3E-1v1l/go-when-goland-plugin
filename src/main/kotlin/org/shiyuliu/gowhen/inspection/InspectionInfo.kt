package org.shiyuliu.gowhen.inspection

import com.intellij.codeInspection.ProblemHighlightType

data class InspectionInfo(
    val message: String,
    val highlightType: ProblemHighlightType,
)

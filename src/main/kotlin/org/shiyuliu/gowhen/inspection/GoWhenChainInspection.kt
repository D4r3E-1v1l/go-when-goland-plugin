package org.shiyuliu.gowhen.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.shiyuliu.gowhen.chain.ChainScanner

class GoWhenChainInspection : LocalInspectionTool() {
    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor>? {
        if (!file.name.endsWith(".go")) {
            return null
        }

        val issues = ChainScanner.scan(file)
        if (issues.isEmpty()) {
            return null
        }

        return issues.map { issue ->
            val info = InspectionInfoFactory.from(issue)
            val quickFixes = InspectionQuickFixFactory.from(issue)

            manager.createProblemDescriptor(
                file,
                TextRange(issue.startOffset, issue.endOffset),
                info.message,
                info.highlightType,
                isOnTheFly,
                *quickFixes,
            )
        }.toTypedArray()
    }
}

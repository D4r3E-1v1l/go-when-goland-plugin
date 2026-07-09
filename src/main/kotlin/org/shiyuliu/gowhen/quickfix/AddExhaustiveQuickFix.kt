package org.shiyuliu.gowhen.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.shiyuliu.gowhen.chain.ChainIssue

class AddExhaustiveQuickFix(
    private val issue: ChainIssue,
) : LocalQuickFix {
    override fun getFamilyName(): String {
        return "Add .Exhaustive()"
    }

    override fun applyFix(
        project: Project,
        descriptor: ProblemDescriptor,
    ) {
        val file = descriptor.psiElement.containingFile ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val insertion = FixAppender.buildExhaustiveInsertion(
            text = document.text,
            issue = issue,
        )

        WriteCommandAction.runWriteCommandAction(
            project,
            familyName,
            null,
            Runnable {
                document.insertString(
                    insertion.offset,
                    insertion.text,
                )

                PsiDocumentManager.getInstance(project)
                    .commitDocument(document)
            },
            file,
        )
    }
}

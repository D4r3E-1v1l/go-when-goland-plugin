package org.shiyuliu.gowhen.imports

import com.goide.psi.GoFile
import com.goide.psi.GoImportSpec
import org.shiyuliu.gowhen.imports.constants.ImportConstants
import org.shiyuliu.gowhen.imports.model.ImportedPackage

object ImportsParser {
    fun parse(goFile: GoFile): ImportedPackage? {
        return goFile.imports
            .asSequence()
            .mapNotNull { importSpec ->
                parseImportSpec(importSpec)
            }
            .toList().firstOrNull()
    }

    private fun parseImportSpec(importSpec: GoImportSpec): ImportedPackage? {
        val path = importSpec.path

        if (path != ImportConstants.WHEN_IMPORT_PATH) {
            return null
        }

        if (importSpec.isForSideEffects) {
            return null
        }

        if (importSpec.isDot) {
            return null
        }

        val alias = importSpec.alias
            ?: ImportConstants.DEFAULT_WHEN_ALIAS

        if (alias == ImportConstants.BLANK_IMPORT_ALIAS) {
            return null
        }

        if (alias == ImportConstants.DOT_IMPORT_ALIAS) {
            return null
        }

        return ImportedPackage(
            alias = alias,
            path = path,
        )
    }
}


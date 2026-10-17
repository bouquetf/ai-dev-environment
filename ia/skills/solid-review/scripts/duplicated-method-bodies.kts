// Duplicated Java method bodies (CRBPM).
//
// IntelliJ's "Locate duplicates" analysis is not exposed over MCP, so duplication
// is detected here with a normalized-body fingerprint: comments and whitespace are
// stripped, then bodies of at least `minStatements` statements are compared.
// Only exact structural duplicates are reported; near-duplicates (renamed
// variables, reordered statements) still require the manual review checklist.
//
// Before running: replace the `comparedFiles` entries with the project-relative
// paths of the locally modified file set, plus untouched siblings from the same
// packages as read-only comparison targets. Then pass the whole content to
// `idea-run_inspection_kts` with `contextPath` set to the reviewed file, once per
// locally modified file.

import org.intellij.lang.annotations.Language
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod

@Language("HTML")
val htmlDescription = """
    <html>
    <body>
        Reports method bodies duplicated inside the reviewed file or across the
        compared files.
    </body>
    </html>
""".trimIndent()

val minStatements = 4

val comparedFiles = listOf(
    "crbpm/<module>/src/main/java/<package>/<FirstReviewedFile>.java",
    "crbpm/<module>/src/main/java/<package>/<SecondReviewedFile>.java",
)

fun fingerprintOf(method: PsiMethod): String? {
    val body = method.body ?: return null
    if (body.statements.size < minStatements) {
        return null
    }
    return body.statements
        .joinToString(" ") { statement -> statement.text }
        .replace(Regex("//[^\n]*"), " ")
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

val duplicatedMethodBodies = localInspection { psiFile, inspection ->
    val externalFingerprints = HashMap<String, String>()
    comparedFiles.forEach { relativePath: String ->
        val comparedFile: PsiFile = inspection.findPsiFileByRelativeToProjectPath(relativePath) ?: return@forEach
        if (comparedFile == psiFile) {
            return@forEach
        }
        comparedFile.descendantsOfType<PsiMethod>().forEach { method: PsiMethod ->
            val fingerprint = fingerprintOf(method) ?: return@forEach
            externalFingerprints.putIfAbsent(fingerprint, "$relativePath#${method.name}")
        }
    }

    val alreadySeenInReviewedFile = HashMap<String, PsiMethod>()
    psiFile.descendantsOfType<PsiMethod>().forEach { method: PsiMethod ->
        val fingerprint = fingerprintOf(method) ?: return@forEach
        val methodAnchor = method.nameIdentifier ?: method

        val previousOccurrence = alreadySeenInReviewedFile[fingerprint]
        if (previousOccurrence != null) {
            inspection.registerProblem(
                methodAnchor,
                "Body of '${method.name}' duplicates '${previousOccurrence.name}' in the same file: extract the shared logic"
            )
        } else {
            alreadySeenInReviewedFile[fingerprint] = method
        }

        val externalOwner = externalFingerprints[fingerprint]
        if (externalOwner != null) {
            inspection.registerProblem(
                methodAnchor,
                "Body of '${method.name}' duplicates $externalOwner: extract the shared logic into a common collaborator"
            )
        }
    }
}

listOf(
    InspectionKts(
        id = "CrbpmDuplicatedMethodBodies",
        localTool = duplicatedMethodBodies,
        name = "CRBPM duplicated method bodies",
        htmlDescription = htmlDescription,
        level = HighlightDisplayLevel.WARNING,
    )
)

// Structural SOLID smells for Java sources (CRBPM).
//
// Not auto-activated by the IDE on purpose: the file extension is `.kts`, not
// `.inspection.kts`. Pass the whole content of this file to the JetBrains MCP
// tool `idea-run_inspection_kts` with `contextPath` set to the reviewed file.
//
// Thresholds are heuristics meant to surface candidates for a human decision,
// not hard rules.

import org.intellij.lang.annotations.Language
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

@Language("HTML")
val htmlDescription = """
    <html>
    <body>
        Structural SOLID smells: classes with too many collaborators, oversized public
        surfaces, long methods and wide parameter lists.
    </body>
    </html>
""".trimIndent()

val maxMethodStatements = 25
val maxParameters = 5
val maxCollaborators = 6
val maxPublicMethods = 15

val solidStructureSmells = localInspection { psiFile, inspection ->
    psiFile.descendantsOfType<PsiClass>().forEach { javaClass: PsiClass ->
        if (javaClass.isInterface) {
            return@forEach
        }
        val classAnchor = javaClass.nameIdentifier ?: javaClass

        val collaborators = javaClass.fields.count { field: PsiField ->
            !field.hasModifierProperty("static")
        }
        if (collaborators > maxCollaborators) {
            inspection.registerProblem(
                classAnchor,
                "Class has $collaborators instance fields (> $maxCollaborators): likely more than one responsibility (SRP)"
            )
        }

        val publicMethods = javaClass.methods.count { method: PsiMethod ->
            method.hasModifierProperty("public") && !method.isConstructor
        }
        if (publicMethods > maxPublicMethods) {
            inspection.registerProblem(
                classAnchor,
                "Class exposes $publicMethods public methods (> $maxPublicMethods): consider splitting the interface (SRP/ISP)"
            )
        }

        javaClass.methods.forEach { method: PsiMethod ->
            val methodAnchor = method.nameIdentifier ?: method

            val statements = method.body?.statements?.size ?: 0
            if (statements > maxMethodStatements) {
                inspection.registerProblem(
                    methodAnchor,
                    "Method '${method.name}' has $statements top-level statements (> $maxMethodStatements): extract intention-revealing methods"
                )
            }

            val parameters = method.parameterList.parametersCount
            if (parameters > maxParameters) {
                inspection.registerProblem(
                    methodAnchor,
                    "Method '${method.name}' takes $parameters parameters (> $maxParameters): introduce a parameter object"
                )
            }
        }
    }
}

listOf(
    InspectionKts(
        id = "CrbpmSolidStructureSmells",
        localTool = solidStructureSmells,
        name = "CRBPM SOLID structure smells",
        htmlDescription = htmlDescription,
        level = HighlightDisplayLevel.WARNING,
    )
)

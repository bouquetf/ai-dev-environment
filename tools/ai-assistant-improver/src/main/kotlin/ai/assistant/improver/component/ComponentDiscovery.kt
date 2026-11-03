package ai.assistant.improver.component

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object ComponentDiscovery {
    private val skillRoots = listOf(
        ".cursor/skills",
        ".agents/skills",
        ".claude/skills",
        ".opencode/skills",
        ".junie/skills",
        "ia/skills",
    )

    private val agentRoots = listOf(
        ".cursor/agents",
        ".claude/agents",
        ".opencode/agents",
        ".junie/agents",
    )

    private val ruleRoots = listOf(
        ".cursor/rules",
        ".claude/rules",
        ".opencode/rules",
        ".junie/rules",
    )

    fun discover(root: Path): List<ComponentId> {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        val skills = skillRoots.flatMap { relative -> skillDirectories(root, resolve(root, relative)) }
        val agents = agentRoots.flatMap { relative -> filesUnder(root, resolve(root, relative), ::isMarkdown) }
        val rules = ruleRoots.flatMap { relative -> filesUnder(root, resolve(root, relative), ::isRuleFile) } +
            guideRules(root)
        return listOf(
            skills.map { ComponentId(ComponentKind.SKILL, it) },
            agents.map { ComponentId(ComponentKind.AGENT, it) },
            rules.map { ComponentId(ComponentKind.RULE, it) },
        ).flatMap { paths -> paths.sortedBy { it.relativePath } }
    }

    private fun skillDirectories(root: Path, directory: Path): List<String> {
        val found = linkedSetOf<String>()
        walk(directory) { file ->
            if (file.fileName?.toString() == "SKILL.md") {
                found += relative(root, file.parent)
            }
        }
        return found.toList()
    }

    private fun filesUnder(root: Path, directory: Path, include: (String) -> Boolean): List<String> {
        val found = linkedSetOf<String>()
        walk(directory) { file ->
            val name = file.fileName?.toString() ?: return@walk
            if (include(name)) found += relative(root, file)
        }
        return found.toList()
    }

    private fun guideRules(root: Path): List<String> {
        val found = mutableListOf<String>()
        val claude = root.resolve("CLAUDE.md")
        if (isRegularFile(claude)) found += "CLAUDE.md"
        val guidelines = resolve(root, ".junie/guidelines.md")
        if (isRegularFile(guidelines)) found += ".junie/guidelines.md"
        return found
    }

    private fun walk(directory: Path, visitor: (Path) -> Unit) {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return
        Files.walkFileTree(
            directory,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
                    if (attrs.isSymbolicLink) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (attrs.isRegularFile) visitor(file)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                    FileVisitResult.CONTINUE
            },
        )
    }

    private fun isMarkdown(name: String): Boolean = name.endsWith(".md")

    private fun isRuleFile(name: String): Boolean = name.endsWith(".md") || name.endsWith(".mdc")

    private fun isRegularFile(path: Path): Boolean = Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)

    private fun resolve(root: Path, relative: String): Path =
        relative.split('/').fold(root) { parent, name -> parent.resolve(name) }

    private fun relative(root: Path, path: Path): String =
        root.relativize(path).joinToString("/")
}

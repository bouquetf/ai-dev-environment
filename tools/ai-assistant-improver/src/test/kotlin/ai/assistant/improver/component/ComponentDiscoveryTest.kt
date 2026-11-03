package ai.assistant.improver.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ComponentDiscoveryTest {
    @Test
    fun `fixture covers every root, both guides, and homonymous skills`(@TempDir tempDir: Path) {
        listOf(
            ".cursor/skills/review/SKILL.md",
            ".agents/skills/alpha/SKILL.md",
            ".claude/skills/beta/SKILL.md",
            ".opencode/skills/gamma/SKILL.md",
            ".junie/skills/delta/SKILL.md",
            "ia/skills/review/SKILL.md",
            ".cursor/agents/reviewer.md",
            ".claude/agents/team/reviewer.md",
            ".opencode/agents/helper.md",
            ".junie/agents/guide.md",
            ".cursor/rules/kotlin/style.mdc",
            ".claude/rules/notes.md",
            ".opencode/rules/extra.mdc",
            ".junie/rules/local.md",
            ".junie/guidelines.md",
            "README.md",
            "SKILL.md",
            ".cursor/skills/ignored/skill.md",
            ".cursor/agents/notes.mdc",
            ".cursor/rules/keep.txt",
            "docs/CLAUDE.md",
            "ia/agents/stray.md",
            ".agents/agents/stray.md",
        ).forEach { write(tempDir, it) }
        write(tempDir, "CLAUDE.md", "guide")

        assertEquals(expectedFixture(), ComponentDiscovery.discover(tempDir))
    }

    @Test
    fun `a skill root and a nested child are both skills`(@TempDir tempDir: Path) {
        write(tempDir, ".cursor/skills/SKILL.md")
        write(tempDir, ".cursor/skills/outer/SKILL.md")
        write(tempDir, ".cursor/skills/outer/inner/SKILL.md")

        assertEquals(
            listOf(
                ComponentId(ComponentKind.SKILL, ".cursor/skills"),
                ComponentId(ComponentKind.SKILL, ".cursor/skills/outer"),
                ComponentId(ComponentKind.SKILL, ".cursor/skills/outer/inner"),
            ),
            ComponentDiscovery.discover(tempDir),
        )
    }

    @Test
    fun `a file placed where a skill root should be is skipped`(@TempDir tempDir: Path) {
        val cursor = Files.createDirectories(tempDir.resolve(".cursor"))
        Files.writeString(cursor.resolve("skills"), "x")

        assertTrue(ComponentDiscovery.discover(tempDir).isEmpty())
    }

    private fun expectedFixture(): List<ComponentId> = listOf(
        ComponentKind.SKILL to ".agents/skills/alpha",
        ComponentKind.SKILL to ".claude/skills/beta",
        ComponentKind.SKILL to ".cursor/skills/review",
        ComponentKind.SKILL to ".junie/skills/delta",
        ComponentKind.SKILL to ".opencode/skills/gamma",
        ComponentKind.SKILL to "ia/skills/review",
        ComponentKind.AGENT to ".claude/agents/team/reviewer.md",
        ComponentKind.AGENT to ".cursor/agents/reviewer.md",
        ComponentKind.AGENT to ".junie/agents/guide.md",
        ComponentKind.AGENT to ".opencode/agents/helper.md",
        ComponentKind.RULE to ".claude/rules/notes.md",
        ComponentKind.RULE to ".cursor/rules/kotlin/style.mdc",
        ComponentKind.RULE to ".junie/guidelines.md",
        ComponentKind.RULE to ".junie/rules/local.md",
        ComponentKind.RULE to ".opencode/rules/extra.mdc",
        ComponentKind.RULE to "CLAUDE.md",
    ).map { (kind, path) -> ComponentId(kind, path) }

    private fun write(root: Path, relative: String, content: String = "x") {
        val path = relative.split('/').fold(root) { parent, name -> parent.resolve(name) }
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
    }
}

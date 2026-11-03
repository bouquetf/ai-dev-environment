package ai.assistant.improver.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ComponentSelectionTest {
    @Test
    fun `two skills with the same folder name keep distinct ids`(@TempDir tempDir: Path) {
        write(tempDir, ".cursor/skills/review/SKILL.md")
        write(tempDir, "ia/skills/review/SKILL.md")

        val discovered = ComponentDiscovery.discover(tempDir)

        assertEquals(
            listOf(
                ComponentId(ComponentKind.SKILL, ".cursor/skills/review"),
                ComponentId(ComponentKind.SKILL, "ia/skills/review"),
            ),
            discovered,
        )
        assertNotEquals(discovered[0], discovered[1])
        assertTrue(discovered.none { it.relativePath.endsWith("SKILL.md") })
    }

    @Test
    fun `first discovery selects every component`(@TempDir tempDir: Path) {
        write(tempDir, "ia/skills/review/SKILL.md")
        val selection = ComponentSelection()

        val rows = selection.components("repo", ComponentDiscovery.discover(tempDir))

        assertEquals(listOf(DiscoveredComponent(ComponentId(ComponentKind.SKILL, "ia/skills/review"), true)), rows)
        assertTrue(selection.entries().isEmpty())
    }

    @Test
    fun `unchecking is kept when the tree is read again and does not mutate the previous selection`(
        @TempDir tempDir: Path,
    ) {
        write(tempDir, "ia/skills/review/SKILL.md", "kept")
        val discovered = ComponentDiscovery.discover(tempDir)
        val id = discovered.single()
        val initial = ComponentSelection()
        val unchecked = initial.setSelected("repo", id, false)

        assertTrue(initial.components("repo", discovered).single().selected)
        assertFalse(unchecked.components("repo", ComponentDiscovery.discover(tempDir)).single().selected)
        assertEquals("kept", Files.readString(tempDir.resolve("ia").resolve("skills").resolve("review").resolve("SKILL.md")))
    }

    @Test
    fun `a file that appears later is selected and the previous uncheck remains`(@TempDir tempDir: Path) {
        write(tempDir, "ia/skills/review/SKILL.md")
        val review = ComponentDiscovery.discover(tempDir).single()
        val unchecked = ComponentSelection().setSelected("repo", review, false)
        write(tempDir, ".cursor/skills/other/SKILL.md")

        val rows = unchecked.components("repo", ComponentDiscovery.discover(tempDir))

        assertEquals(
            listOf(
                DiscoveredComponent(ComponentId(ComponentKind.SKILL, ".cursor/skills/other"), true),
                DiscoveredComponent(ComponentId(ComponentKind.SKILL, "ia/skills/review"), false),
            ),
            rows,
        )
    }

    @Test
    fun `an empty directory yields an empty list`(@TempDir tempDir: Path) {
        write(tempDir, "README.md", "notes")

        val discovered = ComponentDiscovery.discover(tempDir)

        assertTrue(discovered.isEmpty())
        assertTrue(ComponentSelection().components("repo", discovered).isEmpty())
    }

    @Test
    fun `a missing directory yields an empty list`(@TempDir tempDir: Path) {
        assertTrue(ComponentDiscovery.discover(tempDir.resolve("missing")).isEmpty())
    }

    @Test
    fun `clearing a selection removes the stored uncheck`() {
        val id = ComponentId(ComponentKind.SKILL, "ia/skills/review")
        val unchecked = ComponentSelection().setSelected("repo", id, false)

        val cleared = unchecked.setSelected("repo", id, true)
        val once = cleared.setSelected("repo", id, false)
        val twice = once.setSelected("repo", id, false)

        assertEquals(ComponentSelection(), cleared)
        assertEquals(once, twice)
        assertEquals(1, twice.entries().size)
    }

    @Test
    fun `xml writes insertion order and round trips`() {
        val skill = ComponentId(ComponentKind.SKILL, "ia/skills/review")
        val agent = ComponentId(ComponentKind.AGENT, ".cursor/agents/reviewer.md")
        val selection = ComponentSelection()
            .setSelected("repo", skill, false)
            .setSelected("repo", agent, false)

        val xml = selection.toXml()

        assertTrue(xml.indexOf("<kind>skill</kind>") < xml.indexOf("<kind>agent</kind>"))
        assertEquals(selection, ComponentSelection.fromXml(xml))
        assertEquals(xml, ComponentSelection.fromXml(xml).toXml())
    }

    @Test
    fun `repository path with an ampersand round trips`() {
        val selection = ComponentSelection()
            .setSelected("a & b", ComponentId(ComponentKind.SKILL, "ia/skills/review"), false)

        val xml = selection.toXml()

        assertTrue(xml.contains("a &amp; b"))
        assertEquals(selection, ComponentSelection.fromXml(xml))
    }

    @Test
    fun `empty document restores an empty selection`() {
        assertEquals(ComponentSelection(), ComponentSelection.fromXml("<componentSelection>\n</componentSelection>"))
    }

    @Test
    fun `xml with an unknown kind is refused`() {
        val xml = """
            <componentSelection>
              <deselected>
                <repository>repo</repository>
                <kind>prompt</kind>
                <path>ia/skills/review</path>
              </deselected>
            </componentSelection>
        """.trimIndent()
        assertThrows<IllegalArgumentException> { ComponentSelection.fromXml(xml) }
    }

    @Test
    fun `xml with a duplicate component is refused`() {
        val xml = """
            <componentSelection>
              <deselected>
                <repository>repo</repository>
                <kind>skill</kind>
                <path>ia/skills/review</path>
              </deselected>
              <deselected>
                <repository>repo</repository>
                <kind>skill</kind>
                <path>ia/skills/review</path>
              </deselected>
            </componentSelection>
        """.trimIndent()
        val error = assertThrows<IllegalArgumentException> { ComponentSelection.fromXml(xml) }
        assertTrue(error.message!!.contains("Duplicate component:"))
    }

    @Test
    fun `a blank repository path is refused`() {
        val id = ComponentId(ComponentKind.SKILL, "ia/skills/review")
        assertThrows<IllegalArgumentException> { ComponentSelection().setSelected("  ", id, false) }
    }

    private fun write(root: Path, relative: String, content: String = "x") {
        val path = relative.split('/').fold(root) { parent, name -> parent.resolve(name) }
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
    }
}

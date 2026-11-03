package ai.assistant.improver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.nio.file.Path
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

class PluginDescriptorTest {
    private val moduleDir: Path = Path.of(System.getProperty("user.dir"))

    @Test
    fun `descriptor names the plugin AI Assistant Improver`() {
        val root = pluginRoot()
        assertEquals("ai.assistant.improver", text(root, "id"))
        assertEquals("AI Assistant Improver", text(root, "name"))
    }

    @Test
    fun `descriptor targets since build 262 without an until build`() {
        val ideaVersion = pluginRoot().getElementsByTagName("idea-version").item(0) as Element
        assertEquals("262", ideaVersion.getAttribute("since-build"))
        assertFalse(ideaVersion.hasAttribute("until-build"))
    }

    @Test
    fun `gradle properties record the reference IDEA 2026_2_4 build`() {
        val properties = Properties()
        moduleDir.resolve("gradle.properties").toFile().bufferedReader().use { properties.load(it) }
        assertEquals("2026.2.4", properties.getProperty("platformVersion"))
        assertEquals("262.11879.100", properties.getProperty("platformBuild"))
        assertEquals("262", properties.getProperty("sinceBuild"))
    }

    @Test
    fun `the test process does not start an IDE`() {
        assertNull(System.getProperty("idea.home.path"))
        assertNull(System.getProperty("idea.plugins.path"))
    }

    private fun pluginRoot(): Element {
        val xml = moduleDir.resolve("src/main/resources/META-INF/plugin.xml").toFile()
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return builder.parse(xml).documentElement
    }

    private fun text(root: Element, tag: String): String =
        root.getElementsByTagName(tag).item(0).textContent.trim()
}

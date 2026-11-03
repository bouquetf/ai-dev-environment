package ai.assistant.improver.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class PluginRegistrationTest {
    private val descriptor: Element by lazy {
        val xml = Path.of(System.getProperty("user.dir"))
            .resolve("src/main/resources/META-INF/plugin.xml")
            .toFile()
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml).documentElement
    }

    private fun elements(tag: String): List<Element> {
        val nodes = descriptor.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    @Test
    fun `project services register assistants then repositories`() {
        val services = elements("projectService")
        assertEquals(2, services.size)
        assertEquals(
            "ai.assistant.improver.settings.EnabledAssistantsService",
            services[0].getAttribute("serviceImplementation"),
        )
        assertEquals(
            "ai.assistant.improver.settings.SourceRepositoriesService",
            services[1].getAttribute("serviceImplementation"),
        )
    }

    @Test
    fun `exactly one application service registers LanguageService`() {
        val services = elements("applicationService")
        assertEquals(1, services.size)
        assertEquals("ai.assistant.improver.settings.LanguageService", services[0].getAttribute("serviceImplementation"))
    }

    @Test
    fun `exactly one project configurable registers the settings page`() {
        val configurables = elements("projectConfigurable")
        assertEquals(1, configurables.size)
        val configurable = configurables[0]
        assertEquals("ai.assistant.improver.settings.AssistantSettingsConfigurable", configurable.getAttribute("instance"))
        assertEquals("ai.assistant.improver.settings", configurable.getAttribute("id"))
    }

    @Test
    fun `no application configurable is registered`() {
        assertEquals(0, elements("applicationConfigurable").size)
    }

    @Test
    fun `descriptor still targets since build 262 without an until build`() {
        val ideaVersion = elements("idea-version").single()
        assertEquals("262", ideaVersion.getAttribute("since-build"))
        assertFalse(ideaVersion.hasAttribute("until-build"))
    }
}

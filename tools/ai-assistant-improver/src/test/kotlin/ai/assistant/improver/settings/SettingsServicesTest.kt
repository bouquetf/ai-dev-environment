package ai.assistant.improver.settings

import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.assistant.UnknownAssistantException
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import ai.assistant.improver.repository.SourceRepository
import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentKind
import ai.assistant.improver.component.ComponentSelection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SettingsServicesTest {
    @Test
    fun `assistant service round trips its bean`() {
        val service = EnabledAssistantsService()
        assertTrue(service.current().enabledIds().isEmpty())
        service.replace(EnabledAssistants().enable(AssistantId.JUNIE).enable(AssistantId.CURSOR))
        val bean = service.state
        assertEquals(listOf("cursor", "junie"), bean.ids)

        val restored = EnabledAssistantsService()
        restored.loadState(bean)
        assertEquals(listOf(AssistantId.CURSOR, AssistantId.JUNIE), restored.current().enabledIds())
    }

    @Test
    fun `assistant service refuses unknown identifiers`() {
        val bean = EnabledAssistantsBean().apply { ids = mutableListOf("copilot") }
        assertThrows<UnknownAssistantException> { EnabledAssistantsService().loadState(bean) }
    }

    @Test
    fun `assistant service refuses duplicate identifiers like the xml codec`() {
        val bean = EnabledAssistantsBean().apply { ids = mutableListOf("cursor", "cursor") }
        assertThrows<IllegalArgumentException> { EnabledAssistantsService().loadState(bean) }
    }

    @Test
    fun `language service round trips its bean`() {
        val service = LanguageService()
        assertEquals(UiLanguage.ENGLISH, service.current().language)
        service.replace(LanguagePreference(UiLanguage.FRENCH))
        val bean = service.state
        assertEquals("fr", bean.language)

        val restored = LanguageService()
        restored.loadState(bean)
        assertEquals(UiLanguage.FRENCH, restored.current().language)
    }

    @Test
    fun `language service refuses unknown codes`() {
        val bean = LanguagePreferenceBean().apply { language = "de" }
        assertThrows<IllegalArgumentException> { LanguageService().loadState(bean) }
    }

    @Test
    fun `repository service round trips its bean`() {
        val service = SourceRepositoriesService()
        assertTrue(service.current().entries().isEmpty())
        service.replace(
            SourceRepositories.restore(
                listOf(
                    SourceRepository("C:\\one", RepositoryMode.DOWNLOAD_ONLY),
                    SourceRepository("C:\\two", RepositoryMode.SYNCHRONISATION),
                ),
            ),
        )
        val bean = service.state
        assertEquals(listOf("C:\\one", "C:\\two"), bean.repositories.map { it.path })
        assertEquals(listOf("download-only", "synchronisation"), bean.repositories.map { it.mode })

        val restored = SourceRepositoriesService()
        restored.loadState(bean)
        assertEquals(service.current(), restored.current())
    }

    @Test
    fun `repository service refuses an unknown mode`() {
        val bean = SourceRepositoriesBean().apply {
            repositories = mutableListOf(
                SourceRepositoryBean().apply {
                    path = "C:\\one"
                    mode = "publish"
                },
            )
        }
        assertThrows<IllegalArgumentException> { SourceRepositoriesService().loadState(bean) }
    }

    @Test
    fun `repository service refuses a duplicate path`() {
        val bean = SourceRepositoriesBean().apply {
            repositories = mutableListOf(
                SourceRepositoryBean().apply {
                    path = "C:\\one"
                    mode = "download-only"
                },
                SourceRepositoryBean().apply {
                    path = "C:\\one"
                    mode = "synchronisation"
                },
            )
        }
        assertThrows<IllegalArgumentException> { SourceRepositoriesService().loadState(bean) }
    }

    @Test
    fun `repository service does not require the directory to exist`() {
        val bean = SourceRepositoriesBean().apply {
            repositories = mutableListOf(
                SourceRepositoryBean().apply {
                    path = "C:\\missing\\repo"
                    mode = "synchronisation"
                },
            )
        }
        val service = SourceRepositoriesService()
        service.loadState(bean)
        assertEquals("C:\\missing\\repo", service.current().entries().single().path)
        assertEquals(RepositoryMode.SYNCHRONISATION, service.current().entries().single().mode)
    }

    @Test
    fun `component selection service round trips its bean`() {
        val service = ComponentSelectionService()
        assertTrue(service.current().entries().isEmpty())
        service.replace(
            ComponentSelection()
                .setSelected("C:\\repo", ComponentId(ComponentKind.SKILL, "ia/skills/review"), false)
                .setSelected("C:\\repo", ComponentId(ComponentKind.AGENT, ".cursor/agents/reviewer.md"), false),
        )
        val bean = service.state
        assertEquals(listOf("ia/skills/review", ".cursor/agents/reviewer.md"), bean.deselected.map { it.path })
        assertEquals(listOf("skill", "agent"), bean.deselected.map { it.kind })

        val restored = ComponentSelectionService()
        restored.loadState(bean)
        assertEquals(service.current(), restored.current())
    }

    @Test
    fun `component selection service refuses an unknown kind`() {
        val bean = ComponentSelectionBean().apply {
            deselected = mutableListOf(
                DeselectedComponentBean().apply {
                    repository = "C:\\repo"
                    kind = "prompt"
                    path = "ia/skills/review"
                },
            )
        }
        assertThrows<IllegalArgumentException> { ComponentSelectionService().loadState(bean) }
    }

    @Test
    fun `component selection service refuses a duplicate`() {
        val bean = ComponentSelectionBean().apply {
            deselected = mutableListOf(
                DeselectedComponentBean().apply {
                    repository = "C:\\repo"
                    kind = "skill"
                    path = "ia/skills/review"
                },
                DeselectedComponentBean().apply {
                    repository = "C:\\repo"
                    kind = "skill"
                    path = "ia/skills/review"
                },
            )
        }
        assertThrows<IllegalArgumentException> { ComponentSelectionService().loadState(bean) }
    }

    @Test
    fun `component selection service does not require the files to exist`() {
        val bean = ComponentSelectionBean().apply {
            deselected = mutableListOf(
                DeselectedComponentBean().apply {
                    repository = "C:\\missing\\repo"
                    kind = "skill"
                    path = "ia/skills/missing"
                },
            )
        }
        val service = ComponentSelectionService()
        service.loadState(bean)
        val entry = service.current().entries().single()
        assertEquals("C:\\missing\\repo", entry.repositoryPath)
        assertEquals(ComponentKind.SKILL, entry.id.kind)
        assertEquals("ia/skills/missing", entry.id.relativePath)
    }
}

package ai.assistant.improver.settings

import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.assistant.UnknownAssistantException
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import ai.assistant.improver.repository.SourceRepository
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
}

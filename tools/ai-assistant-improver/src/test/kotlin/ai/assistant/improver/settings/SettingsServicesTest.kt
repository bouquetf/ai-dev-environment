package ai.assistant.improver.settings

import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.assistant.UnknownAssistantException
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage
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
}

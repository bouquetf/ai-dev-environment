package ai.assistant.improver.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BundleKeysTest {
    private val english = mapOf(
        "settings.page.title" to "AI Assistant Improver",
        "settings.assistants.heading" to "Assistants",
        "settings.assistants.cursor" to "Cursor",
        "settings.assistants.claude" to "Claude",
        "settings.assistants.opencode" to "OpenCode",
        "settings.assistants.junie" to "Junie",
        "settings.language.heading" to "Language",
        "settings.language.en" to "English",
        "settings.language.fr" to "French",
        "settings.error.unknownAssistant" to "Unknown assistant: {0}",
    )

    private val french = english + mapOf(
        "settings.language.heading" to "Langue",
        "settings.language.en" to "Anglais",
        "settings.language.fr" to "Français",
        "settings.error.unknownAssistant" to "Assistant inconnu : {0}",
    )

    @Test
    fun `both bundles have the same keys`() {
        val englishKeys = Labels.load(UiLanguage.ENGLISH).keys()
        assertEquals(englishKeys, Labels.load(UiLanguage.FRENCH).keys())
        assertEquals(english.keys, englishKeys)
    }

    @Test
    fun `bundles carry the expected texts`() {
        val en = Labels.load(UiLanguage.ENGLISH)
        val fr = Labels.load(UiLanguage.FRENCH)
        english.forEach { (key, value) -> assertEquals(value, en.text(key), key) }
        french.forEach { (key, value) -> assertEquals(value, fr.text(key), key) }
    }

    @Test
    fun `unknown assistant message substitutes the identifier`() {
        assertEquals(
            "Unknown assistant: copilot",
            Labels.load(UiLanguage.ENGLISH).text("settings.error.unknownAssistant", "copilot"),
        )
        assertEquals(
            "Assistant inconnu : copilot",
            Labels.load(UiLanguage.FRENCH).text("settings.error.unknownAssistant", "copilot"),
        )
    }

    @Test
    fun `missing key is refused`() {
        assertThrows<IllegalArgumentException> { Labels.load(UiLanguage.ENGLISH).text("missing.key") }
    }
}

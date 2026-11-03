package ai.assistant.improver.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LanguagePreferenceTest {
    @Test
    fun `default language is English`() {
        assertEquals(UiLanguage.ENGLISH, LanguagePreference().language)
    }

    @Test
    fun `language can switch to French without mutating the original`() {
        val initial = LanguagePreference()
        assertEquals(UiLanguage.FRENCH, initial.withLanguage(UiLanguage.FRENCH).language)
        assertEquals(UiLanguage.ENGLISH, initial.language)
    }

    @Test
    fun `xml form is canonical`() {
        assertEquals("<uiLanguage>en</uiLanguage>", LanguagePreference().toXml())
        assertEquals("<uiLanguage>fr</uiLanguage>", LanguagePreference(UiLanguage.FRENCH).toXml())
    }

    @Test
    fun `xml round trips for both languages`() {
        UiLanguage.entries.forEach { language ->
            val preference = LanguagePreference(language)
            assertEquals(preference, LanguagePreference.fromXml(preference.toXml()))
        }
    }

    @Test
    fun `unknown language code is refused`() {
        assertThrows<IllegalArgumentException> { LanguagePreference.fromXml("<uiLanguage>de</uiLanguage>") }
    }
}

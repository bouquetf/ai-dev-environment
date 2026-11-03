package ai.assistant.improver.settings

import ai.assistant.improver.i18n.LanguagePreference

interface LanguageStore {
    fun read(): LanguagePreference
    fun write(value: LanguagePreference)
}

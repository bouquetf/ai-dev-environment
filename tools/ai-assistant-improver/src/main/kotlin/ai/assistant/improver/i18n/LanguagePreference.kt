package ai.assistant.improver.i18n

data class LanguagePreference(val language: UiLanguage = UiLanguage.ENGLISH) {
    fun withLanguage(language: UiLanguage): LanguagePreference = copy(language = language)

    fun toXml(): String = "<uiLanguage>${language.code}</uiLanguage>"

    companion object {
        private val pattern = Regex("""<uiLanguage>([^<]*)</uiLanguage>""")

        fun fromXml(xml: String): LanguagePreference {
            val match = pattern.matchEntire(xml.trim())
                ?: throw IllegalArgumentException("Invalid language document: $xml")
            return LanguagePreference(UiLanguage.fromCode(match.groupValues[1].trim()))
        }
    }
}

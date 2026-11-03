package ai.assistant.improver.i18n

enum class UiLanguage(val code: String) {
    ENGLISH("en"),
    FRENCH("fr");

    companion object {
        fun fromCode(code: String): UiLanguage =
            entries.firstOrNull { it.code == code } ?: throw IllegalArgumentException("Unknown language: $code")
    }
}

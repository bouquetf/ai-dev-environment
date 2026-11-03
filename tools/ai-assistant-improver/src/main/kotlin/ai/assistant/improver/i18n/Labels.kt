package ai.assistant.improver.i18n

import java.text.MessageFormat
import java.util.Properties

class Labels private constructor(private val values: Map<String, String>) {
    fun text(key: String): String = values[key] ?: throw IllegalArgumentException("Missing label: $key")

    fun text(key: String, vararg args: Any): String = MessageFormat.format(text(key), *args)

    fun keys(): Set<String> = values.keys

    companion object {
        private const val BASE = "messages/AiAssistantImproverBundle"

        fun load(language: UiLanguage, classLoader: ClassLoader = Labels::class.java.classLoader): Labels {
            val path = when (language) {
                UiLanguage.ENGLISH -> "$BASE.properties"
                UiLanguage.FRENCH -> "${BASE}_fr.properties"
            }
            val stream = classLoader.getResourceAsStream(path)
                ?: throw IllegalStateException("Missing bundle: $path")
            val properties = Properties()
            stream.reader(Charsets.UTF_8).use { properties.load(it) }
            return Labels(properties.stringPropertyNames().associateWith { properties.getProperty(it) })
        }
    }
}

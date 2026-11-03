package ai.assistant.improver.settings

import ai.assistant.improver.i18n.LanguagePreference
import com.intellij.openapi.application.ApplicationManager

class ApplicationLanguageStore : LanguageStore {
    private val service: LanguageService
        get() = ApplicationManager.getApplication().getService(LanguageService::class.java)

    override fun read(): LanguagePreference = service.current()

    override fun write(value: LanguagePreference) {
        service.replace(value)
    }
}

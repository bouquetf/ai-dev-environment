package ai.assistant.improver.settings

import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

class LanguagePreferenceBean {
    var language: String = UiLanguage.ENGLISH.code
}

@State(name = "AiAssistantImproverLanguage", storages = [Storage("ai-assistant-improver-language.xml")])
class LanguageService : PersistentStateComponent<LanguagePreferenceBean> {
    private var preference = LanguagePreference()

    fun current(): LanguagePreference = preference

    fun replace(value: LanguagePreference) {
        preference = value
    }

    override fun getState(): LanguagePreferenceBean =
        LanguagePreferenceBean().apply { language = preference.language.code }

    override fun loadState(state: LanguagePreferenceBean) {
        preference = LanguagePreference(UiLanguage.fromCode(state.language))
    }
}

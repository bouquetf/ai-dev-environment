package ai.assistant.improver.settings

import ai.assistant.improver.assistant.EnabledAssistants
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

class EnabledAssistantsBean {
    var ids: MutableList<String> = mutableListOf()
}

@State(name = "AiAssistantImproverAssistants", storages = [Storage("ai-assistant-improver.xml")])
class EnabledAssistantsService : PersistentStateComponent<EnabledAssistantsBean> {
    private var assistants = EnabledAssistants()

    fun current(): EnabledAssistants = assistants

    fun replace(value: EnabledAssistants) {
        assistants = value
    }

    override fun getState(): EnabledAssistantsBean =
        EnabledAssistantsBean().apply { ids = assistants.enabledIds().map { it.wire }.toMutableList() }

    override fun loadState(state: EnabledAssistantsBean) {
        assistants = EnabledAssistants.fromWires(state.ids)
    }
}

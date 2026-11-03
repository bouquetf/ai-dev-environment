package ai.assistant.improver.settings

import ai.assistant.improver.assistant.EnabledAssistants

interface AssistantStore {
    fun read(): EnabledAssistants
    fun write(value: EnabledAssistants)
}

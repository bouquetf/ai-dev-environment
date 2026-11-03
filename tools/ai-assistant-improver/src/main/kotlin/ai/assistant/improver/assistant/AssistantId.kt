package ai.assistant.improver.assistant

enum class AssistantId(val wire: String) {
    CURSOR("cursor"),
    CLAUDE("claude"),
    OPENCODE("opencode"),
    JUNIE("junie");

    companion object {
        fun fromWire(value: String): AssistantId =
            entries.firstOrNull { it.wire == value } ?: throw UnknownAssistantException(value)
    }
}

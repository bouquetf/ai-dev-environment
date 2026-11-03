package ai.assistant.improver.assistant

class UnknownAssistantException(val wire: String) : IllegalArgumentException("Unknown assistant: $wire")

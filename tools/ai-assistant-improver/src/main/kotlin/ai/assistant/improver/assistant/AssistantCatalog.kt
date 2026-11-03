package ai.assistant.improver.assistant

object AssistantCatalog {
    val assistants: List<AssistantId> = listOf(
        AssistantId.CURSOR,
        AssistantId.CLAUDE,
        AssistantId.OPENCODE,
        AssistantId.JUNIE,
    )
}

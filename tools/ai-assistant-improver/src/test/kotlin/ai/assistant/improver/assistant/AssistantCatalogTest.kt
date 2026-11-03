package ai.assistant.improver.assistant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AssistantCatalogTest {
    @Test
    fun `catalog contains exactly the four assistants in order`() {
        val expected = listOf(AssistantId.CURSOR, AssistantId.CLAUDE, AssistantId.OPENCODE, AssistantId.JUNIE)
        assertEquals(expected, AssistantCatalog.assistants)
        assertEquals(expected, AssistantId.entries.toList())
    }

    @Test
    fun `wire identifiers are stable`() {
        assertEquals(listOf("cursor", "claude", "opencode", "junie"), AssistantCatalog.assistants.map { it.wire })
    }
}

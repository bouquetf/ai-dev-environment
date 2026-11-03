package ai.assistant.improver.component

enum class ComponentKind(val wire: String) {
    SKILL("skill"),
    AGENT("agent"),
    RULE("rule");

    companion object {
        fun fromWire(value: String): ComponentKind =
            entries.firstOrNull { it.wire == value }
                ?: throw IllegalArgumentException("Unknown component kind: $value")
    }
}

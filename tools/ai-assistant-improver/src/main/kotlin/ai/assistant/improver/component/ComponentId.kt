package ai.assistant.improver.component

data class ComponentId(val kind: ComponentKind, val relativePath: String) {
    init {
        require(relativePath.isNotBlank()) { "Blank component path" }
        require('\\' !in relativePath) { "Component path contains a backslash: $relativePath" }
        require(!relativePath.startsWith('/')) { "Component path is absolute: $relativePath" }
    }
}

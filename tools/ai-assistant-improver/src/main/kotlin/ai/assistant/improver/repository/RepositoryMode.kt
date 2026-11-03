package ai.assistant.improver.repository

enum class RepositoryMode(val wire: String) {
    DOWNLOAD_ONLY("download-only"),
    SYNCHRONISATION("synchronisation");

    companion object {
        fun fromWire(value: String): RepositoryMode =
            entries.firstOrNull { it.wire == value }
                ?: throw IllegalArgumentException("Unknown repository mode: $value")
    }
}

package ai.assistant.improver.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BundleKeysTest {
    private val english = mapOf(
        "settings.page.title" to "AI Assistant Improver",
        "settings.assistants.heading" to "Assistants",
        "settings.assistants.cursor" to "Cursor",
        "settings.assistants.claude" to "Claude",
        "settings.assistants.opencode" to "OpenCode",
        "settings.assistants.junie" to "Junie",
        "settings.language.heading" to "Language",
        "settings.language.en" to "English",
        "settings.language.fr" to "French",
        "settings.error.unknownAssistant" to "Unknown assistant: {0}",
        "settings.repositories.heading" to "Repositories",
        "settings.repositories.add" to "Add",
        "settings.repositories.browse" to "Browse",
        "settings.repositories.remove" to "Remove",
        "settings.repositories.mode.download-only" to "Download only",
        "settings.repositories.mode.synchronisation" to "Synchronisation",
        "settings.error.repository.empty" to "Enter a directory that contains .git.",
        "settings.error.repository.notDirectory" to "Not a directory: {0}",
        "settings.error.repository.notGit" to "This directory does not contain .git: {0}",
        "settings.error.repository.duplicate" to "This repository is already listed: {0}",
        "settings.components.skill" to "Skill",
        "settings.components.agent" to "Agent",
        "settings.components.rule" to "Rule",
    )

    private val french = english + mapOf(
        "settings.language.heading" to "Langue",
        "settings.language.en" to "Anglais",
        "settings.language.fr" to "Français",
        "settings.error.unknownAssistant" to "Assistant inconnu : {0}",
        "settings.repositories.heading" to "Dépôts",
        "settings.repositories.add" to "Ajouter",
        "settings.repositories.browse" to "Parcourir",
        "settings.repositories.remove" to "Retirer",
        "settings.repositories.mode.download-only" to "Téléchargement seul",
        "settings.error.repository.empty" to "Indiquez un répertoire qui contient .git.",
        "settings.error.repository.notDirectory" to "Pas un répertoire : {0}",
        "settings.error.repository.notGit" to "Ce répertoire ne contient pas .git : {0}",
        "settings.error.repository.duplicate" to "Ce dépôt est déjà dans la liste : {0}",
        "settings.components.rule" to "Règle",
    )

    @Test
    fun `both bundles have the same keys`() {
        val englishKeys = Labels.load(UiLanguage.ENGLISH).keys()
        assertEquals(englishKeys, Labels.load(UiLanguage.FRENCH).keys())
        assertEquals(english.keys, englishKeys)
    }

    @Test
    fun `bundles carry the expected texts`() {
        val en = Labels.load(UiLanguage.ENGLISH)
        val fr = Labels.load(UiLanguage.FRENCH)
        english.forEach { (key, value) -> assertEquals(value, en.text(key), key) }
        french.forEach { (key, value) -> assertEquals(value, fr.text(key), key) }
    }

    @Test
    fun `unknown assistant message substitutes the identifier`() {
        assertEquals(
            "Unknown assistant: copilot",
            Labels.load(UiLanguage.ENGLISH).text("settings.error.unknownAssistant", "copilot"),
        )
        assertEquals(
            "Assistant inconnu : copilot",
            Labels.load(UiLanguage.FRENCH).text("settings.error.unknownAssistant", "copilot"),
        )
    }

    @Test
    fun `missing key is refused`() {
        assertThrows<IllegalArgumentException> { Labels.load(UiLanguage.ENGLISH).text("missing.key") }
    }
}

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
        "settings.import.action" to "Import",
        "settings.import.confirmTitle" to "Overwrite local changes",
        "settings.import.confirm" to "Overwrite local changes in {0}?",
        "settings.import.noTarget" to "{0} has no folder for {1}.",
        "settings.import.kept" to "Local changes kept for {0}.",
        "settings.publish.action" to "Publish",
        "settings.publish.confirmTitle" to "Publish local changes",
        "settings.publish.intro" to "Publish these files to the source repository?",
        "settings.publish.message" to "Commit message",
        "settings.publish.defaultMessage" to "Publish local changes",
        "settings.publish.nothing" to "No local changes to publish.",
        "settings.publish.done" to "Published {0}.",
        "settings.publish.cancelled" to "Publication cancelled.",
        "settings.publish.emptyMessage" to "Enter a commit message.",
        "settings.publish.divergence" to "The source repository and the local files both changed. Nothing was published for {0}.",
        "settings.publish.noOrigin" to "No origin remote. Nothing was committed for {0}.",
        "settings.publish.pushFailed" to "The commit stays local and was not pushed for {0}.",
        "settings.publish.commitFailed" to "The commit failed for {0}.",
        "settings.publish.detached" to "The source repository is not on a branch. Nothing was committed for {0}.",
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
        "settings.import.action" to "Importer",
        "settings.import.confirmTitle" to "Écraser les modifications locales",
        "settings.import.confirm" to "Écraser les modifications locales dans {0} ?",
        "settings.import.noTarget" to "{0} : aucun dossier pour {1}.",
        "settings.import.kept" to "Modifications locales conservées pour {0}.",
        "settings.publish.action" to "Publier",
        "settings.publish.confirmTitle" to "Publier les modifications locales",
        "settings.publish.intro" to "Publier ces fichiers vers le dépôt source ?",
        "settings.publish.message" to "Message de commit",
        "settings.publish.defaultMessage" to "Publier les modifications locales",
        "settings.publish.nothing" to "Aucune modification locale à publier.",
        "settings.publish.done" to "Publication effectuée pour {0}.",
        "settings.publish.cancelled" to "Publication annulée.",
        "settings.publish.emptyMessage" to "Indiquez un message de commit.",
        "settings.publish.divergence" to "Le dépôt source et les fichiers locaux ont divergé. Aucune publication pour {0}.",
        "settings.publish.noOrigin" to "Pas de remote origin. Aucun commit pour {0}.",
        "settings.publish.pushFailed" to "Le commit reste en local, sans push, pour {0}.",
        "settings.publish.commitFailed" to "Le commit a échoué pour {0}.",
        "settings.publish.detached" to "Branche courante absente. Aucun commit pour {0}.",
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

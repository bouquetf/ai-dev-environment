---
name: story-fr
description: Découpage de spécifications en User Stories (Français) ; continue de poser des questions tant qu'il reste des ambiguïtés
---

# Skill: Stories (FR)

Ce guide décrit comment générer des user stories à partir d'une spécification en considérant la documentation et l'automatisation des tests.

## Qu'est-ce que je fais

Je découpe une spécification en user stories exploitables, en tenant compte de la documentation du logiciel et de l'automatisation des tests. Chaque story doit apporter une valeur probante à l'utilisateur final.

## Quand m'utiliser

Utilisez cette skill lorsque vous avez une spécification de projet et que vous voulez générer des user stories structurées, testables et documentées.

## Organisation des fichiers (`tmp/`)

Les stories vivent sous `tmp/` (répertoire de travail jetable, hors livrables `docs/`). Chaque story a son propre identifiant **`<id>`** sur 3 chiffres (`001`, `004`, …) — normaliser l'argument utilisateur (`1` → `001`, `story-004` → `004`).

```text
tmp/
  <id>/
    story.md              # contenu de la user story
    questions.md          # questions temporaires (workflow story — supprimé à la fin)
  plan-implementation.md  # plan global multi-stories (généré en fin de workflow)
```

| Fichier | Rôle |
|---------|------|
| `tmp/<id>/story.md` | User story #XXX (titre, critères, tests, dépendances) |
| `tmp/<id>/questions.md` | Clarifications en cours pour cette story |
| `tmp/plan-implementation.md` | Ordre d'implémentation et dépendances entre stories |

Les **tâches** d'une story sont créées par la skill `tasks-fr` dans `tmp/<id>/story/` ; une tâche terminée est renommée `task-YYY-done.md` (voir skill `tasks-fr`).

## Workflow

### Cas 1 : Aucune story n'existe (début depuis zéro)

**Étape 1 - Lecture de la spécification :**
- L'utilisateur donne le chemin de la spécification en argument (ex: specification.md ou docs/specification/specification.md)
- Lire la spécification fournie
- Générer des user stories qui répondent aux critères :
  - Considèrent la documentation du logiciel existante
  - Intègrent l'automatisation des tests
  - Apportent une valeur probante à l'utilisateur final
- Créer le répertoire `tmp/` s'il n'existe pas
- Pour chaque story, créer `tmp/<id>/` et générer `tmp/<id>/story.md` (ex. `tmp/001/story.md`, `tmp/002/story.md`)

**Étape 2 - Création du fichier de questions :**
- Créer un fichier temporaire `tmp/questions.md` (clarifications globales sur le lot de stories)
- Ce fichier contiendra les questions auxquelles l'utilisateur devra répondre

**Étape 3 - Analyse et clarification :**
- Analyser les stories générées
- Vérifier la cohérence vis-à-vis de la spécification
- Identifier ce qui n'est pas clair ou ce qu'il manque
- Ajouter des questions de clarification dans questions.md avec :
  - Le format obligatoire de la règle « qualité des questions » (enjeu, contexte, dépendances, propositions détaillées, emplacement de réponse)
  - Des références à la spécification / documentation si applicable
  - Des propositions dont **chaque** option explique le comportement résultant et les implications pour les stories concernées

**Étape 4 - Mise à jour :**
- L'utilisateur répond aux questions dans questions.md
- L'utilisateur tape : J'ai répondu aux questions, mets à jour les stories
- Mettre à jour les stories en fonction des réponses données
- **Relire les stories mises à jour** : s'il reste des ambiguïtés, points flous, inconsistances avec la spécification, ou sujets qui méritent une réponse supplémentaire → régénérer `questions.md` avec ces nouvelles questions
- Retourner à l'Étape 3 (ne pas clôturer tant qu'il reste quelque chose à clarifier)

**Étape 5 - Génération du plan d'implémentation et fin du workflow :**
- Quand, après relecture, il n'y a vraiment plus rien à clarifier (plus de questions utiles dans questions.md)
- Supprimer le fichier questions.md
- Analyser les dépendances entre les stories (section ## Dépendances de chaque story)
- Générer le fichier `tmp/plan-implementation.md` contenant :
  - Ordre d'implémentation recommandé (basé sur les dépendances, la valeur utilisateur, la complexité)
  - Stories pouvant être développées en parallèle (sans dépendances croisées)
  - Justifications de l'ordre proposé
  - Diagramme de dépendances au format Mermaid
- Annoncer la fin du workflow

## Règle : clarification continue (obligatoire)

**Ne jamais clôturer tant qu'il reste quelque chose à clarifier.** Après chaque mise à jour des stories, relire le lot et poser de **nouvelles** questions si :

- un critère d'acceptation, un test, une dépendance ou une valeur utilisateur reste ambigu ou incomplet ;
- une réponse en soulève d'autres (périmètre, rôles, cas limites, ordre entre stories) ;
- deux stories se contredisent ou chevauchent sans décision explicite ;
- un point mérite une confirmation avant d'être figé dans le plan d'implémentation.

**Préférer demander plutôt que d'assumer.** Une `questions.md` vide n'est valide qu'après une relecture active qui n'a trouvé aucun doute raisonnable — pas parce que le cycle précédent n'a « plus » de placeholders formels.

## Règle : qualité des questions (obligatoire)

Chaque entrée de `questions.md` doit permettre de répondre **en connaissance de cause**, sans devoir relire toutes les stories ni deviner les enjeux. Une question trop courte (« Quels rôles ? », « Cypress ou Playwright ? ») est **insuffisante**.

Pour **chaque** question, inclure **tous** les blocs suivants :

| Bloc | Contenu attendu |
|------|-----------------|
| **Enjeu** | Pourquoi la réponse compte pour la/les story(s) (critères, tests, dépendances, plan d'implémentation) |
| **Contexte** | Extraits pertinents de la story / spec ; ce qui est déjà décidé ; ce qui reste flou ; chevauchements éventuels avec d'autres stories |
| **Dépendances** | Stories, critères ou tests impactés par la réponse |
| **Question** | Formulation précise, **une** décision |
| **Propositions** | ≥ 2 options concrètes (+ « Autre ») ; chaque option : comportement, implications sur les stories/tests, pertinence |
| **Réponse** | Emplacement vide pour l'utilisateur |

**Interdit :** questions sans ancrage story/spec ; options nues sans implications ; plusieurs décisions indépendantes dans une seule question.

**Obligatoire :** citer la story concernée (`Story #00X`), la section (critère, test, dépendance…), et si utile un extrait court. Si une option est recommandée, le justifier sans l'imposer.

### Cas 2 : Des stories existent déjà

- Démarrer directement à l'Étape 3 du Cas 1
- Lire les stories existantes dans `tmp/*/story.md`
- Analyser et itérer jusqu'à résolution complète (même règle de clarification continue)

## Format des fichiers

### Story (`tmp/<id>/story.md`)
``markdown
# Story #XXX : [Titre de la story]

## Description
En tant que [rôle], je veux [action] afin de [bénéfice].

## Critères d'acceptation
- [ ] Critère 1
- [ ] Critère 2
- [ ] Critère 3

## Tests automatisés
- Test unitaire : [Description]
- Test d'intégration : [Description]
- Test E2E : [Description]

## Documentation
- [Lien vers la documentation impactée]
- [Nouvelle documentation à créer]

## Valeur utilisateur
[Description de la valeur probante pour l'utilisateur final]

## Dépendances
- [Story #YYY]
- [Documentation existante]
``

### questions.md (`tmp/questions.md` ou `tmp/<id>/questions.md`)
``markdown
# Questions de clarification - Stories

## Contexte global
- Spécification : [chemin]
- Stories générées / concernées : [liste]
- Point d'attention : [résumé des ambiguïtés principales]

## Questions

### 1. [Story #002 — Critère d'acceptation] — Matrice des rôles auth
**Enjeu :** Sans rôles explicites, les critères d'acceptation de #002 restent non testables et #005 (partage) sera incohérente.
**Contexte :**
- Story #002 mentionne l'authentification mais pas la gestion des rôles.
- Spec : multi-utilisateurs sur un même board.
- Story #005 suppose déjà un « invité lecture seule » sans que #002 le définisse.
**Dépendances :** Critères de #002 ; description de #005 ; tests E2E login/permissions.
**Question :** Quels rôles doivent être gérés dès #002, et avec quels droits sur un board ?
**Propositions :**
- a) **Admin / User / Guest** — Admin : tout + membres ; User : CRUD cartes ; Guest : lecture seule. Implications : #002 ajoute 3 rôles ; #005 s'aligne ; tests E2E × 3 profils.
- b) **Admin / User seulement** — Pas de Guest au MVP. Implications : retirer Guest de #005 ; simplifier les tests ; report lecture seule.
- c) Autre : fournir une matrice rôle × action (créer board, inviter, éditer carte, supprimer).
**Réponse :**

### 2. [Story #004 — Test automatisé] — Framework E2E pour le drag-and-drop
**Enjeu :** Le choix du runner E2E conditionne la rédaction des scénarios de #004 et la CI.
**Contexte :**
- #004 exige des tests E2E pour le drag-and-drop des tâches.
- Inventaire projet : [indiquer si Cypress/Playwright/Selenium est déjà présent, ou aucun].
**Dépendances :** Section « Tests automatisés » de #004 ; éventuellement autres stories UI.
**Question :** Quel framework E2E utiliser pour les scénarios de #004 ?
**Propositions :**
- a) **Playwright** — Déjà / à ajouter : [état repo]. Implications : API moderne, bon support DnD ; aligner la doc de test.
- b) **Cypress** — Déjà / à ajouter : [état repo]. Implications : écosystème connu ; vérifier support DnD dans la version du projet.
- c) **Reporter / ne pas automatiser le DnD au MVP** — Implications : critère E2E de #004 à reformuler (manuel ou test composant).
- d) Autre : préciser outil et où vivent les specs.
**Réponse :**

## Inconsistances détectées
### I1. [Story #002 vs #005] — Guest non défini
**Constat :** #005 référence un invité lecture seule ; #002 ne définit aucun rôle Guest.
**Pourquoi c'est un problème :** Risque de stories contradictoires dans le plan d'implémentation.
**Question :** Faut-il introduire Guest dans #002, ou retirer Guest de #005 ?
**Propositions :** (même niveau de détail)
**Réponse :**
``

### plan-implementation.md (`tmp/plan-implementation.md`)
``markdown
# Plan d'implémentation des stories

## Ordre d'implémentation recommandé
1. **Story #001** - [Titre] (Priorité : Haute)
   - Justification : [Dépendance nulle, valeur utilisateur élevée]
2. **Story #002** - [Titre] (Priorité : Haute)
   - Justification : [Dépend de Story #001, nécessaire pour les fonctionnalités suivantes]
3. **Story #003** - [Titre] (Priorité : Moyenne)
   - Justification : [Peut être développée en parallèle de #004]

## Stories développables en parallèle
- **Groupe 1** : Story #003, Story #004 (aucune dépendance croisée)
- **Groupe 2** : Story #005, Story #006 (dépendent uniquement des stories déjà implémentées)

## Diagramme de dépendances (format Mermaid)
``mermaid
flowchart TD
    %% Stories ordonnées
    S001[Story #001 : Titre] --> S002[Story #002 : Titre]
    S002 --> S003[Story #003 : Titre]
    S002 --> S004[Story #004 : Titre]

    %% Groupes parallèles
    subgraph Parallel1
        S003
        S004
    end

    S003 --> S005[Story #005 : Titre]
    S004 --> S005
    S005 --> S006[Story #006 : Titre]

    subgraph Parallel2
        S005
        S006
    end
``

## Notes
- Les stories doivent être implémentées dans l'ordre indiqué pour respecter les dépendances
- Les groupes en parallèle peuvent être développés simultanément par différentes équipes
``

## Comment passer d'une étape à l'autre

| Transition | Action de l'utilisateur | Action de l'agent |
|------------|------------------------|-------------------|
| Étape 1 → 2 | Donner le chemin de la spécification en argument | Génère les stories initiales et crée questions.md |
| Étape 2 → 3 | Automatique après création de questions.md | Analyse les stories et ajoute les questions |
| Étape 3 → 4 | Taper J'ai répondu aux questions, mets à jour les stories | Met à jour les stories selon les réponses |
| Étape 4 → 3 | Automatique après mise à jour | Re-analyse les stories ; **régénère `questions.md` s'il reste des ambiguïtés** |
| Étape 4 → 5 | Automatique (plus de questions **après relecture**) | Supprime questions.md, génère plan-implementation.md, annonce la fin |

**Important :** L'agent ne surveille pas les modifications de fichiers en temps réel. L'utilisateur doit toujours donner une instruction explicite pour que l'agent passe à l'étape suivante.

## Exemple d'utilisation

**Cas 1 - Début depuis zéro :**
``
/ stories specification.md
``

**Cas 2 - Stories existantes :**
``
/ stories
``
(L'agent détecte automatiquement les stories existantes dans `tmp/*/story.md` et commence l'analyse)

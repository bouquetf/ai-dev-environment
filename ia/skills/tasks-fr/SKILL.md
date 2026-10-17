---
name: tasks-fr
description: Découpage de User Stories en tâches de code exploitables (Prompts) ; continue de poser des questions tant qu'il reste des ambiguïtés
---

# Skill: Tasks

Ce guide décrit comment découper une story en tâches "prompt" exécutables par un agent de code avec tests, documentation et exemples d'API.

## Qu'est-ce que je fais

Je prends une story existante, je la découpe en tâches élémentaires. Chaque tâche contient un prompt le plus détaillé possible, prêt à être exécuté par un agent de code. Je vérifie la cohérence des tâches générées et je clarifie avec l'utilisateur via un fichier `questions.md` jusqu'à résolution complète.

## Quand m'utiliser

Utilisez cette skill après avoir créé vos user stories avec la skill `story-fr`, quand vous voulez passer à l'implémentation.

## Organisation des fichiers (`tmp/`)

Chaque story a son identifiant **`<id>`** sur 3 chiffres (`001`, `004`, …). Normaliser l'argument utilisateur (`1` → `001`, `story-004` → `004`).

```text
tmp/
  <id>/
    story.md                    # user story (source)
    story/
      task-001.md               # tâche en attente
      task-001-done.md          # tâche terminée (renommée)
      task-002.md
      task-001-examples.http    # exemples HTTP (si applicable)
      task-001-examples-done.http  # renommé avec la tâche
      questions.md              # clarifications temporaires (supprimé à la fin)
```

| Fichier | Rôle |
|---------|------|
| `tmp/<id>/story.md` | Story lue en entrée |
| `tmp/<id>/story/task-YYY.md` | Prompt détaillé — **en attente** |
| `tmp/<id>/story/task-YYY-done.md` | Même prompt — **terminée** (suffixe `-done` avant l'extension) |
| `tmp/<id>/story/questions.md` | Questions de clarification sur les tâches |

**Convention `-done` :** dès qu'une tâche est exécutée et validée (critère « tests verts » satisfait), **renommer** le fichier prompt et tout fichier associé (`task-YYY-examples.http` → `task-YYY-examples-done.http`). Ne pas supprimer le contenu : le renommage trace l'avancement. Les tâches en attente sont celles dont le nom correspond à `task-*.md` **sans** `-done`.

## Workflow

### Étape 1 - Lecture de la story
- Prendre le numéro de story fourni en argument (ex: `/ tasks-fr 001`)
- Normaliser l'identifiant : `001`
- Lire le fichier `tmp/001/story.md`
- Extraire la description, les critères d'acceptation, les tests et la documentation attendue
- Lire la spécification et les autres stories (`tmp/*/story.md`) pour comprendre le contexte global

### Étape 2 - Découpage et génération des tâches avec prompts détaillés
- Analyser la story pour identifier les unités de travail atomiques
- **Chaque tâche doit pouvoir être livrée avec la suite de tests entièrement verte** : pas d'état intermédiaire où des tests existants échouent, sont ignorés (`@Disabled`), supprimés ou commentés pour masquer une régression. Si un refactor est trop large pour tenir en une tâche, le redécouper ou exiger une migration atomique (code + tests + imports dans le même livrable).
- Pour chaque unité, générer un prompt extrêmement détaillé incluant :
  - **Objectif précis** de la tâche
  - **Description fonctionnelle** : comportement attendu, cas nominaux et cas limites
  - **Fichiers concernés** : chemins exacts, nature de la modification (création, modification, suppression)
  - **Contraintes techniques** : framework, patterns, conventions du projet, gestion d'erreurs, performance, sécurité
  - **Signature des fonctions/classes** : noms, paramètres, types, valeurs de retour
  - **Structure des données** : schémas, modèles, DTO, interfaces
  - **Tests à implémenter** : scénarios unitaires, intégration, E2E avec cas de test concrets
  - **Critère de fin — tests verts** : commandes Gradle exactes à exécuter avant de clôturer la tâche ; modules et profils concernés ; interdiction de livrer avec des tests en échec
  - **Documentation** : quoi documenter et où
  - **Exemples d'utilisation** : extraits de code, requêtes API HTTP
- Créer le répertoire `tmp/<id>/story/` s'il n'existe pas
- Générer les fichiers `tmp/<id>/story/task-YYY.md`

### Étape 3 - Vérification de cohérence
- Analyser les tâches générées entre elles :
  - Vérifier qu'il n'y a pas de dépendances circulaires ou d'ordre d'exécution problématique
  - Vérifier que chaque tâche a des entrées et sorties claires
  - Vérifier que la somme des tâches couvre bien 100% des critères d'acceptation de la story
  - **Vérifier que chaque tâche inclut un critère de fin « tests verts »** avec les commandes de validation adaptées au périmètre modifié (modules Gradle touchés, profils `-Pcrbpm.engines=…` si applicable)
  - **Vérifier qu'aucune tâche ne laisse la suite en rouge** : déplacements de tests colocalisés avec le code déplacé ; refactors transversaux (renommage d'enum, changement de package) couvrent tous les appels et tests dans la même tâche
  - Identifier les doublons ou chevauchements entre tâches
  - Vérifier la cohérence des nommages, types, signatures entre tâches
- Analyser la cohérence avec le projet existant :
  - Vérifier la compatibilité avec les conventions et l'architecture existante
  - Vérifier que les fichiers référencés existent ou sont bien à créer

### Étape 4 - Création de questions.md
- Créer un fichier `tmp/<id>/story/questions.md`
- Ajouter des questions de clarification pour :
  - Les choix d'implémentation non tranchés
  - Les incohérences entre tâches
  - Les ambiguïtés de la story
  - Les dépendances et l'ordre d'exécution
  - Les propositions techniques alternatives
- Respecter **intégralement** la règle « qualité des questions » : chaque question doit inclure enjeu, contexte factuel (extraits tâche/story/code existant), dépendances, propositions détaillées avec implications, et emplacement de réponse
- Ancrer le contexte dans le projet (chemins de fichiers, modules Gradle, patterns déjà présents) pour que l'utilisateur tranche sans relire tous les prompts

### Étape 5 - Mise à jour
- L'utilisateur répond aux questions dans `questions.md`
- L'utilisateur tape : `J'ai répondu, mets à jour les tâches`
- Mettre à jour les tâches en fonction des réponses
- **Relire les tâches mises à jour** : s'il reste des ambiguïtés, choix non tranchés, inconsistances entre tâches, ou points qui méritent une réponse supplémentaire → régénérer `questions.md`
- Retourner à l'Étape 3 (ne pas clôturer tant qu'il reste quelque chose à clarifier)

### Étape 6 - Fin du workflow
- Quand, après relecture, il n'y a vraiment plus rien à clarifier
- Supprimer le fichier `questions.md`
- Présenter le plan final à l'utilisateur

## Règle : clarification continue (obligatoire)

**Ne jamais clôturer tant qu'il reste quelque chose à clarifier.** Après chaque mise à jour des tâches, relire le lot et poser de **nouvelles** questions si :

- un choix d'implémentation, une signature, un schéma ou un critère de tests reste ambigu ou incomplet ;
- une réponse en soulève d'autres (ordre des tâches, modules touchés, profils Gradle, contrats d'API) ;
- deux tâches se contredisent ou se chevauchent sans décision explicite ;
- un point mérite une confirmation avant qu'un agent de code n'exécute le prompt.

**Préférer demander plutôt que d'assumer.** Ne pas inventer de décisions techniques pour « finir » plus vite. Une `questions.md` vide n'est valide qu'après une relecture active sans doute raisonnable.

## Règle : qualité des questions (obligatoire)

Chaque entrée de `questions.md` doit permettre de répondre **en connaissance de cause**, sans devoir relire tous les prompts de tâches. Une question trop courte (« JSON API ou custom ? », « Ordre 001 puis 002 ? ») est **insuffisante**.

Pour **chaque** question, inclure **tous** les blocs suivants :

| Bloc | Contenu attendu |
|------|-----------------|
| **Enjeu** | Impact sur les prompts, l'ordre d'exécution, les modules, les tests verts |
| **Contexte** | Extraits des tâches / story ; fichiers ou patterns déjà présents dans le repo ; ce qui est figé vs flou |
| **Dépendances** | Tâches, signatures, schémas ou commandes de test impactés |
| **Question** | Une seule décision, précise |
| **Propositions** | ≥ 2 options (+ « Autre ») ; chaque option : comportement concret, implications (fichiers, tests, ordre), pertinence |
| **Réponse** | Emplacement vide pour l'utilisateur |

**Interdit :** options nues sans implications ; jargon non relié au code existant ; plusieurs décisions indépendantes dans une question.

**Obligatoire :** citer `Tâche #YYY`, chemins (`crbpm/...`), et si utile un extrait de signature ou de critère. Recommandation possible avec justification, sans imposer.

### Exécution et clôture d'une tâche (agent de code)

Lorsqu'un agent implémente une tâche à partir de `tmp/<id>/story/task-YYY.md` :

1. **Sélection** : choisir la prochaine tâche **sans** suffixe `-done`, en respectant l'ordre des dépendances déclarées dans les prompts.
2. **Implémentation** : suivre le prompt ; satisfaire le critère « tests verts ».
3. **Clôture obligatoire** — une fois la tâche validée :
   - Renommer `task-YYY.md` → `task-YYY-done.md`
   - Renommer aussi les fichiers associés : `task-YYY-examples.http` → `task-YYY-examples-done.http` (même règle : `-done` avant l'extension)
   - Ne renommer **qu'après** succès des tests ; si la tâche échoue, laisser le fichier sans `-done`
4. **Références** : dans les tâches suivantes, les dépendances pointent vers « Tâche #YYY » (numéro), pas le nom de fichier — le statut se lit via la présence ou non de `-done`.

Lors d'une **mise à jour** des tâches (Étape 5), ne modifier que les fichiers **sans** `-done`, sauf demande explicite de l'utilisateur.

## Principe : tests verts entre chaque tâche

Chaque tâche générée est un **livrable mergeable** : à la fin de son exécution, l'agent de code doit pouvoir lancer la suite de tests concernée et obtenir **zéro échec**.

| Règle | Détail |
|-------|--------|
| Pas de régression tolérée | Ne pas désactiver, supprimer ou assouplir des assertions pour « faire passer » la CI |
| Déplacement atomique | Déplacer code **et** tests dans la même tâche ; lancer `:ancien-module:test` **et** `:nouveau-module:test` si les deux existent encore |
| Refactor transversal | Renommage d'API, suppression d'enum, changement de signature : mettre à jour **tous** les producteurs, consommateurs et tests dans la même tâche |
| Profils build | Si la story introduit des profils Gradle (`-Pcrbpm.engines=…`), préciser quel profil est obligatoire à chaque tâche (souvent le défaut rétrocompatible jusqu'à la tâche « wrapper-only ») |
| Nouveaux tests | Les tests ajoutés complètent la couverture ; ils ne remplacent pas la régression existante |
| Commande type | Depuis `crbpm/` : `.\gradlew :module:test` (et `build` si packaging touché). Pour une validation complète : skill `run-tests-dood` ou `.\gradlew test` sur les modules impactés |
| Clôture `-done` | Après tests verts : renommer `task-YYY.md` → `task-YYY-done.md` (+ fichiers `.http` associés) |

Inclure systématiquement une section **« Critère de fin — tests verts »** dans chaque `task-YYY.md`, avec les commandes exactes, la liste des suites qui doivent rester vertes, et l'instruction de renommage `-done`.

## Format des prompts détaillés

### Structure du prompt (task-YYY.md)

```markdown
# Tâche #YYY - Story #XXX : [Titre de la tâche]

## Objectif
[Description concise de ce que cette tâche doit livrer]

## Contexte
- Story #XXX : `tmp/<id>/story.md`
- Dépend de : Tâche #ZZZ (si applicable)
- Nécessaire pour : Tâche #WWW (si applicable)

## Prompt

En tant qu'agent de code, tu dois implémenter ce qui suit.

### Description fonctionnelle
[Description détaillée du comportement attendu]

**Cas nominaux :**
- [Cas 1]
- [Cas 2]

**Cas limites :**
- [Cas limite 1]
- [Cas limite 2]

**Gestion d'erreurs :**
- [Erreur 1] → [Comportement attendu]
- [Erreur 2] → [Comportement attendu]

### Fichiers concernés

| Fichier | Action | Description |
|---------|--------|-------------|
| `src/feature/exemple.ts` | Créer | Nouveau service |
| `src/feature/types.ts` | Modifier | Ajouter le type X |
| `src/feature/__tests__/exemple.test.ts` | Créer | Tests unitaires |

### Signatures

```typescript
// Fonction à implémenter
function exemple(
  param1: string,
  param2: number
): Promise<Resultat>
```

### Contraintes techniques
- **Framework** : Utiliser [framework] (version X)
- **Pattern** : Suivre le pattern [XXX] déjà utilisé dans [fichier de référence]
- **Style** : Respecter la config ESLint et Prettier du projet
- **Performance** : [contrainte si applicable]
- **Sécurité** : [contrainte si applicable]
- **Tests** : Coverage minimum de 80% pour cette tâche

### Tests à implémenter

#### Tests unitaires
- **Fichier** : `src/feature/__tests__/exemple.test.ts`
- Scénario 1 : [Description]
  - Données : [Input]
  - Résultat attendu : [Output]
- Scénario 2 : [Description]
  - Données : [Input]
  - Résultat attendu : [Output]

#### Tests d'intégration
- **Fichier** : `src/feature/__tests__/integration.test.ts`
- Scénario : [Description]

### Critère de fin — tests verts (obligatoire)

Avant de considérer la tâche terminée :

1. **Régression** : toute la suite listée ci-dessous passe **sans échec** ; aucun test existant désactivé ou supprimé pour masquer une régression.
2. **Commandes** (depuis `crbpm/`) :
   - `.\gradlew :module-a:test :module-b:test` — adapter aux modules touchés
   - `.\gradlew :backend:build -Pcrbpm.engines=wrapper,c8` — si profils engines applicables
3. **Périmètre** : [modules, profils Gradle, live ITs gated si concernés]
4. **Migration** : [si déplacement de code/tests — exiger colocalisation code + tests dans la même tâche]
5. **Clôture** : renommer ce fichier en `task-YYY-done.md` (et `task-YYY-examples.http` → `task-YYY-examples-done.http` si présent) **uniquement** après validation des tests ci-dessus.

### Documentation

#### Documentation à créer
- `docs/feature/README.md` : Documentation de la nouvelle fonctionnalité
- `docs/feature/exemples.md` : Exemples d'utilisation

#### Documentation à mettre à jour
- `docs/api/README.md` : Ajouter la section sur le nouvel endpoint

### Exemples d'API (si applicable)
- Fichier : `tmp/<id>/story/task-YYY-examples.http`

Voir les exemples de requêtes dans le fichier .http associé.
```

### Format du fichier questions.md

```markdown
# Questions de clarification - Story #XXX

## Contexte global
- Story #XXX : [Titre] (`tmp/<id>/story.md`)
- Tâches générées : [liste avec titres]
- Point d'attention : [ambiguïtés / incohérences principales]

## Questions

### 1. [Tâche #002] — Format des erreurs API
**Enjeu :** Le contrat d'erreur est référencé par #002 (endpoints) et #003 (tests + `.http`) ; un choix tardif force à réécrire les deux prompts.
**Contexte :**
- Story : « erreurs structurées » sans schéma précis.
- Tâche #002 propose un body d'erreur ; le module API existant utilise déjà [citer le pattern réel du repo, ex. `WRP-AREA-NNNN` + `traceId`].
- Aucune décision écrite sur JSON:API vs envelope maison.
**Dépendances :** Signatures/DTO de #002 ; assertions de #003 ; exemples `.http`.
**Question :** Quel format de réponse d'erreur pour les endpoints de cette story ?
**Propositions :**
- a) **Alignement CRBPM existant** (`code`, `message`, `traceId`, HTTP status) — Implications : réutiliser les types/handlers existants ; #002/#003 citent les mêmes champs ; pas de JSON:API.
- b) **JSON:API errors** — Implications : nouveau contrat ; migration ou dual-stack à clarifier ; plus de travail dans #002 et tests.
- c) Autre : coller un exemple JSON attendu pour 400 et 404.
**Réponse :**

### 2. [Incohérence #001 vs #002] — Nom du DTO de création
**Enjeu :** #002 importe un type que #001 nomme autrement → échec de compilation entre tâches « vertes ».
**Contexte :**
- Tâche #001 définit `CreateTaskRequest`.
- Tâche #002 référence `NewTaskDto` dans ses signatures.
- Package cible : `com.moodys.crbpm.…` [chemin].
**Dépendances :** Signatures #001/#002 ; tests des deux tâches.
**Question :** Quel nom canonique pour le DTO de création ?
**Propositions :**
- a) **`CreateTaskRequest`** — Aligner #002 et ses tests sur #001.
- b) **`NewTaskDto`** — Renommer dans #001 (et tests #001) avant #002.
- c) Autre : nom + justification (convention du module).
**Réponse :**

### 3. [Ambiguïté story] — Périmètre des profils Gradle engines
**Enjeu :** Sans profils explicites, le critère « tests verts » de chaque tâche est ambigu (wrapper seul vs wrapper,c8).
**Contexte :** Story touche éventuellement `engine-spi` / `engine-c8` ; défaut repo documenté dans AGENTS.md.
**Dépendances :** Section « Critère de fin — tests verts » de chaque tâche.
**Question :** Quels profils `-Pcrbpm.engines=…` sont obligatoires à la clôture de #001, #002, #003 ?
**Propositions :**
- a) **Défaut rétrocompatible** (`wrapper,c8` ou défaut projet) pour toutes les tâches jusqu'à une tâche « wrapper-only » dédiée.
- b) **Profil minimal dès #001** — Préciser la matrice tâche × profil.
- c) Autre : tableau tâche → commandes Gradle exactes.
**Réponse :**
```

## Comment passer d'une étape à l'autre

| Transition | Action de l'utilisateur | Action de l'agent |
|------------|------------------------|-------------------|
| Étape 1 | Donner le numéro de story (ex: `/ tasks 001`) | Lit la story, le contexte projet |
| Étape 1 → 2 | Automatique | Découpe et génère les prompts détaillés |
| Étape 2 → 3 | Automatique | Vérifie la cohérence des tâches |
| Étape 3 → 4 | Automatique | Crée `questions.md` avec les points à clarifier |
| Étape 4 → 5 | Répondre dans `questions.md` puis taper `J'ai répondu, mets à jour les tâches` | Met à jour les tâches selon les réponses |
| Étape 5 → 3 | Automatique | Re-vérifie la cohérence ; **régénère `questions.md` s'il reste des ambiguïtés** |
| Étape 5 → 6 | Automatique (plus de questions **après relecture**) | Supprime `questions.md`, présente le plan final |

**Important :** L'agent ne surveille pas les modifications de fichiers en temps réel. L'utilisateur doit donner une instruction explicite pour passer à l'étape suivante.

## Exemple d'utilisation

**Lancement :**
```
/ tasks 001
```

**Lecture de `tmp/001/story.md` :**
Story décrivant la création d'une API REST de gestion de tâches avec création, lecture, mise à jour et suppression.

**Découpage en 3 tâches avec prompts détaillés :**
- `tmp/001/story/task-001.md` : Modèle de données, base et validations
- `tmp/001/story/task-002.md` : Endpoints CRUD complets avec signatures et gestion d'erreurs
- `tmp/001/story/task-003.md` : Tests automatisés, documentation et fichiers `.http`

**Vérification de cohérence :**
- Tâche #002 référence un type défini dans tâche #001 → OK
- Tâche #003 couvre tous les cas de test des critères d'acceptation → OK
- Gestion d'erreurs : tâche #002 et spécification alignées → OK
- Chaque tâche inclut un critère de fin « tests verts » avec commandes Gradle → OK
- Aucune tâche ne laisse la suite en rouge entre deux livrables → OK

**Création de `tmp/001/story/questions.md` :**
- Question sur le format de retour des erreurs (JSON API vs standard)
- Question sur l'ordre d'exécution des tâches (tâche #001 doit précéder #002)

**Mise à jour :**
L'utilisateur répond, l'agent met à jour les tâches, itération jusqu'à résolution complète.

**Exécution (agent de code) :**
Après implémentation de la tâche #001 et tests verts :
- `tmp/001/story/task-001.md` → `tmp/001/story/task-001-done.md`
- Passer à `task-002.md` (sans `-done`) une fois les dépendances satisfaites.

---
name: tasks-fr
description: Découpage pragmatique de User Stories en tâches "prompt" exécutables ; alignement sur l'existant, hypothèses et questions uniquement bloquantes
---

# Skill: Tasks (FR)

Ce guide décrit comment découper une story en tâches "prompt" détaillées, prêtes à être exécutées par un agent de code, avec tests, documentation et exemples d'API. L'objectif est d'obtenir des prompts exécutables sans cycles de questionnement interminables.

## Qu'est-ce que je fais

Je prends une story existante, j'inspecte le codebase pour m'aligner sur les conventions en vigueur, je propose un découpage avec hypothèses intégrées et ne pose que des questions **réellement bloquantes** pour produire des tâches avec critère "tests verts" satisfaisable.

## Quand m'utiliser

Utilisez cette skill après avoir créé vos user stories avec `story-fr`, quand vous voulez générer des prompts prêts à exécuter.

## Organisation des fichiers (`docs/stories/`)

```text
docs/stories/
  <id>/
    story.md                    # user story (source)
    story/
      task-001.md               # tâche en attente
      task-001-done.md          # tâche terminée (renommée)
      task-002.md
      task-001-examples.http
      task-001-examples-done.http
      questions.md              # questions BLOQUANTES uniquement (supprimé à la fin)
      assumptions.md            # hypothèses/conventions (supprimé à la fin)
```

| Fichier | Rôle |
|---------|------|
| `docs/stories/<id>/story.md` | Story lue en entrée |
| `docs/stories/<id>/story/task-YYY.md` | Prompt détaillé — **en attente** |
| `docs/stories/<id>/story/task-YYY-done.md` | Prompt détaillé — **terminée** (suffixe `-done` avant l'extension) |
| `docs/stories/<id>/story/questions.md` | Questions **bloquantes** uniquement |
| `docs/stories/<id>/story/assumptions.md` | Conventions inférées/appliquées (naming, erreurs, tests, structure) |

**Convention `-done` :** renommer le fichier prompt et les fichiers associés (`task-YYY-examples.http` → `task-YYY-examples-done.http`) **uniquement après** validation des tests verts. Ne pas supprimer le contenu.

## Workflow

### Étape 0 - Inspection & alignement sur l'existant
- Normaliser l'id story (`1` → `001`)
- Lire `docs/stories/<id>/story.md`, `docs/stories/plan-implementation.md` (si présent), la spécification
- **Inspecter le codebase** autour des zones impactées : patterns de nommage, gestion d'erreurs, structure DTO/Service/Controller, organisation des tests, conventions Gradle
- **Si deux patterns concurrents** existent, ils seront abordés uniquement s'ils créent une incohérence bloquante. Sinon, **aligner sur le pattern majoritairement utilisé**

### Étape 1 - Génération directe des tâches
- Découper la story en unités atomiques livrables **avec tests verts**
- Pour chaque tâche, générer un prompt détaillé avec : objectif, contexte, fichiers, signatures, contraintes, tests à implémenter, **critère de fin "tests verts"** avec commandes exactes, documentation, exemples `.http` si API
- **Appliquer les conventions inférées** directement (noms de classes/fichiers, format d'erreurs, placement des tests)
- Créer `docs/stories/<id>/story/` et générer `task-YYY.md`

### Étape 2 - Vérification de cohérence ciblée
- Vérifier dépendances, ordonnancement, couverture des critères d'acceptation
- Vérifier cohérence des signatures entre tâches
- Vérifier qu'aucune tâche ne laisse la suite en rouge
- **Vérifier que chaque tâche contient un critère "tests verts" concret**

### Étape 3 - Hypothèses vs blocages
- Lister les conventions appliquées dans `assumptions.md` avec `[INFÉRÉ - APPLIQUÉ]` ou `[PROPOSÉ - APPLIQUÉ]`
- N'émettre des questions dans `questions.md` que si un élément est **réellement bloquant** pour écrire un prompt exécutable ou garantir des "tests verts" :
  - Dépendance circulaire non résolvable
  - Contrat entre tâches non déductible et incohérent avec le code existant
  - Ambiguïté rendant impossible de déterminer les modules/commandes de test à exécuter
- **Si aucun blocage** : aller directement à l'étape 4

### Étape 4 - Clarification minimale
- L'utilisateur peut répondre : `"J'ai répondu, mets à jour les tâches"`, `"Accepte toutes les hypothèses"`, `"Applique mes directives : ..."`, ou retour ciblé
- Mettre à jour uniquement les tâches **sans** `-done`
- **Maximum 2 rounds** de questions bloquantes. Au-delà, assumer intelligemment et présenter le plan final

### Étape 5 - Clôture
- Supprimer `questions.md` et `assumptions.md` (s'ils existent)
- Présenter le plan d'exécution (ordre, dépendances, commandes type)
- Annoncer prêt pour exécution par agent de code

## Règle : clarification minimale (obligatoire)

- **Aligner sur l'existant en priorité** : si un pattern identique existe dans le repo, l'utiliser sans poser de question
- **Questions uniquement bloquantes** : uniquement ce qui empêche un prompt exécutable avec critère "tests verts" satisfaisable
- **Préférer hypothèses** : conventions, nommage, organisation → `assumptions.md`
- **Critère d'arrêt** : *"Suffisant pour générer des prompts exécutables permettant d'obtenir des tests verts à chaque tâche"* — pas "zéro doute"
- **Max 2 rounds** : limiter strictement les itérations
- **Accepter directives** : appliquer immédiatement

## Règle : qualité des questions (adaptable)

### Format condensé (préféré)
```markdown
### Q[X] - [Tâche/Contexte] - [Décision bloquante]
**Enjeu :** Impact sur exécution / tests verts
**Question :** [UNE décision]
**Propositions :**
- a) [Option 1] — Comportement + implications (fichiers/tests/ordre)
- b) [Option 2] — Comportement + implications
- c) Autre : [préciser]
**Réponse :**
```

### Format complet (seulement impasse complexe)
Inclure : **Enjeu**, **Contexte** (extraits tâche/story/code), **Dépendances**, **Question**, **Propositions** détaillées, **Réponse**. Citer `Tâche #YYY`, chemins, signatures si utile.

**Interdits** : préférences esthétiques sans impact, options nues, multiples décisions.

## Principe : tests verts entre chaque tâche

- Chaque tâche doit être **mergeable** avec suite verte à la fin
- **Déplacement atomique** : code + tests dans la même tâche
- **Refactor transversal** : tous les appels/tests mis à jour dans la même tâche
- **Pas de contournement** : jamais désactiver/supprimer des tests pour masquer une régression
- **Critère obligatoire** dans chaque `task-YYY.md` avec commandes exactes par modules/profils

## Exécution et clôture (agent de code)

1. Prendre la prochaine tâche **sans** `-done`, respecter l'ordre des dépendances
2. Suivre le prompt, valider avec les commandes du "Critère de fin — tests verts"
3. **Clôturer uniquement si tests verts** : renommer `task-YYY.md` → `task-YYY-done.md` et `task-YYY-examples.http` → `task-YYY-examples-done.http`
4. Ne jamais renommer en cas d'échec

**Lors de mise à jour (étape 4)** : ne modifier que les fichiers **sans** `-done`

## Format des prompts (task-YYY.md)

```markdown
# Tâche #YYY - Story #XXX : [Titre]

## Objectif
[Ce que cette tâche doit livrer]

## Contexte
- Story #XXX : `docs/stories/<id>/story.md`
- Dépend de : Tâche #ZZZ (si applicable)
- Nécessaire pour : Tâche #WWW (si applicable)
- Aligné sur : [référence vers pattern existant dans le repo]

## Prompt
[Instructions détaillées pour l'agent de code]

### Fichiers concernés
| Fichier | Action | Description |
|---------|--------|-------------|
| ... | Créer/Modifier/Supprimer | ... |

### Signatures / Structures
[Types, DTO, interfaces, signatures clés]

### Tests à implémenter
[Scénarios unitaires/intégration/E2E concrets]

### Critère de fin — tests verts (obligatoire)
1. Commandes depuis `crbpm/` (adapter aux modules touchés)
   - `.\gradlew :module:test` ...
2. Aucune régression, aucun test désactivé/supprimé
3. Si déplacement/refactor : colocalisation code+tests
4. Clôture : renommer en `task-YYY-done.md` (+ `.http` associé) après validation

### Documentation
[À créer / à mettre à jour]

### Exemples d'API (si applicable)
`docs/stories/<id>/story/task-YYY-examples.http`
```

## assumptions.md (exemple)
```markdown
# Hypothèses & Conventions - Tâches Story #XXX

## Conventions inférées (appliquées)
- [INFÉRÉ - APPLIQUÉ] Nommage DTO : `*Request`/`*Response` (pattern observé dans `...`) 
- [INFÉRÉ - APPLIQUÉ] Gestion d'erreurs : aligné sur `ErrorResponse` existant
- [INFÉRÉ - APPLIQUÉ] Tests : placement `src/test/...` ou `src/integrationTest/...` selon module

## Choix proposés (appliqués avec justification)
- [PROPOSÉ - APPLIQUÉ] Découpage : [justification par dépendances]
- [PROPOSÉ - APPLIQUÉ] Ordre : [justification]

## À valider uniquement si impact bloquant
- [À VALIDER - BLOQUANT] [uniquement en cas de vraie impasse]
```

## questions.md (uniquement bloquant)
```markdown
# Questions BLOQUANTES - Story #XXX

## Contexte global
- Story : [titre]
- Tâches générées : [liste]
- Codebase aligné sur : [patterns identifiés]
- Round : [1/2]

## Questions bloquantes
### 1. [Tâche #XXX / Incohérence] — [Titre]
**Enjeu :** [Pourquoi bloque un prompt exécutable ou des tests verts]
**Contexte :** [Extraits pertinents]
**Dépendances :** [Tâches/signatures/modules]
**Question :** [UNE décision]
**Propositions :**
- a) [...]
- b) [...]
- c) Autre : [...]
**Réponse :**
```

## Comment passer d'une étape à l'autre

| Transition | Action utilisateur | Action agent |
|------------|--------------------|--------------|
| Début | `/ tasks-fr <id>` | Inspecte story + codebase, génère tâches alignées + assumptions |
| Après génération | `"Accepte toutes les hypothèses"` OU répondre questions bloquantes OU `"Applique mes directives : ..."` | Met à jour si réponses/directives |
| Mise à jour | `"J'ai répondu, mets à jour les tâches"` | Met à jour uniquement tâches sans `-done` ; re-vérifie (max 2 rounds) |
| Fin | Automatique si pas de blocages | Supprime questions.md/assumptions.md, présente plan final |

**Important :** Ne pas modifier les tâches avec `-done`. Toujours donner une instruction explicite.

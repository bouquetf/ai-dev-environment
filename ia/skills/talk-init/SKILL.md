---
name: talk-init
description: Initialise le cadrage d'une présentation dans un document général, anime un brainstorming itératif via questions.md, et conserve les idées hors décision dans feedback.md. À utiliser explicitement au début d'une présentation, avant le plan, les slides, les assets ou la génération Marp.
disable-model-invocation: true
---

# Talk init

Cette skill cadre une présentation avant toute conception de slides. Elle s'inspire du cycle de clarification de `specification-fr`, mais reste au niveau du sujet, de l'audience et des thèmes à aborder.

## Limites

- Ne pas créer le plan détaillé, les slides, les notes d'orateur, les assets ou le PDF.
- Ne pas installer ni exécuter Marp.
- Ne pas rechercher ou produire les preuves détaillées des futurs messages.
- Ne pas prendre une décision éditoriale importante à la place de l'utilisateur.

## Entrée

L'utilisateur fournit un répertoire de présentation, par exemple :

```text
docs/presentations/skill-based-dev-system/
```

S'il ne le fournit pas, demander le chemin avant de créer des fichiers.

Respecter les règles de langue du dépôt. Dans CRBPM, la conversation peut être en français, mais les fichiers sous `docs/` sont en anglais.

## Fichiers

Créer et maintenir dans le répertoire indiqué :

```text
presentation-brief.md
questions.md
feedback.md
```

`presentation-brief.md` est le document général durable. `questions.md` contient uniquement le cycle de clarification en cours. `feedback.md` garde chaque idée et son état.

Reprendre `feedback.md` s'il existe : ajouter les nouvelles idées, ne pas réécrire ni supprimer les entrées déjà là.

## État de feedback.md

Le statut fait foi, pas le titre de section.

- `new` — pas encore triée. Elle reste sous `## Open`.
- `held` — triée, pas incluse. Elle reste sous `## Open`. Une question ou une note dit pourquoi.
- `accepted` — incluse. Elle va sous `## Resolved`. La note nomme le fichier de destination et l'endroit précis.

À chaque passage :

- lire le statut de chaque entrée ;
- pour `accepted`, vérifier que l'idée est encore dans la destination notée. Si elle y est, ne pas la reposer et ne pas la modifier. Si elle a disparu, la remettre à cet endroit. Si la note n'a pas de destination, poser une question ;
- pour `held` ou `new`, ne pas les traiter comme incluses ;
- quand une entrée passe à `accepted`, la déplacer sous `## Resolved` sans changer le texte de l'idée ;
- avant de terminer, dire le compte : `accepted`, `held`, `new`. Ne pas annoncer que les feedbacks sont inclus tant qu'une entrée `new` ou `held` existe.

## Workflow

### 1. Lire l'existant

- Lire `presentation-brief.md`, `questions.md` et `feedback.md` s'ils existent.
- Lire uniquement les documents explicitement fournis par l'utilisateur à ce stade.
- Ne pas inspecter le code ou collecter des sources sans demande explicite.
- Ne pas traiter les entrées de `feedback.md` comme des réponses. Une entrée `accepted` est déjà incluse : la vérifier, ne pas la reposer. Intégrer une entrée `new` ou `held` au brief seulement si l'utilisateur le demande, ou selon la règle de destination ci-dessous.

### 2. Initialiser le brief

Créer ou mettre à jour `presentation-brief.md` avec les faits connus et des marqueurs `[TO CLARIFY: ...]` pour le reste.

Le brief couvre au minimum :

1. **Working title**
2. **Purpose** — pourquoi cette présentation existe
3. **Audience** — rôles, niveau technique, connaissance préalable
4. **General subject** — périmètre en une formulation courte
5. **Expected format** — durée, langue, présentiel/distanciel, questions, démo éventuelle, format de livraison
6. **Desired outcomes** — ce que l'audience doit comprendre, décider ou essayer
7. **Core message** — une phrase que l'audience doit retenir
8. **High-level topics** — thèmes possibles, sans ordre de slides
9. **Out of scope**
10. **Constraints and available material**

Ne pas confondre **high-level topics** avec un sommaire : à ce stade, ce sont des candidats issus du brainstorming.

### 3. Créer ou reprendre le feedback

- Si `feedback.md` n'existe pas, le créer avec le modèle ci-dessous, sans idée inventée.
- S'il existe, le conserver tel quel, puis y ajouter seulement les idées nouvelles.
- Écrire les entrées en anglais lorsque le fichier est sous `docs/`.
- Chaque entrée contient l'idée, pourquoi elle compte, une destination proposée (`brief` ou `later`) et un statut (`new`, `accepted` ou `held`).
- Une idée nouvelle arrive sous `## Open` avec le statut `new`.

Quand l'utilisateur apporte une idée en dehors des questions en cours :

- l'ajouter à `feedback.md` avec le statut `new` ;
- ne pas modifier le brief si l'utilisateur n'a pas demandé de choisir une destination ;
- s'il demande si l'idée va dans le brief ou plus tard, recommander dans le chat, puis :
  - intégrer au brief ce qui précise l'ouverture ou le message sans ajouter de thème ni rouvrir un arbitrage de format déjà clos, passer l'entrée à `accepted`, et la déplacer sous `## Resolved` ;
  - laisser en `later` ce qui ajouterait un thème, une pratique ou un temps de parole au-delà du cadrage figé, et garder le statut `held` sous `## Open`.

### 4. Générer le brainstorming dans questions.md

Écrire uniquement les questions qui influencent réellement le cadrage. Regrouper les questions par :

- audience et contexte ;
- objectif et message central ;
- format et contraintes ;
- thèmes candidats ;
- arbitrages de périmètre.

Chaque question doit pouvoir être comprise sans relire toute la conversation et contenir :

- **Stake** — pourquoi la réponse compte ;
- **Context** — ce qui est déjà connu ;
- **Dependencies** — ce que la réponse orientera ensuite ;
- **Question** — une seule décision ;
- **Proposals** — au moins deux options concrètes, plus `Other` ;
- **Answer** — emplacement laissé vide.

Pour le brainstorming des sujets, proposer une liste initiale fondée sur le sujet général, puis demander à l'utilisateur de :

- garder les thèmes essentiels ;
- écarter les thèmes hors périmètre ;
- ajouter les thèmes manquants ;
- identifier le thème sur lequel passer le plus de temps.

Ne pas transformer ces thèmes en titres de slides.

### 5. Attendre

Après avoir écrit `questions.md` :

- résumer en chat les décisions déjà connues ;
- indiquer les questions qui bloquent la suite ;
- demander à l'utilisateur de remplir les réponses puis de dire explicitement de continuer ;
- s'arrêter. Ne pas surveiller le fichier et ne pas poursuivre automatiquement.

### 6. Itérer après la réponse de l'utilisateur

Quand l'utilisateur demande de continuer :

1. lire les réponses dans `questions.md` ;
2. relire `feedback.md` : vérifier les entrées `accepted`, et ne pas promouvoir les entrées `new` ou `held` ;
3. intégrer les décisions dans `presentation-brief.md` ;
4. relire le brief pour détecter ambiguïtés, contradictions et thèmes trop larges ;
5. régénérer `questions.md` avec uniquement les questions encore utiles ;
6. attendre de nouveau s'il reste une question.

Ne pas considérer une réponse vague comme une décision. Poser une question de suivi si elle change le message, le périmètre ou le format.

### 7. Clôturer

Le cadrage est terminé uniquement lorsque :

- audience, sujet général et format attendu sont explicites ;
- le but et le message central sont cohérents ;
- les thèmes de haut niveau sont sélectionnés ;
- le hors-périmètre est visible ;
- aucune question utile ne reste.

Supprimer alors `questions.md`. Conserver `feedback.md`. Annoncer le compte `accepted` / `held` / `new`. Si tout est `accepted`, dire que chaque idée est incluse à l'endroit noté et qu'une skill ultérieure ne doit pas les rouvrir. S'il reste du `held` ou du `new`, nommer ces entrées. Annoncer que `presentation-brief.md` est prêt pour `talk-slides`. Ne pas créer cette structure dans `talk-init`.

## Modèle de presentation-brief.md

```markdown
# Presentation brief — [Working title]

## Purpose
[Why this talk exists]

## Audience
- Roles:
- Technical level:
- Prior knowledge:

## General subject
[One short statement]

## Expected format
- Duration:
- Language:
- Setting:
- Interaction:
- Deliverables:

## Desired outcomes
- [Outcome]

## Core message
[One sentence]

## High-level topics
- [Candidate or selected topic]

## Out of scope
- [Boundary]

## Constraints and available material
- [Constraint or source material]
```

## Modèle de question

```markdown
### 1. [Area] — [Decision]

**Stake:** [Why this answer changes the presentation.]

**Context:**
- Already known: [...]
- Still unclear: [...]

**Dependencies:** [What this answer will shape next.]

**Question:** [One precise decision.]

**Proposals:**
- a) **[Option]** — [Resulting presentation and implications.]
- b) **[Option]** — [Resulting presentation and implications.]
- c) **Other** — [What the user must specify.]

**Answer:**
```

## Modèle de feedback.md

```markdown
# Feedback — [Working title]

`## Open` holds entries with status `new` or `held`. `## Resolved` holds entries with status `accepted`. Append new ideas under Open. Do not rewrite an existing idea.

## Open

### [Short title]

- Idea:
- Why it matters:
- Proposed destination: brief | later
- Status: new
- Note:

## Resolved
```

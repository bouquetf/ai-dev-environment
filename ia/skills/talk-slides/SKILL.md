---
name: talk-slides
description: Découpe une présentation déjà cadrée en slides à partir de presentation-brief.md et des feedbacks. Une idée par slide, trois axes maximum, trois développements maximum par axe. Centralise questions et retours libres dans questions.md, puis met à jour le brief ou le découpage. À utiliser quand l'utilisateur demande le découpage en slides, le plan de slides, ou talk-slides, après talk-init.
disable-model-invocation: true
---

# Talk slides

Cette skill découpe une présentation déjà cadrée en slides. Elle consomme `presentation-brief.md` et les feedbacks déjà écrits. Elle ne produit ni Marp, ni notes d'orateur, ni PDF.

## Limites

- Ne pas créer le fichier Marp, les notes d'orateur, les assets ou le PDF.
- Ne pas installer ni exécuter Marp.
- Ne pas inspecter le code ni collecter des sources sans demande explicite.
- Ne pas ajouter un axe, un développement ou une idée que le brief n'a pas retenu pour faire tenir un feedback.
- Ne pas réécrire le texte écrit par l'utilisateur dans `questions.md`.

## Sens des limites

- **Axe** — une grande partie du talk. Le découpage en compte au plus trois.
- **Développement** — un mouvement sous un axe. Chaque axe en compte au plus trois. Chaque développement est exactement une slide.
- **Idée générale** — la seule phrase que cette slide doit faire passer.

Une slide peut afficher au plus trois supports courts, tous au service de son idée générale. Un quatrième support, ou une seconde idée, est une autre slide. Cette slide doit encore tenir dans les trois développements de son axe. Si elle ne tient pas, demander ce qui sort.

Deux slides de cadre sont permises en dehors des trois axes : le titre, et la clôture. Chacune a une seule idée générale et aucun développement. Une ouverture qui porte un argument prend un développement dans un axe.

## Entrée

L'utilisateur fournit un répertoire de présentation, par exemple :

```text
docs/presentations/skill-based-dev-system/
```

S'il ne le fournit pas, demander le chemin avant de créer des fichiers.

Le répertoire doit contenir `presentation-brief.md`. S'il manque, s'arrêter et demander de cadrer d'abord la présentation. Ne pas inventer de brief.

Respecter les règles de langue du dépôt. Dans CRBPM, la conversation peut être en français, mais les fichiers sous `docs/` sont en anglais.

## Fichiers

Dans le répertoire indiqué :

```text
presentation-brief.md
feedback.md
slide-outline.md
questions.md
```

- `presentation-brief.md` reste le cadrage. Le mettre à jour seulement quand un feedback change l'audience, le but, le message, le format, le périmètre ou le hors-périmètre.
- `feedback.md` garde chaque idée et son état. Le lire. Ne pas en réécrire les idées. On peut changer le statut, la note de destination, et déplacer l'entrée entre `## Open` et `## Resolved`.
- `slide-outline.md` est le découpage durable.
- `questions.md` est le seul canal avec l'utilisateur : questions de clarification, et retours libres hors questions.

## État de feedback.md

Le statut fait foi, pas le titre de section. Même règle que `talk-init` :

- `new` — pas encore triée, sous `## Open` ;
- `held` — pas incluse, sous `## Open` ;
- `accepted` — incluse, sous `## Resolved`, avec une note qui nomme la destination.

À chaque passage :

- lire le statut de chaque entrée avant de découper ;
- pour `accepted`, vérifier que l'idée est encore dans la destination notée. Si elle y est, ne pas la reposer et ne pas la modifier. Si elle a disparu, la remettre à cet endroit ;
- pour `held` ou `new`, ne pas les traiter comme incluses ;
- quand une entrée passe à `accepted`, la déplacer sous `## Resolved` sans changer le texte de l'idée ;
- avant de terminer, dire le compte : `accepted`, `held`, `new`. Ne pas clôturer tant qu'une entrée `new` existe. Une entrée `held` ne reste que si une question est ouverte.

## Feedbacks

Lire les deux sources :

- chaque entrée de `feedback.md`, en commençant par les `accepted` pour vérifier qu'elles sont toujours présentes ;
- la section `## Feedback` de `questions.md`, y compris les lignes sans réponse à une question.

Pour chaque retour qui n'est pas déjà `accepted` :

- S'il change l'audience, le but, le message central, le format, le périmètre ou le hors-périmètre, mettre à jour `presentation-brief.md`, puis aligner `slide-outline.md`.
- S'il change l'ordre, l'accent, l'idée d'une slide, un axe ou un développement, mettre à jour seulement `slide-outline.md`.
- S'il peut aller dans les deux, s'il contredit le brief, ou s'il est trop vague pour choisir, ne rien trancher. Écrire une question.
- S'il ajoute un thème au-delà des trois axes ou des trois développements, demander ce qui sort. Ne pas dépasser les limites en silence.

Après intégration, noter la destination sans modifier le texte de l'idée :

- dans `feedback.md`, statut `accepted`, une note qui nomme `brief` ou `slide-outline` et l'endroit précis, puis déplacer l'entrée sous `## Resolved` ;
- dans `questions.md`, une ligne `Agent:` sous le retour de l'utilisateur.

Laisser un retour non intégré sous `## Open`, avec le statut `held`, tant qu'une question le concerne.

## Workflow

### 1. Lire

Lire `presentation-brief.md`, `feedback.md`, `slide-outline.md` et `questions.md` s'ils existent.

Si le brief contient encore un marqueur `[TO CLARIFY]`, le traiter comme une question. Ne pas découper comme si le cadrage était clos.

### 2. Intégrer les retours déjà clairs

Appliquer la règle des feedbacks. Ne pas traiter un retour vague comme une décision.

### 3. Découper

Créer ou mettre à jour `slide-outline.md`.

- Couvrir chaque thème retenu du brief par un développement, par une slide de cadre, ou par une ligne explicite dans `Not on slides`.
- Laisser hors slides tout ce que le brief marque hors périmètre.
- Respecter la durée et le format du brief. Si les développements ne tiennent pas dans ce format, demander quoi fusionner ou retirer.
- Reprendre un découpage existant. Ne pas en écarter une slide déjà confirmée sans un retour ou une contradiction.

### 4. Chercher ce qui bloque

Avant d'écrire les questions, relire le brief, les feedbacks et le découpage ensemble. Bloquer sur :

- une contradiction entre le brief et le découpage ;
- une idée de slide qui en contient deux ;
- un quatrième axe ou un quatrième développement ;
- un thème retenu qui n'apparaît nulle part ;
- un élément hors périmètre qui a une slide ;
- un feedback dont la destination ou le sens reste ambigu ;
- une entrée `accepted` absente de la destination notée ;
- une entrée `new`.

S'il n'y a aucun blocage, aller à la clôture.

### 5. Écrire questions.md

Conserver tel quel le texte de l'utilisateur dans `## Feedback`. On peut ajouter une ligne `Agent:` sous un retour. On ne modifie pas ses phrases.

Remplacer seulement `## Questions`. N'y mettre que les questions qui changent encore le brief ou le découpage.

Chaque question doit pouvoir être comprise sans relire la conversation et contenir :

- **Stake** — pourquoi la réponse change le découpage ;
- **Context** — ce qui est déjà connu ;
- **Dependencies** — ce que la réponse modifiera ;
- **Question** — une seule décision ;
- **Proposals** — au moins deux options concrètes, plus `Other` ;
- **Answer** — emplacement laissé vide.

Puis :

- résumer en chat les choix déjà posés dans le découpage ;
- indiquer les questions qui bloquent ;
- dire que les retours hors questions se mettent dans `## Feedback` ;
- demander de répondre, puis de dire explicitement de continuer ;
- s'arrêter. Ne pas surveiller le fichier et ne pas poursuivre automatiquement.

### 6. Continuer

Quand l'utilisateur demande de continuer :

1. lire les réponses sous chaque `Answer:` ;
2. lire aussi `## Feedback`, y compris les retours qui ne répondent à aucune question ;
3. relire `feedback.md` selon son état : vérifier les `accepted`, traiter les `new` et les `held` ;
4. intégrer ce qui est clair dans le brief ou le découpage ;
5. recontrôler les limites et les contradictions ;
6. réécrire `## Questions` avec seulement ce qui bloque encore ;
7. attendre de nouveau s'il reste une question.

Une réponse vague n'est pas une décision. Poser une question de suivi si elle change l'idée d'une slide, un axe, ou le brief.

### 7. Clôturer

Le découpage est terminé uniquement lorsque :

- il y a au plus trois axes et trois développements par axe ;
- chaque développement est une slide avec une seule idée générale ;
- chaque thème retenu du brief est une slide ou une ligne de `Not on slides` ;
- le hors-périmètre du brief n'a pas de slide ;
- chaque entrée `accepted` est présente dans la destination notée ;
- chaque entrée `held` a une question ouverte, et aucune entrée `new` ne reste ;
- aucune question utile ne reste.

Laisser `questions.md` en place. Vider `## Questions` en indiquant qu'aucune question n'est ouverte. Conserver `## Feedback`.

Annoncer le compte `accepted` / `held` / `new`. Si tout est `accepted`, dire que chaque idée est incluse à l'endroit noté. Annoncer que `slide-outline.md` est prêt pour `talk-deck`. Ne pas créer le deck ici. Dire que de nouveaux retours peuvent être ajoutés dans `## Feedback` de `questions.md` ou sous `## Open` de `feedback.md`, avec le statut `new`, puis que l'utilisateur peut redemander cette skill.

## Modèle de slide-outline.md

```markdown
# Slide outline — [Working title]

## Rules applied
- One general idea per slide.
- At most 3 axes.
- At most 3 developments per axis.
- Each development is one slide.
- At most 3 short supports on a slide, all serving that one idea.

## Framing slides

### Title
- General idea: [One sentence.]
- Supports: [Up to 3, or none.]

### Close
- General idea: [One sentence.]
- Supports: [Up to 3, or none.]

## Axis 1 — [Name]

### 1. [Development name]
- General idea: [One sentence.]
- Supports: [Up to 3 short supports.]
- Source: [Brief topic or feedback entry.]

## Not on slides
- [Brief item or feedback kept out, and why.]

## Check
- Axes: [n]/3
- Developments: [per axis]
- One idea per slide: [yes or the slide that breaks the rule]
- Brief topics covered: [list]
- Feedback state: [n accepted, n held, n new. Each accepted entry is present in its noted destination.]
- Out of scope absent: [yes]
```

## Modèle de questions.md

```markdown
# Questions — [Working title]

Answer under each **Answer:** heading.
Write any other comment under **Feedback**. Those notes are not answers.
The agent does not rewrite your sentences. It may add a line that starts with `Agent:`.
When you are done, say explicitly to continue.

## Feedback

-

## Questions

### 1. [Area] — [Decision]

**Stake:** [Why this answer changes the outline or the brief.]

**Context:**
- Already known: [...]
- Still unclear: [...]

**Dependencies:** [What this answer will change.]

**Question:** [One precise decision.]

**Proposals:**
- a) **[Option]** — [What changes in the brief or the outline.]
- b) **[Option]** — [What changes in the brief or the outline.]
- c) **Other** — [What the user must specify.]

**Answer:**
```

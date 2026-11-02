---
name: feedback-fr
description: Génération de stories à partir de feedbacks utilisateurs, en filtrant ceux déjà traités ; pose uniquement des questions bloquantes, avec au plus une vague de suivi
---

# Skill: Feedback → Stories / Spécification (FR)

Ce guide décrit comment transformer des feedbacks utilisateurs en user stories exploitables, en ne traitant que les retours les plus récents qui n'ont pas encore été pris en compte dans les stories existantes.

## Qu'est-ce que je fais

Je prends le fichier `docs/specification/feedbacks.md`, j'identifie les feedbacks qui n'ont pas encore été traités. Je **regroupe d'abord les feedbacks par thème** — plusieurs feedbacks sur un même sujet donnent une seule unité de travail. Pour chaque groupe :
- **Si le sujet est simple** → je génère une ou plusieurs stories directement (au même format que story-fr) avec une clarification bornée aux décisions bloquantes.
- **Si le sujet est complexe** (nouvelle fonctionnalité transverse, multiple sous-sujets, impact architectural) → je crée une sous-spécification dans `docs/specification/features/`, je l'affine via le processus specification-fr, puis je la découpe en stories via story-fr.

**Règle :** plusieurs feedbacks liés → un seul regroupement → une seule story (si simple) ou une seule sous-spécification (si complexe). Pas de doublon.

## Quand m'utiliser

Utilisez cette skill lorsque :
- Vous avez collecté des retours utilisateurs dans `docs/specification/feedbacks.md`
- Vous voulez transformer ces retours en stories structurées et testables
- Vous voulez éviter de retraiter des feedbacks déjà pris en compte

## Prérequis

Le fichier `docs/specification/feedbacks.md` doit être structuré comme suit :

```markdown
## 2025-05-14

* Feedback 1
* Feedback 2
...

## [DATE]

* Feedback N
...
```

Chaque feedback traité reçoit un marqueur `[PROCESSED: story-XXX]` :

```markdown
* Feedback 1 [PROCESSED: story-018]
```

## Workflow

### Étape 1 - Lecture du fichier de feedbacks

- Lire `docs/specification/feedbacks.md`
- Repérer les feedbacks NON marqués `[PROCESSED: story-XXX]` ou `[PROCESSED: feature-XXX.md]` (ce sont ceux à traiter)
- Si TOUS les feedbacks sont marqués : annoncer qu'il n'y a rien de nouveau à traiter → fin du workflow

### Étape 2 - Analyse des stories existantes

- Lire les stories dans `docs/stories/done/` ou `docs/stories/`
- Vérifier si certains feedbacks non marqués sont déjà couverts par des stories existantes
  - Exemple : "Avoir un écran avec la liste des zones" peut déjà exister dans Story #009
  - Si c'est le cas, ajouter le marqueur `[PROCESSED: story-XXX]` (donc déjà couvert) et ne pas recréer de story
- Les feedbacks restants (ni marqués PROCESSED, ni couverts) sont à transformer en nouvelles stories

### Étape 3 - Regroupement et décision : story directe ou sous-spécification

- Regrouper les feedbacks par thème commun (ex : "éditeur", "révision", "UI/UX", "archives")
- Un feedback seul peut devenir une story s'il est suffisamment riche
- Plusieurs feedbacks liés peuvent être regroupés dans une même story
- **Décider du niveau de complexité** de chaque groupe (ou feedback seul) :
  - **Sujet simple** → aller en **Étape 4a** (génération directe de stories)
  - **Sujet complexe** → aller en **Étape 4b** (création d'une sous-spécification)
- Critères indicatifs de complexité (un seul suffit) :
  - Plusieurs rôles ou permissions distincts
  - Impacts multiples sur des fonctionnalités existantes
  - Sous-sujets nombreux et indépendants
  - Changement architectural ou nouvelle table en base
  - Nécessite plusieurs stories pour couvrir le périmètre

---

### Chemin A — Sujet simple : Génération directe de stories

#### Étape 4a - Génération des stories

- Lire les stories dans `docs/stories/done/` pour déterminer le prochain numéro de story
- Créer les nouvelles stories dans `docs/stories/` en suivant le format story-fr :

```markdown
# Story #XXX : [Titre]

## Description
En tant que [rôle], je veux [action] afin de [bénéfice].

## Origine
Feedback du [DATE] : "[texte du feedback]"

## Critères d'acceptation
- [ ] Critère 1
- [ ] Critère 2

## Tests automatisés
- Test unitaire : [Description]
- Test d'intégration : [Description]
- Test E2E : [Description]

## Documentation
- [Nouvelle documentation à créer]

## Valeur utilisateur
[Description de la valeur probante pour l'utilisateur final]

## Dépendances
- [Story #YYY si applicable]
```

- Ajouter une section `## Origine` pour tracer le feedback source
- Marquer chaque feedback traité dans `docs/specification/feedbacks.md` avec `[PROCESSED: story-XXX]` uniquement à la clôture, après résolution des points bloquants
- Exemple : `* Ne pas afficher d'accueil [PROCESSED: story-018]`

#### Étape 5a - Création du fichier de questions

- Créer ou compléter `docs/stories/questions.md` uniquement si une décision bloquante doit être posée
- Le fichier contient les questions de clarification bloquantes pour les nouvelles stories générées
- Appliquer la règle « qualité des questions » (identique à story-fr / specification-fr) : enjeu, contexte ancré dans le feedback + stories existantes, dépendances, propositions détaillées avec implications, emplacement de réponse
- Pour chaque question, rappeler le feedback source (date + extrait) et les stories déjà proches si un risque de doublon existe

#### Étape 6a - Analyse et clarification

- Analyser les nouvelles stories
- Vérifier la cohérence avec la spécification `docs/specification/specification.md`
- Vérifier la cohérence avec les stories existantes dans `docs/stories/done/`
- Ajouter des questions dans `docs/stories/questions.md` uniquement si une décision bloquante reste nécessaire (format « qualité des questions » obligatoire)

#### Étape 7a - Mise à jour

- L'utilisateur répond aux questions dans `docs/stories/questions.md`
- L'utilisateur tape : `J'ai répondu aux questions, mets à jour les stories`
- Mettre à jour les stories selon les réponses
- **Relire les stories mises à jour** pour vérifier leur cohérence et que les réponses ont bien été prises en compte
- Ne créer **qu'une seule vague de suivi au maximum** pour cette unité de travail, et seulement si une réponse révèle un nouveau point bloquant qui n'était pas raisonnablement prévisible dans la première vague
- Si aucun point bloquant ne subsiste, supprimer `questions.md` et passer à l'Étape 8 ; ne pas relancer une analyse dans le seul but de trouver d'autres questions
- Si un point bloquant majeur reste sans réponse après l'unique vague de suivi, le signaler explicitement comme limite ouverte et ne pas marquer le feedback `[PROCESSED: …]` tant que son périmètre ne peut pas être traité de façon fiable

---

### Chemin B — Sujet complexe : Sous-spécification puis découpage en stories

#### Étape 4b - Création de la sous-spécification

- Créer le répertoire `docs/specification/features/` s'il n'existe pas
- Créer le fichier `docs/specification/features/feedback-XXX.md` (XXX = nom court du sujet, ex: `feedback-archives-matchs.md`)
- Copier le texte du feedback dans le fichier comme base de spécification
- Ajouter la section `## Origine` avec la date et le texte du feedback source
- Structurer le fichier avec des placeholders `[À_CLARIFIER: ...]` uniquement pour les décisions bloquantes ; documenter les hypothèses retenues pour les points non bloquants
- Créer `docs/specification/features/questions.md` uniquement si des décisions bloquantes restent à trancher ; le fichier contient :
  - Le contexte global (feedbacks regroupés, sous-spécification cible)
  - Les questions de clarification au format « qualité des questions » (enjeu, contexte, dépendances, propositions détaillées, réponse)
  - Les inconsistances qui entraînent une décision bloquante (même niveau de détail)
  - Pour chaque question : extrait du feedback d'origine et lien avec la spécification principale si pertinent
- Marquer le feedback dans `docs/specification/feedbacks.md` avec `[PROCESSED: feature-XXX.md]` uniquement à la clôture, après résolution des points bloquants

#### Étape 5b - Itération de spécification

- L'utilisateur répond aux questions dans `docs/specification/features/questions.md`
- L'utilisateur tape : `J'ai répondu aux questions, continue`
- Lire les réponses, mettre à jour `feedback-XXX.md` en remplaçant les placeholders par les réponses
- Résoudre les inconsistances identifiées
- **Relire la sous-spécification** : les placeholders ou inconsistances restants ne justifient un suivi que s'ils bloquent une décision de périmètre, d'architecture, de critères d'acceptation ou de découpage en stories
- Ne créer **qu'une seule vague de suivi au maximum** pour cette unité de travail, et seulement si une réponse révèle un nouveau point bloquant qui n'était pas raisonnablement prévisible dans la première vague
- Si aucun point bloquant ne subsiste, supprimer `questions.md` et passer à l'Étape 6b ; documenter les hypothèses non bloquantes dans la sous-spécification
- Si un point bloquant majeur reste sans réponse après l'unique vague de suivi, le signaler explicitement comme limite ouverte et ne pas marquer le feedback `[PROCESSED: …]` tant que son périmètre ne peut pas être traité de façon fiable

#### Étape 6b - Découpage en stories (story-fr)

- La sous-spécification est prête à être découpée (plus de placeholders bloquants ni de décision bloquante ouverte)
- Exécuter le workflow story-fr sur `docs/specification/features/feedback-XXX.md` :
  - Lire la spécification
  - Découper en user stories dans `docs/stories/` (chacune avec la section `## Origine` pointant vers `feature-XXX.md`)
  - Créer `docs/stories/questions.md` uniquement pour les décisions bloquantes, en appliquant la règle de clarification bornée ci-dessous ; le quota de suivi du groupe ne se réinitialise pas au découpage en stories
- Une fois les stories finalisées (aucun point bloquant ouvert) :
  - Mettre à jour `feedbacks.md` si des sous-feedbacks doivent être marqués individuellement
  - Annoncer les stories créées

---

### Étape 8 - Fin du workflow

- Quand, pour tous les chemins, il ne reste aucun point bloquant après la vague initiale et, si nécessaire, l'unique vague de suivi
- Supprimer le fichier `questions.md` s'il existe
- Marquer les feedbacks nouvellement traités dans `docs/specification/feedbacks.md` avec `[PROCESSED: …]` après résolution des points bloquants
- Déplacer les nouvelles stories dans `docs/stories/done/` (ou les laisser dans `docs/stories/` selon la convention)
- **Mettre à jour le plan d'implémentation** `docs/stories/plan-implementation.md` (voir règle ci-dessous)
- Mettre à jour `AGENTS.md` et `workflow.md` si nécessaire
- Annoncer la fin du workflow

## Règle : plan d'implémentation (obligatoire)

**À chaque fois que des stories sont intégrées** (création ou finalisation, chemins A et B), réécrire ou mettre à jour `docs/stories/plan-implementation.md` (même format que story-fr) :

- Recalculer l'ordre recommandé, les groupes en parallèle, les justifications et le diagramme Mermaid à partir de **toutes** les stories encore ouvertes.
- **Cacher les stories déjà terminées** : dossiers `docs/stories/<id>-done/` (suffixe `-done`). Elles n'apparaissent **ni** dans l'ordre, **ni** dans les groupes parallèles, **ni** dans le graphe. Mention courte possible uniquement dans `## Notes` (ex. « #001–#005 déjà livrées »).
- Les dépendances vers une story `-done` sont considérées comme satisfaites : ne pas bloquer une story ouverte sur une story terminée dans le diagramme.
- Si le fichier n'existe pas encore, le créer. Ne pas régénérer le plan tant que des questions bloquantes restent ouvertes.

## Règle : clarification bornée (obligatoire)

Le but des questions est de débloquer une décision nécessaire, pas d'éliminer toute incertitude. Poser une question uniquement si la réponse peut changer de façon importante au moins un des éléments suivants :

- le périmètre ou les critères d'acceptation, au point de rendre la story non testable ou de provoquer un travail sensiblement différent ;
- le choix entre nouvelle story, extension d'une story existante ou sous-spécification ;
- le découpage, une dépendance structurante ou une décision d'architecture ;
- le fait qu'un feedback soit déjà couvert ou puisse être marqué `[PROCESSED: …]`.

Ne pas questionner le naming, le polish, des détails d'implémentation, des cas limites non demandés ou une préférence sans impact matériel. Ne pas redemander une décision déjà donnée, sauf si la réponse contredit une autre exigence et rend le résultat incohérent.

Quand un point n'est pas bloquant, retenir l'hypothèse la plus simple compatible avec le feedback, la spécification et les stories existantes, puis la noter brièvement dans la story ou la sous-spécification si elle a un effet visible. Ne pas transformer chaque hypothèse en question.

Pour chaque unité de travail (un groupe de feedbacks et toutes les stories ou sous-spécifications qui en découlent), préparer une vague initiale de questions, puis **au plus une vague de suivi** si les réponses font apparaître un nouveau blocage. Ce quota vaut pour tout le groupe, y compris le passage de la sous-spécification aux stories ; il ne se réinitialise pas à chaque étape. Après cette vague, trancher les détails restants par hypothèse documentée et clôturer ; ne pas relancer une relecture pour chercher d'autres questions. Si un blocage majeur demeure après l'unique vague de suivi, le signaler explicitement comme limite ouverte plutôt que poursuivre une boucle de questions ; ne pas marquer le feedback `[PROCESSED: …]` si son périmètre ne peut pas être traité de façon fiable.

## Règle : qualité des questions (obligatoire)

Chaque entrée de `questions.md` (chemin A : `docs/stories/questions.md`, chemin B : `docs/specification/features/questions.md`, ou questions story-fr) doit permettre de répondre **en connaissance de cause**. Une question trop courte (« On fait une story ou une feature ? », « Quel rôle ? ») est **insuffisante**.

Pour **chaque** question, inclure **tous** les blocs suivants :

| Bloc | Contenu attendu |
|------|-----------------|
| **Enjeu** | Impact sur le regroupement, la story / sous-spécification, le marquage `[PROCESSED: …]`, ou le chevauchement avec l'existant |
| **Contexte** | Extrait(s) du feedback ; stories/features déjà proches ; ce qui est déjà décidé dans la spec ; ce qui reste flou |
| **Dépendances** | Autres feedbacks du même thème ; stories existantes ; sections de la sous-spécification |
| **Question** | Une seule décision, précise |
| **Propositions** | ≥ 2 options (+ « Autre ») ; chaque option : comportement, implications (nouvelle story vs extension, simple vs complexe), pertinence |
| **Réponse** | Emplacement vide pour l'utilisateur |

**Interdit :** options nues ; questions sans rappeler le feedback source ; plusieurs décisions indépendantes dans une question.

**Obligatoire pour une question bloquante :** citer date + extrait du feedback et les ids de stories/features concernés lorsqu'ils existent. Adapter la longueur du contexte et des propositions à l'enjeu ; ne pas ajouter de détails spéculatifs. Recommandation possible avec justification, sans imposer.

### Exemple de question (chemin A ou B)

```markdown
# Questions de clarification - Feedbacks

## Contexte global
- Feedbacks non traités regroupés : thème « éditeur / aide »
- Stories déjà proches : story-010 (lock éditeur)
- Décision en cours : story directe vs sous-spécification

## Questions

### 1. [Feedback 2025-05-14 — Popup d'aide] — Périmètre du contenu d'aide
**Enjeu :** Sans liste de commandes/raccourcis à couvrir, la story #019 aura des critères non testables et risque de grossir en « feature éditeur ».
**Contexte :**
- Feedback : « Popup d'aide sur l'éditeur : affiche la liste de commandes et les raccourcis clavier associés »
- Existant : story-010 traite le lock, pas l'aide.
- Spec principale : [indiquer si une section éditeur existe].
**Dépendances :** Critères d'acceptation de la nouvelle story ; éventuelle sous-spécification si le catalogue de commandes est large.
**Question :** La popup doit-elle lister **toutes** les commandes de l'éditeur au MVP, ou un sous-ensemble prioritaire ?
**Propositions :**
- a) **Sous-ensemble MVP** (ex. save, undo/redo, lock, search) — Implications : story simple ; liste figée dans les critères ; extension ultérieure possible.
- b) **Catalogue complet généré depuis le registre de commandes** — Implications : plutôt sous-spécification / story technique de registre ; plus de travail.
- c) Autre : coller la liste des commandes/raccourcis attendus.
**Réponse :**

### 2. [Regroupement] — Feedback « pas d'accueil » vs navigation existante
**Enjeu :** Évite de créer une story redondante si la landing est déjà définie ailleurs.
**Contexte :**
- Feedback : « Ne pas afficher d'accueil : on tombe direct sur Mes équipes »
- Stories done : [citer si une story navigation/landing existe].
**Dépendances :** Marquage `[PROCESSED: story-XXX]` vs nouvelle story.
**Question :** Traiter comme nouvelle story de routing, ou marquer comme déjà couvert par [story existante] ?
**Propositions :**
- a) **Nouvelle story** — Redirect post-login vers « Mes équipes » ; critères E2E dédiés.
- b) **Déjà couvert par story-XXX** — Marquer `[PROCESSED: story-XXX]` ; pas de nouvelle story.
- c) Autre : préciser l'écart restant par rapport à story-XXX.
**Réponse :**

```

### Exemple de détail non bloquant (ne pas ajouter comme question)

Si le feedback demande une popup d'aide sans préciser son intitulé, choisir un libellé cohérent avec le vocabulaire existant et le noter comme hypothèse seulement si cela affecte les critères. Ne pas interrompre le traitement pour demander une préférence de nommage.

```markdown
## [DATE]

* [Texte du feedback] [PROCESSED: story-XXX]
* [Texte du feedback] [PROCESSED: feature-XXX.md]
* [Texte du feedback]

## [AUTRE DATE]

* [Texte du feedback] [PROCESSED: story-XXX]
```

Les feedbacks marqués `[PROCESSED: story-XXX]` ou `[PROCESSED: feature-XXX.md]` sont ignorés.
Les feedbacks sans marqueur sont à traiter.

## Exemple

### Fichier feedbacks.md initial
```markdown
## 2025-05-14

* Ne pas afficher d'accueil : on tombe direct sur "Mes équipes"
* Afficher un message d'erreur lorsque l'on tente d'ouvrir l'éditeur et que quelqu'un d'autre a le lock [PROCESSED: story-010]
* Popup d'aide sur l'éditeur : affiche la liste de commandes et les raccourcis clavier associés
```

### Stories générées
- `docs/stories/story-018.md` : "Suppression de la page d'accueil" (basé sur feedback #1)
- `docs/stories/story-019.md` : "Popup d'aide dans l'éditeur" (basé sur feedback #3)

### Fichier feedbacks.md après traitement
```markdown
## 2025-05-14

* Ne pas afficher d'accueil : on tombe direct sur "Mes équipes" [PROCESSED: story-018]
* Afficher un message d'erreur lorsque l'on tente d'ouvrir l'éditeur et que quelqu'un d'autre a le lock [PROCESSED: story-010]
* Popup d'aide sur l'éditeur : affiche la liste de commandes et les raccourcis clavier associés [PROCESSED: story-019]
```

## Comment passer d'une étape à l'autre

### Chemin A — Sujet simple

| Transition | Action de l'utilisateur | Action de l'agent |
|------------|------------------------|-------------------|
| Étape 1 → 2 | Donner le chemin du fichier de feedbacks en argument | Lit les feedbacks, analyse les stories existantes |
| Étape 2 → 3 | Automatique | Regroupe les feedbacks non traités par thème et décide simple/complexe |
| Étape 3 → 4a | Automatique (sujet simple) | Génère les nouvelles stories dans docs/stories/ |
| Étape 4a → 5a | Automatique après génération | Crée questions.md uniquement si des décisions bloquantes restent à clarifier |
| Étape 5a → 6a | Automatique | Analyse les stories et ajoute les questions |
| Étape 6a → 7a | Taper "J'ai répondu aux questions, mets à jour les stories" | Met à jour les stories selon les réponses |
| Étape 7a → 6a | Automatique après mise à jour | Re-vérifie les réponses ; une unique vague de suivi est permise si une réponse révèle un nouveau point bloquant |
| Étape 7a → 8 | Automatique (aucun point bloquant restant) | Supprime questions.md, marque les feedbacks traités, met à jour le plan (sans stories `-done`), annonce la fin |

### Chemin B — Sujet complexe

| Transition | Action de l'utilisateur | Action de l'agent |
|------------|------------------------|-------------------|
| Étape 1 → 2 | Donner le chemin du fichier de feedbacks en argument | Lit les feedbacks, analyse les stories existantes |
| Étape 2 → 3 | Automatique | Regroupe les feedbacks non traités par thème et décide simple/complexe |
| Étape 3 → 4b | Automatique (sujet complexe) | Crée la sous-spécification et questions.md uniquement si des décisions bloquantes restent à clarifier |
| Étape 4b → 5b | Automatique | Attend les réponses de l'utilisateur |
| Étape 5b → 5b | Taper "J'ai répondu aux questions, continue" | Met à jour et relit la spec ; une unique vague de suivi est permise si les réponses révèlent un nouveau point bloquant |
| Étape 5b → 6b | Automatique (aucun point bloquant restant) | Supprime questions.md, documente les hypothèses non bloquantes et exécute story-fr sur la sous-spécification |
| Étape 6b → 8 | Automatique (stories sans point bloquant ouvert) | Annonce les stories créées, met à jour le plan (sans stories `-done`) |

**Important :** L'agent ne surveille pas les modifications de fichiers en temps réel. L'utilisateur doit toujours donner une instruction explicite pour que l'agent passe à l'étape suivante.

## Exemples d'utilisation

### Cas simple — Génération directe de stories

```
/ feedback feedbacks.md
```

L'agent lit `docs/specification/feedbacks.md`, identifie les feedbacks non marqués, vérifie les stories existantes, et génère les nouvelles stories avec une clarification bornée aux décisions bloquantes.

### Cas complexe — Sous-spécification puis stories

```
/ feedback feedbacks.md
```

L'agent lit `docs/specification/feedbacks.md`, détecte qu'un feedback (ex: "Archives des matchs") est trop complexe pour une story unique, puis :

1. Crée `docs/specification/features/feedback-archives-matchs.md`
2. Génère `docs/specification/features/questions.md` avec des questions détaillées (enjeu, contexte issu du feedback, propositions avec implications — rôles, relations entre données, etc.)
3. Traite les décisions bloquantes avec une vague initiale et, si une réponse révèle un nouveau blocage, au plus une vague de suivi
4. Supprime `questions.md`
5. Exécute story-fr sur la spécification pour la découper en plusieurs stories
6. Marque le feedback original comme `[PROCESSED: feature-archives-matchs.md]`

### Gestion des doublons avec les stories existantes

Si un feedback non marqué est déjà couvert par une story existante :

1. L'agent l'identifie lors de l'Étape 2
2. Il ajoute le marqueur `[PROCESSED: story-XXX]` dans le fichier feedbacks.md
3. Il n'en génère pas de nouvelle story
4. Il signale le doublon dans le bilan, sans créer de question si aucune décision bloquante n'est nécessaire

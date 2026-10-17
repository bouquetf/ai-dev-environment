---
name: feedback-fr
description: Génération de stories à partir de feedbacks utilisateurs, en filtrant ceux déjà traités ; continue de poser des questions tant qu'il reste des ambiguïtés
---

# Skill: Feedback → Stories / Spécification (FR)

Ce guide décrit comment transformer des feedbacks utilisateurs en user stories exploitables, en ne traitant que les retours les plus récents qui n'ont pas encore été pris en compte dans les stories existantes.

## Qu'est-ce que je fais

Je prends le fichier `docs/specification/feedbacks.md`, j'identifie les feedbacks qui n'ont pas encore été traités. Je **regroupe d'abord les feedbacks par thème** — plusieurs feedbacks sur un même sujet donnent une seule unité de travail. Pour chaque groupe :
- **Si le sujet est simple** → je génère une ou plusieurs stories directement (au même format que story-fr) avec le processus itératif de questions/réponses.
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
- Marquer chaque feedback traité dans `docs/specification/feedbacks.md` avec `[PROCESSED: story-XXX]`
- Exemple : `* Ne pas afficher d'accueil [PROCESSED: story-018]`

#### Étape 5a - Création du fichier de questions

- Créer `docs/stories/questions.md` (ou s'il existe déjà, le compléter)
- Contient les questions de clarification pour les nouvelles stories générées
- Appliquer la règle « qualité des questions » (identique à story-fr / specification-fr) : enjeu, contexte ancré dans le feedback + stories existantes, dépendances, propositions détaillées avec implications, emplacement de réponse
- Pour chaque question, rappeler le feedback source (date + extrait) et les stories déjà proches si un risque de doublon existe

#### Étape 6a - Analyse et clarification

- Analyser les nouvelles stories
- Vérifier la cohérence avec la spécification `docs/specification/specification.md`
- Vérifier la cohérence avec les stories existantes dans `docs/stories/done/`
- Ajouter des questions dans `docs/stories/questions.md` si nécessaire (format « qualité des questions » obligatoire)

#### Étape 7a - Mise à jour

- L'utilisateur répond aux questions dans `docs/stories/questions.md`
- L'utilisateur tape : `J'ai répondu aux questions, mets à jour les stories`
- Mettre à jour les stories selon les réponses
- **Relire les stories mises à jour** : s'il reste des ambiguïtés, points flous, inconsistances, ou sujets qui méritent une réponse supplémentaire → régénérer / compléter `questions.md`
- Retourner à l'Étape 6a (ne pas clôturer tant qu'il reste quelque chose à clarifier)

---

### Chemin B — Sujet complexe : Sous-spécification puis découpage en stories

#### Étape 4b - Création de la sous-spécification

- Créer le répertoire `docs/specification/features/` s'il n'existe pas
- Créer le fichier `docs/specification/features/feedback-XXX.md` (XXX = nom court du sujet, ex: `feedback-archives-matchs.md`)
- Copier le texte du feedback dans le fichier comme base de spécification
- Ajouter la section `## Origine` avec la date et le texte du feedback source
- Structurer le fichier avec des placeholders `[À_CLARIFIER: ...]` sur les points flous
- Créer `docs/specification/features/questions.md` avec :
  - Le contexte global (feedbacks regroupés, sous-spécification cible)
  - Les questions de clarification au format « qualité des questions » (enjeu, contexte, dépendances, propositions détaillées, réponse)
  - Les inconsistances détectées (même niveau de détail)
  - Pour chaque question : extrait du feedback d'origine et lien avec la spécification principale si pertinent
- **Marquer le feedback** dans `docs/specification/feedbacks.md` avec le marqueur `[PROCESSED: feature-XXX.md]`

#### Étape 5b - Itération de spécification

- L'utilisateur répond aux questions dans `docs/specification/features/questions.md`
- L'utilisateur tape : `J'ai répondu aux questions, continue`
- Lire les réponses, mettre à jour `feedback-XXX.md` en remplaçant les placeholders par les réponses
- Résoudre les inconsistances identifiées
- **Relire la sous-spécification** : s'il reste des `[À_CLARIFIER: ...]`, `[INCONSISTANCE: ...]`, **ou tout point encore ambigu / flou / digne d'une réponse supplémentaire** :
  - **Oui** → générer un nouveau `questions.md` avec les questions restantes (et les nouvelles) → retour au début de l'Étape 5b
  - **Non** → supprimer `questions.md` → passer à l'Étape 6b

#### Étape 6b - Découpage en stories (story-fr)

- La sous-spécification est complète (plus de placeholders **et** plus d'ambiguïté utile après relecture)
- Exécuter le workflow story-fr sur `docs/specification/features/feedback-XXX.md` :
  - Lire la spécification
  - Découper en user stories dans `docs/stories/` (chacune avec la section `## Origine` pointant vers `feature-XXX.md`)
  - Créer `docs/stories/questions.md` si des points restent flous
  - Appliquer le cycle itératif de questions/réponses propre à story-fr (**règle de clarification continue** : ne pas clôturer tant qu'il reste quelque chose à clarifier)
- Une fois les stories finalisées (plus de questions utiles après relecture) :
  - Mettre à jour `feedbacks.md` si des sous-feedbacks doivent être marqués individuellement
  - Annoncer les stories créées

---

### Étape 8 - Fin du workflow

- Quand, pour tous les chemins, il n'y a vraiment plus rien à clarifier après relecture
- Supprimer le fichier `questions.md` s'il existe
- Déplacer les nouvelles stories dans `docs/stories/done/` (ou les laisser dans `docs/stories/` selon la convention)
- Mettre à jour `AGENTS.md` et `workflow.md` si nécessaire
- Annoncer la fin du workflow

## Règle : clarification continue (obligatoire)

**Ne jamais clôturer tant qu'il reste quelque chose à clarifier.** Après chaque mise à jour (stories ou sous-spécification), relire le résultat et poser de **nouvelles** questions si :

- un feedback, un critère, un rôle ou un périmètre reste ambigu ou incomplet ;
- une réponse en soulève d'autres (implications, cas limites, chevauchement avec des stories existantes) ;
- le regroupement simple vs complexe, ou le découpage en stories, mérite une confirmation ;
- un point mérite une réponse supplémentaire avant de marquer un feedback `[PROCESSED: …]` comme définitivement traité.

**Préférer demander plutôt que d'assumer.** L'absence de placeholders formels ne suffit pas : s'il reste un doute raisonnable, écrire la question et continuer le cycle.

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

**Obligatoire :** citer date + extrait du feedback, et les ids de stories/features concernés. Recommandation possible avec justification, sans imposer.

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
| Étape 4a → 5a | Automatique après génération | Crée questions.md avec les points à clarifier |
| Étape 5a → 6a | Automatique | Analyse les stories et ajoute les questions |
| Étape 6a → 7a | Taper "J'ai répondu aux questions, mets à jour les stories" | Met à jour les stories selon les réponses |
| Étape 7a → 6a | Automatique après mise à jour | Re-vérifie la cohérence ; **régénère `questions.md` s'il reste des ambiguïtés** |
| Étape 7a → 8 | Automatique (plus de questions **après relecture**) | Supprime questions.md, marque les feedbacks traités, annonce la fin |

### Chemin B — Sujet complexe

| Transition | Action de l'utilisateur | Action de l'agent |
|------------|------------------------|-------------------|
| Étape 1 → 2 | Donner le chemin du fichier de feedbacks en argument | Lit les feedbacks, analyse les stories existantes |
| Étape 2 → 3 | Automatique | Regroupe les feedbacks non traités par thème et décide simple/complexe |
| Étape 3 → 4b | Automatique (sujet complexe) | Crée docs/specification/features/feedback-XXX.md + questions.md |
| Étape 4b → 5b | Automatique | Attend les réponses de l'utilisateur |
| Étape 5b → 5b | Taper "J'ai répondu aux questions, continue" | Met à jour la spec ; **relit** ; régénère `questions.md` s'il reste placeholders **ou** ambiguïtés |
| Étape 5b → 6b | Automatique (plus de placeholders **ni d'ambiguïté utile après relecture**) | Supprime questions.md, exécute story-fr sur la sous-spécification |
| Étape 6b → 8 | Automatique (stories finalisées **après clarification continue**) | Annonce les stories créées |

**Important :** L'agent ne surveille pas les modifications de fichiers en temps réel. L'utilisateur doit toujours donner une instruction explicite pour que l'agent passe à l'étape suivante.

## Exemples d'utilisation

### Cas simple — Génération directe de stories

```
/ feedback feedbacks.md
```

L'agent lit `docs/specification/feedbacks.md`, identifie les feedbacks non marqués, vérifie les stories existantes, et génère les nouvelles stories avec le processus itératif de clarification.

### Cas complexe — Sous-spécification puis stories

```
/ feedback feedbacks.md
```

L'agent lit `docs/specification/feedbacks.md`, détecte qu'un feedback (ex: "Archives des matchs") est trop complexe pour une story unique, puis :

1. Crée `docs/specification/features/feedback-archives-matchs.md`
2. Génère `docs/specification/features/questions.md` avec des questions détaillées (enjeu, contexte issu du feedback, propositions avec implications — rôles, relations entre données, etc.)
3. Itère avec l'utilisateur jusqu'à spécification complète
4. Supprime `questions.md`
5. Exécute story-fr sur la spécification pour la découper en plusieurs stories
6. Marque le feedback original comme `[PROCESSED: feature-archives-matchs.md]`

### Gestion des doublons avec les stories existantes

Si un feedback non marqué est déjà couvert par une story existante :

1. L'agent l'identifie lors de l'Étape 2
2. Il ajoute le marqueur `[PROCESSED: story-XXX]` dans le fichier feedbacks.md
3. Il n'en génère pas de nouvelle story
4. Il peut notifier l'utilisateur du doublon dans questions.md

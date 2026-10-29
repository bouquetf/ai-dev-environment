---
name: specification-fr
description: Création pragmatique de spécifications de projet (Français) ; approche top-down avec brief, hypothèses et questions uniquement bloquantes
---

# Skill: Spécification (FR)

Ce guide décrit comment générer une spécification de projet suffisante pour dériver des user stories exploitables, en privilégiant un cadrage rapide et pragmatique.

## Qu'est-ce que je fais

Je guide la création d'une spécification structurée en partant d'un brief utilisateur et de l'inspection du codebase, en posant uniquement des questions **bloquantes** et en formulant des hypothèses pour le reste.

## Quand m'utiliser

Utilisez cette skill lorsque vous voulez cadrer un projet ou une fonctionnalité pour ensuite le découper en user stories avec `story-fr`.

## Organisation des fichiers (`docs/specification/`)

```text
docs/specification/
  specification.md   # Spécification cible
  questions.md        # Questions BLOQUANTES uniquement (temporaire — supprimé à la fin)
  assumptions.md      # Hypothèses proposées/inférées (temporaire — supprimé à la fin)
```

| Fichier | Rôle |
|---------|------|
| `specification.md` | Spécification structurée, suffisante pour générer des user stories testables |
| `questions.md` | Questions bloquantes nécessitant un arbitrage explicite (max 2 rounds) |
| `assumptions.md` | Décisions inférées depuis le codebase/brief avec justification, à valider ou invalider |

## Processus

### Étape 0 - Brief & Contexte (TOP-DOWN)
- **Créer** `docs/specification/` s'il n'existe pas
- **Extraire le brief** : si l'utilisateur fournit un brief (objectifs, utilisateurs, périmètre/hors-périmètre, contraintes, stack), l'appliquer directement sans questions
- **Inspecter le codebase** : lire `README.md`, `AGENTS.md`, `package.json`, `pom.xml`, `build.gradle*`, `docs/` pour identifier stack, conventions, architecture, patterns existants
- **Vérifier** s'il existe déjà `docs/specification/specification.md` — l'utiliser comme base

### Étape 1 - Brouillon initial avec hypothèses
- **Générer un brouillon** `specification.md` structuré (Vue d'ensemble, Objectifs, Périmètre/Hors-périmètre, Acteurs, Fonctionnalités principales, Contraintes, Architecture/Tech, Questions ouvertes résiduelles)
- **Inférer intelligemment** : s'appuyer sur le codebase pour proposer stack, style, conventions cohérents avec l'existant
- **Lister les hypothèses** dans `assumptions.md` avec `[INFÉRÉ]` ou `[PROPOSÉ]`, justification et impact
- **Ne pas poser de questions** sur ce qui est clairement inférable ou déjà précisé dans le brief

### Étape 2 - Identifier uniquement les blocages
- **Analyser** le brouillon pour ne détecter que les **blocages réels** pour passer à `story-fr` :
  - Problème résolu / premier livrable utile indéterminable
  - Acteurs principaux non identifiables
  - Contradiction directe entre éléments déjà écrits
  - Décision produit/technique rendant impossible de définir un périmètre MVP cohérent
- **Si aucun blocage** : générer `assumptions.md` avec les hypothèses restantes, proposer à l'utilisateur d'accepter ou de donner des directives
- **Si blocages détectés** : créer `questions.md` avec **maximum 3-5 questions globales** (format condensé par défaut)

### Étape 3 - Mise à jour selon réponses
- L'utilisateur peut répondre :
  - `"J'ai répondu aux questions, mets à jour la spécification"`
  - `"Accepte toutes les hypothèses"`
  - `"Applique mes directives : ..."`
  - Retour ciblé par section
- Mettre à jour `specification.md` et `assumptions.md` en conséquence
- **Relire** : ne régénérer `questions.md` que s'il apparaît de **nouveaux blocages**. Maximum **2 rounds** au total

### Étape 4 - Clôture pragmatique
- Quand il n'y a plus de blocages (ou après 2 rounds avec hypothèses appliquées) :
  - Supprimer `questions.md` et `assumptions.md`
  - Vérifier rapidement que la spec est **suffisante pour dériver des user stories testables** (périmètre MVP clair, acteurs identifiés, fonctionnalités principales définies)
  - Annoncer que la spécification est **prête pour `story-fr`/`story-en`**

## Règle : clarification ciblée (obligatoire)

- **Uniquement bloquant** : poser des questions seulement si elles empêchent de définir le problème/périmètre MVP ou de résoudre une contradiction directe
- **Assumer intelligemment** : tout le reste devient hypothèse dans `assumptions.md`, justifiée et applicable
- **Critère d'arrêt** : *"Suffisamment claire pour permettre à `story-fr`/`story-en` de générer des user stories testables sans deviner le périmètre MVP"* — PAS "aucune ambiguïté possible"
- **Max 2 rounds** : au-delà, formuler des hypothèses raisonnées et conclure
- **Accepter les directives** : appliquer immédiatement sans questionner

## Règle : qualité des questions (adaptable)

### Format condensé (préféré)
```markdown
### Q[X] - [Contexte court] - [Décision à prendre]
**Enjeu :** Impact sur le cadrage MVP / découpage en stories
**Question :** [Formulation précise, UNE décision]
**Propositions :**
- a) [Option 1] — Comportement + implications
- b) [Option 2] — Comportement + implications
- c) Autre : [préciser]
**Réponse :**
```

### Format complet (uniquement pour de vrais blocages)
Inclure : **Enjeu**, **Contexte**, **Dépendances**, **Question**, **Propositions** (≥2 + Autre avec implications), **Réponse**. Citer sections de `specification.md` si pertinent.

**Interdits** : questions sans contexte, options nues, plusieurs décisions dans une seule question.

## Format des fichiers

### specification.md (structure cible)
```markdown
# Spécification - [Nom du projet]

## Vue d'ensemble
[Problème résolu, valeur apportée]

## Objectifs
- [Objectif principal]
- [Objectifs secondaires si utiles]

## Périmètre / Hors-périmètre
### Dans le périmètre (MVP)
- [...]
### Hors périmètre
- [...]

## Acteurs / Utilisateurs
- [Rôle principal + contexte d'usage]

## Fonctionnalités principales
- [Fonctionnalités clés pour le MVP]

## Contraintes
- [Techniques, non-fonctionnelles, légales si connues]

## Architecture / Stack (proposé)
[Stack cohérente avec le codebase existant + justification]

## Hypothèses résiduelles
[Références vers assumptions.md si encore en attente]
```

### assumptions.md
```markdown
# Hypothèses - Spécification

## Inférées depuis le codebase
- [INFÉRÉ] Stack : [identifié] — Justif: [fichiers détectés] — Statut: [APPLIQUÉ]

## Proposées au vu du brief
- [PROPOSÉ] Périmètre MVP : [...] — Justif: [...] — Statut: [À VALIDER/ACCEPTÉ/REFUSÉ]

## Globales appliquées
- [APPLIQUÉ] [...] — Impacts sur le cadrage
```

### questions.md (uniquement bloquant)
```markdown
# Questions BLOQUANTES - Spécification

## Contexte global
- Brief : [résumé]
- Codebase inspecté : [éléments clés]
- Round : [1/2]

## Questions bloquantes
### 1. [GLOBAL/MVP] — [Titre]
**Enjeu :** [Pourquoi bloque le découpage en stories]
**Question :** [UNE décision]
**Propositions :** [...]
**Réponse :**
```

## Comment passer d'une étape à l'autre

| Transition | Action utilisateur | Action agent |
|------------|--------------------|--------------|
| Début | Donner chemin spec/brief + contexte | Inspecte codebase, extrait brief, génère brouillon + assumptions |
| Brouillon → Clarification | "Accepte toutes les hypothèses" OU répondre questions bloquantes OU "Applique mes directives : ..." | Applique décisions, affine spec |
| Après mise à jour | Même forme | Ne relance questions que si nouveaux blocages (max 2 rounds) |
| Fin | Automatique si suffisant | Supprime questions.md/assumptions.md, confirme prêt pour `story-fr` |

**Important :** L'agent ne surveille pas les fichiers en temps réel. Donner une instruction explicite à chaque étape.

**Astuce :** Plus votre brief est précis (problème, utilisateurs, périmètre), moins de questions seront posées.

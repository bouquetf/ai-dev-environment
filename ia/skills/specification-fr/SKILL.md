---
name: specification-fr
description: Création itérative de spécifications de projet (Français) ; continue de poser des questions tant qu'il reste des ambiguïtés
---

# Skill: Spécification (FR)

Ce guide décrit comment générer une spécification de projet itérative avec questions et réponses jusqu'à résolution complète.

## Qu'est-ce que je fais

Je guide la création d'une spécification de projet complète à travers un processus itératif de questions, réponses et clarification jusqu'à ce que toutes les inconsistances soient résolues.

## Quand m'utiliser

Utilisez cette skill lorsque vous voulez créer une spécification de projet structurée à partir d'une idée sommaire, en itérant jusqu'à ce que tout soit clair et cohérent.

## Étapes du processus

### Étape 1 : Initiation
- Créer un répertoire de travail documentaire (ex: `docs/specification/`) s’il n’existe pas déjà
- Générer le fichier `questions.md` s’il n’existe pas.
- Si un fichier de spécifications existe déjà, utiliser son contenu comme base de spécification.
- Sinon, générer ce fichier specification.md vide et demander à l'utilisateur de décrire sommairement ce que fera le projet dans questions.md.
- Poser dans questions.md des questions ouvertes pour mieux comprendre le projet

### Étape 2 : Création de spécification
- Lire les réponses de l'utilisateur dans `questions.md`
- Mettre à jour `specification.md` avec :
  - La description du projet structurée
  - Des placeholders clairs (ex: `[À_CLARIFIER: ...]`) pour identifier les points flous
  - Des marqueurs d'inconsistances (ex: `[INCONSISTANCE: ...]`)
- Générer un nouveau fichier `questions.md` contenant :
  - Les questions nécessaires pour résoudre les placeholders
  - Pour **chaque** question : enjeu, contexte factuel, dépendances, propositions détaillées (voir règle « qualité des questions »)
  - Les inconsistances détectées à résoudre (même niveau de détail)

### Étape 3 : Mise à jour
- Lire les réponses dans `questions.md`
- Mettre à jour `specification.md` en remplaçant les placeholders par les réponses
- Résoudre les inconsistances identifiées

### Étape 4 : Itération
- Relancer l'Étape 2 avec un nouvel agent
- Vérifier s'il reste des placeholders, inconsistances, **ou tout point encore ambigu / flou / digne d'une réponse supplémentaire** (voir règle ci-dessous)
- Si oui, continuer l'itération : régénérer `questions.md` et attendre les réponses
- Si non, supprimer le fichier questions.md et considérer la spécification comme complète

## Règle : clarification continue (obligatoire)

**Ne jamais clôturer tant qu'il reste quelque chose à clarifier.** Après chaque lecture des réponses et chaque mise à jour de `specification.md`, relire le résultat et poser de **nouvelles** questions si :

- un point reste ambigu, contradictoire ou incomplet ;
- une réponse en soulève d'autres (implications non tranchées, cas limites, rôles, contraintes, périmètre) ;
- un choix technique ou produit mérite une confirmation avant d'être figé ;
- un placeholder a été remplacé par une formulation encore vague (« à définir plus tard », « selon le besoin », etc.).

**Préférer demander plutôt que d'assumer.** Ne pas inventer de décisions métier ou techniques pour « finir » plus vite. Une absence de placeholder `[À_CLARIFIER: …]` / `[INCONSISTANCE: …]` ne suffit pas : si le texte laisse encore un doute raisonnable, écrire la question dans `questions.md` (avec contexte et propositions quand c'est utile) et **continuer le cycle**.

Clôturer uniquement quand, après cette relecture, **aucune** ambiguïté, inconsistance ou question utile ne reste — puis annoncer clairement que la spécification est complète.

## Règle : qualité des questions (obligatoire)

Chaque entrée de `questions.md` doit permettre de répondre **en connaissance de cause**, sans devoir relire toute la spécification ni deviner les enjeux. Une question trop courte (« Quel auth ? », « SSO ou OAuth ? ») est **insuffisante**.

Pour **chaque** question, inclure **tous** les blocs suivants :

| Bloc | Contenu attendu |
|------|-----------------|
| **Enjeu** | Pourquoi la réponse compte (impact produit, technique, périmètre, stories futures) |
| **Contexte** | Ce qui est déjà décidé ou connu ; extraits / faits pertinents ; contraintes déjà figées ; ce qui reste flou |
| **Dépendances** | Ce qui sera bloqué ou orienté par la réponse (autres sections, placeholders, choix liés) |
| **Question** | Formulation précise, non ambiguë, limitée à **une** décision |
| **Propositions** | Au moins 2 options concrètes (plus « Autre ») ; **chaque** option décrit le comportement résultant, les implications, et quand elle est pertinente |
| **Réponse** | Emplacement vide pour l'utilisateur (`- [ ]` ou ligne `Réponse :`) |

**Interdit :** questions sans contexte ; listes d'options nues (`a) SSO b) OAuth`) sans expliquer ce que chaque choix implique ; jargon non défini ; regrouper plusieurs décisions indépendantes dans une seule question.

**Obligatoire :** ancrer le contexte dans le projet (références à des sections de `specification.md`, feedbacks, contraintes déjà écrites). Si une option est recommandée, le signaler explicitement avec la justification, sans imposer le choix.

## Format des fichiers

### questions.md
```markdown
# Questions de spécification

## Contexte global
[Résumé du projet + état d'avancement de la spécification + fichiers concernés]

## Questions

### 1. [Réf. placeholder / section] — [Titre court de la décision]
**Enjeu :** [Pourquoi trancher maintenant ; impact si on se trompe ou si on reporte]
**Contexte :**
- Déjà décidé / connu : […]
- Extrait ou fait pertinent : […]
- Ce qui reste flou : […]
**Dépendances :** [Sections / placeholders / choix qui dépendent de cette réponse]
**Question :** [Une seule décision, formulée clairement]
**Propositions :**
- a) **[Nom]** — [Comportement concret]. Implications : […]. Pertinent si : […].
- b) **[Nom]** — [Comportement concret]. Implications : […]. Pertinent si : […].
- c) Autre : [à préciser — indiquer ce qu'il faut décrire]
**Réponse :**

### 2. …
…

## Inconsistances à résoudre
### I1. [Titre]
**Constat :** [Les deux affirmations / contraintes en conflit, avec citations]
**Pourquoi c'est un problème :** […]
**Question :** [Comment trancher]
**Propositions :** (même niveau de détail que ci-dessus)
**Réponse :**
```

### specification.md
```markdown
# Spécification - [Nom du projet]

## Vue d'ensemble
[Description générale]

## Fonctionnalités
[Liste des fonctionnalités]

## [Section avec placeholder]
[À_CLARIFIER: Précisions nécessaires sur...]

## [Section avec inconsistances]
[INCONSISTANCE: Conflit identifié entre...]
```

## Exemple d'utilisation

**Lancement de la skill :**
```
/ specification
```

**Étape 1 - L'agent crée `docs/specification/questions.md` :**
```markdown
# Questions de spécification

## Contexte global
Aucune spécification existante. Objectif : cadrer le projet avant de rédiger `specification.md`.

## Questions

### 1. Périmètre — Objectif et fonctionnalité principale
**Enjeu :** Sans objectif clair, la spécification risque de mélanger plusieurs produits. Cette réponse structure toute la suite.
**Contexte :**
- Déjà décidé / connu : rien — démarrage à froid.
- Ce qui reste flou : nature du produit, problème résolu, utilisateurs.
**Dépendances :** Public cible, contraintes techniques, découpage en stories.
**Question :** Quel problème le projet résout-il, et quelle est la fonctionnalité principale du premier livrable utile ?
**Propositions :**
- a) **Outil collaboratif métier** — Ex. gestion de tâches / workflows partagés. Implications : multi-utilisateurs, droits, temps réel possible.
- b) **Outil individuel / monoutilisateur d'abord** — Livrer la valeur solo puis collaborer plus tard. Implications : auth et partage reportés.
- c) Autre : décrire le problème, l'utilisateur principal, et le premier écran / flux critique.
**Réponse :**

### 2. Utilisateurs — Public cible
**Enjeu :** Détermine personas, vocabulaire UI, et exigences d'accessibilité / mobile.
**Contexte :** Dépend de la réponse à la question 1.
**Dépendances :** Rôles, permissions, parcours prioritaires.
**Question :** Qui sont les utilisateurs principaux du premier livrable, et dans quel contexte d'usage (bureau, terrain, mobile) ?
**Propositions :**
- a) **Équipes professionnelles** (ex. dev agile, ops) — jargon métier OK ; prioriser productivité et intégrations.
- b) **Grand public / non experts** — UI simple, onboarding, mobile-first.
- c) Autre : préciser rôles, fréquence d'usage, et appareils dominants.
**Réponse :**

### 3. Contraintes — Technique et non-fonctionnel
**Enjeu :** Évite de promettre temps réel, SSO ou offline sans base réaliste.
**Contexte :** Aucune contrainte connue pour l'instant.
**Dépendances :** Stack, hébergement, choix auth, stratégie temps réel.
**Question :** Quelles contraintes sont non négociables (plateformes, stack, conformité, perf, intégrations existantes) ?
**Propositions :**
- a) **Peu de contraintes** — stack libre ; prioriser time-to-market. Implications : l'agent proposera des defaults prudents à valider.
- b) **Contraintes fortes** — lister stack, cloud, SSO, RGPD, SLA. Implications : figés dans la spec dès le départ.
- c) Autre : lister must-have vs nice-to-have.
**Réponse :**
```

**L'utilisateur répond dans `questions.md` :**
```markdown
### 1. … **Réponse :** a) Application web de tâches collaboratives avec tableaux Kanban ; premier livrable = un board partagé avec colonnes et cartes.
### 2. … **Réponse :** a) Équipes de développement agile, usage bureau + tablette.
### 3. … **Réponse :** a) Compatible mobile (responsive) obligatoire ; pas de stack imposée.
```

**Étape 2 - L'agent génère `specification.md` :**
```markdown
# Spécification - Application Kanban Collaborative

## Vue d'ensemble
Application web permettant aux équipes de développement agile de gérer leurs tâches via des tableaux Kanban collaboratifs.

## Fonctionnalités
- Création de tableaux et colonnes personnalisables
- Gestion des tâches avec drag-and-drop
- [À_CLARIFIER: Système d'authentification (SSO, email, OAuth ?)]
- [À_CLARIFIER: Gestion des rôles et permissions]

## Contraintes techniques
- Compatible mobile (responsive design)
- [INCONSISTANCE: Pas de tech stack imposée mais besoin de temps réel pour la collaboration]
```

**Et génère un nouveau `questions.md` :**
```markdown
# Questions de spécification

## Contexte global
Kanban collaboratif pour équipes agiles, responsive mobile. Placeholders ouverts : auth, rôles. Inconsistance : temps réel souhaité sans stack imposée.

## Questions

### 1. [À_CLARIFIER: Authentification] — Mode d'auth du MVP
**Enjeu :** Conditionne modèle utilisateur, onboarding, et éventuelle dépendance IdP / BaaS.
**Contexte :**
- Déjà décidé : app web multi-utilisateurs (équipes agiles), pas de stack imposée.
- Flou : SSO entreprise vs social login vs email/password pour le MVP.
**Dépendances :** Section rôles/permissions ; hébergement ; conformité.
**Question :** Quel mécanisme d'authentification pour le premier livrable ?
**Propositions :**
- a) **Email + mot de passe** — Compte local. Implications : reset password, sécurité basique à prévoir ; pas de SSO au MVP.
- b) **OAuth social** (Google et/ou GitHub) — Moins de friction pour devs. Implications : dépendances providers ; email toujours requis pour notifications.
- c) **SSO entreprise (OIDC/SAML)** — Requis si clients corporate dès le jour 1. Implications : config IdP, plus long à livrer.
- d) Autre : préciser providers et si multi-tenant.
**Réponse :**

### 2. [À_CLARIFIER: Rôles] — Matrice de droits sur un board
**Enjeu :** Sans rôles clairs, les critères d'acceptation des stories collab resteront flous.
**Contexte :** Collaboration multi-utilisateurs sur un même board ; mobile responsive.
**Dépendances :** Invitations, partage de board, audit éventuel.
**Question :** Quels rôles existent sur un board, et que peut faire chacun (créer/éditer/supprimer cartes, gérer membres, admin workspace) ?
**Propositions :**
- a) **Admin / Membre / Invité** — Admin : tout + membres ; Membre : CRUD cartes ; Invité : lecture seule.
- b) **Admin / Membre seulement** — Pas d'invité lecture seule au MVP.
- c) Autre : fournir une mini-matrice rôle × action.
**Réponse :**

### 3. [INCONSISTANCE: Temps réel vs stack libre] — Stratégie de sync collaborative
**Constat :** Besoin de collaboration « temps réel » évoqué, mais aucune stack imposée — risque de sous-estimer l'infra.
**Pourquoi c'est un problème :** WebSocket maison ≠ BaaS ≠ polling ; le choix change coût et stories techniques.
**Question :** Quel niveau de « temps réel » et quelle approche pour le MVP ?
**Propositions :**
- a) **Sync quasi temps réel via BaaS** (ex. Supabase/Firebase) — Implications : vendor lock-in partial, livraison rapide.
- b) **WebSocket / SSE custom** — Implications : plus de contrôle, plus de travail infra et de tests.
- c) **Refresh / polling court au MVP** — Implications : UX moins fluide, report du vrai temps réel.
- d) Autre : préciser latence acceptable et conflits d'édition (last-write-wins vs OT/CRDT).
**Réponse :**
```

**Transition Étape 1 → Étape 2 :**
L'utilisateur doit informer l'agent que les questions sont répondues en tapant simplement :
```
J'ai répondu aux questions, continue
```
L'agent ne détecte pas automatiquement les modifications de fichier. L'utilisateur doit explicitement donner l'instruction de continuer.

**Étape 3 - L'utilisateur répond dans `questions.md`, puis tape :**
```
Questions répondues, mets à jour la spécification
```
L'agent met à jour `specification.md` en remplaçant les placeholders par les réponses.

**Transition Étape 3 → Étape 4 :**
L'agent vérifie automatiquement s'il reste des placeholders. S'il en reste, il relance l'Étape 2 avec un nouveau cycle de questions.

**Étape 4 - Itération :**
L'agent relance l'Étape 2, vérifie qu'il n'y a plus de placeholders, la spécification est complète !

## Comment passer d'une étape à l'autre

| Transition | Action de l'utilisateur | Action de l'agent |
|------------|------------------------|-------------------|
| Étape 1 → 2 | Taper `continue` ou `j'ai répondu` | Lit `questions.md` et génère `specification.md` + nouveau `questions.md` |
| Étape 2 → 3 | Taper `continue` ou `questions répondues` | Lit les réponses et met à jour `specification.md` |
| Étape 3 → 4 | Automatique | Vérifie placeholders **et** ambiguïtés restantes ; relance l'Étape 2 (nouveau `questions.md`) s'il reste quelque chose à clarifier |
| Fin | Automatique | Annonce que la spécification est complète **uniquement** après relecture sans question utile |

**Important :** L'agent ne surveille pas les modifications de fichiers en temps réel. L'utilisateur doit toujours donner une instruction explicite pour que l'agent passe à l'étape suivante, sauf pour la transition automatique Étape 3 → 4.

## Critère de fin

La spécification est terminée quand **toutes** ces conditions sont vraies :

1. `specification.md` ne contient plus aucun placeholder `[À_CLARIFIER: ...]` ou `[INCONSISTANCE: ...]`.
2. Après relecture post-mise à jour, aucun point ambigu, flou ou digne d'une réponse supplémentaire n'a été identifié.
3. `questions.md` a été régénéré vide de questions utiles (ou n'a plus lieu d'exister) — puis est supprimé.

## Suite du workflow

Une fois la spécification complète, utiliser la skill **`story-fr`** pour découper en user stories. Chaque story est écrite dans `tmp/<id>/story.md` ; les tâches d'implémentation iront dans `tmp/<id>/story/` (skill **`tasks-fr`**). Une tâche exécutée est renommée `task-YYY-done.md`.

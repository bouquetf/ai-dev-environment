# Feedback — Skill-based development system

`## Open` holds entries with status `new` or `held`. `## Resolved` holds entries with status `accepted`. Append new ideas under Open. Do not rewrite an existing idea.

## Open

- 10 mn
- tout le monde sait ce qu’est une skill, …, les gens devraient savoir
- dire quelques mots sur le partage des skills, donc plutôt pour commencer
- compréhension de l’anglais/français -> anglais potentiellement plus efficace, fr plus cher en token, certaines langues sont pas super efficaces voire inconnues
- demo -> revoir github de source des skills pour le projet de plugin

## Resolved

### Opening stance

- Idea: People usually look at what AI does well. This talk focuses on how the human can do better at the work they are already good at.
- Why it matters: It sets the opening and sharpens the core message without adding a topic or taking time from the three-stage chain.
- Proposed destination: brief
- Status: accepted
- Note: brief, Opening and the first high-level topic. Also slide-outline, Axis 1, Opening shift.

### Native language

- Idea: Interacting with the AI in the speaker's native language lets the speaker be more precise, avoid inconsistencies, stay more relevant, and reach subtleties that would otherwise be forgotten or left unsaid.
- Why it matters: It explains why several excerpts are the French skills inside an English talk. It is a claim about how the speaker works, not a comparison of the French and English skill files.
- Proposed destination: later
- Status: accepted
- Note: slide-outline, Axis 3, "In your tools and your language", one support. Brief, how a skill works. The line is no longer tied to a file excerpt.

### Autres

- Ajouter en fin de présentation/conclusion que la méthode a été inspirée de BMAD et des pratiquse de harness engineering
  - Status: accepted
  - Note: slide-outline, close slide support. Brief unchanged.
- Ajouter aussi le fait de toujours se baser sur un problème réel personnel pour écrire une skill
  - Status: accepted
  - Note: slide-outline, folded into the close general idea. Brief unchanged.
- Les agents IA permettent d’adapter le monde aux spécificités de chacun : excellent outil d’intégration
  - Status: accepted
  - Note: slide-outline, one support on the opening slide. Inclusion. Brief unchanged.

### Talk order

- Idea: L’organisation de la présentation ne me convient pas vraiment. J’aimerais cet ordre :

D’abord une introduction basée sur l’idée que l’on entend souvent des présentations qui montrent ce que l’IA fait de bien et qu’aujourd’hui, j’aimerais montrer ce que je fais de bien grâce à l’IA.
En développement, l’idée générale est de me focaliser sur mes forces et déléguer ce avec quoi j’ai du mal à l’IA.

Ensuite j’aimerais une présentation générale du workflow de développement :
1. spécification
2. découpage en stories
3. découpage de chaque stories tâches/tutoriels/prompts pour générer le code, doc et les tests pour valider la conformité à la story
4. revue du code
5. validation du résultat dans un environnement simulé aussi proche de la prod possible
6. description + commit

Avec boucles de feedback qui revient à l’étape de spécification

Ensuite une étape où je rentre plus dans les détails de comment fonctionne une skill (type specification-fr/en).

- intégration des skills dans mon environnement de dev (intellij, protocole ACP, intégration de curson, commande)
- utilisation du langage naturel maternel, éviter les ambiguïtés, éviter les inconsistances, …
- mécanisme de questions/réponse
- génération de fichiers .md local au repos : éviter systèmes externes et serveurs MCP autant uqe possible (optimisation des tokens)

On termine par comment créer une skill.

- se focaliser sur ce que l’on a envie de faire
- expliciter le résultat attendu d’une étape
- expliciter ce sur quoi on a besoin d’être guidé/aidé
- demander à cursor d’écrire la skill pour nous

En conclusion les choses à retenir

- Objectif : focaliser sur ses forces et adresser ses difficultés à l’IA
- Inspiration : BMAD method, harness engineering
- Minset : spécifier aussi précisément le contexte plutôt que donner des instructions
- Why it matters: It replaces the previous slide order with the speaker's sequence.
- Proposed destination: brief
- Status: accepted
- Note: brief, Opening, High-level topics, Desired outcomes, Out of scope, and Constraints. slide-outline, Axis 1 Opening shift; Axis 2 The loop; Axis 3 all three developments; Close supports. slide-deck.md, the seven slides in that order.

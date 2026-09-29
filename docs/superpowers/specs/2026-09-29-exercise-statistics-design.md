# US-5.3 — Statistiques d'exercices

## Objectif

Ajouter un écran Android permettant à l'utilisateur de comprendre ses points forts et les groupes musculaires à travailler. L'écran utilise, en attendant la future API d'analyse des mouvements, un historique local fictif structuré et remplaçable.

## Expérience utilisateur

- Une troisième destination « Statistiques » est ajoutée à la navigation inférieure existante.
- Le haut de l'écran affiche le pourcentage global d'exercices réalisés parfaitement.
- Un unique mannequin stylisé vu de face colore séparément les épaules/dos, pectoraux, bras, tronc et jambes.
- La couleur traduit la progression : rouge de 0 à 39 %, orange de 40 à 69 %, vert de 70 à 100 %, gris lorsqu'aucune donnée n'existe.
- Toucher une zone du mannequin sélectionne le groupe musculaire correspondant et filtre les détails affichés sous le mannequin.
- Une liste « Par groupe musculaire » affiche le score agrégé et le nombre de séances de chaque groupe.
- Une liste « Par exercice » affiche, pour chaque exercice, le nombre de séances, son score moyen et son taux de réalisations parfaites.
- Un contrôle permet de basculer entre les deux listes. L'écran reste verticalement défilable sur les petits appareils.

## Données et calculs

Une entrée d'historique associe un exercice du catalogue à une date et à un score de qualité compris entre 0 et 100. Une séance est considérée parfaite lorsque son score est supérieur ou égal à 90.

- Le taux de perfection est le nombre de séances parfaites divisé par le nombre total de séances, arrondi à l'entier le plus proche.
- Le score d'un exercice est la moyenne arrondie de ses scores de qualité.
- Le score d'un groupe musculaire est la moyenne arrondie de toutes les séances des exercices appartenant au groupe.
- Un ensemble fixe d'entrées fictives couvre les exercices du dépôt et plusieurs niveaux de progression.
- Une collection vide produit des valeurs à zéro et l'état visuel « aucune donnée », sans division par zéro.

## Architecture

- `ExerciseSession` représente une séance terminée.
- `StatisticsRepository` expose l'historique local fictif. Son interface statique simple pourra être remplacée par une source persistante ou distante.
- `StatisticsCalculator` contient les agrégations pures et testables, indépendantes d'Android.
- `StatisticsViewModel` transforme le catalogue et l'historique en état d'écran et gère le groupe sélectionné.
- `MuscleBodyView` dessine le mannequin et ses zones, applique les couleurs de progression et convertit les touchers en sélection de groupe.
- `StatisticsActivity` lie l'état aux vues, affiche les deux modes de détail et participe à la navigation inférieure existante.

## Gestion des cas limites

- Les séances faisant référence à un exercice inconnu sont ignorées.
- Les scores entrants sont bornés entre 0 et 100 pour protéger l'affichage et les calculs.
- Les groupes sans séance restent visibles en gris avec la mention « Aucune donnée ».
- Une sélection sans exercice correspondant affiche un état vide explicite.
- Le mannequin fournit des descriptions d'accessibilité et les statistiques restent disponibles dans les listes textuelles.

## Validation

- Tests unitaires des calculs : arrondi, seuil de perfection, regroupement, exercice inconnu, score hors limites et historique vide.
- Tests unitaires du ViewModel : état initial, sélection musculaire et filtrage des détails.
- Build Android complet et suite de tests Gradle.
- Vérification manuelle de l'écran sur un appareil ou émulateur si une cible Android est disponible.

## Hors périmètre

- Connexion à une API, base de données ou caméra.
- Analyse réelle des mouvements.
- Modèle anatomique 3D ou vue arrière.
- Modification des fonctionnalités de création et de lecture d'exercices existantes, en dehors de l'ajout de la navigation vers les statistiques.

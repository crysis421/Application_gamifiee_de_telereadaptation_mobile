# Problèmes connus

Relevé au 01/10/2026, sur `main`. Chaque point a été vérifié dans le code ou sur
l'émulateur. Les corrections déjà faites ne figurent pas ici.

Ordre : du plus visible au plus anecdotique.

---

## 1. Les statistiques sont entièrement factices

**Ce qui se passe.** L'écran Statistiques affiche des chiffres inventés, identiques à
chaque ouverture, quoi que fasse l'utilisateur.

**Pourquoi.** `Statistic/StatisticsRepository.java` contient une liste de 15 séances
écrites en dur :

```java
private static final List<ExerciseSession> SESSIONS = Arrays.asList(
        session("squat", 18, 95), session("squat", 12, 88), ...
```

Rien n'écrit jamais dans ce dépôt — aucune classe en dehors de `StatisticsViewModel`
ne le référence. Et à la fin d'une vraie séance, `accueil/ui/SummaryActivity.java`
s'arrête sur un commentaire :

```java
// TODO (US-7.1) : c'est ici que la session devra être envoyée au journal de recherche.
```

**Conséquence.** La carte musculaire, le pourcentage global et les classements par
muscle et par exercice ne reflètent rien de réel. C'est le point le plus visible lors
d'une démonstration.

**Piste.** Rendre `StatisticsRepository` alimentable, l'enregistrer comme le catalogue,
et y écrire depuis `SummaryActivity` à la fin de chaque séance. Attention : il existe
deux classes `ExerciseSession` distinctes, celle des statistiques
(`exerciseId`, `completedAtEpochMillis`, `qualityScore`) et celle de la séance
(`validatedReps`, `failedReps`, `durationMillis`). Il faudra convertir de l'une à
l'autre, ou les unifier.

---

## 2. Les favoris ne mènent nulle part

Marquer un exercice en favori fonctionne et l'état persiste — le cœur de la carte
« Exercice du jour » et le bouton du récapitulatif écrivent bien dans
`FavoritesRepository` (SharedPreferences).

Mais **aucun écran ne permet de les consulter** : le catalogue ne les affiche pas et
n'offre pas de filtre « favoris ». `getFavoriteIds()` n'est appelé nulle part dans
l'application. La fonctionnalité s'arrête au stockage.

**Piste.** Un filtre « favoris » dans le catalogue (US-6.1).

---

## 3. La caméra MediaPipe est mal rattachée

`HolisticCameraActivity` n'est atteignable que depuis **un bouton du formulaire de
création d'exercice** (`CatalogueExercice/CreateExerciseActivity.java`). C'est un
emplacement inattendu : la détection de pose sert pendant une séance, pas pendant la
saisie d'un exercice.

Par ailleurs l'écran de séance (`accueil/ui/ExerciseActivity`) affiche toujours un
aperçu caméra factice avec deux boutons manuels de validation, sans lien avec
MediaPipe. Les deux travaux ne sont pas encore raccordés.

---

## 4. `ExercisePlayerActivity` ne marque pas son onglet

Les trois onglets appellent `setSelectedItemId` pour montrer où l'on se trouve, sauf
`CatalogueExercice/ExercisePlayerActivity.java`. Sur cet écran, la barre du bas
n'indique rien.

---

## 5. Avertissement « 16 KB » au lancement

Au démarrage, Android affiche un dialogue *Android App Compatibility* : l'application
n'est pas compatible avec les pages mémoire de 16 Ko, à cause de
`lib/x86_64/libimage_processing_util_jni.so`, livré par **CameraX 1.3.4**.

Ce n'est pas une erreur, l'application tourne en mode compatibilité. Ça deviendrait
bloquant pour une publication sur le Play Store.

**Piste.** Passer `cameraxVersion` de `1.3.4` à `1.4.x` dans `app/build.gradle.kts`.

---

## 6. Le modèle MediaPipe est versionné deux fois

Deux fichiers de 13,7 Mo, aux empreintes identiques :

- `app/src/main/assets/holistic_landmarker.task` — **nécessaire**, c'est celui qui est
  embarqué dans l'APK
- `app/holistic_landmarker.task` — **inutile**, hors du dossier `assets`, il n'est pas
  packagé

Le second peut être supprimé sans rien casser. Il fait porter 13,7 Mo inutiles à chaque
clone du dépôt.

---

## 7. Dossiers et packages ne concordent pas

Le commit `878b229` « réorganisation du projet par dossier » a déplacé 23 fichiers dans
`CatalogueExercice/`, `Muscle/` et `Statistic/` **sans toucher aux déclarations
`package`**, qui disent toutes encore `com.uphf.saes5`.

Ça compile — le plugin Android passe tous les `.java` à javac sans exiger que
l'arborescence corresponde — mais le dossier et le package ne veulent plus dire la même
chose, et Android Studio le signale.

**Deux sorties possibles**, à trancher en équipe :

- en faire de vrais packages (`com.uphf.saes5.catalogue`, `.muscle`, `.statistiques`),
  ce qui touche environ 25 fichiers, le manifeste, les layouts et les tests ;
- remettre les fichiers à la racine, ce qui rétablit la cohérence sans modifier une
  seule ligne mais annule l'organisation voulue.

À faire quand les branches en cours seront fusionnées : le chantier entre en conflit
avec tout le monde.

---

## 8. État des branches

- **`creerexo`** — branche orpheline, aucun ancêtre commun avec `main`, impossible à
  fusionner normalement. C'est en fait un autre projet (package
  `com.example.mouvetoi`), avec **525 fichiers indésirables sur 548** (`.gradle/`,
  `.idea/`, `app/build/`, `local.properties`), faute de `.gitignore` à la racine. À
  supprimer après vérification.
- **`user-profile-selyanek`** — la page de profil a été écrite **dans**
  `activity_main.xml` au lieu d'un `activity_profile.xml` dédié, donc elle écrase
  l'accueil. Conflit garanti. Ses couleurs sont claires alors que l'application est
  noir et jaune, et ses libellés sont en dur dans le layout au lieu de `strings.xml`.
- **`feature/us-5-3-statistiques-exercices`** — périmée, 6 fichiers en conflit avec
  `main` seul. Son contenu est déjà dans `main` sous une autre forme.

---

## 9. Le catalogue n'est pas une vraie base de données

`ExerciseRepository` enregistre le catalogue dans les SharedPreferences, sérialisé en
JSON. C'est volontairement sommaire, en attendant le remaniement « base de données et
modèle de données » du backlog. Deux effets de bord :

- supprimer un des cinq exercices d'origine est définitif, il ne revient pas ;
- les exercices par défaut sont figés par installation : en ajouter plus tard ne les
  fera pas apparaître chez ceux qui ont déjà lancé l'application.

`resetToDefaults()` existe dans le dépôt si l'on veut brancher un bouton
« réinitialiser le catalogue ».

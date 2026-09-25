PROJET MouveToi — US-2.1 Créer un exercice
============================================

CONTENU
- Un écran d'accueil avec un bouton "Créer un exercice"
- Un formulaire complet : nom, description, groupe musculaire, matériel,
  difficulté, durée estimée
- Validation des champs obligatoires avant "enregistrement"
- Pas de catalogue : à l'enregistrement, un message confirme la création
  et l'écran se ferme

OUVRIR LE PROJET
1. Décompresse ce zip.
2. Dans Android Studio : File > Open... puis sélectionne le dossier
   "MouveToi" décompressé (celui qui contient build.gradle et settings.gradle).
3. Ce projet ne contient PAS le wrapper Gradle (gradlew / gradle-wrapper.jar)
   car ce sont des fichiers binaires. Au premier ouverture, Android Studio
   va très probablement :
   - soit te proposer automatiquement de générer le wrapper manquant
     (clique sur "OK" / "Fix" dans le bandeau qui apparaît),
   - soit te permettre de choisir "Use Gradle from: 'wrapper'" et
     Android Studio le régénérera tout seul,
   - soit, en dernier recours, change ce paramètre dans
     File > Settings > Build Tools > Gradle > "Use Gradle from" en
     sélectionnant une installation Gradle locale déjà présente sur ta
     machine (ou celle intégrée à Android Studio).
4. Laisse la synchronisation Gradle se terminer (ça télécharge les
   dépendances la première fois, connexion internet nécessaire).
5. Lance l'app sur un émulateur ou un téléphone connecté (bouton ▶️ vert).

STRUCTURE
MouveToi/
  build.gradle, settings.gradle, gradle.properties   -> config Gradle du projet
  app/build.gradle                                   -> config Gradle du module app
  app/src/main/AndroidManifest.xml
  app/src/main/java/com/example/mouvetoi/
    MainActivity.java          -> écran d'accueil avec le bouton
    CreerExerciceActivity.java -> le formulaire + validation
    Exercice.java               -> le modèle de données
  app/src/main/res/layout/     -> les deux écrans XML
  app/src/main/res/values/     -> textes, couleurs, thème, listes déroulantes
  app/src/main/res/drawable + mipmap-anydpi-v26/ -> icône de l'app

À ADAPTER
- Le package est "com.example.mouvetoi" — change-le si besoin (Android
  Studio propose un renommage automatique via clic droit > Refactor > Rename
  sur le package, dans l'explorateur de projet).
- La persistance (BDD/API) n'est pas branchée : l'objet Exercice est
  juste construit en mémoire dans CreerExerciceActivity, prêt à être
  envoyé à un ViewModel/Repository plus tard.

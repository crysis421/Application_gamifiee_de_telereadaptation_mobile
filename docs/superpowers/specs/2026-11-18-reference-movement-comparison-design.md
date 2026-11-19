# US-1.2 — Comparaison à un modèle de référence

## Intent

Lorsqu'un utilisateur lance un exercice, la caméra doit comparer son mouvement au modèle
enregistré pour cet exercice. L'utilisateur reçoit sans délai perceptible un score de similitude
et un retour « mouvement validé » ou « à corriger ».

## Goals

- Associer durablement un mouvement de référence et un seuil de similitude à chaque exercice.
- Utiliser les angles articulaires plutôt que les coordonnées absolues afin de résister à la
  taille, à la position et à la distance de l'utilisateur.
- Afficher un score vivant pendant la séance puis valider une répétition complète lorsque la
  séquence atteint le seuil.
- Conserver le compteur, le chronomètre, les sons, le récapitulatif et le journal de séance déjà
  présents.
- Rester compatible avec les exercices et mouvements enregistrés avant cette évolution.

## Non-goals

- Reconnaître automatiquement n'importe quel exercice sans modèle enregistré.
- Synchroniser les modèles avec un serveur ou une base de données distante.
- Construire un éditeur articulation par articulation.
- Remplacer MediaPipe ou CameraX.

## Approaches considered

1. **Rediriger vers l'écran MediaPipe existant.** Rapide, mais le score arrive après une capture
   fixe et l'utilisateur quitte la vraie séance.
2. **Intégrer MediaPipe à la séance avec un moteur de comparaison séparé — choisi.** L'écran de
   séance garde son comportement, tandis que les calculs restent testables sans Android.
3. **Construire immédiatement un automate complet départ/cible/retour.** Plus précis pour le
   comptage, mais dépasse US-1.2 et exige des modèles annotés plus riches.

## Data model and persistence

`Exercise` reçoit `referenceMovementId` et `similarityThreshold`. Le seuil par défaut vaut 80 et
est borné entre 1 et 100. Les anciens JSON qui ne contiennent pas ces champs utilisent
l'identifiant de l'exercice comme référence et le seuil 80.

Le formulaire de création génère l'identifiant définitif dès son ouverture. Le bouton
« Enregistrer le modèle » enregistre donc directement le mouvement sous cet identifiant, puis le
même identifiant est persisté avec l'exercice. En modification, un nouvel enregistrement remplace
le modèle existant.

## Comparison algorithm

Les 33 points MediaPipe restent normalisés autour des hanches et de la longueur du torse pour le
stockage existant. Pour la comparaison, chaque image est transformée en huit angles : coudes,
épaules, hanches et genoux, à gauche et à droite. Une translation, une mise à l'échelle ou un
miroir de la pose ne change donc pas ces valeurs.

Le moteur pré-calcule les angles du modèle. À chaque nouvel échantillon (100 ms) :

- le meilleur rapprochement avec une image du modèle produit le score de posture immédiat ;
- une fenêtre glissante de la longueur du modèle est comparée par DTW pour produire le score du
  mouvement complet ;
- une séquence dépassant le seuil émet une seule validation, vide la fenêtre et attend une
  nouvelle séquence avant de pouvoir valider à nouveau.

Les différences angulaires sont normalisées sur 180 degrés. Les scores sont bornés de 0 à 100.
Les images incomplètes sont ignorées.

## Session behavior

`ExerciseActivity` demande la permission caméra, affiche le flux et le squelette, puis alimente le
moteur. Le bandeau affiche le score courant et devient vert au-dessus du seuil, rouge en dessous.
Une séquence validée appelle le compteur existant et les sons existants. Le score moyen des
séquences terminées est transmis au récapitulatif et au journal.

Si aucun modèle n'est disponible, la séance ne plante pas : elle explique qu'il faut enregistrer
une référence et conserve les boutons manuels comme solution de démonstration. Avec un modèle,
ces boutons sont masqués.

## Performance and lifecycle

La référence angulaire est calculée une seule fois. La fenêtre est bornée à la taille du modèle et
CameraX conserve uniquement la dernière image disponible. L'analyse s'arrête dans le cycle de vie
de l'activité et libère MediaPipe, l'analyseur et l'exécuteur.

## Verification

- Tests unitaires des angles, invariances, scores DTW, seuils et émission unique des validations.
- Tests de sérialisation et de compatibilité des anciens exercices.
- Tests du score moyen transmis à la séance et au journal.
- Compilation de l'APK et vérification manuelle sur émulateur avec et sans modèle enregistré.

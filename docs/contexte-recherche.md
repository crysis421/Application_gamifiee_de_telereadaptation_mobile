# Contexte de recherche

Le projet s'appuie sur le poster **« Implementing Real-Time Markerless Motion Capture in Smartphone Exergames »**
(Mathieu Phosanarack, Laura Wallard, Sophie Lepreux, Christophe Kolski, Eugénie Avril —
LAMIH UMR CNRS 8201, Univ. Polytechnique Hauts-de-France, Valenciennes ;
Mardi des Chercheurs 2025, Avril 2025, ⟨hal-05026604⟩).
PDF : https://drive.google.com/file/d/1nns2OxuVbwSDBewwakZQisQyyS402zN6/view

## Points clés du poster

- La caméra du smartphone + estimation de pose par IA rendent les exergames accessibles et
  abordables pour la rééducation et l'activité physique (pas de matériel spécialisé).
- **Compromis précision / latence** : les modèles légers améliorent la réactivité mais
  dégradent la précision du mouvement.
- Le **positionnement de l'utilisateur** conditionne la visibilité et le suivi. Solutions :
  éléments d'interface de grande taille, feedback auditif, exercices centrés sur le haut du corps
  (permettant de rester près de l'écran).
- Pistes d'optimisation : réduire le nombre de calculs/seconde en n'estimant pas la pose à chaque
  frame (frame skipping), off-loading de l'estimation vers un serveur/cloud.
- Travaux futurs : modèles légers plus précis, interactions vocales/gestuelles, accessibilité pour
  utilisateurs à besoins spécifiques, études utilisateurs pour valider le design d'interaction.

## Chaîne technique visée

Caméra mobile → flux image temps réel → estimation de pose IA → position des articulations
exploitée pour des contrôles basés sur le mouvement.

## Références du poster

1. Phosanarack, M., Avril, E., Lepreux, S., Wallard, L., & Kolski, C. (2025). *User-centered
   personalized gamification: an umbrella review.* User Modeling and User-Adapted Interaction.
   https://doi.org/10.1007/s11257-024-09423-z
2. Meulenberg, C. J. W., De Bruin, E. D., & Marusic, U. (2022). *A Perspective on Implementation of
   Technology-Driven Exergames for Adults as Telerehabilitation Services.* Frontiers in Psychology, 13.
3. Bazarevsky, V., & Grishchenko, I. (2020). *On-device, Real-time Body Pose Tracking with MediaPipe
   BlazePose.* https://research.google/blog/on-device-real-time-body-pose-tracking-with-mediapipe-blazepose/
4. Jo, B., & Kim, S. (2022). *Comparative Analysis of OpenPose, PoseNet, and MoveNet Models for Pose
   Estimation in Mobile Devices.* Traitement du Signal, 39(1), 119-124. https://doi.org/10.18280/ts.390111

# Backlog — Application d'exercices physiques du quotidien

Application Android d'activité physique à domicile utilisant la caméra et l'estimation de pose
(MediaPipe) pour valider les mouvements, avec gamification modulable et journalisation à visée de
recherche. Voir [contexte-recherche.md](contexte-recherche.md).

## Rôles / Personas

| Rôle | Description |
|---|---|
| Utilisateur (U) | Personne qui réalise les exercices à domicile. |
| Administrateur (A) | Gère le contenu (exercices) et la configuration de l'application. |
| Chercheur (C) | Exploite l'application comme matériel de recherche : paramétrage des conditions de test, analyse des logs. |
| Coach certifié (K) | (Optionnel — à confirmer) Crée des exercices validés dans le catalogue. |

## Légende des priorités

- 🟢 **MVP** — Itération 1, indispensable au fonctionnement de base.
- 🔵 **Futur** — Itérations ultérieures.
- ⚪ **À confirmer** — Dépend d'une décision client (aspect compétitif/social, rôle coach…).

---

## Epic 1 — Moteur de reconnaissance des mouvements

### US-1.1 — Détection de pose en temps réel 🟢
**En tant qu'** utilisateur
**Je souhaite** que l'application détecte ma posture via la caméra du téléphone en temps réel
**Afin de** réaliser des exercices sans capteur ni matériel spécialisé

Critères d'acceptation :
- [ ] La caméra s'active après demande explicite de permission ; un refus est géré (message + pas de crash).
- [ ] Les points clés du corps (squelette) sont détectés via MediaPipe et affichés en surimpression sur le flux vidéo.
- [ ] La détection tourne à une fréquence cible fluide (seuil FPS à définir lors des tests device, ex. 15–30 FPS).
- [ ] Si aucune personne n'est détectée pendant N secondes, un message d'aide au cadrage s'affiche.

### US-1.2 — Comparaison à un modèle de référence 🟢
**En tant qu'** utilisateur
**Je souhaite** que mon mouvement soit comparé à une position/mouvement de référence
**Afin de** savoir si je réalise correctement l'exercice

Critères d'acceptation :
- [ ] Un score de similarité est calculé entre ma pose et la référence de l'exercice.
- [ ] Un seuil de tolérance (configurable) déclenche la validation ou l'invalidation du mouvement.
- [ ] Le résultat (validé / à corriger) est renvoyé sans latence perceptible.
- [ ] La comparaison tient compte des angles articulaires plutôt que des positions absolues (indépendance à la taille/distance de l'utilisateur).

### US-1.3 — Comptage des répétitions 🟢
**En tant qu'** utilisateur
**Je souhaite** que l'application compte mes répétitions valides
**Afin de** suivre ma progression pendant l'exercice

Critères d'acceptation :
- [ ] Une répétition n'est comptée que si le mouvement complet est validé (pose de départ → pose cible → retour).
- [ ] Le compteur est affiché en gros et mis à jour en temps réel.
- [ ] Les mouvements incomplets ou incorrects ne sont pas comptés.

### US-1.4 — IA légère et performances 🟢
**En tant qu'** utilisateur
**Je souhaite** une reconnaissance fluide, sans lag perceptible
**Afin de** conserver une expérience réactive et agréable

Critères d'acceptation :
- [ ] Utilisation d'un modèle léger adapté au mobile (ex. MediaPipe / BlazePose « lite »).
- [ ] Possibilité de ne pas estimer la pose à chaque frame (paramètre de frame skipping configurable), pour réduire le nombre de calculs/seconde.
- [ ] La latence perçue reste sous un seuil à définir (ex. < 100 ms).
- [ ] (Futur 🔵) Option d'off-loading de l'estimation vers un serveur/cloud.

---

## Epic 2 — Moteur de création d'exercices

### US-2.1 — Créer un exercice 🟢
**En tant qu'** administrateur (ou coach certifié ⚪)
**Je souhaite** créer un exercice en renseignant nom, description, mouvement de référence, matériel et difficulté
**Afin d'** enrichir le catalogue proposé aux utilisateurs

Critères d'acceptation :
- [ ] Formulaire avec les champs : nom, description, groupe(s) musculaire(s), matériel, difficulté, durée estimée.
- [ ] Les champs obligatoires sont validés avant l'enregistrement.
- [ ] Après enregistrement, l'exercice apparaît dans le catalogue.
- [ ] L'exercice est associé à une position/mouvement de référence (voir US-2.2).

### US-2.2 — Définir la position de référence 🟢
**En tant qu'** administrateur
**Je souhaite** enregistrer une pose/mouvement de référence en me plaçant devant la caméra
**Afin de** fournir un modèle de comparaison au moteur de reconnaissance

Critères d'acceptation :
- [ ] Les points clés sont capturés lors d'une démonstration face caméra.
- [ ] Possibilité de définir une tolérance (ex. par articulation / angle).
- [ ] Prévisualisation de la référence et possibilité de la ré-enregistrer avant validation.

### US-2.3 — Modifier / supprimer un exercice 🟢
**En tant qu'** administrateur
**Je souhaite** modifier ou supprimer un exercice existant
**Afin de** corriger ou retirer un contenu obsolète

Critères d'acceptation :
- [ ] L'édition permet de changer les métadonnées et/ou la référence.
- [ ] Une confirmation est demandée avant suppression.
- [ ] La suppression retire l'exercice du catalogue sans casser les logs/historique existants.

---

## Epic 3 — Interface utilisateur & feedback

### US-3.1 — Interface large et lisible à distance 🟢
**En tant qu'** utilisateur
**Je souhaite** une interface avec de grands éléments visuels
**Afin de** lire les informations et interagir alors que je suis loin de l'écran (le suivi corps entier m'éloigne)

Critères d'acceptation :
- [ ] Boutons, compteurs et textes de grande taille avec contrastes élevés.
- [ ] Les informations clés (répétitions, validation, chrono) restent lisibles à plusieurs mètres.
- [ ] La navigation principale reste utilisable de loin (peu d'actions fines requises pendant l'exercice).

### US-3.2 — Feedback visuel en temps réel 🟢
**En tant qu'** utilisateur
**Je souhaite** un retour visuel immédiat sur ma posture
**Afin de** corriger mon mouvement pendant l'exercice

Critères d'acceptation :
- [ ] Le squelette change de couleur/état selon la validité (ex. vert = correct, rouge = à corriger).
- [ ] Un indicateur clair signale la validation d'une répétition.

### US-3.3 — Feedback auditif 🟢
**En tant qu'** utilisateur
**Je souhaite** des retours sonores (validation, erreur, encouragement)
**Afin d'** être guidé sans avoir à lire l'écran de loin

Critères d'acceptation :
- [ ] Sons distincts pour validation, erreur et fin d'exercice.
- [ ] Les retours auditifs sont activables/désactivables (lien avec le moteur de gamification et le paramétrage recherche).

### US-3.4 — Exercices centrés sur le haut du corps 🔵
**En tant qu'** utilisateur
**Je souhaite** pouvoir choisir des exercices centrés sur le haut du corps
**Afin de** rester plus proche de l'écran et garder une bonne visibilité

Critères d'acceptation :
- [ ] Le catalogue permet de filtrer/identifier les exercices « haut du corps ».
- [ ] Ces exercices restent reconnus même à faible distance de la caméra.

---

## Epic 4 — Moteur de gamification

### US-4.1 — Activer / désactiver les éléments de jeu 🟢
**En tant que** chercheur (ou administrateur)
**Je souhaite** activer ou désactiver individuellement chaque élément de gamification
**Afin de** comparer différentes conditions expérimentales

Critères d'acceptation :
- [ ] Chaque élément de jeu (sons, points, rangs, exercice du jour…) possède un interrupteur on/off.
- [ ] La configuration est persistée et appliquée à la session utilisateur.
- [ ] L'état actif des éléments est journalisé (voir Epic 7) pour rattacher les données à une condition de test.

### US-4.2 — Exercice du jour 🟢
**En tant qu'** utilisateur
**Je souhaite** voir un « exercice du jour » proposé
**Afin de** garder une routine et rester engagé

Critères d'acceptation :
- [ ] Un exercice est mis en avant chaque jour sur l'accueil.
- [ ] La réalisation de l'exercice du jour est traçable.

### US-4.3 — Sons de récompense 🟢
**En tant qu'** utilisateur
**Je souhaite** entendre des sons de récompense lors de mes réussites
**Afin de** ressentir une satisfaction qui me motive

Critères d'acceptation :
- [ ] Un son de récompense se déclenche sur événement positif (répétition/série réussie).
- [ ] Désactivable via US-4.1.

### US-4.4 — Points et multiplicateurs (mode arcade) 🔵
**En tant qu'** utilisateur
**Je souhaite** gagner des points et des multiplicateurs selon la qualité de mes mouvements
**Afin de** viser la réussite parfaite (100 %) du mouvement

Critères d'acceptation :
- [ ] Les points augmentent avec la précision du mouvement.
- [ ] Un multiplicateur récompense les réussites consécutives ; une erreur le réinitialise (aspect « punitif »).
- [ ] Le score est affiché en temps réel.

### US-4.5 — Rangs par muscle et étoiles par exercice 🔵
**En tant qu'** utilisateur
**Je souhaite** améliorer mon rang sur chaque groupe musculaire et gagner des étoiles par exercice
**Afin de** visualiser ma progression et rester motivé

Critères d'acceptation :
- [ ] Un rang par groupe musculaire évolue avec les points accumulés.
- [ ] Des étoiles sont attribuées par exercice au-delà de seuils de points.
- [ ] La progression est visible depuis le profil / les statistiques.

### US-4.6 — Compétition : classement / 1v1 ⚪
**En tant qu'** utilisateur
**Je souhaite** me comparer aux autres via un tableau des scores et/ou des duels 1v1
**Afin de** me motiver par la compétition

Critères d'acceptation :
- [ ] (À confirmer avec le client — risque d'être jugé trop compétitif.)
- [ ] Si retenu : un classement basé sur les scores et/ou un mode 1v1 comparant les scores obtenus.
- [ ] Nécessite un système de profil et de contacts (voir Epic 8).

---

## Epic 5 — Interface d'accueil & statistiques

### US-5.1 — Affichage du nombre de pas 🟢
**En tant qu'** utilisateur
**Je souhaite** voir mon nombre de pas sur l'accueil
**Afin de** suivre mon activité physique globale

Critères d'acceptation :
- [ ] Les pas sont récupérés via l'API recorder d'Android.
- [ ] Le nombre est mis à jour et affiché en grand sur l'accueil.
- [ ] L'absence de permission/capteur est gérée proprement.

### US-5.2 — Nombre d'exercices réalisés 🟢
**En tant qu'** utilisateur
**Je souhaite** voir combien d'exercices j'ai réalisés
**Afin de** mesurer mon assiduité

Critères d'acceptation :
- [ ] Un compteur total (et/ou par période) est affiché sur l'accueil.
- [ ] Le compteur s'incrémente à chaque exercice terminé.

### US-5.3 — Statistiques d'exercices 🔵
**En tant qu'** utilisateur
**Je souhaite** consulter mes statistiques d'entraînement
**Afin de** comprendre mes forces et mes points à travailler

Critères d'acceptation :
- [ ] Un mannequin/schéma corporel colore les muscles selon le travail effectué.
- [ ] Le pourcentage d'exercices réalisés « parfaitement » est affiché.
- [ ] Les statistiques sont consultables par exercice et/ou par groupe musculaire.

### US-5.4 — Modulation des paramètres de l'accueil 🟢
**En tant que** chercheur
**Je souhaite** activer/désactiver ou configurer les éléments affichés sur l'accueil
**Afin de** tester différentes conditions expérimentales

Critères d'acceptation :
- [ ] Chaque bloc de l'accueil (pas, exercices réalisés, stats…) peut être affiché/masqué.
- [ ] La configuration est persistée et journalisée avec la condition de test.

---

## Epic 6 — Catalogue & recherche d'exercices

### US-6.1 — Consulter et filtrer le catalogue 🟢
**En tant qu'** utilisateur
**Je souhaite** parcourir et filtrer les exercices
**Afin de** trouver rapidement un exercice adapté

Critères d'acceptation :
- [ ] Filtres disponibles : groupe musculaire, difficulté, durée, matériel.
- [ ] Chaque exercice affiche ses métadonnées clés (nom, difficulté, matériel).
- [ ] La sélection d'un exercice permet de le démarrer.

### US-6.2 — Rangs/étoiles visibles dans le catalogue 🔵
**En tant qu'** utilisateur
**Je souhaite** voir mon rang (étoiles) sur chaque exercice du catalogue
**Afin de** repérer les exercices que je maîtrise ou qu'il me reste à progresser

Critères d'acceptation :
- [ ] Les étoiles obtenues (US-4.5) apparaissent sur la fiche de chaque exercice.

### US-6.3 — Retrouver un exercice à partir d'un mouvement 🔵
**En tant qu'** utilisateur
**Je souhaite** effectuer un mouvement devant la caméra pour retrouver l'exercice correspondant
**Afin de** lancer un exercice sans le chercher manuellement

Critères d'acceptation :
- [ ] (Fonction avancée) Le mouvement capté est comparé aux références du catalogue.
- [ ] Le/les exercice(s) le(s) plus proche(s) sont proposés.

---

## Epic 7 — Journalisation & matériel de recherche

### US-7.1 — Journalisation exploitable des sessions 🟢
**En tant que** chercheur
**Je souhaite** des logs structurés et exploitables des sessions d'exercice
**Afin de** les analyser après coup

Critères d'acceptation :
- [ ] Chaque session enregistre au minimum : horodatage, exercice, nombre de répétitions, score de similarité/précision, validations/erreurs, paramètres actifs (gamification et accueil).
- [ ] Les logs sont dans un format exploitable (ex. CSV / JSON).
- [ ] Aucune vidéo n'est stockée (trop lourd) ; seules les données choisies sont conservées (à définir précisément avec l'équipe).

### US-7.2 — Export des logs 🟢
**En tant que** chercheur
**Je souhaite** exporter les logs
**Afin de** les traiter dans un outil d'analyse externe

Critères d'acceptation :
- [ ] Export possible des logs sur une période donnée.
- [ ] Le fichier exporté est directement lisible par un tableur / outil d'analyse.

### US-7.3 — Configurer une condition de test 🟢
**En tant que** chercheur
**Je souhaite** définir une combinaison de paramètres (gamification + accueil)
**Afin de** mener différents tests comparables entre eux

Critères d'acceptation :
- [ ] Un ensemble de paramètres peut être défini, nommé et appliqué.
- [ ] L'identifiant de la condition de test est associé aux logs générés.

---

## Epic 8 — Comptes, profils & social

### US-8.1 — Gestion des rôles ⚪
**En tant qu'** administrateur
**Je souhaite** distinguer les rôles (administrateur, utilisateur, coach certifié)
**Afin de** limiter la création/suppression d'exercices aux profils autorisés

Critères d'acceptation :
- [ ] (Choix du modèle de rôles à confirmer — Option 1 : admin/utilisateur ; Option 2 : admin/utilisateur/coach.)
- [ ] La création/édition/suppression d'exercices est réservée aux rôles autorisés.

### US-8.2 — Profil utilisateur 🔵
**En tant qu'** utilisateur
**Je souhaite** disposer d'un profil regroupant mes stats et rangs
**Afin de** suivre ma progression au fil du temps

Critères d'acceptation :
- [ ] Le profil affiche rangs par muscle, étoiles, historique.
- [ ] Onglet « profil » accessible depuis la navigation principale.

### US-8.3 — Contacts / comparaison sociale ⚪
**En tant qu'** utilisateur
**Je souhaite** ajouter/supprimer des contacts et comparer mes rangs et stats
**Afin de** me motiver socialement

Critères d'acceptation :
- [ ] (À confirmer — potentiellement hors périmètre, l'app n'étant pas centrée sur le social.)
- [ ] Si retenu : ajout/suppression d'amis et comparaison des rangs/stats.

---

## Epic 9 — Éléments transverses

### US-9.1 — Permissions & confidentialité 🟢
**En tant qu'** utilisateur
**Je souhaite** être informé de l'usage de la caméra et de mes données
**Afin de** donner un consentement éclairé (contexte de recherche)

Critères d'acceptation :
- [ ] Les permissions (caméra, activité physique) sont demandées avec explication.
- [ ] Le traitement de la pose se fait de préférence on-device (pas de flux vidéo stocké).
- [ ] Une notice/consentement adapté au cadre recherche est présenté.

### US-9.2 — Identité de l'application ⚪
**En tant qu'** équipe projet
**Je souhaite** définir le nom, la charte graphique (couleurs) et les onglets (accueil, catalogue, profil)
**Afin d'** offrir une ambiance cohérente et une navigation claire

Critères d'acceptation :
- [ ] Nom et palette de couleurs validés.
- [ ] Navigation à onglets : accueil, catalogue, profil (à confirmer).

---

## État d'implémentation — limites connues

Les écarts entre ce qui est annoncé et ce qui fonctionne réellement sont recensés dans
[problemes-connus.md](problemes-connus.md), tenu à jour à part pour ne pas alourdir ce
backlog.

## Remaniements demandés (à appliquer — pas encore faits)

1. **Regrouper les US de paramétrage pour le chercheur** — aujourd'hui éclatées entre US-4.1
   (interrupteurs gamification), US-5.4 (blocs de l'accueil) et US-7.3 (condition de test).
2. **Regrouper les US sur les effets sonores** — aujourd'hui US-3.3 (feedback auditif) et
   US-4.3 (sons de récompense).
3. **Ajouter une US sur la création de la base de données et le modèle de données**
   (entités exercices, références de pose, sessions/logs, profils, paramètres).

## Points à clarifier avec le client (avant de figer le périmètre)

1. **Compétition vs collaboration** : classement / 1v1 souhaités, ou activités de groupe, ou aucun des deux ? (impacte US-4.6 et US-8.3)
2. **Modèle de rôles** : admin + utilisateur, ou ajout d'un rôle coach certifié ? (US-8.1)
3. **Dimension sociale** : profils/contacts nécessaires ou hors périmètre ?
4. **Données conservées dans les logs** : quelles métriques exactement (pas de vidéo) ?
5. **Nom + ambiance de l'application.**

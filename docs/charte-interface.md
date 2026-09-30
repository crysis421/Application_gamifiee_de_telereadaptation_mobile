# Charte d'interface — application exergame

Document de référence pour que **tous les écrans de l'app se ressemblent**, quel que soit
celui qui les développe. Tout ce qui suit existe déjà dans le projet : il n'y a rien à
recréer, seulement à réutiliser.

- Stack : **Java + XML/Views** (pas de Jetpack Compose), Material 3 (`com.google.android.material`).
- Thème appliqué à toute l'app : `Theme.MyApplication` (parent `Theme.Material3.DayNight.NoActionBar`).
- `minSdk` 24, `viewBinding` activé.

**Règle n°1 :** on n'écrit jamais une couleur ou une taille « en dur » dans un layout.
On utilise un attribut de thème (`?attr/...`) ou une ressource (`@color/...`, `@dimen/...`).
Si une valeur manque, on l'ajoute dans `colors.xml` / `dimens.xml` et on prévient le groupe.

---

## 1. Couleurs

Fichiers : `app/src/main/res/values/colors.xml` (clair) et
`app/src/main/res/values-night/colors.xml` (sombre).

La palette est branchée sur le thème, donc **dans les layouts on utilise les attributs
`?attr/...`**, pas les `@color/md_...`. C'est ce qui fait que le mode sombre fonctionne
tout seul.

### 1.1 Couleur principale (vert = activité physique)

| Rôle | Attribut à utiliser | Clair | Sombre | Quand s'en servir |
|---|---|---|---|---|
| Primary | `?attr/colorPrimary` | `#006C4E` | `#69DBAA` | Boutons pleins, compteur de répétitions, titre du récap |
| On primary | `?attr/colorOnPrimary` | `#FFFFFF` | `#003825` | Texte/icône **posé sur** primary |
| Primary container | `?attr/colorPrimaryContainer` | `#86F8C4` | `#005138` | Blocs verts doux, mises en avant |
| On primary container | `?attr/colorOnPrimaryContainer` | `#002115` | `#86F8C4` | Texte posé sur primary container |

### 1.2 Couleur secondaire (cartes de contenu)

| Rôle | Attribut | Clair | Sombre | Usage |
|---|---|---|---|---|
| Secondary | `?attr/colorSecondary` | `#4B635A` | `#B1CCC1` | Accents discrets |
| On secondary | `?attr/colorOnSecondary` | `#FFFFFF` | `#1D352C` | Texte sur secondary |
| Secondary container | `?attr/colorSecondaryContainer` | `#CDE9DC` | `#344C42` | **Fond des cartes principales** (ex. carte « Exercice du jour ») |
| On secondary container | `?attr/colorOnSecondaryContainer` | `#072018` | `#CDE9DC` | Tout le texte de ces cartes |

### 1.3 Fonds et textes neutres

| Rôle | Attribut | Clair | Sombre | Usage |
|---|---|---|---|---|
| Surface / background | `?attr/colorSurface` | `#FBFDF9` | `#191C1B` | Fond des écrans (appliqué par le thème, ne pas le redéclarer) |
| On surface | `?attr/colorOnSurface` | `#191C1B` | `#E1E3E0` | Texte principal, titres |
| Surface variant | `?attr/colorSurfaceVariant` | `#DBE5DE` | `#3F4945` | Fond des tuiles de stats, zone caméra |
| On surface variant | `?attr/colorOnSurfaceVariant` | `#3F4945` | `#BFC9C4` | Texte secondaire, libellés, chrono |
| Outline | `?attr/colorOutline` | `#6F7975` | `#899390` | Bordures, traits pointillés |

### 1.4 Erreur

| Rôle | Attribut | Clair | Sombre |
|---|---|---|---|
| Error | `?attr/colorError` | `#BA1A1A` | `#FFB4AB` |
| On error | `?attr/colorOnError` | `#FFFFFF` | `#690005` |
| Error container | `?attr/colorErrorContainer` | `#FFDAD6` | `#93000A` |
| On error container | `?attr/colorOnErrorContainer` | `#410002` | `#FFDAD6` |

À réserver aux **vraies erreurs** (caméra indisponible, permission refusée, échec de
sauvegarde) — pas à un mouvement raté, qui n'est pas une erreur de l'app.

### 1.5 Succès (hors Material)

Material 3 n'a pas de rôle « succès ». On a donc ajouté trois couleurs maison, qui
s'utilisent **en `@color/...`** (elles basculent quand même en mode sombre) :

| Ressource | Clair | Sombre | Usage |
|---|---|---|---|
| `@color/app_success` | `#2E7D32` | `#7EDB85` | Icône + texte « fait aujourd'hui », coche de validation |
| `@color/app_success_container` | `#C3F0C4` | `#12521B` | Fond du bandeau « Répétition validée », badge « objectif atteint » |
| `@color/app_on_success_container` | `#002204` | `#C3F0C4` | Texte/icône posés sur ce fond |

**Convention de sens :** vert = mouvement validé / objectif atteint ; rouge = erreur
technique. Un mouvement non validé se signale par un retour **neutre** (texte sur
`?attr/colorOnSurfaceVariant`), pas par du rouge.

### 1.6 Ce qu'il ne faut pas faire

- ❌ `android:textColor="#FFFFFF"` ou `"@android:color/black"` → illisible en mode sombre.
- ❌ Réintroduire `purple_500` / `teal_200` ou toute couleur du template Android par défaut.
- ❌ Utiliser `@color/md_primary` directement dans un layout → utiliser `?attr/colorPrimary`.
- ❌ Ajouter une nouvelle teinte (bleu, orange…) sans en parler au groupe.

---

## 2. Typographie

Police : celle du système (Roboto). Pas de police custom.

Contrainte forte du projet : pendant l'exercice, **l'utilisateur est à plusieurs mètres de
l'écran**. Toutes les tailles sont donc volontairement plus grandes que la normale.
Elles sont centralisées dans `app/src/main/res/values/dimens.xml` :

| Ressource | Taille | Style | Usage |
|---|---|---|---|
| `@dimen/text_counter_huge` | `96sp` | bold, `?attr/colorPrimary` | Compteur de répétitions plein écran (ex. « 7 / 12 ») |
| `@dimen/text_stat_value` | `40sp` | bold | Valeur chiffrée d'une tuile de stat |
| `@dimen/text_title` | `28sp` | bold | Titre d'écran, nom de l'exercice, chrono |
| `@dimen/text_body_large` | `18sp` | normal | **Texte courant par défaut** (descriptions, métadonnées, messages) |
| `@dimen/button_text_large` | `22sp` | — | Libellé des boutons d'action principaux |

Tailles littérales tolérées pour le petit texte (elles restent rares, à ne pas multiplier) :

- `16sp` : libellé en majuscules sous le compteur.
- `15sp` : libellé d'une tuile de stat.
- `14sp` : sur-titre en majuscules d'une carte (ex. « EXERCICE DU JOUR »).
- `18sp` : libellé des boutons secondaires / tertiaires.

**Sur-titre (eyebrow)** — le motif utilisé pour coiffer une carte :

```xml
android:textAllCaps="true"
android:letterSpacing="0.12"
android:textSize="14sp"
android:textStyle="bold"
```

Règles : toujours en `sp` pour du texte (jamais `dp`), jamais en dessous de `14sp`,
et aucun texte important au-dessous de `18sp` sur l'écran d'exercice.

---

## 3. Espacements

| Ressource | Valeur | Usage |
|---|---|---|
| `@dimen/screen_margin` | `20dp` | Padding du conteneur racine de **chaque** écran |
| `@dimen/block_spacing` | `16dp` | Espace entre deux gros blocs d'un écran |

Valeurs d'appoint, à garder dans cette échelle : **4 / 6 / 8 / 12 / 16 / 20 dp**.
Repères concrets utilisés dans l'app :

- Padding intérieur d'une carte : `20dp` (carte de contenu), `16dp` (tuile de stat).
- Écart entre deux tuiles côte à côte : `12dp` (via un `<Space>`).
- Écart entre une ligne de tuiles et la suivante : `12dp`.
- Marge au-dessus d'un bouton principal : `20dp` ; entre deux boutons empilés : `4dp` à `8dp`.
- Icône + texte sur une même ligne : `8dp` entre les deux.

---

## 4. Formes et rayons

On n'utilise **que** ces trois rayons :

| Rayon | Où |
|---|---|
| `24dp` | Grande carte de contenu (`app:cardCornerRadius`) |
| `20dp` | Tuile de stat, boutons (`app:cornerRadius`), zone caméra |
| `12dp` | Petits badges / puces (si besoin) |

Autres règles de forme :

- **Cartes sans ombre ni bordure** : `app:cardElevation="0dp"` + `app:strokeWidth="0dp"`.
  La hiérarchie se fait par la couleur de fond, pas par l'élévation.
- Zone caméra / emplacement vide : bordure **pointillée** `2dp` en `?attr/colorOutline`,
  fond `?attr/colorSurfaceVariant`, rayon `20dp` → voir `@drawable/bg_camera_placeholder`,
  à réutiliser tel quel pour tout placeholder.

---

## 5. Composants

### 5.1 Boutons

Trois niveaux, à respecter pour que l'utilisateur sache où cliquer :

| Niveau | Style | Hauteur | Taille de texte | Exemple |
|---|---|---|---|---|
| Principal | par défaut (plein, Material 3) | `@dimen/button_height_large` (**72dp**) | `@dimen/button_text_large` | « Commencer », « Retour à l'accueil » |
| Secondaire | `Widget.Material3.Button.OutlinedButton` | `56dp` | `18sp` | « Mouvement raté », « Ajouter aux favoris » |
| Tertiaire | `Widget.Material3.Button.TextButton` | `56dp` | `18sp` | « Terminer », « Recommencer » |

Toujours `app:cornerRadius="20dp"` sur les boutons plein et outlined, et
`android:layout_width="match_parent"` pour les actions d'écran.
Bouton icône seul : `Widget.Material3.Button.IconButton`, **56dp × 56dp**, `app:iconSize="28dp"`,
avec un `android:contentDescription` obligatoire.

Un seul bouton principal visible par écran.

### 5.2 Carte de contenu

Modèle : `@layout/view_daily_exercise_card`.

```
MaterialCardView
  app:cardBackgroundColor="?attr/colorSecondaryContainer"
  app:cardCornerRadius="24dp"
  app:cardElevation="0dp"
  app:strokeWidth="0dp"
  └─ LinearLayout vertical, android:padding="20dp", animateLayoutChanges="true"
       ├─ sur-titre en majuscules (14sp) + bouton icône à droite
       ├─ titre (@dimen/text_title, bold)
       ├─ méta (@dimen/text_body_large)
       ├─ lien « Voir le détail » (TextButton)
       ├─ contenu déroulant (visibility="gone" au départ)
       │    ├─ description / objectif (@dimen/text_body_large)
       │    └─ badge succès
       └─ bouton principal (marge haute 16dp)
```

Tout le texte d'une carte est en `?attr/colorOnSecondaryContainer`.

### 5.2 bis Carte déroulante (accordéon)

Motif à réutiliser dès qu'une carte contient plus que l'essentiel — l'accueil doit rester
compact quand d'autres blocs viendront s'y ajouter.

**Pas de chevron.** L'action est portée par un **lien texte** dont le libellé bascule
(« Voir le détail » ↔ « Réduire »), comme sur les fiches Google Play ou les descriptions
YouTube : le texte annonce l'action, il n'y a aucune icône à interpréter. C'est plus sûr
qu'une flèche pour un public peu habitué, et ça reste lisible de loin.

- **Replié, la carte garde tout ce qui permet de décider** : sur-titre, titre, ligne de
  métadonnées et bouton d'action principal. Seul le détail (consigne, objectif, badges) est
  masqué. On ne cache jamais l'action principale derrière un pliage.
- Le lien est un `Widget.Material3.Button.TextButton` en `@dimen/text_body_large`,
  `layout_height="48dp"` (cible tactile), avec `android:layout_marginStart="-8dp"` pour
  compenser le padding interne du bouton et aligner le texte sur le contenu de la carte.
  Sa couleur par défaut (`?attr/colorPrimary`) passe le contraste sur
  `?attr/colorSecondaryContainer` en clair comme en sombre.
- `android:animateLayoutChanges="true"` sur le conteneur vertical de la carte : le pliage
  s'anime sans code supplémentaire.
- Le pliage se fait en `VISIBLE` / `GONE` (contrairement au bandeau de feedback, qui utilise
  `INVISIBLE`).
- L'état est conservé dans `onSaveInstanceState` pour survivre à une rotation d'écran.
- Accessibilité : rien à faire de plus, le libellé du bouton **est** l'annonce faite au
  lecteur d'écran. C'est justement l'avantage du lien sur le chevron, qui aurait exigé un
  `contentDescription` mis à jour à chaque bascule.

Référence : `@layout/view_daily_exercise_card` + `MainActivity.applyDetailsState()`.

### 5.3 Tuile de statistique

Modèle : `@layout/view_stat_tile` — à réutiliser via `<include>` plutôt que recopier.

Fond `?attr/colorSurfaceVariant`, rayon `20dp`, padding `16dp`, contenu centré :
valeur (`@dimen/text_stat_value`, bold, `?attr/colorOnSurface`) au-dessus du libellé
(`15sp`, `?attr/colorOnSurfaceVariant`). Se pose en grille de 2 colonnes
(`layout_weight="1"` + `<Space>` de `12dp`), ou pleine largeur si la tuile est seule.

### 5.4 Badge « succès »

Ligne horizontale : icône `@drawable/ic_check_circle` en `24dp` + texte bold
`@dimen/text_body_large`, séparés de `8dp`.

- Sur fond de carte → icône et texte en `@color/app_success`, sans fond.
- En badge autonome → fond `@color/app_success_container`, contenu en
  `@color/app_on_success_container`, padding `12dp / 8dp / 16dp / 8dp`.

### 5.5 Bandeau de feedback (pendant l'exercice)

Superposé en haut de la zone caméra, marge `12dp`, padding `16dp`, texte centré, bold,
`@dimen/text_body_large`, fond `@color/app_success_container`, texte
`@color/app_on_success_container`.

On le masque avec `android:visibility="invisible"` (et non `gone`) pour que la mise en
page ne saute pas quand il apparaît.

### 5.6 Structure d'écran

- Racine : `ScrollView` + `LinearLayout` vertical pour un écran de lecture (accueil, récap) ;
  `LinearLayout` vertical plein écran pour l'exercice (pas de scroll pendant l'effort).
- `android:padding="@dimen/screen_margin"` sur le conteneur, jamais de marges au cas par cas.
- `android:fillViewport="true"` sur les `ScrollView`.
- Titre d'écran en haut : `@dimen/text_title`, bold.

---

## 6. Icônes

- Format **vector drawable** `24dp × 24dp`, `viewport 24`, `fillColor="@android:color/white"`
  + teinte appliquée par-dessus (`app:tint` / `android:tint`) — jamais de PNG.
- Source : icônes Material (Android Studio → clic droit sur `res` → New → Vector Asset).
- Nommage : `ic_<nom>.xml` (`ic_check_circle`, `ic_favorite`, `ic_favorite_border`).
- Disponibles aujourd'hui : `ic_check_circle`, `ic_favorite`, `ic_favorite_border`.
- Pas d'icône de déroulement (chevron) : voir §5.2 bis, l'ouverture passe par un lien texte.
- Taille d'affichage : `24dp` dans le texte, `28dp` dans un bouton (`app:iconSize`).
- Toute icône porteuse de sens a un `contentDescription` ; une icône décorative a
  `android:contentDescription="@null"`.

---

## 7. Accessibilité et lisibilité

Le public visé (personnes qui font leur séance à domicile, à distance de l'écran) impose :

1. **Cible tactile ≥ 48dp**, et 56–72dp pour les actions pendant l'exercice.
2. **Texte en `sp`** uniquement, pour suivre les réglages système.
3. Ne jamais coder une information **uniquement par la couleur** : le vert est toujours
   accompagné d'une icône (coche) ou d'un mot.
4. Mode sombre à vérifier systématiquement (Android Studio : bascule dans l'aperçu, ou
   Paramètres → Affichage sur l'émulateur).
5. Toutes les chaînes dans `strings.xml`, jamais de texte en dur dans un layout —
   les libellés sont en français.
6. Les textes de démo utilisent `tools:text` (invisible à l'exécution), pas `android:text`.

---

## 8. Nommage des fichiers

| Type | Convention | Exemple |
|---|---|---|
| Layout d'activité | `activity_<nom>.xml` | `activity_summary.xml` |
| Composant réutilisable | `view_<nom>.xml` | `view_stat_tile.xml` |
| Fond / forme | `bg_<nom>.xml` | `bg_camera_placeholder.xml` |
| Icône | `ic_<nom>.xml` | `ic_favorite_border.xml` |
| id de vue | `lowerCamelCase` | `@+id/startButton` |
| String | `<écran>_<élément>` | `summary_goal_reached` |

---

## 9. Check-list avant de partager un écran

- [ ] Aucune couleur hexadécimale dans le layout (que des `?attr/` ou `@color/app_...`).
- [ ] Aucune taille de texte inventée : `@dimen/...` ou une des tailles tolérées du §2.
- [ ] Padding racine = `@dimen/screen_margin`.
- [ ] Rayons uniquement en `24dp` / `20dp` / `12dp`, cartes sans ombre.
- [ ] Un seul bouton principal, hauteur `@dimen/button_height_large`.
- [ ] Composant déjà existant réutilisé par `<include>` au lieu d'être recopié.
- [ ] Écran vérifié en **clair et en sombre**.
- [ ] Textes dans `strings.xml`, `contentDescription` sur les icônes utiles.

---

## 10. Ajouter une valeur à la charte

Si un écran a besoin d'une couleur, d'une taille ou d'une forme absente :

1. On ne l'écrit pas en dur dans le layout.
2. On l'ajoute dans `colors.xml` **et** `values-night/colors.xml` (ou `dimens.xml`).
3. On la nomme par son **rôle**, pas par son apparence (`app_warning`, pas `orange`).
4. On met ce document à jour et on prévient le groupe.

> Nota : la palette reste **provisoire** tant que l'identité visuelle de l'application
> (US-9.2) n'est pas tranchée. Comme tout passe par les attributs de thème, un changement
> de palette ne touchera que `colors.xml` — à condition que personne n'ait écrit de
> couleur en dur ailleurs.

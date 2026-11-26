package com.uphf.saes5.domain.model;

/**
 * Élément de jeu ou bloc de l'accueil que le chercheur peut désactiver (US-4.1, US-5.4).
 *
 * <p>Tous sont actifs par défaut. Ajouter un élément tient en une ligne ici : l'écran de
 * réglages se construit à partir de cette liste, et la configuration journalisée la suit
 * automatiquement.</p>
 *
 * <p>La clé est écrite dans les préférences et dans les journaux : elle ne doit plus changer
 * une fois des données collectées, sous peine de rendre les sessions incomparables. Pour la
 * même raison, un nouvel élément s'ajoute toujours en fin de liste.</p>
 */
public enum GameFeature {
    DAILY_EXERCISE("daily_exercise"),
    FAVORITES("favorites"),
    STARS("stars"),
    STATISTICS("statistics"),
    SOUNDS("sounds"),
    // Ajouté après la collecte des premières données : toujours en fin de liste, pour que le
    // début de l'instantané journalisé ne change pas.
    ARCADE("arcade"),
    // Blocs de l'accueil (US-5.4). Journalisés comme les éléments de jeu : ils font partie de
    // la condition de test.
    HOME_STEPS("home_steps", Group.HOME),
    HOME_EXERCISES_DONE("home_exercises_done", Group.HOME),
    HOME_CATALOG("home_catalog", Group.HOME);

    /** Section de l'écran de réglages où l'interrupteur apparaît. */
    public enum Group {
        /** Éléments de jeu (US-4.1). */
        GAME,
        /** Blocs de l'accueil (US-5.4). */
        HOME
    }

    private final String key;
    private final Group group;

    GameFeature(String key) {
        this(key, Group.GAME);
    }

    GameFeature(String key, Group group) {
        this.key = key;
        this.group = group;
    }

    public String getKey() {
        return key;
    }

    public Group getGroup() {
        return group;
    }
}

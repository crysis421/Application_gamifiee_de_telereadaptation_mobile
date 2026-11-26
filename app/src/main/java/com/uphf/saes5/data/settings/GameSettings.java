package com.uphf.saes5.data.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import com.uphf.saes5.R;
import com.uphf.saes5.domain.model.TestCondition;

/**
 * Interrupteurs des éléments de gamification (US-4.1).
 *
 * <p>Chaque élément de jeu a son interrupteur, persisté et appliqué à toute la session. Tous
 * sont actifs par défaut : l'application se comporte normalement tant que le chercheur n'a
 * rien désactivé.</p>
 *
 * <p>Ajouter un élément tient en une ligne dans {@link Feature} : l'écran de réglages se
 * construit à partir de cette liste, et {@link #snapshot()} la suit automatiquement.</p>
 */
public final class GameSettings {

    /**
     * Un élément de jeu désactivable.
     *
     * <p>La clé est écrite dans les préférences et dans les journaux : elle ne doit plus
     * changer une fois des données collectées, sous peine de rendre les sessions
     * incomparables.</p>
     */
    public enum Feature {
        DAILY_EXERCISE("daily_exercise", R.string.setting_daily_exercise, R.string.setting_daily_exercise_help),
        FAVORITES("favorites", R.string.setting_favorites, R.string.setting_favorites_help),
        STARS("stars", R.string.setting_stars, R.string.setting_stars_help),
        STATISTICS("statistics", R.string.setting_statistics, R.string.setting_statistics_help),
        SOUNDS("sounds", R.string.setting_sounds, R.string.setting_sounds_help),
        // Ajouté après la collecte des premières données : toujours en fin de liste, pour que
        // le début de l'instantané journalisé ne change pas.
        ARCADE("arcade", R.string.setting_arcade, R.string.setting_arcade_help),
        // Blocs de l'accueil (US-5.4). Journalisés comme les éléments de jeu : ils font partie
        // de la condition de test.
        HOME_STEPS("home_steps", R.string.setting_home_steps,
                R.string.setting_home_steps_help, Group.HOME),
        HOME_EXERCISES_DONE("home_exercises_done", R.string.setting_home_exercises_done,
                R.string.setting_home_exercises_done_help, Group.HOME),
        HOME_CATALOG("home_catalog", R.string.setting_home_catalog,
                R.string.setting_home_catalog_help, Group.HOME);

        private final String key;
        @StringRes private final int labelRes;
        @StringRes private final int helpRes;
        private final Group group;

        Feature(String key, @StringRes int labelRes, @StringRes int helpRes) {
            this(key, labelRes, helpRes, Group.GAME);
        }

        Feature(String key, @StringRes int labelRes, @StringRes int helpRes, Group group) {
            this.key = key;
            this.labelRes = labelRes;
            this.helpRes = helpRes;
            this.group = group;
        }

        public String getKey() { return key; }
        @StringRes public int getLabelRes() { return labelRes; }
        @StringRes public int getHelpRes() { return helpRes; }
        public Group getGroup() { return group; }
    }

    /** Section de l'écran de réglages où l'interrupteur apparaît. */
    public enum Group {
        /** Éléments de jeu (US-4.1). */
        GAME,
        /** Blocs de l'accueil (US-5.4). */
        HOME
    }

    private static final String PREFS_NAME = "exergame_prefs";
    private static final String KEY_PREFIX = "setting_";
    private static final String KEY_CONDITION_NAME = "condition_name";
    private static final String KEY_SAVED_CONDITIONS = "saved_conditions";

    @Nullable
    private static SharedPreferences preferences;

    private GameSettings() {}

    /**
     * Le nom donné à la condition de test courante (US-7.3), ou une chaîne vide.
     *
     * <p>Il accompagne chaque séance journalisée : la combinaison de réglages est déjà dans
     * {@link #snapshot()}, ce nom sert à la désigner dans l'analyse.</p>
     */
    public static String getConditionName() {
        if (preferences == null) {
            return "";
        }
        return preferences.getString(KEY_CONDITION_NAME, "");
    }

    public static void setConditionName(String name) {
        if (preferences == null) {
            return;
        }
        preferences.edit().putString(KEY_CONDITION_NAME, name == null ? "" : name.trim()).apply();
    }

    /** Conditions de test enregistrées (US-7.3), par ordre alphabétique. */
    public static List<TestCondition> getSavedConditions() {
        if (preferences == null) return new ArrayList<>();
        return TestCondition.fromJson(preferences.getString(KEY_SAVED_CONDITIONS, null));
    }

    /** Enregistre la configuration courante sous ce nom, en remplaçant un homonyme. */
    public static void saveCurrentCondition(@NonNull String name) {
        if (preferences == null) return;
        TestCondition condition = TestCondition.capture(name, GameSettings::isEnabled);
        storeConditions(TestCondition.save(getSavedConditions(), condition));
        setConditionName(condition.getName());
    }

    /** Applique tous les interrupteurs de la condition, et reprend son nom pour les journaux. */
    public static void applyCondition(@NonNull TestCondition condition) {
        condition.applyTo(GameSettings::setEnabled);
        setConditionName(condition.getName());
    }

    public static void deleteCondition(@NonNull String name) {
        if (preferences == null) return;
        storeConditions(TestCondition.remove(getSavedConditions(), name));
    }

    private static void storeConditions(List<TestCondition> conditions) {
        if (preferences == null) return;
        preferences.edit().putString(KEY_SAVED_CONDITIONS, TestCondition.toJson(conditions)).apply();
    }

    /** Branche les réglages sur leur stockage. Appelé au démarrage, depuis SaeS5Application. */
    public static synchronized void init(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Indique si l'élément est actif.
     *
     * <p>Vrai tant que rien n'a été désactivé, et aussi quand {@link #init(Context)} n'a pas
     * été appelé — en test unitaire par exemple : l'absence de réglages ne doit jamais
     * éteindre une fonctionnalité.</p>
     */
    public static boolean isEnabled(@NonNull Feature feature) {
        if (preferences == null) {
            return true;
        }
        return preferences.getBoolean(KEY_PREFIX + feature.getKey(), true);
    }

    public static void setEnabled(@NonNull Feature feature, boolean enabled) {
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_PREFIX + feature.getKey(), enabled).apply();
    }

    /**
     * La configuration active, sous une forme stable destinée aux journaux (US-7.1) et à
     * l'identification d'une condition de test (US-7.3).
     *
     * <p>Exemple : {@code daily_exercise=1;favorites=0;stars=1;statistics=1;sounds=1}. Les
     * éléments sont toujours dans le même ordre, celui de {@link Feature}, pour que deux
     * sessions restent comparables.</p>
     */
    public static String snapshot() {
        return snapshotOf(Feature.values());
    }

    @VisibleForTesting
    static String snapshotOf(Feature[] features) {
        StringBuilder result = new StringBuilder();
        for (Feature feature : features) {
            if (result.length() > 0) {
                result.append(';');
            }
            result.append(feature.getKey()).append('=').append(isEnabled(feature) ? '1' : '0');
        }
        return result.toString();
    }
}

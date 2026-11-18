package com.uphf.saes5;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

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
        SOUNDS("sounds", R.string.setting_sounds, R.string.setting_sounds_help);

        private final String key;
        @StringRes private final int labelRes;
        @StringRes private final int helpRes;

        Feature(String key, @StringRes int labelRes, @StringRes int helpRes) {
            this.key = key;
            this.labelRes = labelRes;
            this.helpRes = helpRes;
        }

        public String getKey() { return key; }
        @StringRes public int getLabelRes() { return labelRes; }
        @StringRes public int getHelpRes() { return helpRes; }
    }

    private static final String PREFS_NAME = "exergame_prefs";
    private static final String KEY_PREFIX = "setting_";
    private static final String KEY_CONDITION_NAME = "condition_name";

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

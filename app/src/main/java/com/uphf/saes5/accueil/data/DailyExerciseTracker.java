package com.uphf.saes5.accueil.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Trace la réalisation de l'exercice du jour (critère d'acceptation de l'US-4.2) et compte le
 * nombre total d'exercices terminés (utile à l'US-5.2).
 */
public class DailyExerciseTracker {

    private static final String PREFS_NAME = "exergame_prefs";
    private static final String KEY_LAST_COMPLETED_DAY = "daily_exercise_last_completed_day";
    private static final String KEY_COMPLETED_COUNT = "completed_exercises_count";

    private final SharedPreferences preferences;

    public DailyExerciseTracker(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Indique si l'exercice du jour a déjà été réalisé aujourd'hui. */
    public boolean isCompletedToday() {
        String lastDay = preferences.getString(KEY_LAST_COMPLETED_DAY, null);
        return DailyExerciseProvider.todayKey().equals(lastDay);
    }

    /** Enregistre la réalisation de l'exercice du jour. */
    public void markCompletedToday() {
        preferences.edit()
                .putString(KEY_LAST_COMPLETED_DAY, DailyExerciseProvider.todayKey())
                .putInt(KEY_COMPLETED_COUNT, getCompletedCount() + 1)
                .apply();
    }

    /** Nombre total d'exercices terminés depuis l'installation. */
    public int getCompletedCount() {
        return preferences.getInt(KEY_COMPLETED_COUNT, 0);
    }
}

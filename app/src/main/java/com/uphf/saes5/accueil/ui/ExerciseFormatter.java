package com.uphf.saes5.accueil.ui;

import android.content.Context;

import com.uphf.saes5.Exercise;
import com.uphf.saes5.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Mise en forme des métadonnées d'exercice pour l'affichage. */
public final class ExerciseFormatter {

    private ExerciseFormatter() {
    }

    /**
     * Ligne « Groupe musculaire · Difficulté · Durée estimée ».
     *
     * <p>La difficulté est déjà un libellé lisible dans le catalogue (« Facile », « Moyen »,
     * « Difficile »), il n'y a donc rien à traduire ici.</p>
     */
    public static String metaLine(Context context, Exercise exercise) {
        return context.getString(
                R.string.exercise_meta,
                exercise.getMuscleGroup(),
                exercise.getDifficulty(),
                context.getString(R.string.duration_minutes, exercise.getDurationMinutes()));
    }

    /** Durée au format {@code mm:ss} (ou {@code h:mm:ss} au-delà d'une heure). */
    public static String formatDuration(long millis) {
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(Math.max(0L, millis));
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}

package com.uphf.saes5.accueil.ui;

import android.content.Context;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.uphf.saes5.R;
import com.uphf.saes5.accueil.model.Exercise;

/** Mise en forme des métadonnées d'exercice pour l'affichage. */
public final class ExerciseFormatter {

    private ExerciseFormatter() {
    }

    /** Libellé lisible de la difficulté. */
    public static String difficultyLabel(Context context, int difficulty) {
        switch (difficulty) {
            case Exercise.DIFFICULTY_HARD:
                return context.getString(R.string.difficulty_hard);
            case Exercise.DIFFICULTY_MEDIUM:
                return context.getString(R.string.difficulty_medium);
            case Exercise.DIFFICULTY_EASY:
            default:
                return context.getString(R.string.difficulty_easy);
        }
    }

    /** Ligne « Groupe musculaire · Difficulté · Durée estimée ». */
    public static String metaLine(Context context, Exercise exercise) {
        int minutes = Math.max(1, Math.round(exercise.getEstimatedDurationSeconds() / 60f));
        return context.getString(
                R.string.exercise_meta,
                exercise.getMuscleGroup(),
                difficultyLabel(context, exercise.getDifficulty()),
                context.getString(R.string.duration_minutes, minutes));
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

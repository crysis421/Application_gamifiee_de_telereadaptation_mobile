package com.uphf.saes5.accueil.data;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.uphf.saes5.accueil.model.Exercise;

/**
 * Choisit l'exercice du jour (US-4.2).
 *
 * <p>Le tirage est aléatoire mais <em>déterministe pour une journée donnée</em> : le générateur
 * est initialisé avec la date du jour. L'exercice mis en avant reste donc le même tant que la
 * journée n'a pas changé, même si l'application est fermée et relancée.</p>
 */
public class DailyExerciseProvider {

    private static final SimpleDateFormat DAY_KEY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final ExerciseRepository repository;

    public DailyExerciseProvider(ExerciseRepository repository) {
        this.repository = repository;
    }

    /** Clé de la journée en cours, au format {@code yyyy-MM-dd}. */
    public static String todayKey() {
        return DAY_KEY_FORMAT.format(new Date());
    }

    /**
     * L'exercice mis en avant aujourd'hui, ou {@code null} si le catalogue est vide.
     */
    @Nullable
    public Exercise getExerciseOfTheDay() {
        return getExerciseForDay(todayKey());
    }

    /**
     * L'exercice mis en avant pour la journée indiquée. Exposé pour les tests et pour un
     * éventuel historique.
     */
    @Nullable
    public Exercise getExerciseForDay(String dayKey) {
        List<Exercise> all = repository.getAll();
        if (all.isEmpty()) {
            return null;
        }
        int index = new Random(dayKey.hashCode()).nextInt(all.size());
        return all.get(index);
    }
}

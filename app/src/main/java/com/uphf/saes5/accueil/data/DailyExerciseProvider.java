package com.uphf.saes5.accueil.data;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.uphf.saes5.Exercise;
import com.uphf.saes5.ExerciseRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Choisit l'exercice du jour (US-4.2) parmi ceux du catalogue.
 *
 * <p>Le tirage est aléatoire mais <em>déterministe pour une journée donnée</em> : le générateur
 * est initialisé avec la date du jour. L'exercice mis en avant reste donc le même tant que la
 * journée n'a pas changé, même si l'application est fermée et relancée.</p>
 *
 * <p>La source est {@link ExerciseRepository}, le catalogue partagé : un exercice ajouté depuis
 * l'écran de création peut donc devenir l'exercice du jour.</p>
 */
public final class DailyExerciseProvider {

    private static final SimpleDateFormat DAY_KEY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private DailyExerciseProvider() {
    }

    /** Clé de la journée en cours, au format {@code yyyy-MM-dd}. */
    public static String todayKey() {
        return DAY_KEY_FORMAT.format(new Date());
    }

    /** L'exercice du catalogue mis en avant aujourd'hui, ou {@code null} si le catalogue est vide. */
    @Nullable
    public static Exercise ofTheDay() {
        return pickForDay(ExerciseRepository.getAll(), todayKey());
    }

    /**
     * L'exercice mis en avant pour la journée indiquée. Exposé pour les tests et pour un
     * éventuel historique.
     */
    @VisibleForTesting
    @Nullable
    public static Exercise pickForDay(List<Exercise> catalogue, String dayKey) {
        if (catalogue == null || catalogue.isEmpty()) {
            return null;
        }
        int index = new Random(dayKey.hashCode()).nextInt(catalogue.size());
        return catalogue.get(index);
    }
}

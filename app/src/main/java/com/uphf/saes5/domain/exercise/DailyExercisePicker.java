package com.uphf.saes5.domain.exercise;

import androidx.annotation.Nullable;

import com.uphf.saes5.domain.model.Exercise;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/** Tirage de l'exercice du jour parmi le catalogue (US-4.2). */
public final class DailyExercisePicker {

    private DailyExercisePicker() {}

    /** Clé de la journée en cours, au format {@code yyyy-MM-dd}, dans le fuseau de l'horloge. */
    public static String dayKey(Clock clock) {
        return LocalDate.now(clock).toString();
    }

    /**
     * Tirage initial pour une journée donnée.
     *
     * <p>Aléatoire mais reproductible : le générateur est initialisé avec la date. Ce tirage
     * dépend en revanche du contenu du catalogue au moment où il a lieu, d'où la mémorisation
     * du choix pour la journée par le dépôt de l'exercice du jour.</p>
     */
    @Nullable
    public static Exercise pickForDay(@Nullable List<Exercise> catalogue, String dayKey) {
        if (catalogue == null || catalogue.isEmpty()) {
            return null;
        }
        int index = new Random(dayKey.hashCode()).nextInt(catalogue.size());
        return catalogue.get(index);
    }
}

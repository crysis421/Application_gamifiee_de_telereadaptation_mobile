package com.uphf.saes5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class ExerciseRepository {
    // Liste modifiable (avant : Arrays.asList, impossible d'ajouter un élément)
    private static final List<Exercise> exercises = new ArrayList<>(Arrays.asList(
            new Exercise("squat", "Squat", "Jambes", "Facile", 5, "Aucun", 3),
            new Exercise("pompes", "Pompes", "Pectoraux", "Moyen", 7, "Tapis", 4),
            new Exercise("fentes", "Fentes", "Jambes", "Facile", 6, "Aucun", 2),
            new Exercise("planche", "Planche", "Tronc", "Difficile", 4, "Aucun", 5),
            new Exercise("rowing", "Rowing élastique", "Dos", "Moyen", 8, "Élastique", 1)
    ));

    private ExerciseRepository() {}

    public static List<Exercise> getAll() {
        return new ArrayList<>(exercises);
    }

    public static void add(Exercise exercise) {
        exercises.add(exercise);
    }

    public static String newId() {
        return "custom_" + UUID.randomUUID();
    }
}

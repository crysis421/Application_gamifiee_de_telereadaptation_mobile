package com.uphf.saes5;

import java.util.Arrays;
import java.util.List;

public final class ExerciseRepository {
    private ExerciseRepository() {}

    public static List<Exercise> getAll() {
        return Arrays.asList(
                new Exercise("squat", "Squat", "Jambes", "Facile", 5, "Aucun", 3),
                new Exercise("pompes", "Pompes", "Pectoraux", "Moyen", 7, "Tapis", 4),
                new Exercise("fentes", "Fentes", "Jambes", "Facile", 6, "Aucun", 2),
                new Exercise("planche", "Planche", "Tronc", "Difficile", 4, "Aucun", 5),
                new Exercise("rowing", "Rowing élastique", "Dos", "Moyen", 8, "Élastique", 1)
        );
    }
}

package com.uphf.saes5;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class ExerciseRepository {
    // Liste modifiable (avant : Arrays.asList, impossible d'ajouter un élément)
    private static final List<Exercise> exercises = new ArrayList<>(Arrays.asList(
            new Exercise("squat", "Squat", "Jambes", "Facile", 5, "Aucun", 3,
                    "Pieds écartés à la largeur des épaules, descends en fléchissant les genoux "
                            + "en gardant le dos droit, puis remonte.", 10),
            new Exercise("pompes", "Pompes", "Pectoraux", "Moyen", 7, "Tapis", 4,
                    "Mains au sol à l'aplomb des épaules, corps gainé. Descends jusqu'à frôler "
                            + "le sol, puis pousse pour remonter.", 12),
            new Exercise("fentes", "Fentes", "Jambes", "Facile", 6, "Aucun", 2,
                    "Avance une jambe et fléchis les deux genoux jusqu'à l'angle droit, puis "
                            + "reviens en poussant sur la jambe avant. Alterne à chaque répétition.", 14),
            new Exercise("planche", "Planche", "Tronc", "Difficile", 4, "Aucun", 5,
                    "En appui sur les avant-bras et la pointe des pieds, garde le corps aligné "
                            + "des épaules aux talons sans creuser le dos.", 8),
            new Exercise("rowing", "Rowing élastique", "Dos", "Moyen", 8, "Élastique", 1,
                    "Élastique sous les pieds, buste légèrement penché. Tire les coudes vers "
                            + "l'arrière en serrant les omoplates, puis relâche en contrôlant.", 12)
    ));

    private ExerciseRepository() {}

    public static List<Exercise> getAll() {
        return new ArrayList<>(exercises);
    }

    public static void add(Exercise exercise) {
        exercises.add(exercise);
    }

    /**
     * Remplace l'exercice portant le même identifiant, en conservant sa place dans la liste.
     *
     * @return {@code false} si l'exercice n'est plus au catalogue, auquel cas rien n'est modifié.
     */
    public static boolean update(Exercise exercise) {
        if (exercise == null) {
            return false;
        }
        for (int i = 0; i < exercises.size(); i++) {
            if (exercises.get(i).getId().equals(exercise.getId())) {
                exercises.set(i, exercise);
                return true;
            }
        }
        return false;
    }

    /**
     * Retire un exercice du catalogue.
     *
     * @return {@code false} s'il n'y était déjà plus.
     */
    public static boolean remove(String id) {
        if (id == null) {
            return false;
        }
        for (int i = 0; i < exercises.size(); i++) {
            if (id.equals(exercises.get(i).getId())) {
                exercises.remove(i);
                return true;
            }
        }
        return false;
    }

    /** L'exercice portant cet identifiant, ou {@code null} s'il n'est plus au catalogue. */
    @Nullable
    public static Exercise findById(String id) {
        if (id == null) {
            return null;
        }
        for (Exercise exercise : exercises) {
            if (id.equals(exercise.getId())) {
                return exercise;
            }
        }
        return null;
    }

    public static String newId() {
        return "custom_" + UUID.randomUUID();
    }
}

package com.uphf.saes5;

import java.util.ArrayList;
import java.util.List;

public class ExerciseRepository {

    // Simple in-memory repository with sample exercises
    public static List<Exercise> getAllExercises() {
        List<Exercise> list = new ArrayList<>();
        list.add(new Exercise("ex1", "Squat", "Jambes", "Facile", 5, "Aucun", 3));
        list.add(new Exercise("ex2", "Pompes", "Pectoraux", "Moyen", 7, "Tapis", 4));
        list.add(new Exercise("ex3", "Fente", "Jambes", "Facile", 6, "Aucun", 2));
        list.add(new Exercise("ex4", "Planche", "Tronc", "Difficile", 4, "Aucun", 5));
        list.add(new Exercise("ex5", "Rowing élastique", "Dos", "Moyen", 8, "Élastique", 1));
        return list;
    }
}
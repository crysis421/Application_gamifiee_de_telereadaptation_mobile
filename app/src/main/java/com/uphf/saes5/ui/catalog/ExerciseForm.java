package com.uphf.saes5.ui.catalog;

/**
 * Ce que l'utilisateur a saisi dans le formulaire d'exercice, tel quel : la validation est
 * faite par {@link ExerciseEditorViewModel}, pas par l'écran.
 */
public final class ExerciseForm {

    final String name;
    final String durationText;
    final String targetRepsText;
    final String equipment;
    final String description;
    final String muscle;
    final String difficulty;
    final float rating;
    final int threshold;

    public ExerciseForm(String name, String durationText, String targetRepsText,
                        String equipment, String description, String muscle, String difficulty,
                        float rating, int threshold) {
        this.name = name;
        this.durationText = durationText;
        this.targetRepsText = targetRepsText;
        this.equipment = equipment;
        this.description = description;
        this.muscle = muscle;
        this.difficulty = difficulty;
        this.rating = rating;
        this.threshold = threshold;
    }
}

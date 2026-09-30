package com.uphf.saes5.accueil.model;

/**
 * Un exercice du catalogue.
 *
 * <p>Modèle volontairement minimal : il sera remplacé/étendu par l'équipe en charge du
 * catalogue (US-2.1) et de la base de données. Les champs correspondent aux métadonnées
 * listées dans le backlog : nom, description, groupe musculaire, matériel, difficulté,
 * durée estimée.</p>
 */
public class Exercise {

    public static final int DIFFICULTY_EASY = 1;
    public static final int DIFFICULTY_MEDIUM = 2;
    public static final int DIFFICULTY_HARD = 3;

    private final String id;
    private final String name;
    private final String description;
    private final String muscleGroup;
    private final String equipment;
    private final int difficulty;
    private final int targetReps;
    private final int estimatedDurationSeconds;

    public Exercise(String id,
                    String name,
                    String description,
                    String muscleGroup,
                    String equipment,
                    int difficulty,
                    int targetReps,
                    int estimatedDurationSeconds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.muscleGroup = muscleGroup;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.targetReps = targetReps;
        this.estimatedDurationSeconds = estimatedDurationSeconds;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMuscleGroup() {
        return muscleGroup;
    }

    public String getEquipment() {
        return equipment;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getTargetReps() {
        return targetReps;
    }

    public int getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }
}

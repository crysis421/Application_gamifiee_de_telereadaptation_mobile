package com.uphf.saes5;

public class Exercise {
    /** Objectif retenu quand l'exercice n'en précise pas, notamment ceux créés par l'utilisateur. */
    public static final int DEFAULT_TARGET_REPS = 10;

    private final String id;
    private final String name;
    private final String muscleGroup;
    private final String difficulty;
    private final int durationMinutes;
    private final String equipment;
    private final int stars;
    /** Consigne d'exécution. Vide tant que l'exercice n'en a pas : l'accueil masque alors le bloc. */
    private final String description;
    /** Nombre de répétitions visé pendant une séance. */
    private final int targetReps;

    public Exercise(String id, String name, String muscleGroup, String difficulty,
                    int durationMinutes, String equipment, int stars) {
        this(id, name, muscleGroup, difficulty, durationMinutes, equipment, stars,
                "", DEFAULT_TARGET_REPS);
    }

    public Exercise(String id, String name, String muscleGroup, String difficulty,
                    int durationMinutes, String equipment, int stars,
                    String description, int targetReps) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.difficulty = difficulty;
        this.durationMinutes = durationMinutes;
        this.equipment = equipment;
        this.stars = stars;
        this.description = description == null ? "" : description;
        this.targetReps = targetReps > 0 ? targetReps : DEFAULT_TARGET_REPS;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getMuscleGroup() { return muscleGroup; }
    public String getDifficulty() { return difficulty; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getEquipment() { return equipment; }
    public int getStars() { return stars; }
    public String getDescription() { return description; }
    public boolean hasDescription() { return !description.isEmpty(); }
    public int getTargetReps() { return targetReps; }
}

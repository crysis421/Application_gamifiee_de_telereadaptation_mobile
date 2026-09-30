package com.uphf.saes5;

public class Exercise {
    private final String id;
    private final String name;
    private final String muscleGroup;
    private final String difficulty;
    private final int durationMinutes;
    private final String equipment;
    private final int stars;

    public Exercise(String id, String name, String muscleGroup, String difficulty,
                    int durationMinutes, String equipment, int stars) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.difficulty = difficulty;
        this.durationMinutes = durationMinutes;
        this.equipment = equipment;
        this.stars = stars;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getMuscleGroup() { return muscleGroup; }
    public String getDifficulty() { return difficulty; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getEquipment() { return equipment; }
    public int getStars() { return stars; }
}

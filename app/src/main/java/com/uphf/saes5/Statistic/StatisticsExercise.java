package com.uphf.saes5;

public final class StatisticsExercise {
    private final String id;
    private final String name;
    private final String muscleGroup;

    public StatisticsExercise(String id, String name, String muscleGroup) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMuscleGroup() {
        return muscleGroup;
    }
}

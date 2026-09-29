package com.uphf.saes5;

public final class ExerciseStatistic {
    private final String exerciseId;
    private final String exerciseName;
    private final String muscleGroup;
    private final int averageScore;
    private final int perfectPercentage;
    private final int sessionCount;

    public ExerciseStatistic(String exerciseId, String exerciseName, String muscleGroup,
                             int averageScore, int perfectPercentage, int sessionCount) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.muscleGroup = muscleGroup;
        this.averageScore = averageScore;
        this.perfectPercentage = perfectPercentage;
        this.sessionCount = sessionCount;
    }

    public String getExerciseId() { return exerciseId; }
    public String getExerciseName() { return exerciseName; }
    public String getMuscleGroup() { return muscleGroup; }
    public int getAverageScore() { return averageScore; }
    public int getPerfectPercentage() { return perfectPercentage; }
    public int getSessionCount() { return sessionCount; }
}

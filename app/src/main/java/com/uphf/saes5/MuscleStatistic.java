package com.uphf.saes5;

public final class MuscleStatistic {
    private final String muscleGroup;
    private final int averageScore;
    private final int sessionCount;
    private final boolean hasData;

    public MuscleStatistic(String muscleGroup, int averageScore, int sessionCount, boolean hasData) {
        this.muscleGroup = muscleGroup;
        this.averageScore = averageScore;
        this.sessionCount = sessionCount;
        this.hasData = hasData;
    }

    public String getMuscleGroup() { return muscleGroup; }
    public int getAverageScore() { return averageScore; }
    public int getSessionCount() { return sessionCount; }
    public boolean hasData() { return hasData; }
}

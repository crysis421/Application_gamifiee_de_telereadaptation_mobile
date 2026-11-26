package com.uphf.saes5.domain.statistics;

public final class ProfileHistoryEntry {
    private final String exerciseName;
    private final String muscleGroup;
    private final long completedAtEpochMillis;
    private final int qualityScore;

    public ProfileHistoryEntry(String exerciseName, String muscleGroup,
                               long completedAtEpochMillis, int qualityScore) {
        this.exerciseName = exerciseName;
        this.muscleGroup = muscleGroup;
        this.completedAtEpochMillis = completedAtEpochMillis;
        this.qualityScore = qualityScore;
    }

    public String getExerciseName() { return exerciseName; }
    public String getMuscleGroup() { return muscleGroup; }
    public long getCompletedAtEpochMillis() { return completedAtEpochMillis; }
    public int getQualityScore() { return qualityScore; }
    public boolean earnedStar() { return qualityScore >= 90; }
}

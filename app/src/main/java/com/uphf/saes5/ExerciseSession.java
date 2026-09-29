package com.uphf.saes5;

public final class ExerciseSession {
    private final String exerciseId;
    private final long completedAtEpochMillis;
    private final int qualityScore;

    public ExerciseSession(String exerciseId, long completedAtEpochMillis, int qualityScore) {
        this.exerciseId = exerciseId;
        this.completedAtEpochMillis = completedAtEpochMillis;
        this.qualityScore = Math.max(0, Math.min(100, qualityScore));
    }

    public String getExerciseId() { return exerciseId; }
    public long getCompletedAtEpochMillis() { return completedAtEpochMillis; }
    public int getQualityScore() { return qualityScore; }
}

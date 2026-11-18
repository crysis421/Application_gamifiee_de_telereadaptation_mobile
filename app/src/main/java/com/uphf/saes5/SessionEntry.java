package com.uphf.saes5;

import androidx.annotation.Nullable;

/**
 * Une séance terminée, telle qu'elle est journalisée (US-7.1).
 *
 * <p>Objet sans dépendance Android, pour rester testable hors émulateur. Il ne contient que
 * des données choisies : aucune vidéo, aucune image, conformément à la contrainte de l'US.</p>
 */
public final class SessionEntry {

    /** Score de similarité inconnu : le moteur de comparaison n'est pas branché sur la séance. */
    public static final int SCORE_UNAVAILABLE = -1;

    private final long timestampMillis;
    private final String exerciseId;
    private final String exerciseName;
    private final int validatedReps;
    private final int failedReps;
    private final int targetReps;
    private final long durationMillis;
    private final int similarityScore;
    private final String settings;
    private final String conditionName;

    public SessionEntry(long timestampMillis,
                        String exerciseId,
                        String exerciseName,
                        int validatedReps,
                        int failedReps,
                        int targetReps,
                        long durationMillis,
                        int similarityScore,
                        String settings,
                        @Nullable String conditionName) {
        this.timestampMillis = timestampMillis;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.validatedReps = validatedReps;
        this.failedReps = failedReps;
        this.targetReps = targetReps;
        this.durationMillis = durationMillis;
        this.similarityScore = similarityScore;
        this.settings = settings == null ? "" : settings;
        this.conditionName = conditionName == null ? "" : conditionName;
    }

    public long getTimestampMillis() { return timestampMillis; }
    public String getExerciseId() { return exerciseId; }
    public String getExerciseName() { return exerciseName; }
    public int getValidatedReps() { return validatedReps; }
    public int getFailedReps() { return failedReps; }
    public int getTargetReps() { return targetReps; }
    public long getDurationMillis() { return durationMillis; }
    public String getSettings() { return settings; }
    public String getConditionName() { return conditionName; }

    /** Le score de similarité, ou {@link #SCORE_UNAVAILABLE} s'il n'a pas été mesuré. */
    public int getSimilarityScore() { return similarityScore; }

    public boolean hasSimilarityScore() { return similarityScore != SCORE_UNAVAILABLE; }

    /** Mouvements tentés, validés et ratés confondus. */
    public int getTotalAttempts() { return validatedReps + failedReps; }

    /** Pourcentage de mouvements validés, arrondi. 0 si rien n'a été tenté. */
    public int getSuccessRate() {
        int attempts = getTotalAttempts();
        if (attempts == 0) {
            return 0;
        }
        return Math.round((validatedReps * 100f) / attempts);
    }

    public boolean isGoalReached() {
        return targetReps > 0 && validatedReps >= targetReps;
    }
}

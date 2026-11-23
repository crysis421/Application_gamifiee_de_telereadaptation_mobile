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
    /** Modèle et part d'images analysées (US-1.4), vide pour une séance sans caméra. */
    private final String analysisSettings;
    private final int averageLatencyMillis;

    /** Latence inconnue : séance manuelle, ou entrée journalisée avant l'US-1.4. */
    public static final int LATENCY_UNAVAILABLE = -1;
    /** Pas de points : élément de jeu désactivé, ou entrée journalisée avant l'US-4.4. */
    public static final int POINTS_UNAVAILABLE = -1;

    /** Score arcade de la séance (US-4.4), ou {@link #POINTS_UNAVAILABLE}. */
    private final int arcadePoints;
    /** Séance quittée avant la fin, sans passer par le récapitulatif (US-7.1). */
    private final boolean abandoned;

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
        this(timestampMillis, exerciseId, exerciseName, validatedReps, failedReps, targetReps,
                durationMillis, similarityScore, settings, conditionName, "", LATENCY_UNAVAILABLE);
    }

    public SessionEntry(long timestampMillis,
                        String exerciseId,
                        String exerciseName,
                        int validatedReps,
                        int failedReps,
                        int targetReps,
                        long durationMillis,
                        int similarityScore,
                        String settings,
                        @Nullable String conditionName,
                        @Nullable String analysisSettings,
                        int averageLatencyMillis) {
        this(timestampMillis, exerciseId, exerciseName, validatedReps, failedReps, targetReps,
                durationMillis, similarityScore, settings, conditionName, analysisSettings,
                averageLatencyMillis, POINTS_UNAVAILABLE);
    }

    public SessionEntry(long timestampMillis,
                        String exerciseId,
                        String exerciseName,
                        int validatedReps,
                        int failedReps,
                        int targetReps,
                        long durationMillis,
                        int similarityScore,
                        String settings,
                        @Nullable String conditionName,
                        @Nullable String analysisSettings,
                        int averageLatencyMillis,
                        int arcadePoints) {
        this(timestampMillis, exerciseId, exerciseName, validatedReps, failedReps, targetReps,
                durationMillis, similarityScore, settings, conditionName, analysisSettings,
                averageLatencyMillis, arcadePoints, false);
    }

    public SessionEntry(long timestampMillis,
                        String exerciseId,
                        String exerciseName,
                        int validatedReps,
                        int failedReps,
                        int targetReps,
                        long durationMillis,
                        int similarityScore,
                        String settings,
                        @Nullable String conditionName,
                        @Nullable String analysisSettings,
                        int averageLatencyMillis,
                        int arcadePoints,
                        boolean abandoned) {
        this.abandoned = abandoned;
        this.arcadePoints = arcadePoints < 0 ? POINTS_UNAVAILABLE : arcadePoints;
        this.analysisSettings = analysisSettings == null ? "" : analysisSettings;
        this.averageLatencyMillis = averageLatencyMillis < 0
                ? LATENCY_UNAVAILABLE
                : averageLatencyMillis;
        this.timestampMillis = timestampMillis;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.validatedReps = validatedReps;
        this.failedReps = failedReps;
        this.targetReps = targetReps;
        this.durationMillis = durationMillis;
        this.similarityScore = similarityScore == SCORE_UNAVAILABLE
                ? SCORE_UNAVAILABLE
                : Math.max(0, Math.min(100, similarityScore));
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
    public String getAnalysisSettings() { return analysisSettings; }
    /** Latence moyenne de l'analyse, ou {@link #LATENCY_UNAVAILABLE}. */
    public int getAverageLatencyMillis() { return averageLatencyMillis; }
    public boolean hasLatency() { return averageLatencyMillis != LATENCY_UNAVAILABLE; }
    public int getArcadePoints() { return arcadePoints; }
    public boolean hasArcadePoints() { return arcadePoints != POINTS_UNAVAILABLE; }
    public boolean isAbandoned() { return abandoned; }

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

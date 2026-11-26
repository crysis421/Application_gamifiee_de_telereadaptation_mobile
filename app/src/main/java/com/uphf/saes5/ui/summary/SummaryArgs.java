package com.uphf.saes5.ui.summary;

import com.uphf.saes5.domain.model.SessionEntry;

/**
 * Ce que la séance transmet au récapitulatif : les mesures de la séance qui vient de finir.
 *
 * <p>Objet sans Android : le ViewModel de la séance le produit, et seul l'écran du
 * récapitulatif sait le mettre dans un {@code Intent}.</p>
 */
public final class SummaryArgs {

    private final String exerciseId;
    private final int validatedReps;
    private final int failedReps;
    private final long durationMillis;
    private final int similarityScore;
    private final int averageLatencyMillis;
    private final int arcadePoints;

    /**
     * @param similarityScore      moyenne des scores, ou {@link SessionEntry#SCORE_UNAVAILABLE}
     * @param averageLatencyMillis latence moyenne, ou {@link SessionEntry#LATENCY_UNAVAILABLE}
     * @param arcadePoints         points gagnés, ou {@link SessionEntry#POINTS_UNAVAILABLE}
     */
    public SummaryArgs(String exerciseId, int validatedReps, int failedReps, long durationMillis,
                       int similarityScore, int averageLatencyMillis, int arcadePoints) {
        this.exerciseId = exerciseId;
        this.validatedReps = validatedReps;
        this.failedReps = failedReps;
        this.durationMillis = durationMillis;
        this.similarityScore = similarityScore;
        this.averageLatencyMillis = averageLatencyMillis;
        this.arcadePoints = arcadePoints;
    }

    public String getExerciseId() { return exerciseId; }
    public int getValidatedReps() { return validatedReps; }
    public int getFailedReps() { return failedReps; }
    public long getDurationMillis() { return durationMillis; }
    public int getSimilarityScore() { return similarityScore; }
    public int getAverageLatencyMillis() { return averageLatencyMillis; }
    public int getArcadePoints() { return arcadePoints; }
}

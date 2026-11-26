package com.uphf.saes5.ui.summary;

import android.content.Intent;

import com.uphf.saes5.domain.model.SessionEntry;

/**
 * Ce que la séance transmet au récapitulatif : les mesures de la séance qui vient de finir.
 *
 * <p>Les noms d'extras sont ceux des versions précédentes de l'écran.</p>
 */
public final class SummaryArgs {

    private static final String EXTRA_EXERCISE_ID = "extra_exercise_id";
    private static final String EXTRA_VALIDATED_REPS = "extra_validated_reps";
    private static final String EXTRA_FAILED_REPS = "extra_failed_reps";
    private static final String EXTRA_DURATION_MILLIS = "extra_duration_millis";
    private static final String EXTRA_SIMILARITY_SCORE = "extra_similarity_score";
    private static final String EXTRA_AVERAGE_LATENCY = "extra_average_latency";
    private static final String EXTRA_ARCADE_POINTS = "extra_arcade_points";

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

    public static SummaryArgs fromIntent(Intent intent) {
        return new SummaryArgs(
                intent.getStringExtra(EXTRA_EXERCISE_ID),
                intent.getIntExtra(EXTRA_VALIDATED_REPS, 0),
                intent.getIntExtra(EXTRA_FAILED_REPS, 0),
                intent.getLongExtra(EXTRA_DURATION_MILLIS, 0L),
                intent.getIntExtra(EXTRA_SIMILARITY_SCORE, SessionEntry.SCORE_UNAVAILABLE),
                intent.getIntExtra(EXTRA_AVERAGE_LATENCY, SessionEntry.LATENCY_UNAVAILABLE),
                intent.getIntExtra(EXTRA_ARCADE_POINTS, SessionEntry.POINTS_UNAVAILABLE));
    }

    public Intent writeTo(Intent intent) {
        return intent.putExtra(EXTRA_EXERCISE_ID, exerciseId)
                .putExtra(EXTRA_VALIDATED_REPS, validatedReps)
                .putExtra(EXTRA_FAILED_REPS, failedReps)
                .putExtra(EXTRA_DURATION_MILLIS, durationMillis)
                .putExtra(EXTRA_SIMILARITY_SCORE, similarityScore)
                .putExtra(EXTRA_AVERAGE_LATENCY, averageLatencyMillis)
                .putExtra(EXTRA_ARCADE_POINTS, arcadePoints);
    }

    public String getExerciseId() { return exerciseId; }
    public int getValidatedReps() { return validatedReps; }
    public int getFailedReps() { return failedReps; }
    public long getDurationMillis() { return durationMillis; }
    public int getSimilarityScore() { return similarityScore; }
    public int getAverageLatencyMillis() { return averageLatencyMillis; }
    public int getArcadePoints() { return arcadePoints; }
}

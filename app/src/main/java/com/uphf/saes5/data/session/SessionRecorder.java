package com.uphf.saes5.data.session;

import com.uphf.saes5.data.settings.AnalysisSettings;
import com.uphf.saes5.data.settings.GameSettings;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.SessionEntry;

/**
 * Construit et journalise l'entrée d'une séance (US-7.1), qu'elle soit terminée ou abandonnée.
 *
 * <p>Un seul endroit décide de ce qui accompagne une séance : réglages de jeu et de l'accueil,
 * nom de la condition de test, réglages d'analyse. Le récapitulatif et l'abandon écrivent
 * ainsi exactement les mêmes champs.</p>
 */
public final class SessionRecorder {

    private SessionRecorder() {}

    /**
     * @param similarityScore      moyenne des scores, ou {@link SessionEntry#SCORE_UNAVAILABLE}
     * @param averageLatencyMillis latence moyenne, ou {@link SessionEntry#LATENCY_UNAVAILABLE}
     * @param arcadePoints         points gagnés, ou {@link SessionEntry#POINTS_UNAVAILABLE}
     */
    public static SessionEntry build(Exercise exercise, int validatedReps, int failedReps,
                                     long durationMillis, int similarityScore,
                                     int averageLatencyMillis, int arcadePoints,
                                     boolean abandoned) {
        return new SessionEntry(
                System.currentTimeMillis(),
                exercise.getId(),
                exercise.getName(),
                validatedReps,
                failedReps,
                exercise.getTargetReps(),
                durationMillis,
                similarityScore,
                GameSettings.snapshot(),
                GameSettings.getConditionName(),
                // US-1.4 : le modèle et la latence n'ont de sens que si la caméra a tourné.
                averageLatencyMillis == SessionEntry.LATENCY_UNAVAILABLE
                        ? ""
                        : AnalysisSettings.snapshot(),
                averageLatencyMillis,
                arcadePoints,
                abandoned);
    }

    public static void record(Exercise exercise, int validatedReps, int failedReps,
                              long durationMillis, int similarityScore,
                              int averageLatencyMillis, int arcadePoints, boolean abandoned) {
        SessionLog.record(build(exercise, validatedReps, failedReps, durationMillis,
                similarityScore, averageLatencyMillis, arcadePoints, abandoned));
    }
}

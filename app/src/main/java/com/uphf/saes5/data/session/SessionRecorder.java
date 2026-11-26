package com.uphf.saes5.data.session;

import com.uphf.saes5.data.settings.AnalysisSettingsRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.SessionEntry;

import java.time.Clock;

/**
 * Construit et journalise l'entrée d'une séance (US-7.1), qu'elle soit terminée ou abandonnée.
 *
 * <p>Un seul endroit décide de ce qui accompagne une séance : réglages de jeu et de l'accueil,
 * nom de la condition de test, réglages d'analyse. Le récapitulatif et l'abandon écrivent
 * ainsi exactement les mêmes champs.</p>
 */
public final class SessionRecorder {

    private final SessionRepository sessions;
    private final GameSettingsRepository gameSettings;
    private final AnalysisSettingsRepository analysisSettings;
    private final Clock clock;

    public SessionRecorder(SessionRepository sessions, GameSettingsRepository gameSettings,
                           AnalysisSettingsRepository analysisSettings, Clock clock) {
        this.sessions = sessions;
        this.gameSettings = gameSettings;
        this.analysisSettings = analysisSettings;
        this.clock = clock;
    }

    /**
     * @param similarityScore      moyenne des scores, ou {@link SessionEntry#SCORE_UNAVAILABLE}
     * @param averageLatencyMillis latence moyenne, ou {@link SessionEntry#LATENCY_UNAVAILABLE}
     * @param arcadePoints         points gagnés, ou {@link SessionEntry#POINTS_UNAVAILABLE}
     */
    public SessionEntry build(Exercise exercise, int validatedReps, int failedReps,
                              long durationMillis, int similarityScore,
                              int averageLatencyMillis, int arcadePoints, boolean abandoned) {
        return new SessionEntry(
                clock.millis(),
                exercise.getId(),
                exercise.getName(),
                validatedReps,
                failedReps,
                exercise.getTargetReps(),
                durationMillis,
                similarityScore,
                gameSettings.snapshot(),
                gameSettings.getConditionName(),
                // US-1.4 : le modèle et la latence n'ont de sens que si la caméra a tourné.
                averageLatencyMillis == SessionEntry.LATENCY_UNAVAILABLE
                        ? ""
                        : analysisSettings.snapshot(),
                averageLatencyMillis,
                arcadePoints,
                abandoned);
    }

    public void record(Exercise exercise, int validatedReps, int failedReps,
                       long durationMillis, int similarityScore,
                       int averageLatencyMillis, int arcadePoints, boolean abandoned) {
        sessions.record(build(exercise, validatedReps, failedReps, durationMillis,
                similarityScore, averageLatencyMillis, arcadePoints, abandoned));
    }
}

package com.uphf.saes5.ui.recording;

import androidx.annotation.Nullable;

/** État de l'écran d'enregistrement d'un modèle, calculé par {@link MovementRecordingViewModel}. */
public final class RecordingUiState {

    public enum Phase {
        /** Caméra en préparation. */
        WAITING,
        /** « Placez-vous… » : le temps de se mettre en position. */
        COUNTDOWN,
        RECORDING,
        DONE,
        /** L'analyse de pose a échoué : plus rien ne peut être enregistré. */
        UNAVAILABLE
    }

    /** Issue de la dernière captation. */
    public enum Result { NONE, TOO_FEW_FRAMES, SAVED, SAVE_FAILED, SCORE }

    private final Phase phase;
    private final int secondsLeft;
    private final Result result;
    private final int frameCount;
    private final int score;
    @Nullable private final String error;

    RecordingUiState(Phase phase, int secondsLeft, Result result, int frameCount, int score,
                     @Nullable String error) {
        this.phase = phase;
        this.secondsLeft = secondsLeft;
        this.result = result;
        this.frameCount = frameCount;
        this.score = score;
        this.error = error;
    }

    public Phase getPhase() { return phase; }
    /** Secondes restantes du compte à rebours ou de la captation. */
    public int getSecondsLeft() { return secondsLeft; }
    public Result getResult() { return result; }
    /** Images captées : pendant la captation, puis dans le modèle enregistré. */
    public int getFrameCount() { return frameCount; }
    /** Score de ressemblance au modèle, en mode comparaison. */
    public int getScore() { return score; }
    /** Message d'erreur de l'analyse, en phase {@link Phase#UNAVAILABLE}. */
    @Nullable public String getError() { return error; }
    /** Le bouton « Recommencer » n'apparaît qu'une fois la captation terminée. */
    public boolean isActionVisible() { return phase == Phase.DONE; }
}

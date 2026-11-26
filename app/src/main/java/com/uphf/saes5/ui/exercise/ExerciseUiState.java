package com.uphf.saes5.ui.exercise;

import androidx.annotation.Nullable;

import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.Movement;
import com.uphf.saes5.domain.movement.PostureFeedback;

/** Ce que l'écran de séance affiche, calculé par {@link ExerciseSessionViewModel}. Immuable. */
public final class ExerciseUiState {

    /** Message sous le score : où en est la comparaison avec le modèle. */
    public enum Status {
        REFERENCE_READY,
        POSTURE_CORRECT,
        POSTURE_TO_CORRECT,
        /** Corps en partie hors du cadre : rien n'est compté. */
        BODY_NOT_VISIBLE,
        POSE_NOT_DETECTED,
        MANUAL_REFERENCE_MISSING,
        MANUAL_PERMISSION_REFUSED,
        MANUAL_CAMERA_ERROR
    }

    /** Valeur des scores et latences indisponibles. */
    public static final int UNAVAILABLE = -1;

    private final String exerciseName;
    private final int validatedReps;
    private final int targetReps;
    private final long startedAtElapsed;
    private final boolean manualMode;
    private final int similarity;
    private final Status status;
    private final int threshold;
    private final PostureFeedback.State posture;
    private final boolean arcadeVisible;
    private final int arcadeTotal;
    private final int arcadeMultiplier;
    private final int averageLatency;
    private final boolean latencyAboveTarget;
    @Nullable private final Movement preview;
    private final boolean soundsEnabled;
    private final long goalReachedAtElapsed;
    private final AnalysisModel analysisModel;
    private final int frameStride;

    ExerciseUiState(String exerciseName, int validatedReps, int targetReps, long startedAtElapsed,
                    boolean manualMode, int similarity, Status status, int threshold,
                    PostureFeedback.State posture, boolean arcadeVisible, int arcadeTotal,
                    int arcadeMultiplier, int averageLatency, boolean latencyAboveTarget,
                    @Nullable Movement preview, boolean soundsEnabled, long goalReachedAtElapsed,
                    AnalysisModel analysisModel, int frameStride) {
        this.exerciseName = exerciseName;
        this.validatedReps = validatedReps;
        this.targetReps = targetReps;
        this.startedAtElapsed = startedAtElapsed;
        this.manualMode = manualMode;
        this.similarity = similarity;
        this.status = status;
        this.threshold = threshold;
        this.posture = posture;
        this.arcadeVisible = arcadeVisible;
        this.arcadeTotal = arcadeTotal;
        this.arcadeMultiplier = arcadeMultiplier;
        this.averageLatency = averageLatency;
        this.latencyAboveTarget = latencyAboveTarget;
        this.preview = preview;
        this.soundsEnabled = soundsEnabled;
        this.goalReachedAtElapsed = goalReachedAtElapsed;
        this.analysisModel = analysisModel;
        this.frameStride = frameStride;
    }

    public String getExerciseName() { return exerciseName; }
    public int getValidatedReps() { return validatedReps; }
    public int getTargetReps() { return targetReps; }
    /** Début de la séance (horloge {@code elapsedRealtime}) : base du chronomètre affiché. */
    public long getStartedAtElapsed() { return startedAtElapsed; }
    /** Validation à la main : pas de modèle, caméra refusée ou en erreur. */
    public boolean isManualMode() { return manualMode; }
    /** Ressemblance au modèle, en %, ou {@link #UNAVAILABLE}. */
    public int getSimilarity() { return similarity; }
    public Status getStatus() { return status; }
    /** Seuil de similarité de l'exercice, affiché avec {@link Status#REFERENCE_READY}. */
    public int getThreshold() { return threshold; }
    /** Couleur du squelette (US-3.2). */
    public PostureFeedback.State getPosture() { return posture; }
    public boolean isArcadeVisible() { return arcadeVisible; }
    public int getArcadeTotal() { return arcadeTotal; }
    public int getArcadeMultiplier() { return arcadeMultiplier; }
    /** Latence moyenne de l'analyse (US-1.4), ou {@link #UNAVAILABLE}. */
    public int getAverageLatency() { return averageLatency; }
    public boolean isLatencyAboveTarget() { return latencyAboveTarget; }
    /** Le geste montré par le mannequin, ou {@code null} sans modèle. */
    @Nullable public Movement getPreview() { return preview; }
    public boolean isSoundsEnabled() { return soundsEnabled; }
    /** Moment où l'objectif a été atteint, ou -1 : base du délai de fin automatique. */
    public long getGoalReachedAtElapsed() { return goalReachedAtElapsed; }
    /** Modèle de détection à utiliser par la caméra (US-1.4). */
    public AnalysisModel getAnalysisModel() { return analysisModel; }
    /** Une image analysée sur {@code frameStride}. */
    public int getFrameStride() { return frameStride; }
}

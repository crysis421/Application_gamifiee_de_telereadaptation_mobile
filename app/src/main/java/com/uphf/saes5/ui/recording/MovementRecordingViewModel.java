package com.uphf.saes5.ui.recording;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.domain.model.Movement;
import com.uphf.saes5.domain.movement.MovementMath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Enregistrement du modèle de référence d'un exercice, ou comparaison de la caméra à ce modèle.
 *
 * <p>Déroulé : trois secondes pour se placer, six secondes de captation à une image toutes les
 * 100 ms, puis enregistrement (ou score). Le temps est mesuré ici, à partir d'une horloge, et
 * non par un minuteur de l'écran : une rotation pendant la captation ne la relance pas.</p>
 */
public final class MovementRecordingViewModel extends ViewModel {

    public enum Mode { RECORD, COMPARE }

    static final long COUNTDOWN_MILLIS = 3_000;
    static final long RECORD_MILLIS = 6_000;
    static final long SAMPLE_INTERVAL_MILLIS = 100;
    static final int MIN_FRAMES = 20;

    private final Mode mode;
    private final String movementName;
    private final MovementRepository movements;
    private final LongSupplier elapsedRealtime;
    @Nullable private final Movement reference;
    private final MutableLiveData<RecordingUiState> state = new MutableLiveData<>();

    private final List<float[]> frames = new ArrayList<>();
    private RecordingUiState.Phase phase = RecordingUiState.Phase.WAITING;
    private RecordingUiState.Result result = RecordingUiState.Result.NONE;
    private long phaseStartedAt;
    private long lastSampleAt;
    private int score;
    @Nullable private String error;

    public MovementRecordingViewModel(Mode mode, String movementName,
                                      MovementRepository movements, LongSupplier elapsedRealtime) {
        this.mode = mode;
        this.movementName = movementName;
        this.movements = movements;
        this.elapsedRealtime = elapsedRealtime;
        this.reference = mode == Mode.COMPARE ? movements.load(movementName) : null;
        publish();
    }

    public LiveData<RecordingUiState> getState() {
        return state;
    }

    /**
     * Mode comparaison sans modèle enregistré, ou modèle vide : l'écran doit se fermer avant
     * d'ouvrir la caméra ou de demander la permission.
     */
    public boolean isMovementMissing() {
        return mode == Mode.COMPARE
                && (reference == null || reference.frames == null || reference.frames.isEmpty());
    }

    /** La caméra est prête : la première fois, le compte à rebours démarre de lui-même. */
    public void onCameraReady() {
        if (phase == RecordingUiState.Phase.WAITING) start();
    }

    /** Lance (ou relance) le compte à rebours, puis la captation. */
    public void start() {
        if (phase == RecordingUiState.Phase.UNAVAILABLE) return;
        phase = RecordingUiState.Phase.COUNTDOWN;
        result = RecordingUiState.Result.NONE;
        phaseStartedAt = elapsedRealtime.getAsLong();
        frames.clear();
        publish();
    }

    /** Fait avancer les phases ; appelée régulièrement par l'écran tant qu'il est visible. */
    public void onTick() {
        long elapsed = elapsedRealtime.getAsLong() - phaseStartedAt;
        if (phase == RecordingUiState.Phase.COUNTDOWN && elapsed >= COUNTDOWN_MILLIS) {
            phase = RecordingUiState.Phase.RECORDING;
            phaseStartedAt = elapsedRealtime.getAsLong();
            lastSampleAt = 0;
            frames.clear();
        } else if (phase == RecordingUiState.Phase.RECORDING && elapsed >= RECORD_MILLIS) {
            finish();
        }
        publish();
    }

    /** Une pose détectée : gardée pendant la captation, une toutes les 100 ms. */
    public void onPoseLandmarks(List<NormalizedLandmark> pose, int imageWidth, int imageHeight) {
        if (phase != RecordingUiState.Phase.RECORDING) return;
        long now = elapsedRealtime.getAsLong();
        if (now - lastSampleAt < SAMPLE_INTERVAL_MILLIS) return;
        float[] frame = MovementMath.normalize(pose, imageWidth, imageHeight);
        if (frame == null) return;
        lastSampleAt = now;
        frames.add(frame);
        publish();
    }

    public void onAnalysisError(String message) {
        phase = RecordingUiState.Phase.UNAVAILABLE;
        error = message;
        publish();
    }

    private void finish() {
        phase = RecordingUiState.Phase.DONE;
        if (frames.size() < MIN_FRAMES) {
            result = RecordingUiState.Result.TOO_FEW_FRAMES;
        } else if (mode == Mode.COMPARE) {
            score = MovementMath.angleScore(reference.frames, frames);
            result = RecordingUiState.Result.SCORE;
        } else {
            save();
        }
    }

    private void save() {
        Movement movement = new Movement();
        movement.name = movementName;
        movement.sampleIntervalMs = (int) SAMPLE_INTERVAL_MILLIS;
        movement.durationSec = (int) (RECORD_MILLIS / 1_000);
        movement.landmarkCount = MovementMath.LANDMARKS;
        movement.frames = new ArrayList<>(frames);
        try {
            movements.save(movement);
            result = RecordingUiState.Result.SAVED;
        } catch (IOException e) {
            result = RecordingUiState.Result.SAVE_FAILED;
        }
    }

    private void publish() {
        long elapsed = elapsedRealtime.getAsLong() - phaseStartedAt;
        long remaining = phase == RecordingUiState.Phase.COUNTDOWN ? COUNTDOWN_MILLIS - elapsed
                : phase == RecordingUiState.Phase.RECORDING ? RECORD_MILLIS - elapsed
                : 0;
        int secondsLeft = (int) Math.max(0, (remaining + 999) / 1_000);
        state.setValue(new RecordingUiState(phase, secondsLeft, result, frames.size(), score, error));
    }
}

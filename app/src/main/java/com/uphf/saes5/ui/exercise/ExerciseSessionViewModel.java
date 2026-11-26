package com.uphf.saes5.ui.exercise;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.data.home.DailyExerciseRepository;
import com.uphf.saes5.data.session.SessionRecorder;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.game.ArcadeScore;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.Movement;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.domain.movement.BodyVisibility;
import com.uphf.saes5.domain.movement.LatencyTracker;
import com.uphf.saes5.domain.movement.MovementComparisonEngine;
import com.uphf.saes5.domain.movement.MovementComparisonResult;
import com.uphf.saes5.domain.movement.MovementMath;
import com.uphf.saes5.domain.movement.PostureFeedback;
import com.uphf.saes5.ui.camera.PoseFrame;
import com.uphf.saes5.ui.common.Event;
import com.uphf.saes5.ui.summary.SummaryArgs;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/**
 * Séance d'exercice : compte les répétitions, juge la posture, tient le score, et termine la
 * séance.
 *
 * <p>Les images de la caméra arrivent par {@link #onPoseFrame}. Elles sont comparées au modèle
 * par le moteur ({@link MovementComparisonEngine}) sur un fil dédié, au plus une toutes les
 * 100 ms. Sans modèle utilisable, ou si la caméra est refusée ou en erreur, la séance passe en
 * validation manuelle.</p>
 *
 * <p>Tout l'état de la séance vit ici : une rotation de l'écran recrée l'Activity, mais pas
 * ce ViewModel. Compteurs, chronomètre, score et délai de fin automatique sont conservés.</p>
 */
public final class ExerciseSessionViewModel extends ViewModel {

    private static final long POSE_SAMPLE_INTERVAL_MILLIS = 100L;
    /** Délai entre l'objectif atteint et l'ouverture du récapitulatif (US-3.1). */
    public static final long GOAL_AUTO_FINISH_DELAY_MILLIS = 3_000L;

    private final Exercise exercise;
    private final SessionRecorder recorder;
    private final DailyExerciseRepository dailyExercise;
    private final LongSupplier elapsedRealtime;
    private final Executor comparisonExecutor;
    private final Executor mainExecutor;
    private final boolean arcadeEnabled;
    private final boolean soundsEnabled;
    @Nullable private final Movement preview;
    @Nullable private final MovementComparisonEngine engine;
    private final PostureFeedback postureFeedback;
    private final LatencyTracker latency = new LatencyTracker();
    private final ArcadeScore arcade = new ArcadeScore();
    private final AtomicBoolean comparisonInFlight = new AtomicBoolean();

    private final MutableLiveData<ExerciseUiState> state = new MutableLiveData<>();
    private final MutableLiveData<Event<ExerciseEvent>> events = new MutableLiveData<>();

    private final long startedAt;
    private int validatedReps;
    private int failedReps;
    private int similarityTotal;
    private int similarityCount;
    private boolean manualMode;
    private int similarity = ExerciseUiState.UNAVAILABLE;
    private ExerciseUiState.Status status;
    private PostureFeedback.State posture = PostureFeedback.State.NEUTRAL;
    private long lastSampleAt;
    /** Le corps était visible à l'image précédente : sa sortie du cadre abandonne la tentative. */
    private boolean bodyFullyVisible;
    private long goalReachedAt = -1;
    private boolean ended;

    /**
     * @param reference          le modèle de l'exercice, ou {@code null} s'il manque
     * @param demo               la démonstration du mannequin, ou {@code null} : le modèle est montré
     * @param comparisonExecutor fil de la comparaison ; arrêté dans {@link #onCleared()}
     * @param mainExecutor       fil principal, où l'état est publié
     */
    public ExerciseSessionViewModel(Exercise exercise, @Nullable Movement reference,
                                    @Nullable Movement demo, GameSettingsRepository gameSettings,
                                    SessionRecorder recorder, DailyExerciseRepository dailyExercise,
                                    LongSupplier elapsedRealtime, Executor comparisonExecutor,
                                    Executor mainExecutor) {
        this.exercise = exercise;
        this.recorder = recorder;
        this.dailyExercise = dailyExercise;
        this.elapsedRealtime = elapsedRealtime;
        this.comparisonExecutor = comparisonExecutor;
        this.mainExecutor = mainExecutor;
        this.arcadeEnabled = gameSettings.isEnabled(GameFeature.ARCADE);
        this.soundsEnabled = gameSettings.isEnabled(GameFeature.SOUNDS);
        this.postureFeedback = new PostureFeedback(exercise.getSimilarityThreshold());
        this.startedAt = elapsedRealtime.getAsLong();

        if (MovementRepository.isUsable(reference)) {
            engine = new MovementComparisonEngine(reference.frames, exercise.getSimilarityThreshold());
            // Le mannequin montre le geste attendu : la démonstration de l'exercice de base,
            // vue sous l'angle le plus lisible, ou à défaut le modèle lui-même.
            preview = demo != null ? demo : reference;
            status = ExerciseUiState.Status.REFERENCE_READY;
        } else {
            engine = null;
            preview = null;
            manualMode = true;
            status = ExerciseUiState.Status.MANUAL_REFERENCE_MISSING;
        }
        publish();
    }

    public LiveData<ExerciseUiState> getState() {
        return state;
    }

    public LiveData<Event<ExerciseEvent>> getEvents() {
        return events;
    }

    // --- caméra ----------------------------------------------------------------------------

    /** Réponse à la demande d'accès à la caméra. */
    public void onCameraPermission(boolean granted) {
        if (!granted) enableManualMode(ExerciseUiState.Status.MANUAL_PERMISSION_REFUSED);
    }

    /** La caméra vient de (re)démarrer : la tentative en cours repart de zéro. */
    public void onCameraStarted() {
        if (engine != null) comparisonExecutor.execute(engine::reset);
    }

    public void onCameraError() {
        enableManualMode(ExerciseUiState.Status.MANUAL_CAMERA_ERROR);
    }

    /** Une image analysée par MediaPipe. Appelée sur le fil principal. */
    public void onPoseFrame(PoseFrame frame) {
        // US-1.4 : chaque image analysée compte dans la latence, même si elle n'est pas comparée.
        latency.record(frame.getLatencyMillis());

        long now = elapsedRealtime.getAsLong();
        if (!isComparing() || now - lastSampleAt < POSE_SAMPLE_INTERVAL_MILLIS
                || !comparisonInFlight.compareAndSet(false, true)) {
            return;
        }
        // Corps en partie hors du cadre : MediaPipe devine les points qu'il ne voit pas, et
        // la comparaison compterait des répétitions sur des membres inventés.
        if (!BodyVisibility.isMostlyVisible(frame.getPoseLandmarks())) {
            comparisonInFlight.set(false);
            onBodyNotFullyVisible();
            return;
        }
        bodyFullyVisible = true;
        float[] normalized = MovementMath.normalize(frame.getPoseLandmarks(),
                frame.getInputImageWidth(), frame.getInputImageHeight());
        if (normalized == null) {
            comparisonInFlight.set(false);
            onPoseEmpty();
            return;
        }
        lastSampleAt = now;
        MovementComparisonEngine comparison = engine;
        comparisonExecutor.execute(() -> {
            MovementComparisonResult result = comparison.addFrame(normalized);
            mainExecutor.execute(() -> applyComparison(result));
        });
    }

    /** Personne dans l'image. */
    public void onPoseEmpty() {
        postureFeedback.reset();
        posture = PostureFeedback.State.NEUTRAL;
        status = ExerciseUiState.Status.POSE_NOT_DETECTED;
        publish();
    }

    // --- boutons ---------------------------------------------------------------------------

    public void onManualValidate() {
        onRepetitionValidated(SessionEntry.SCORE_UNAVAILABLE);
    }

    public void onManualFail() {
        onRepetitionFailed();
    }

    public void onFinishRequested() {
        finish();
    }

    /** Le délai qui suit l'objectif atteint est écoulé. */
    public void onAutoFinishDue() {
        if (goalReachedAt >= 0) finish();
    }

    /**
     * Séance quittée avant la fin : elle est journalisée quand même, marquée abandonnée
     * (US-7.1). Pour l'étude, un abandon est une donnée, pas un trou dans le journal.
     */
    public void onQuitConfirmed() {
        if (ended) return;
        ended = true;
        recorder.record(exercise, validatedReps, failedReps, elapsedMillis(), averageSimilarity(),
                latency.getAverageMillis(), arcadePointsOrNone(), true);
        emit(ExerciseEvent.of(ExerciseEvent.Kind.ABANDONED));
    }

    // --- déroulé ---------------------------------------------------------------------------

    private void applyComparison(MovementComparisonResult result) {
        comparisonInFlight.set(false);
        if (!isComparing()) return;
        similarity = result.isReady() ? result.getMovementScore() : result.getLiveScore();
        // US-3.2 : la couleur suit la posture instantanée, pas la validation de la répétition,
        // qui n'est connue qu'en fin de mouvement.
        posture = postureFeedback.update(result.getLiveScore());
        status = posture == PostureFeedback.State.CORRECT
                ? ExerciseUiState.Status.POSTURE_CORRECT
                : ExerciseUiState.Status.POSTURE_TO_CORRECT;
        publish();

        if (result.isRepetitionFailed()) {
            onRepetitionFailed();
        }
        if (result.isRepetitionValidated()) {
            similarityTotal += result.getMovementScore();
            similarityCount++;
            onRepetitionValidated(result.getMovementScore());
        }
    }

    /**
     * Rien n'est comparé tant que le corps n'est pas assez visible. La tentative en cours est
     * abandonnée en sortant du cadre : une répétition ne peut pas se valider sur un mouvement
     * vu en partie seulement.
     */
    private void onBodyNotFullyVisible() {
        if (bodyFullyVisible && engine != null) {
            comparisonExecutor.execute(engine::reset);
        }
        bodyFullyVisible = false;
        postureFeedback.reset();
        posture = PostureFeedback.State.NEUTRAL;
        similarity = ExerciseUiState.UNAVAILABLE;
        status = ExerciseUiState.Status.BODY_NOT_VISIBLE;
        publish();
    }

    private void onRepetitionValidated(int precision) {
        validatedReps++;
        int gained = arcade.onSuccess(precision);
        // Objectif atteint avec la caméra : la séance se terminera seule (US-3.1).
        // L'utilisateur, à plusieurs mètres, n'a pas à revenir viser « Terminer ».
        if (!manualMode && validatedReps == exercise.getTargetReps()) {
            goalReachedAt = elapsedRealtime.getAsLong();
            publish();
            emit(ExerciseEvent.of(ExerciseEvent.Kind.GOAL_REACHED));
            return;
        }
        publish();
        emit(ExerciseEvent.repValidated(arcadeEnabled ? gained : -1));
    }

    /** Un mouvement a été tenté mais n'est pas conforme à la référence : il n'est pas compté. */
    private void onRepetitionFailed() {
        failedReps++;
        arcade.onFailure();
        publish();
        emit(ExerciseEvent.of(ExerciseEvent.Kind.REP_FAILED));
    }

    private void finish() {
        if (ended) return;
        ended = true;
        // Une séance sans aucune répétition validée n'est pas comptée comme réalisée (US-4.2).
        if (validatedReps > 0) {
            dailyExercise.markCompletedToday();
        }
        emit(ExerciseEvent.finished(new SummaryArgs(exercise.getId(), validatedReps, failedReps,
                elapsedMillis(), averageSimilarity(), latency.getAverageMillis(),
                arcadePointsOrNone())));
    }

    private void enableManualMode(ExerciseUiState.Status reason) {
        manualMode = true;
        similarity = ExerciseUiState.UNAVAILABLE;
        status = reason;
        publish();
    }

    /** La caméra compte : modèle présent, pas de mode manuel, objectif pas encore atteint. */
    private boolean isComparing() {
        return engine != null && !manualMode && goalReachedAt < 0 && !ended;
    }

    private long elapsedMillis() {
        return elapsedRealtime.getAsLong() - startedAt;
    }

    private int averageSimilarity() {
        return similarityCount == 0
                ? SessionEntry.SCORE_UNAVAILABLE
                : Math.round(similarityTotal / (float) similarityCount);
    }

    private int arcadePointsOrNone() {
        return arcadeEnabled ? arcade.getTotal() : SessionEntry.POINTS_UNAVAILABLE;
    }

    private void emit(ExerciseEvent event) {
        events.setValue(new Event<>(event));
    }

    private void publish() {
        state.setValue(new ExerciseUiState(
                exercise.getName(), validatedReps, exercise.getTargetReps(), startedAt,
                manualMode, similarity, status, exercise.getSimilarityThreshold(), posture,
                arcadeEnabled, arcade.getTotal(), arcade.getMultiplier(),
                latency.getAverageMillis(), latency.isAboveTarget(), preview, soundsEnabled,
                goalReachedAt));
    }

    @Override
    protected void onCleared() {
        if (comparisonExecutor instanceof ExecutorService) {
            ((ExecutorService) comparisonExecutor).shutdownNow();
        }
    }
}

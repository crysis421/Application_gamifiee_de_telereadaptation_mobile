package com.uphf.saes5.ui.exercise;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.uphf.saes5.databinding.ActivityExerciseBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import com.uphf.saes5.R;
import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.exercise.MovementStorage;
import com.uphf.saes5.data.home.DailyExerciseTracker;
import com.uphf.saes5.data.session.SessionRecorder;
import com.uphf.saes5.data.settings.GameSettings;
import com.uphf.saes5.domain.exercise.BaseExercises;
import com.uphf.saes5.domain.game.ArcadeScore;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.Movement;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.domain.movement.BodyVisibility;
import com.uphf.saes5.domain.movement.LatencyTracker;
import com.uphf.saes5.domain.movement.MovementComparisonEngine;
import com.uphf.saes5.domain.movement.MovementComparisonResult;
import com.uphf.saes5.domain.movement.MovementMath;
import com.uphf.saes5.domain.movement.PostureFeedback;
import com.uphf.saes5.ui.camera.PoseCameraController;
import com.uphf.saes5.ui.camera.PoseFrame;
import com.uphf.saes5.ui.camera.SoundFeedback;
import com.uphf.saes5.ui.common.EdgeToEdgeSupport;
import com.uphf.saes5.ui.common.ExerciseFormatter;
import com.uphf.saes5.ui.summary.SummaryActivity;

/**
 * Écran d'exécution d'un exercice.
 *
 * <p>CameraX et MediaPipe alimentent un moteur de comparaison par angles. Les boutons manuels
 * restent disponibles uniquement si l'exercice n'a pas encore de modèle ou si la caméra est
 * refusée.</p>
 */
public class ExerciseActivity extends AppCompatActivity implements PoseCameraController.Listener {

    private static final String EXTRA_EXERCISE_ID = "extra_exercise_id";
    private static final long FEEDBACK_DURATION_MILLIS = 1_200L;
    private static final long POSE_SAMPLE_INTERVAL_MILLIS = 100L;
    private static final long REP_PULSE_DURATION_MILLIS = 450L;
    private static final long GOAL_AUTO_FINISH_DELAY_MILLIS = 3_000L;

    private ActivityExerciseBinding binding;
    private Exercise exercise;
    private DailyExerciseTracker dailyExerciseTracker;
    private SoundFeedback soundFeedback;
    private MovementComparisonEngine comparisonEngine;
    private PostureFeedback postureFeedback;
    private final LatencyTracker latencyTracker = new LatencyTracker();
    private final ArcadeScore arcadeScore = new ArcadeScore();
    private PoseCameraController cameraController;
    private long lastPoseSampleRealtime;
    private boolean cameraPermissionRefused;
    private volatile boolean comparisonActive;
    /** Le corps était visible à l'image précédente : sa sortie du cadre abandonne la tentative. */
    private boolean bodyFullyVisible;
    private final ExecutorService comparisonExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean comparisonInFlight = new AtomicBoolean();

    private int validatedReps;
    private int failedReps;
    private int completedSimilarityTotal;
    private int completedSimilarityCount;
    private long startElapsedRealtime;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable chronometerTick = new Runnable() {
        @Override
        public void run() {
            updateChronometer();
            handler.postDelayed(this, 1_000L);
        }
    };
    private final Runnable hideFeedback = () -> binding.feedbackBanner.setVisibility(View.INVISIBLE);
    private final Runnable autoFinish = () -> {
        if (!isFinishing()) finishExercise();
    };
    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    cameraPermissionRefused = false;
                    startCameraComparison();
                } else {
                    cameraPermissionRefused = true;
                    enableManualFallback(R.string.exercise_camera_permission_required);
                }
            });

    public static Intent newIntent(Context context, String exerciseId) {
        Intent intent = new Intent(context, ExerciseActivity.class);
        intent.putExtra(EXTRA_EXERCISE_ID, exerciseId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        String exerciseId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
        exercise = ExerciseRepository.findById(exerciseId);
        if (exercise == null) {
            // L'exercice n'existe plus (catalogue modifié) : on ne peut rien afficher.
            finish();
            return;
        }

        binding = ActivityExerciseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Les marges des barres système vont au calque d'informations, pas à la racine : la
        // caméra passe ainsi sous la barre d'état et la barre de navigation, en plein écran.
        EdgeToEdgeSupport.applySystemBarInsets(binding.hud);

        dailyExerciseTracker = new DailyExerciseTracker(this);
        soundFeedback = new SoundFeedback(this);
        startElapsedRealtime = SystemClock.elapsedRealtime();

        binding.exerciseName.setText(exercise.getName());
        updateRepCounter();
        updateChronometer();

        binding.validateRepButton.setOnClickListener(
                view -> onRepetitionValidated(SessionEntry.SCORE_UNAVAILABLE));
        updateArcadeScore();
        binding.failRepButton.setOnClickListener(view -> onRepetitionFailed());
        binding.finishButton.setOnClickListener(view -> finishExercise());
        prepareReferenceComparison();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmQuit();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(chronometerTick);
        if (comparisonEngine != null && !cameraPermissionRefused
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraComparison();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(chronometerTick);
        stopCameraComparison();
    }

    @Override
    protected void onDestroy() {
        stopCameraComparison();
        comparisonExecutor.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        if (soundFeedback != null) {
            soundFeedback.release();
        }
        super.onDestroy();
    }

    private void prepareReferenceComparison() {
        Movement reference = MovementStorage.load(this, exercise.getReferenceMovementId());
        if (!hasUsableReference(reference)) {
            enableManualFallback(R.string.exercise_reference_missing_fallback);
            return;
        }

        comparisonEngine = new MovementComparisonEngine(
                reference.frames, exercise.getSimilarityThreshold());
        postureFeedback = new PostureFeedback(exercise.getSimilarityThreshold());
        binding.validateRepButton.setVisibility(View.GONE);
        binding.failRepButton.setVisibility(View.GONE);
        binding.comparisonStatus.setText(getString(R.string.exercise_reference_ready,
                exercise.getSimilarityThreshold()));
        // Le mannequin montre le geste attendu : la démonstration de l'exercice de base, vue
        // sous l'angle le plus lisible, ou à défaut le modèle lui-même.
        Movement demo = BaseExercises.loadDemo(this, exercise);
        Movement shown = demo != null ? demo : reference;
        if (binding.modelPreview.setMovement(shown.frames, shown.sampleIntervalMs)) {
            binding.modelPreview.setVisibility(View.VISIBLE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean hasUsableReference(Movement movement) {
        if (movement == null || movement.frames == null || movement.frames.isEmpty()) return false;
        for (float[] frame : movement.frames) {
            if (MovementMath.angleFeatures(frame) != null) return true;
        }
        return false;
    }

    private void startCameraComparison() {
        if (comparisonEngine == null || cameraController != null || isFinishing()) return;
        comparisonActive = true;
        comparisonExecutor.execute(comparisonEngine::reset);
        cameraController = new PoseCameraController(
                this, binding.cameraPreview, binding.poseOverlay, this);
        cameraController.start();
    }

    private void stopCameraComparison() {
        comparisonActive = false;
        if (cameraController == null) return;
        cameraController.stop();
        cameraController = null;
    }

    private void enableManualFallback(@StringRes int reason) {
        stopCameraComparison();
        binding.similarityScore.setText(R.string.exercise_similarity_unavailable);
        binding.comparisonStatus.setText(reason);
        binding.comparisonStatus.setTextColor(
                ContextCompat.getColor(this, R.color.colorOnSurfaceVariant));
        binding.validateRepButton.setVisibility(View.VISIBLE);
        binding.failRepButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResults(@NonNull PoseFrame poseFrame) {
        // US-1.4 : chaque image analysée compte dans la latence, même si elle n'est pas comparée.
        latencyTracker.record(poseFrame.getLatencyMillis());

        long now = SystemClock.elapsedRealtime();
        if (!comparisonActive || now - lastPoseSampleRealtime < POSE_SAMPLE_INTERVAL_MILLIS
                || !comparisonInFlight.compareAndSet(false, true)) return;

        showLatency();
        // Corps en partie hors du cadre : MediaPipe devine les points qu'il ne voit pas, et
        // la comparaison compterait des répétitions sur des membres inventés.
        if (!BodyVisibility.isMostlyVisible(poseFrame.getPoseLandmarks())) {
            comparisonInFlight.set(false);
            onBodyNotFullyVisible();
            return;
        }
        bodyFullyVisible = true;
        float[] frame = MovementMath.normalize(
                poseFrame.getPoseLandmarks(),
                poseFrame.getInputImageWidth(),
                poseFrame.getInputImageHeight());
        if (frame == null || comparisonEngine == null) {
            comparisonInFlight.set(false);
            onEmpty();
            return;
        }
        lastPoseSampleRealtime = now;

        comparisonExecutor.execute(() -> {
            MovementComparisonResult result = comparisonEngine.addFrame(frame);
            runOnUiThread(() -> applyComparisonResult(result));
        });
    }

    private void applyComparisonResult(MovementComparisonResult result) {
        comparisonInFlight.set(false);
        if (!comparisonActive || isFinishing() || isDestroyed()) return;
        int displayedScore = result.isReady()
                ? result.getMovementScore()
                : result.getLiveScore();
        binding.similarityScore.setText(
                getString(R.string.exercise_similarity_score, displayedScore));
        // US-3.2 : la couleur suit la posture instantanée, pas la validation de la répétition,
        // qui n'est connue qu'en fin de mouvement.
        PostureFeedback.State posture = postureFeedback.update(result.getLiveScore());
        binding.poseOverlay.setPostureState(posture);
        if (posture == PostureFeedback.State.CORRECT) {
            showComparisonStatus(R.string.exercise_posture_correct, R.color.app_success);
        } else {
            showComparisonStatus(R.string.exercise_posture_to_correct, R.color.app_error);
        }

        if (result.isRepetitionFailed()) {
            onRepetitionFailed();
        }
        if (result.isRepetitionValidated()) {
            completedSimilarityTotal += result.getMovementScore();
            completedSimilarityCount++;
            onRepetitionValidated(result.getMovementScore());
        }
    }

    /**
     * Rien n'est comparé tant que le corps n'est pas assez visible. La tentative en cours est
     * abandonnée en sortant du cadre : une répétition ne peut pas se valider sur un mouvement
     * vu en partie seulement.
     */
    private void onBodyNotFullyVisible() {
        if (bodyFullyVisible && comparisonEngine != null) {
            comparisonExecutor.execute(comparisonEngine::reset);
        }
        bodyFullyVisible = false;
        if (postureFeedback != null) postureFeedback.reset();
        binding.poseOverlay.setPostureState(PostureFeedback.State.NEUTRAL);
        binding.similarityScore.setText(R.string.exercise_similarity_unavailable);
        showComparisonStatus(R.string.exercise_body_not_fully_visible,
                R.color.colorOnSurfaceVariant);
    }

    @Override
    public void onEmpty() {
        if (postureFeedback != null) postureFeedback.reset();
        binding.poseOverlay.setPostureState(PostureFeedback.State.NEUTRAL);
        showComparisonStatus(R.string.exercise_pose_not_detected,
                R.color.colorOnSurfaceVariant);
    }

    @Override
    public void onError(@NonNull String message) {
        enableManualFallback(R.string.exercise_camera_error_fallback);
    }

    private void showComparisonStatus(@StringRes int messageRes, @ColorRes int colorRes) {
        binding.comparisonStatus.setText(messageRes);
        binding.comparisonStatus.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    /** Une répétition complète et correcte vient d'être détectée. */
    private void onRepetitionValidated(int precision) {
        validatedReps++;
        int gained = arcadeScore.onSuccess(precision);
        updateRepCounter();
        updateArcadeScore();
        pulseRepCounter();
        if (comparisonActive && validatedReps == exercise.getTargetReps()) {
            onGoalReachedHandsFree();
            return;
        }
        if (isArcadeEnabled()) {
            showFeedback(getString(R.string.exercise_feedback_valid_points, gained),
                    R.color.app_success);
        } else {
            showFeedback(R.string.exercise_feedback_valid, R.color.app_success);
        }
        soundFeedback.playValid();
    }

    private boolean isArcadeEnabled() {
        return GameSettings.isEnabled(GameSettings.Feature.ARCADE);
    }

    /** Score et multiplicateur en temps réel (US-4.4). */
    private void updateArcadeScore() {
        if (!isArcadeEnabled()) {
            binding.arcadeScore.setVisibility(View.GONE);
            return;
        }
        binding.arcadeScore.setVisibility(View.VISIBLE);
        binding.arcadeScore.setText(getString(R.string.exercise_arcade_score,
                arcadeScore.getTotal(), arcadeScore.getMultiplier()));
        // Multiplicateur actif : le score passe à la couleur principale pour se remarquer.
        binding.arcadeScore.setTextColor(ContextCompat.getColor(this,
                arcadeScore.getMultiplier() > 1 ? R.color.colorPrimary : R.color.colorOnSurface));
    }

    /** Un mouvement a été tenté mais n'est pas conforme à la référence : il n'est pas compté. */
    private void onRepetitionFailed() {
        failedReps++;
        arcadeScore.onFailure();
        updateArcadeScore();
        showFeedback(R.string.exercise_feedback_failed, R.color.app_error);
        soundFeedback.playFailed();
    }

    /**
     * Objectif atteint pendant une séance suivie par la caméra : la séance se termine seule
     * après un court délai (US-3.1). L'utilisateur, à plusieurs mètres, n'a pas à revenir
     * viser le bouton « Terminer ».
     */
    private void onGoalReachedHandsFree() {
        // Plus rien à compter : la caméra reste affichée, mais les résultats sont ignorés.
        comparisonActive = false;
        soundFeedback.playValid();
        handler.removeCallbacks(hideFeedback);
        binding.feedbackBanner.setText(getString(R.string.exercise_goal_reached_auto_finish,
                GOAL_AUTO_FINISH_DELAY_MILLIS / 1_000L));
        binding.feedbackBanner.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.app_success)));
        binding.feedbackBanner.setVisibility(View.VISIBLE);
        handler.postDelayed(autoFinish, GOAL_AUTO_FINISH_DELAY_MILLIS);
    }

    /**
     * Le compteur grossit puis revient, en vert : de loin, c'est le signal le plus visible
     * qu'une répétition vient d'être comptée (US-3.2).
     */
    private void pulseRepCounter() {
        int primary = ContextCompat.getColor(this, R.color.colorPrimary);
        binding.repCounter.animate().cancel();
        binding.repCounter.setTextColor(ContextCompat.getColor(this, R.color.app_success));
        binding.repCounter.setScaleX(1.25f);
        binding.repCounter.setScaleY(1.25f);
        binding.repCounter.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(REP_PULSE_DURATION_MILLIS)
                .withEndAction(() -> binding.repCounter.setTextColor(primary))
                .start();
    }

    private void updateRepCounter() {
        binding.repCounter.setText(
                getString(R.string.exercise_rep_counter, validatedReps, exercise.getTargetReps()));
    }

    private void updateChronometer() {
        binding.chronometer.setText(ExerciseFormatter.formatDuration(elapsedMillis()));
    }

    private long elapsedMillis() {
        return SystemClock.elapsedRealtime() - startElapsedRealtime;
    }

    /**
     * Affiche le bandeau de feedback dans la couleur demandée.
     *
     * <p>C'est un tint et non une couleur de fond : le fond arrondi défini dans le layout est
     * ainsi conservé. Le texte reste toujours sur {@code colorOnPrimary} (noir), lisible aussi
     * bien sur le vert que sur le rouge.</p>
     */
    private void showFeedback(@StringRes int messageRes, @ColorRes int backgroundRes) {
        showFeedback(getString(messageRes), backgroundRes);
    }

    private void showFeedback(String message, @ColorRes int backgroundRes) {
        binding.feedbackBanner.setText(message);
        binding.feedbackBanner.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, backgroundRes)));
        binding.feedbackBanner.setVisibility(View.VISIBLE);

        handler.removeCallbacks(hideFeedback);
        handler.postDelayed(hideFeedback, FEEDBACK_DURATION_MILLIS);
    }

    private void confirmQuit() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exercise_quit_title)
                .setMessage(R.string.exercise_quit_message)
                .setNegativeButton(R.string.exercise_quit_cancel, null)
                .setPositiveButton(R.string.exercise_quit_confirm, (dialog, which) -> abandon())
                .show();
    }

    /**
     * Séance quittée avant la fin : elle est journalisée quand même, marquée abandonnée
     * (US-7.1). Pour l'étude, un abandon est une donnée, pas un trou dans le journal.
     */
    private void abandon() {
        handler.removeCallbacks(autoFinish);
        SessionRecorder.record(exercise, validatedReps, failedReps, elapsedMillis(),
                averageSimilarity(), latencyTracker.getAverageMillis(), arcadePointsOrNone(),
                true);
        finish();
    }

    private int averageSimilarity() {
        return completedSimilarityCount == 0
                ? SessionEntry.SCORE_UNAVAILABLE
                : Math.round(completedSimilarityTotal / (float) completedSimilarityCount);
    }

    private int arcadePointsOrNone() {
        return isArcadeEnabled() ? arcadeScore.getTotal() : SessionEntry.POINTS_UNAVAILABLE;
    }

    private void finishExercise() {
        long duration = elapsedMillis();
        int averageSimilarity = averageSimilarity();
        soundFeedback.playFinish();

        // Une session sans aucune répétition validée n'est pas comptée comme réalisée (US-4.2).
        if (validatedReps > 0) {
            dailyExerciseTracker.markCompletedToday();
        }

        startActivity(SummaryActivity.newIntent(
                this, exercise.getId(), validatedReps, failedReps, duration,
                averageSimilarity, latencyTracker.getAverageMillis(), arcadePointsOrNone()));
        finish();
    }

    /** Latence moyenne de l'analyse, en rouge si elle dépasse la cible de l'US-1.4. */
    private void showLatency() {
        int average = latencyTracker.getAverageMillis();
        if (average == LatencyTracker.UNAVAILABLE) return;
        binding.latencyLabel.setVisibility(View.VISIBLE);
        binding.latencyLabel.setText(getString(R.string.exercise_latency, average));
        binding.latencyLabel.setTextColor(ContextCompat.getColor(this,
                latencyTracker.isAboveTarget() ? R.color.app_error : R.color.colorOnSurfaceVariant));
    }
}

package com.uphf.saes5.accueil.ui;

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

import com.uphf.saes5.R;
import com.uphf.saes5.HolisticLandmarkerHelper;
import com.uphf.saes5.Movement;
import com.uphf.saes5.MovementComparisonEngine;
import com.uphf.saes5.MovementComparisonResult;
import com.uphf.saes5.MovementMath;
import com.uphf.saes5.MovementStorage;
import com.uphf.saes5.PoseCameraController;
import com.uphf.saes5.PostureFeedback;
import com.uphf.saes5.SessionEntry;
import com.uphf.saes5.SoundFeedback;
import com.uphf.saes5.accueil.data.DailyExerciseTracker;
import com.uphf.saes5.ExerciseRepository;
import com.uphf.saes5.databinding.ActivityExerciseBinding;
import com.uphf.saes5.Exercise;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private PoseCameraController cameraController;
    private long lastPoseSampleRealtime;
    private boolean cameraPermissionRefused;
    private volatile boolean comparisonActive;
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
        EdgeToEdgeSupport.applySystemBarInsets(binding.main);

        dailyExerciseTracker = new DailyExerciseTracker(this);
        soundFeedback = new SoundFeedback(this);
        startElapsedRealtime = SystemClock.elapsedRealtime();

        binding.exerciseName.setText(exercise.getName());
        updateRepCounter();
        updateChronometer();

        binding.validateRepButton.setOnClickListener(view -> onRepetitionValidated());
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
    public void onResults(@NonNull HolisticLandmarkerHelper.ResultBundle resultBundle) {
        long now = SystemClock.elapsedRealtime();
        if (!comparisonActive || now - lastPoseSampleRealtime < POSE_SAMPLE_INTERVAL_MILLIS
                || !comparisonInFlight.compareAndSet(false, true)) return;

        float[] frame = MovementMath.normalize(
                resultBundle.getResult().poseLandmarks(),
                resultBundle.getInputImageWidth(),
                resultBundle.getInputImageHeight());
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

        if (result.isRepetitionValidated()) {
            completedSimilarityTotal += result.getMovementScore();
            completedSimilarityCount++;
            onRepetitionValidated();
        }
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
    private void onRepetitionValidated() {
        validatedReps++;
        updateRepCounter();
        pulseRepCounter();
        if (comparisonActive && validatedReps == exercise.getTargetReps()) {
            onGoalReachedHandsFree();
            return;
        }
        showFeedback(R.string.exercise_feedback_valid, R.color.app_success);
        soundFeedback.playValid();
    }

    /** Un mouvement a été tenté mais n'est pas conforme à la référence : il n'est pas compté. */
    private void onRepetitionFailed() {
        failedReps++;
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
        binding.feedbackBanner.setText(messageRes);
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
                .setPositiveButton(R.string.exercise_quit_confirm, (dialog, which) -> finish())
                .show();
    }

    private void finishExercise() {
        long duration = elapsedMillis();
        int averageSimilarity = completedSimilarityCount == 0
                ? SessionEntry.SCORE_UNAVAILABLE
                : Math.round(completedSimilarityTotal / (float) completedSimilarityCount);
        soundFeedback.playFinish();

        // Une session sans aucune répétition validée n'est pas comptée comme réalisée (US-4.2).
        if (validatedReps > 0) {
            dailyExerciseTracker.markCompletedToday();
        }

        startActivity(SummaryActivity.newIntent(
                this, exercise.getId(), validatedReps, failedReps, duration,
                averageSimilarity));
        finish();
    }
}

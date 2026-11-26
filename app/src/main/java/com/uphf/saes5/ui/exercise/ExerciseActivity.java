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
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.uphf.saes5.R;
import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.databinding.ActivityExerciseBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.Movement;
import com.uphf.saes5.ui.camera.PoseCameraController;
import com.uphf.saes5.ui.camera.PoseFrame;
import com.uphf.saes5.ui.camera.SoundFeedback;
import com.uphf.saes5.ui.common.EdgeToEdgeSupport;
import com.uphf.saes5.ui.common.ExerciseFormatter;
import com.uphf.saes5.ui.summary.SummaryActivity;

import java.util.concurrent.Executors;

/**
 * Écran d'exécution d'un exercice : caméra en plein écran, compteur, score et mannequin.
 *
 * <p>Vue du MVVM : tout ce qui se décide (comptage, posture, score, fin de séance) se décide
 * dans {@link ExerciseSessionViewModel}. L'écran possède ce qui dépend d'Android : la caméra,
 * les sons, les animations, la boîte de dialogue de sortie et la permission. Après une
 * rotation, il se rebranche sur le même ViewModel : la séance continue.</p>
 */
public class ExerciseActivity extends AppCompatActivity implements PoseCameraController.Listener {

    private static final String EXTRA_EXERCISE_ID = "extra_exercise_id";
    private static final long FEEDBACK_DURATION_MILLIS = 1_200L;
    private static final long REP_PULSE_DURATION_MILLIS = 450L;

    private ActivityExerciseBinding binding;
    private ExerciseSessionViewModel viewModel;
    private SoundFeedback sounds;
    @Nullable private PoseCameraController camera;
    private boolean autoFinishScheduled;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable chronometerTick = new Runnable() {
        @Override
        public void run() {
            updateChronometer();
            handler.postDelayed(this, 1_000L);
        }
    };
    private final Runnable hideFeedback =
            () -> binding.feedbackBanner.setVisibility(View.INVISIBLE);
    private final Runnable autoFinish = () -> viewModel.onAutoFinishDue();

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                viewModel.onCameraPermission(granted);
                if (granted) startCamera();
            });

    public static Intent newIntent(Context context, String exerciseId) {
        return new Intent(context, ExerciseActivity.class).putExtra(EXTRA_EXERCISE_ID, exerciseId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        AppContainer container = AppContainer.from(this);
        Exercise exercise = container.exerciseRepository.findById(
                getIntent().getStringExtra(EXTRA_EXERCISE_ID));
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

        MovementRepository movements = container.movementRepository;
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(ExerciseSessionViewModel.class,
                () -> new ExerciseSessionViewModel(exercise,
                        movements.load(exercise.getReferenceMovementId()),
                        movements.loadDemo(exercise),
                        container.gameSettings, container.sessionRecorder,
                        container.dailyExerciseRepository, SystemClock::elapsedRealtime,
                        Executors.newSingleThreadExecutor(),
                        ContextCompat.getMainExecutor(getApplicationContext()))))
                .get(ExerciseSessionViewModel.class);

        ExerciseUiState initial = viewModel.getState().getValue();
        sounds = new SoundFeedback(this, initial.isSoundsEnabled());
        binding.exerciseName.setText(initial.getExerciseName());
        showModelPreview(initial.getPreview());

        binding.validateRepButton.setOnClickListener(view -> viewModel.onManualValidate());
        binding.failRepButton.setOnClickListener(view -> viewModel.onManualFail());
        binding.finishButton.setOnClickListener(view -> viewModel.onFinishRequested());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmQuit();
            }
        });

        viewModel.getState().observe(this, this::render);
        viewModel.getEvents().observe(this, event -> {
            ExerciseEvent content = event.getContentIfNotHandled();
            if (content != null) onEvent(content);
        });

        if (!initial.isManualMode() && !hasCameraPermission() && savedInstanceState == null) {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(chronometerTick);
        startCamera();
        // Retour sur l'écran après l'objectif atteint : le délai de fin reprend là où il en était.
        ExerciseUiState state = viewModel.getState().getValue();
        if (state != null && state.getGoalReachedAtElapsed() >= 0) showGoalReached(state);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(chronometerTick);
        handler.removeCallbacks(autoFinish);
        autoFinishScheduled = false;
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (sounds != null) sounds.release();
        super.onDestroy();
    }

    // --- caméra ----------------------------------------------------------------------------

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ExerciseUiState state = viewModel.getState().getValue();
        if (camera != null || state == null || state.isManualMode() || !hasCameraPermission()
                || isFinishing()) {
            return;
        }
        AppContainer container = AppContainer.from(this);
        camera = new PoseCameraController(this, binding.cameraPreview, binding.poseOverlay, this,
                container.analysisSettings.getModel(), container.analysisSettings.getFrameStride());
        camera.start();
        viewModel.onCameraStarted();
    }

    private void stopCamera() {
        if (camera == null) return;
        camera.stop();
        camera = null;
    }

    @Override
    public void onResults(@NonNull PoseFrame frame) {
        viewModel.onPoseFrame(frame);
    }

    @Override
    public void onEmpty() {
        viewModel.onPoseEmpty();
    }

    @Override
    public void onError(@NonNull String message) {
        viewModel.onCameraError();
    }

    // --- affichage -------------------------------------------------------------------------

    private void showModelPreview(@Nullable Movement preview) {
        if (preview != null && binding.modelPreview.setMovement(
                preview.frames, preview.sampleIntervalMs)) {
            binding.modelPreview.setVisibility(View.VISIBLE);
        }
    }

    private void render(ExerciseUiState state) {
        if (state.isManualMode()) stopCamera();
        binding.validateRepButton.setVisibility(state.isManualMode() ? View.VISIBLE : View.GONE);
        binding.failRepButton.setVisibility(state.isManualMode() ? View.VISIBLE : View.GONE);

        binding.repCounter.setText(getString(R.string.exercise_rep_counter,
                state.getValidatedReps(), state.getTargetReps()));
        binding.similarityScore.setText(state.getSimilarity() == ExerciseUiState.UNAVAILABLE
                ? getString(R.string.exercise_similarity_unavailable)
                : getString(R.string.exercise_similarity_score, state.getSimilarity()));
        binding.poseOverlay.setPostureState(state.getPosture());
        renderStatus(state);
        renderArcade(state);
        renderLatency(state);
        if (state.getGoalReachedAtElapsed() >= 0) showGoalReached(state);
    }

    private void renderStatus(ExerciseUiState state) {
        switch (state.getStatus()) {
            case REFERENCE_READY:
                binding.comparisonStatus.setText(
                        getString(R.string.exercise_reference_ready, state.getThreshold()));
                break;
            case POSTURE_CORRECT:
                showStatus(R.string.exercise_posture_correct, R.color.app_success);
                break;
            case POSTURE_TO_CORRECT:
                showStatus(R.string.exercise_posture_to_correct, R.color.app_error);
                break;
            case BODY_NOT_VISIBLE:
                showStatus(R.string.exercise_body_not_fully_visible, R.color.colorOnSurfaceVariant);
                break;
            case POSE_NOT_DETECTED:
                showStatus(R.string.exercise_pose_not_detected, R.color.colorOnSurfaceVariant);
                break;
            case MANUAL_REFERENCE_MISSING:
                showStatus(R.string.exercise_reference_missing_fallback,
                        R.color.colorOnSurfaceVariant);
                break;
            case MANUAL_PERMISSION_REFUSED:
                showStatus(R.string.exercise_camera_permission_required,
                        R.color.colorOnSurfaceVariant);
                break;
            case MANUAL_CAMERA_ERROR:
                showStatus(R.string.exercise_camera_error_fallback, R.color.colorOnSurfaceVariant);
                break;
        }
    }

    private void showStatus(@StringRes int messageRes, @ColorRes int colorRes) {
        binding.comparisonStatus.setText(messageRes);
        binding.comparisonStatus.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    /** Score et multiplicateur en temps réel (US-4.4). */
    private void renderArcade(ExerciseUiState state) {
        binding.arcadeScore.setVisibility(state.isArcadeVisible() ? View.VISIBLE : View.GONE);
        if (!state.isArcadeVisible()) return;
        binding.arcadeScore.setText(getString(R.string.exercise_arcade_score,
                state.getArcadeTotal(), state.getArcadeMultiplier()));
        // Multiplicateur actif : le score passe à la couleur principale pour se remarquer.
        binding.arcadeScore.setTextColor(ContextCompat.getColor(this,
                state.getArcadeMultiplier() > 1 ? R.color.colorPrimary : R.color.colorOnSurface));
    }

    /** Latence moyenne de l'analyse, en rouge si elle dépasse la cible de l'US-1.4. */
    private void renderLatency(ExerciseUiState state) {
        if (state.getAverageLatency() == ExerciseUiState.UNAVAILABLE) return;
        binding.latencyLabel.setVisibility(View.VISIBLE);
        binding.latencyLabel.setText(getString(R.string.exercise_latency, state.getAverageLatency()));
        binding.latencyLabel.setTextColor(ContextCompat.getColor(this, state.isLatencyAboveTarget()
                ? R.color.app_error
                : R.color.colorOnSurfaceVariant));
    }

    private void updateChronometer() {
        ExerciseUiState state = viewModel.getState().getValue();
        if (state == null) return;
        binding.chronometer.setText(ExerciseFormatter.formatDuration(
                SystemClock.elapsedRealtime() - state.getStartedAtElapsed()));
    }

    /**
     * Objectif atteint avec la caméra : la séance se termine seule après un court délai
     * (US-3.1). Le délai court depuis l'objectif atteint, même si l'écran a été recréé entre
     * les deux.
     */
    private void showGoalReached(ExerciseUiState state) {
        handler.removeCallbacks(hideFeedback);
        binding.feedbackBanner.setText(getString(R.string.exercise_goal_reached_auto_finish,
                ExerciseSessionViewModel.GOAL_AUTO_FINISH_DELAY_MILLIS / 1_000L));
        binding.feedbackBanner.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.app_success)));
        binding.feedbackBanner.setVisibility(View.VISIBLE);
        if (!autoFinishScheduled) {
            autoFinishScheduled = true;
            long remaining = state.getGoalReachedAtElapsed()
                    + ExerciseSessionViewModel.GOAL_AUTO_FINISH_DELAY_MILLIS
                    - SystemClock.elapsedRealtime();
            handler.postDelayed(autoFinish, Math.max(0, remaining));
        }
    }

    // --- événements ------------------------------------------------------------------------

    private void onEvent(ExerciseEvent event) {
        switch (event.getKind()) {
            case REP_VALIDATED:
                pulseRepCounter();
                showFeedback(event.getPointsGained() >= 0
                        ? getString(R.string.exercise_feedback_valid_points, event.getPointsGained())
                        : getString(R.string.exercise_feedback_valid), R.color.app_success);
                sounds.playValid();
                break;
            case REP_FAILED:
                showFeedback(getString(R.string.exercise_feedback_failed), R.color.app_error);
                sounds.playFailed();
                break;
            case GOAL_REACHED:
                pulseRepCounter();
                sounds.playValid();
                break;
            case FINISHED:
                sounds.playFinish();
                startActivity(SummaryActivity.newIntent(this, event.getSummary()));
                finish();
                break;
            case ABANDONED:
                finish();
                break;
        }
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

    /**
     * Affiche le bandeau de retour dans la couleur demandée.
     *
     * <p>C'est un tint et non une couleur de fond : le fond arrondi défini dans le layout est
     * ainsi conservé. Le texte reste sur {@code colorOnPrimary}, lisible sur le vert comme sur
     * le rouge.</p>
     */
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
                .setPositiveButton(R.string.exercise_quit_confirm,
                        (dialog, which) -> viewModel.onQuitConfirmed())
                .show();
    }
}

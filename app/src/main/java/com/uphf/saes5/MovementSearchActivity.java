package com.uphf.saes5;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import com.uphf.saes5.accueil.ui.EdgeToEdgeSupport;
import com.uphf.saes5.accueil.ui.ExerciseActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrouver un exercice en faisant le mouvement devant la caméra (US-6.3).
 *
 * <p>Décompte pour se placer, quelques secondes d'enregistrement, puis comparaison avec le
 * mouvement de référence de chaque exercice du catalogue ({@link MovementMatcher}). Les plus
 * proches sont proposés ; en toucher un lance la séance.</p>
 */
public class MovementSearchActivity extends AppCompatActivity implements PoseCameraController.Listener {

    private static final int COUNTDOWN_SECONDS = 3;
    private static final int RECORD_SECONDS = 4;
    private static final long SAMPLE_INTERVAL_MILLIS = 100L;

    private enum State { COUNTDOWN, RECORDING, DONE }

    private PoseCameraController cameraController;
    private TextView status;
    private LinearLayout results;
    private View retry;
    private CountDownTimer timer;
    private State state = State.DONE;
    private final List<float[]> frames = new ArrayList<>();
    private long lastSampleRealtime;

    private final ActivityResultLauncher<String> requestCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                    startCountdown();
                } else {
                    status.setText(R.string.exercise_camera_permission_required);
                }
            });

    public static Intent newIntent(Context context) {
        return new Intent(context, MovementSearchActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movement_search);
        EdgeToEdgeSupport.applySystemBarInsets(findViewById(R.id.search_root));

        status = findViewById(R.id.search_status);
        results = findViewById(R.id.search_results);
        retry = findViewById(R.id.search_retry);
        retry.setOnClickListener(v -> startCountdown());
        findViewById(R.id.search_cancel).setOnClickListener(v -> finish());

        if (hasCameraPermission()) {
            startCountdown();
        } else {
            requestCamera.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasCameraPermission()) startCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (timer != null) timer.cancel();
        if (state != State.DONE) {
            // Interrompu en plein enregistrement : on repartira proprement.
            state = State.DONE;
            retry.setVisibility(View.VISIBLE);
        }
        if (cameraController != null) {
            cameraController.stop();
            cameraController = null;
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        if (cameraController != null) return;
        PreviewView preview = findViewById(R.id.search_preview);
        cameraController = new PoseCameraController(
                this, preview, findViewById(R.id.search_overlay), this);
        cameraController.start();
    }

    private void startCountdown() {
        if (timer != null) timer.cancel();
        results.removeAllViews();
        retry.setVisibility(View.GONE);
        frames.clear();
        state = State.COUNTDOWN;
        timer = new CountDownTimer(COUNTDOWN_SECONDS * 1_000L, 1_000L) {
            @Override
            public void onTick(long left) {
                status.setText(getString(R.string.search_countdown, (left + 999) / 1_000));
            }

            @Override
            public void onFinish() {
                startRecording();
            }
        }.start();
    }

    private void startRecording() {
        state = State.RECORDING;
        frames.clear();
        lastSampleRealtime = 0;
        timer = new CountDownTimer(RECORD_SECONDS * 1_000L, 1_000L) {
            @Override
            public void onTick(long left) {
                status.setText(getString(R.string.search_recording, (left + 999) / 1_000));
            }

            @Override
            public void onFinish() {
                showMatches();
            }
        }.start();
    }

    private void showMatches() {
        state = State.DONE;
        retry.setVisibility(View.VISIBLE);

        List<MovementMatcher.Candidate> candidates = new ArrayList<>();
        for (Exercise exercise : ExerciseRepository.getAll()) {
            Movement reference = MovementStorage.load(this, exercise.getReferenceMovementId());
            if (reference != null && reference.frames != null && !reference.frames.isEmpty()) {
                candidates.add(new MovementMatcher.Candidate(exercise.getId(), reference.frames));
            }
        }
        if (candidates.isEmpty()) {
            status.setText(R.string.search_no_reference);
            return;
        }
        if (!MovementMatcher.hasMovement(frames)) {
            status.setText(R.string.search_no_movement);
            return;
        }

        List<MovementMatcher.Match> matches = MovementMatcher.rank(candidates, frames);
        status.setText(R.string.search_results);
        for (MovementMatcher.Match match : matches) {
            Exercise exercise = ExerciseRepository.findById(match.getExerciseId());
            if (exercise == null) continue;
            MaterialButton button = (MaterialButton) getLayoutInflater()
                    .inflate(R.layout.item_search_result, results, false);
            button.setText(getString(R.string.search_result, exercise.getName(), match.getScore()));
            button.setOnClickListener(v -> {
                startActivity(ExerciseActivity.newIntent(this, exercise.getId()));
                finish();
            });
            results.addView(button);
        }
    }

    @Override
    public void onResults(@NonNull PoseFrame frame) {
        if (state != State.RECORDING) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastSampleRealtime < SAMPLE_INTERVAL_MILLIS) return;
        float[] normalized = MovementMath.normalize(frame.getPoseLandmarks(),
                frame.getInputImageWidth(), frame.getInputImageHeight());
        if (normalized == null) return;
        lastSampleRealtime = now;
        frames.add(normalized);
    }

    @Override
    public void onEmpty() {
        // Corps hors champ : l'image n'est simplement pas enregistrée.
    }

    @Override
    public void onError(@NonNull String message) {
        if (timer != null) timer.cancel();
        state = State.DONE;
        status.setText(R.string.exercise_camera_error_fallback);
        retry.setVisibility(View.VISIBLE);
    }
}

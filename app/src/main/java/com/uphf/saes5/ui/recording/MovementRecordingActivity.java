package com.uphf.saes5.ui.recording;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.uphf.saes5.R;
import com.uphf.saes5.databinding.ActivityHolisticCameraBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.ui.camera.HolisticCameraController;
import com.uphf.saes5.ui.camera.HolisticLandmarkerHelper;

/**
 * Enregistrement du modèle de référence d'un exercice, ou test de la caméra contre ce modèle.
 *
 * <p>Vue du MVVM : la caméra ({@link HolisticCameraController}) transmet les poses à
 * {@link MovementRecordingViewModel}, qui décide de tout le reste. Une rotation recrée l'écran
 * et rebranche la caméra, sans interrompre la captation en cours.</p>
 */
public class MovementRecordingActivity extends AppCompatActivity
        implements HolisticCameraController.Listener {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_NAME = "name";
    public static final String MODE_RECORD = "record";
    public static final String MODE_COMPARE = "compare";

    /** Le compte à rebours s'affiche à la seconde : un tic toutes les 200 ms suffit. */
    private static final long TICK_MILLIS = 200;
    private static final String DEFAULT_NAME = "mouvement";

    private ActivityHolisticCameraBinding binding;
    private MovementRecordingViewModel viewModel;
    private HolisticCameraController camera;
    private String movementName;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            viewModel.onTick();
            handler.postDelayed(this, TICK_MILLIS);
        }
    };

    private final ActivityResultLauncher<String> requestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    camera.start();
                } else {
                    Toast.makeText(this, R.string.recording_permission_refused,
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    /** Enregistre le modèle associé à un exercice précis. */
    public static Intent recordIntent(Context context, String movementId) {
        return new Intent(context, MovementRecordingActivity.class)
                .putExtra(EXTRA_MODE, MODE_RECORD)
                .putExtra(EXTRA_NAME, movementId);
    }

    /** Compare la caméra au modèle déjà enregistré pour l'exercice. */
    public static Intent compareIntent(Context context, String movementId) {
        return new Intent(context, MovementRecordingActivity.class)
                .putExtra(EXTRA_MODE, MODE_COMPARE)
                .putExtra(EXTRA_NAME, movementId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHolisticCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MovementRecordingViewModel.Mode mode =
                MODE_COMPARE.equals(getIntent().getStringExtra(EXTRA_MODE))
                        ? MovementRecordingViewModel.Mode.COMPARE
                        : MovementRecordingViewModel.Mode.RECORD;
        String name = getIntent().getStringExtra(EXTRA_NAME);
        movementName = name == null || name.trim().isEmpty() ? DEFAULT_NAME : name;
        AppContainer container = AppContainer.from(this);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(
                MovementRecordingViewModel.class,
                () -> new MovementRecordingViewModel(mode, movementName,
                        container.movementRepository, SystemClock::elapsedRealtime)))
                .get(MovementRecordingViewModel.class);

        // Rien à comparer : l'écran se ferme sans ouvrir la caméra ni demander la permission.
        if (viewModel.isMovementMissing()) {
            Toast.makeText(this, getString(R.string.recording_movement_not_found, movementName),
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        binding.btnAction.setOnClickListener(v -> viewModel.start());
        viewModel.getState().observe(this, this::render);

        camera = new HolisticCameraController(this, binding.viewFinder, binding.overlay, this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            camera.start();
        } else {
            requestPermission.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(tick);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null) camera.stop();
    }

    @Override
    public void onCameraReady() {
        viewModel.onCameraReady();
    }

    @Override
    public void onResults(@NonNull HolisticLandmarkerHelper.ResultBundle bundle) {
        viewModel.onPoseLandmarks(bundle.getResult().poseLandmarks(),
                bundle.getInputImageWidth(), bundle.getInputImageHeight());
    }

    @Override
    public void onError(@NonNull String message) {
        viewModel.onAnalysisError(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void render(RecordingUiState state) {
        binding.btnAction.setVisibility(state.isActionVisible() ? View.VISIBLE : View.GONE);
        switch (state.getPhase()) {
            case COUNTDOWN:
                binding.tvStatus.setText(getString(R.string.recording_countdown,
                        state.getSecondsLeft()));
                break;
            case RECORDING:
                binding.tvStatus.setText(getString(R.string.recording_in_progress,
                        state.getSecondsLeft()));
                break;
            case DONE:
                binding.tvStatus.setText(resultText(state));
                break;
            case UNAVAILABLE:
                binding.tvStatus.setText(state.getError());
                break;
            default:
                break;
        }
    }

    private String resultText(RecordingUiState state) {
        switch (state.getResult()) {
            case TOO_FEW_FRAMES:
                return getString(R.string.recording_too_short);
            case SCORE:
                return getString(R.string.recording_score, state.getScore());
            case SAVED:
                return getString(R.string.recording_saved, movementName, state.getFrameCount());
            default:
                return getString(R.string.recording_save_failed);
        }
    }
}

package com.uphf.saes5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.vision.core.RunningMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HolisticCameraActivity extends AppCompatActivity
        implements HolisticLandmarkerHelper.LandmarkerListener {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_NAME = "name";
    public static final String MODE_RECORD = "record";
    public static final String MODE_COMPARE = "compare";

    private static final String TAG = "HolisticCameraActivity";
    private static final int COUNTDOWN_SEC = 10;
    private static final int RECORD_SEC = 20;
    private static final long SAMPLE_INTERVAL_MS = 100;
    private static final int MIN_FRAMES = 20;

    private enum State { WAITING, COUNTDOWN, RECORDING, DONE }

    private PreviewView viewFinder;
    private OverlayView overlay;
    private TextView tvStatus;
    private MaterialButton btnAction;

    private volatile HolisticLandmarkerHelper holisticLandmarkerHelper;
    private ExecutorService backgroundExecutor;
    private ImageAnalysis imageAnalyzer;
    private ProcessCameraProvider cameraProvider;
    private final int cameraFacing = CameraSelector.LENS_FACING_FRONT;

    private String mode;
    private String movementName;
    private Movement reference;

    private State state = State.WAITING;
    private final List<float[]> frames = new ArrayList<>();
    private long lastSampleTime = 0;
    private CountDownTimer countdownTimer;
    private CountDownTimer recordTimer;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holistic_camera);

        viewFinder = findViewById(R.id.view_finder);
        overlay = findViewById(R.id.overlay);
        tvStatus = findViewById(R.id.tv_status);
        btnAction = findViewById(R.id.btn_action);
        backgroundExecutor = Executors.newSingleThreadExecutor();

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = MODE_RECORD;
        movementName = getIntent().getStringExtra(EXTRA_NAME);
        if (movementName == null || movementName.trim().isEmpty()) movementName = "mouvement";

        if (MODE_COMPARE.equals(mode)) {
            reference = MovementStorage.load(this, movementName);
            if (reference == null || reference.frames == null || reference.frames.isEmpty()) {
                Toast.makeText(this, "Mouvement introuvable : " + movementName,
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        btnAction.setOnClickListener(v -> startCountdown());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        backgroundExecutor.execute(() -> {
            holisticLandmarkerHelper = new HolisticLandmarkerHelper(
                    HolisticLandmarkerHelper.DEFAULT_FACE_DETECTION_CONFIDENCE,
                    HolisticLandmarkerHelper.DEFAULT_FACE_PRESENCE_CONFIDENCE,
                    HolisticLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE,
                    HolisticLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE,
                    HolisticLandmarkerHelper.DEFAULT_HAND_LANDMARKS_CONFIDENCE,
                    HolisticLandmarkerHelper.DELEGATE_CPU,
                    RunningMode.LIVE_STREAM,
                    this,
                    this
            );
        });
    }

    // ---------------------------------------------------------------- caméra

    private void startCamera() {
        viewFinder.post(this::setUpCamera);
    }

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(TAG, "Camera provider failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        CameraSelector selector =
                new CameraSelector.Builder().requireLensFacing(cameraFacing).build();

        int rotation = viewFinder.getDisplay().getRotation();

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();
        imageAnalyzer.setAnalyzer(backgroundExecutor, this::detectHolistic);

        cameraProvider.unbindAll();
        try {
            cameraProvider.bindToLifecycle(this, selector, preview, imageAnalyzer);
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
            if (state == State.WAITING) startCountdown();
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void detectHolistic(ImageProxy imageProxy) {
        HolisticLandmarkerHelper helper = holisticLandmarkerHelper;
        if (helper == null || helper.isClose()) {
            imageProxy.close();
            return;
        }
        helper.detectLiveStream(imageProxy, cameraFacing == CameraSelector.LENS_FACING_FRONT);
    }

    // ------------------------------------------- compte à rebours / enregistrement

    private void startCountdown() {
        cancelTimers();
        state = State.COUNTDOWN;
        frames.clear();
        btnAction.setVisibility(View.GONE);

        countdownTimer = new CountDownTimer(COUNTDOWN_SEC * 1000L, 1000) {
            @Override
            public void onTick(long ms) {
                tvStatus.setText("Placez-vous… " + ((ms + 999) / 1000));
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
        lastSampleTime = 0;

        recordTimer = new CountDownTimer(RECORD_SEC * 1000L, 1000) {
            @Override
            public void onTick(long ms) {
                tvStatus.setText("● En cours… " + ((ms + 999) / 1000) + " s");
            }

            @Override
            public void onFinish() {
                finishRecording();
            }
        }.start();
    }

    private void finishRecording() {
        state = State.DONE;
        btnAction.setVisibility(View.VISIBLE);

        if (frames.size() < MIN_FRAMES) {
            tvStatus.setText("Personne détectée trop peu de temps.\nCorps entier visible ? Recommencez.");
            return;
        }

        if (MODE_COMPARE.equals(mode)) {
            int score = MovementMath.score(reference.frames, frames);
            tvStatus.setText("Score : " + score + " / 100");
        } else {
            Movement m = new Movement();
            m.name = movementName;
            m.sampleIntervalMs = (int) SAMPLE_INTERVAL_MS;
            m.durationSec = RECORD_SEC;
            m.landmarkCount = MovementMath.LANDMARKS;
            m.frames = new ArrayList<>(frames);
            try {
                MovementStorage.save(this, m);
                tvStatus.setText("« " + movementName + " » enregistré (" + frames.size() + " images)");
            } catch (IOException e) {
                Log.e(TAG, "Save failed", e);
                tvStatus.setText("Erreur lors de l'enregistrement");
            }
        }
    }

    private void cancelTimers() {
        if (countdownTimer != null) countdownTimer.cancel();
        if (recordTimer != null) recordTimer.cancel();
    }

    // ----------------------------------------------------- résultats MediaPipe

    private void handleFrame(HolisticLandmarkerHelper.ResultBundle b) {
        if (isFinishing()) return;

        overlay.setResults(b.getResult(), b.getInputImageHeight(), b.getInputImageWidth(),
                RunningMode.LIVE_STREAM);
        overlay.invalidate();

        if (state != State.RECORDING) return;

        long now = SystemClock.elapsedRealtime();
        if (now - lastSampleTime < SAMPLE_INTERVAL_MS) return;

        float[] vec = MovementMath.normalize(
                b.getResult().poseLandmarks(), b.getInputImageWidth(), b.getInputImageHeight());
        if (vec == null) return;

        lastSampleTime = now;
        frames.add(vec);
    }

    @Override
    public void onResults(@NonNull HolisticLandmarkerHelper.ResultBundle resultBundle) {
        runOnUiThread(() -> handleFrame(resultBundle));
    }

    @Override
    public void onEmpty() {
        runOnUiThread(() -> {
            if (!isFinishing()) overlay.clear();
        });
    }

    @Override
    public void onError(@NonNull String error, int errorCode) {
        Log.e(TAG, "onError: " + error);
        runOnUiThread(() -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimers();
        if (imageAnalyzer != null) imageAnalyzer.clearAnalyzer();
        backgroundExecutor.execute(() -> {
            HolisticLandmarkerHelper h = holisticLandmarkerHelper;
            if (h != null) h.clearHolisticLandmarker();
        });
        backgroundExecutor.shutdown();
    }
}
package com.uphf.saes5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.vision.core.RunningMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HolisticCameraActivity extends AppCompatActivity
        implements HolisticLandmarkerHelper.LandmarkerListener {

    private static final String TAG = "HolisticCameraActivity";

    private PreviewView viewFinder;
    private OverlayView overlay;

    private HolisticLandmarkerHelper holisticLandmarkerHelper;
    private ExecutorService backgroundExecutor;

    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private final int cameraFacing = CameraSelector.LENS_FACING_FRONT;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
    registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
    if (granted) {
        setUpCamera();
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

        backgroundExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            setUpCamera();
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

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
        ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
        try {
            cameraProvider = cameraProviderFuture.get();
            bindCameraUseCases();
        } catch (Exception e) {
            Log.e(TAG, "Camera provider failed", e);
        }
    }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        CameraSelector cameraSelector =
        new CameraSelector.Builder().requireLensFacing(cameraFacing).build();

        preview = new Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(viewFinder.getDisplay().getRotation())
            .build();

        imageAnalyzer = new ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(viewFinder.getDisplay().getRotation())
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build();

        imageAnalyzer.setAnalyzer(backgroundExecutor, this::detectHolistic);

        cameraProvider.unbindAll();

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            );
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    private void detectHolistic(ImageProxy imageProxy) {
        holisticLandmarkerHelper.detectLiveStream(
            imageProxy,
            cameraFacing == CameraSelector.LENS_FACING_FRONT
        );
    }

    @Override
    public void onResults(@androidx.annotation.NonNull HolisticLandmarkerHelper.ResultBundle resultBundle) {
        runOnUiThread(() -> {
        overlay.setResults(
            resultBundle.getResult(),
            resultBundle.getInputImageHeight(),
            resultBundle.getInputImageWidth(),
            RunningMode.LIVE_STREAM
        );
        overlay.invalidate();
    });
    }

    @Override
    public void onEmpty() {
        runOnUiThread(() -> overlay.clear());
    }

    @Override
    public void onError(@androidx.annotation.NonNull String error, int errorCode) {
        Log.e(TAG, "onError: " + error);
        runOnUiThread(() -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.execute(() -> {
        if (holisticLandmarkerHelper != null) {
            holisticLandmarkerHelper.clearHolisticLandmarker();
        }
    });
        backgroundExecutor.shutdown();
    }
}
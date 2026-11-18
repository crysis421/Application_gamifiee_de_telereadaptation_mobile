package com.uphf.saes5;

import android.annotation.SuppressLint;
import android.util.Log;

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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.vision.core.RunningMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Gère CameraX et MediaPipe pendant une séance, sans logique de score ni de compteur. */
public final class PoseCameraController
        implements HolisticLandmarkerHelper.LandmarkerListener {
    private static final String TAG = "PoseCameraController";

    public interface Listener {
        void onResults(@NonNull HolisticLandmarkerHelper.ResultBundle resultBundle);
        void onEmpty();
        void onError(@NonNull String message);
    }

    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private final OverlayView overlayView;
    private final Listener listener;
    private final int cameraFacing = CameraSelector.LENS_FACING_FRONT;

    private ExecutorService backgroundExecutor;
    private volatile HolisticLandmarkerHelper landmarkerHelper;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private boolean started;

    public PoseCameraController(AppCompatActivity activity, PreviewView previewView,
                                OverlayView overlayView, Listener listener) {
        this.activity = activity;
        this.previewView = previewView;
        this.overlayView = overlayView;
        this.listener = listener;
    }

    public void start() {
        if (started) return;
        started = true;
        backgroundExecutor = Executors.newSingleThreadExecutor();
        backgroundExecutor.execute(() -> landmarkerHelper = new HolisticLandmarkerHelper(
                HolisticLandmarkerHelper.DEFAULT_FACE_DETECTION_CONFIDENCE,
                HolisticLandmarkerHelper.DEFAULT_FACE_PRESENCE_CONFIDENCE,
                HolisticLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE,
                HolisticLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE,
                HolisticLandmarkerHelper.DEFAULT_HAND_LANDMARKS_CONFIDENCE,
                HolisticLandmarkerHelper.DELEGATE_CPU,
                RunningMode.LIVE_STREAM,
                activity,
                this));
        previewView.post(this::setUpCamera);
    }

    private void setUpCamera() {
        if (!started) return;
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(activity);
        future.addListener(() -> {
            if (!started) return;
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Exception error) {
                Log.e(TAG, "Camera provider failed", error);
                onError(error.getMessage() == null ? "Camera unavailable" : error.getMessage(), 0);
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        if (!started || cameraProvider == null || previewView.getDisplay() == null) return;

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();
        int rotation = previewView.getDisplay().getRotation();
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build();
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();
        imageAnalysis.setAnalyzer(backgroundExecutor, this::detectPose);

        cameraProvider.unbindAll();
        try {
            cameraProvider.bindToLifecycle(activity, selector, preview, imageAnalysis);
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception error) {
            Log.e(TAG, "Camera binding failed", error);
            onError(error.getMessage() == null ? "Camera unavailable" : error.getMessage(), 0);
        }
    }

    private void detectPose(ImageProxy imageProxy) {
        HolisticLandmarkerHelper helper = landmarkerHelper;
        if (!started || helper == null || helper.isClose()) {
            imageProxy.close();
            return;
        }
        helper.detectLiveStream(imageProxy, cameraFacing == CameraSelector.LENS_FACING_FRONT);
    }

    public void stop() {
        if (!started && backgroundExecutor == null) return;
        started = false;
        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
            imageAnalysis = null;
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        overlayView.clear();

        ExecutorService executor = backgroundExecutor;
        backgroundExecutor = null;
        if (executor != null && !executor.isShutdown()) {
            executor.execute(() -> {
                HolisticLandmarkerHelper helper = landmarkerHelper;
                landmarkerHelper = null;
                if (helper != null) helper.clearHolisticLandmarker();
            });
            executor.shutdown();
        }
    }

    @Override
    public void onResults(@NonNull HolisticLandmarkerHelper.ResultBundle resultBundle) {
        activity.runOnUiThread(() -> {
            if (!started || activity.isFinishing()) return;
            overlayView.setResults(resultBundle.getResult(), resultBundle.getInputImageHeight(),
                    resultBundle.getInputImageWidth(), RunningMode.LIVE_STREAM);
            listener.onResults(resultBundle);
        });
    }

    @Override
    public void onEmpty() {
        activity.runOnUiThread(() -> {
            if (!started || activity.isFinishing()) return;
            overlayView.clear();
            listener.onEmpty();
        });
    }

    @Override
    public void onError(@NonNull String error, int errorCode) {
        Log.e(TAG, error);
        activity.runOnUiThread(() -> {
            if (started && !activity.isFinishing()) listener.onError(error);
        });
    }
}

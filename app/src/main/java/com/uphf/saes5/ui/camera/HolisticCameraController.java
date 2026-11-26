package com.uphf.saes5.ui.camera;

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

import com.uphf.saes5.ui.widget.OverlayView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CameraX et MediaPipe Holistic (corps, mains, visage) pour l'enregistrement d'un modèle.
 *
 * <p>Ne connaît ni compte à rebours, ni enregistrement : il affiche la caméra et le squelette,
 * et transmet chaque résultat sur le fil principal. Les décisions sont prises par le
 * ViewModel de l'écran.</p>
 */
public final class HolisticCameraController implements HolisticLandmarkerHelper.LandmarkerListener {

    private static final String TAG = "HolisticCameraController";

    /** Ce que l'écran reçoit, toujours sur le fil principal. */
    public interface Listener {
        /** La caméra affiche l'image : la captation peut commencer. */
        void onCameraReady();

        void onResults(@NonNull HolisticLandmarkerHelper.ResultBundle bundle);

        /** L'analyse a échoué : caméra arrêtée. */
        void onError(@NonNull String message);
    }

    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private final OverlayView overlay;
    private final Listener listener;
    private final int cameraFacing = CameraSelector.LENS_FACING_FRONT;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private volatile HolisticLandmarkerHelper helper;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalyzer;
    private boolean unavailable;

    public HolisticCameraController(AppCompatActivity activity, PreviewView previewView,
                                    OverlayView overlay, Listener listener) {
        this.activity = activity;
        this.previewView = previewView;
        this.overlay = overlay;
        this.listener = listener;
    }

    /** Prépare MediaPipe en arrière-plan et branche la caméra. Permission déjà accordée. */
    public void start() {
        backgroundExecutor.execute(() -> helper = new HolisticLandmarkerHelper(
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

    /** Libère la caméra et MediaPipe. */
    public void stop() {
        if (imageAnalyzer != null) imageAnalyzer.clearAnalyzer();
        backgroundExecutor.execute(() -> {
            HolisticLandmarkerHelper current = helper;
            if (current != null) current.clearHolisticLandmarker();
        });
        backgroundExecutor.shutdown();
    }

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(activity);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(TAG, "Camera provider failed", e);
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        if (cameraProvider == null || unavailable || activity.isDestroyed()) return;

        CameraSelector selector =
                new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
        int rotation = previewView.getDisplay().getRotation();

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
        imageAnalyzer.setAnalyzer(backgroundExecutor, this::detect);

        cameraProvider.unbindAll();
        try {
            cameraProvider.bindToLifecycle(activity, selector, preview, imageAnalyzer);
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            listener.onCameraReady();
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void detect(ImageProxy imageProxy) {
        HolisticLandmarkerHelper current = helper;
        if (current == null || current.isClose()) {
            imageProxy.close();
            return;
        }
        current.detectLiveStream(imageProxy, cameraFacing == CameraSelector.LENS_FACING_FRONT);
    }

    @Override
    public void onResults(@NonNull HolisticLandmarkerHelper.ResultBundle bundle) {
        activity.runOnUiThread(() -> {
            if (activity.isFinishing()) return;
            overlay.setResults(bundle.getResult(), bundle.getInputImageHeight(),
                    bundle.getInputImageWidth(), RunningMode.LIVE_STREAM);
            overlay.invalidate();
            listener.onResults(bundle);
        });
    }

    @Override
    public void onEmpty() {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing()) overlay.clear();
        });
    }

    @Override
    public void onError(@NonNull String error, int errorCode) {
        Log.e(TAG, "onError: " + error);
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            unavailable = true;
            if (imageAnalyzer != null) {
                imageAnalyzer.clearAnalyzer();
                imageAnalyzer = null;
            }
            if (cameraProvider != null) cameraProvider.unbindAll();
            listener.onError(error);
        });
    }
}

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

import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.FrameSampling;
import com.uphf.saes5.ui.widget.OverlayView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gère CameraX et MediaPipe pendant une séance, sans logique de score ni de compteur.
 *
 * <p>US-1.4 : le modèle (léger par défaut, ou holistic) et la part d'images analysées viennent
 * de {@link AnalysisSettings}. Une image sautée est rendue à CameraX sans passer par
 * MediaPipe, ce qui réduit réellement le calcul, et pas seulement l'affichage.</p>
 */
public final class PoseCameraController {
    private static final String TAG = "PoseCameraController";

    public interface Listener {
        void onResults(@NonNull PoseFrame frame);
        void onEmpty();
        void onError(@NonNull String message);
    }

    /** Ce qu'il faut d'un détecteur, quel que soit le modèle. */
    private interface Detector {
        void detect(ImageProxy imageProxy, boolean frontCamera);
        boolean isClosed();
        void close();
    }

    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private final OverlayView overlayView;
    private final Listener listener;
    private final int cameraFacing = CameraSelector.LENS_FACING_FRONT;
    private final AnalysisModel model;
    private final int frameStride;

    private ExecutorService backgroundExecutor;
    private volatile Detector detector;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private boolean started;
    private long frameIndex;

    public PoseCameraController(AppCompatActivity activity, PreviewView previewView,
                                OverlayView overlayView, Listener listener) {
        this.activity = activity;
        this.previewView = previewView;
        this.overlayView = overlayView;
        this.listener = listener;
        this.model = AppContainer.from(activity).analysisSettings.getModel();
        this.frameStride = AppContainer.from(activity).analysisSettings.getFrameStride();
    }

    public void start() {
        if (started) return;
        started = true;
        frameIndex = 0;
        backgroundExecutor = Executors.newSingleThreadExecutor();
        backgroundExecutor.execute(() -> {
            try {
                detector = model == AnalysisModel.FULL
                        ? holisticDetector()
                        : liteDetector();
            } catch (LinkageError error) {
                // Last-resort guard if MediaPipe fails before its helper can report the error.
                Log.e(TAG, "MediaPipe native library failed to load", error);
                reportError("Movement analysis unavailable");
            }
        });
        previewView.post(this::setUpCamera);
    }

    private Detector liteDetector() {
        PoseLandmarkerHelper helper = new PoseLandmarkerHelper(activity,
                new PoseLandmarkerHelper.Listener() {
                    @Override public void onResults(@NonNull PoseFrame frame) { deliver(frame); }
                    @Override public void onEmpty() { deliverEmpty(); }
                    @Override public void onError(@NonNull String error) { reportError(error); }
                });
        return new Detector() {
            @Override public void detect(ImageProxy image, boolean front) {
                helper.detectLiveStream(image, front);
            }
            @Override public boolean isClosed() { return helper.isClose(); }
            @Override public void close() { helper.close(); }
        };
    }

    private Detector holisticDetector() {
        HolisticLandmarkerHelper helper = new HolisticLandmarkerHelper(
                HolisticLandmarkerHelper.DEFAULT_FACE_DETECTION_CONFIDENCE,
                HolisticLandmarkerHelper.DEFAULT_FACE_PRESENCE_CONFIDENCE,
                HolisticLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE,
                HolisticLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE,
                HolisticLandmarkerHelper.DEFAULT_HAND_LANDMARKS_CONFIDENCE,
                HolisticLandmarkerHelper.DELEGATE_CPU,
                RunningMode.LIVE_STREAM,
                activity,
                new HolisticLandmarkerHelper.LandmarkerListener() {
                    @Override
                    public void onResults(@NonNull HolisticLandmarkerHelper.ResultBundle bundle) {
                        deliver(new PoseFrame(bundle.getResult().poseLandmarks(),
                                bundle.getInferenceTime(),
                                bundle.getInputImageWidth(),
                                bundle.getInputImageHeight()));
                    }
                    @Override public void onEmpty() { deliverEmpty(); }
                    @Override public void onError(@NonNull String error, int errorCode) {
                        reportError(error);
                    }
                });
        return new Detector() {
            @Override public void detect(ImageProxy image, boolean front) {
                helper.detectLiveStream(image, front);
            }
            @Override public boolean isClosed() { return helper.isClose(); }
            @Override public void close() { helper.clearHolisticLandmarker(); }
        };
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
                reportError(error.getMessage() == null ? "Camera unavailable" : error.getMessage());
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
            reportError(error.getMessage() == null ? "Camera unavailable" : error.getMessage());
        }
    }

    private void detectPose(ImageProxy imageProxy) {
        Detector current = detector;
        boolean analyze = FrameSampling.shouldAnalyze(frameIndex++, frameStride);
        if (!started || current == null || current.isClosed() || !analyze) {
            imageProxy.close();
            return;
        }
        current.detect(imageProxy, cameraFacing == CameraSelector.LENS_FACING_FRONT);
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
                Detector current = detector;
                detector = null;
                if (current != null) current.close();
            });
            executor.shutdown();
        }
    }

    public AnalysisModel getModel() {
        return model;
    }

    private void deliver(@NonNull PoseFrame frame) {
        activity.runOnUiThread(() -> {
            if (!started || activity.isFinishing()) return;
            overlayView.setPose(frame.getPoseLandmarks(), frame.getInputImageHeight(),
                    frame.getInputImageWidth());
            listener.onResults(frame);
        });
    }

    private void deliverEmpty() {
        activity.runOnUiThread(() -> {
            if (!started || activity.isFinishing()) return;
            overlayView.clear();
            listener.onEmpty();
        });
    }

    private void reportError(@NonNull String error) {
        Log.e(TAG, error);
        activity.runOnUiThread(() -> {
            if (started && !activity.isFinishing()) listener.onError(error);
        });
    }
}

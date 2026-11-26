package com.uphf.saes5.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log

import androidx.camera.core.ImageProxy

import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

import com.uphf.saes5.domain.movement.PoseFrame

/**
 * Détection de pose en direct avec le modèle BlazePose « lite » (US-1.4).
 *
 * Même préparation d'image que [HolisticLandmarkerHelper] (rotation, miroir de la caméra
 * frontale), mais sans mains ni visage : seul le corps sert à comparer les mouvements.
 */
class PoseLandmarkerHelper(
    private val context: Context,
    private val listener: Listener,
) {

    interface Listener {
        fun onResults(frame: PoseFrame)
        fun onEmpty()
        fun onError(error: String)
    }

    private var poseLandmarker: PoseLandmarker? = null

    init {
        setup()
    }

    fun isClose(): Boolean = poseLandmarker == null

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    private fun setup() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_ASSET)
                .build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinPoseDetectionConfidence(DEFAULT_CONFIDENCE)
                .setMinPosePresenceConfidence(DEFAULT_CONFIDENCE)
                .setMinTrackingConfidence(DEFAULT_CONFIDENCE)
                .setNumPoses(1)
                .setOutputSegmentationMasks(false)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnResult)
                .setErrorListener { error ->
                    Log.e(TAG, "Erreur d'analyse : ${error.message}", error)
                    listener.onError(error.message ?: "Erreur d'analyse inconnue")
                }
                .build()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Chargement du modèle impossible", e)
            listener.onError("Pose Landmarker failed to initialize")
        } catch (e: LinkageError) {
            Log.e(TAG, "Bibliothèque native MediaPipe absente", e)
            listener.onError("L'analyse des mouvements n'est pas disponible sur cet appareil")
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val frameTime = SystemClock.uptimeMillis()
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, bitmapBuffer.width.toFloat(), bitmapBuffer.height.toFloat())
            }
        }
        val rotated = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
        )
        poseLandmarker?.detectAsync(BitmapImageBuilder(rotated).build(), frameTime)
    }

    private fun returnResult(result: PoseLandmarkerResult, input: MPImage) {
        val latency = SystemClock.uptimeMillis() - result.timestampMs()
        val pose = result.landmarks().firstOrNull()
        if (pose.isNullOrEmpty()) {
            listener.onEmpty()
            return
        }
        listener.onResults(PoseFrame(pose, latency, input.width, input.height))
    }

    companion object {
        private const val TAG = "PoseLandmarkerHelper"
        const val MODEL_ASSET = "pose_landmarker_lite.task"
        private const val DEFAULT_CONFIDENCE = 0.5f
    }
}

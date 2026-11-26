/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uphf.saes5.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

import androidx.core.content.ContextCompat

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlin.math.max
import kotlin.math.min

import com.uphf.saes5.R
import com.uphf.saes5.domain.movement.PostureFeedback

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: HolisticLandmarkerResult? = null
    private var pointPaint = Paint()
    private var poseLinePaint = Paint()
    private var faceLinePaint = Paint()
    private var leftHandLinePaint = Paint()
    private var rightHandLinePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    /** Couleur du squelette selon la posture (US-3.2). Neutre hors séance. */
    private var postureState: PostureFeedback.State = PostureFeedback.State.NEUTRAL

    init {
        initPaints()
    }

    fun setPostureState(state: PostureFeedback.State) {
        if (state == postureState) return
        postureState = state
        applyPostureColor()
        invalidate()
    }

    private fun applyPostureColor() {
        val colorRes = when (postureState) {
            PostureFeedback.State.CORRECT -> R.color.app_success
            PostureFeedback.State.TO_CORRECT -> R.color.app_error
            PostureFeedback.State.NEUTRAL -> R.color.colorPrimary
        }
        val color = ContextCompat.getColor(context!!, colorRes)
        poseLinePaint.color = color
        // Les articulations prennent la même couleur : de loin, c'est la silhouette entière
        // qui doit changer, pas un trait fin.
        pointPaint.color = if (postureState == PostureFeedback.State.NEUTRAL) Color.YELLOW else color
    }

    /** Pose seule, venant du modèle léger (US-1.4) : pas de mains ni de visage. */
    private var poseOnly: List<NormalizedLandmark>? = null

    fun setPose(landmarks: List<NormalizedLandmark>, imageHeight: Int, imageWidth: Int) {
        results = null
        poseOnly = landmarks
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }

    fun clear() {
        results = null
        poseOnly = null
        pointPaint.reset()
        poseLinePaint.reset()
        faceLinePaint.reset()
        leftHandLinePaint.reset()
        rightHandLinePaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL

        poseLinePaint.color = ContextCompat.getColor(context!!, R.color.colorPrimary)
        poseLinePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        poseLinePaint.style = Paint.Style.STROKE

        faceLinePaint.color = Color.CYAN
        faceLinePaint.strokeWidth = LANDMARK_STROKE_WIDTH / 2f
        faceLinePaint.style = Paint.Style.STROKE

        leftHandLinePaint.color = Color.GREEN
        leftHandLinePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        leftHandLinePaint.style = Paint.Style.STROKE

        rightHandLinePaint.color = Color.MAGENTA
        rightHandLinePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        rightHandLinePaint.style = Paint.Style.STROKE

        applyPostureColor()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        poseOnly?.let { drawPose(canvas, it) }
        results?.let { holisticResult ->

            // Calculate scaled image dimensions & offsets to center the image on the canvas
            val scaledImageWidth = imageWidth * scaleFactor
            val scaledImageHeight = imageHeight * scaleFactor
            val offsetX = (width - scaledImageWidth) / 2f
            val offsetY = (height - scaledImageHeight) / 2f

            // 1. Draw Pose Landmarks & Connections
            drawPose(canvas, holisticResult.poseLandmarks())

            // 2. Draw Face Landmarks & Connectors
//            val faceLandmarks = holisticResult.faceLandmarks()
//            if (faceLandmarks.isNotEmpty()) {
//                for (landmark in faceLandmarks) {
//                    canvas.drawPoint(
//                        landmark.x() * imageWidth * scaleFactor + offsetX,
//                        landmark.y() * imageHeight * scaleFactor + offsetY,
//                        pointPaint
//                    )
//                }
//
//                FaceLandmarker.FACE_LANDMARKS_CONNECTORS.filterNotNull().forEach { connector ->
//                    val start = faceLandmarks.getOrNull(connector.start())
//                    val end = faceLandmarks.getOrNull(connector.end())
//                    if (start != null && end != null) {
//                        canvas.drawLine(
//                            start.x() * imageWidth * scaleFactor + offsetX,
//                            start.y() * imageHeight * scaleFactor + offsetY,
//                            end.x() * imageWidth * scaleFactor + offsetX,
//                            end.y() * imageHeight * scaleFactor + offsetY,
//                            faceLinePaint
//                        )
//                    }
//                }
//            }

            // 3. Draw Left Hand Landmarks & Connections
            val leftHandLandmarks = holisticResult.leftHandLandmarks()
            if (leftHandLandmarks.isNotEmpty()) {
                for (landmark in leftHandLandmarks) {
                    canvas.drawPoint(
                        landmark.x() * imageWidth * scaleFactor + offsetX,
                        landmark.y() * imageHeight * scaleFactor + offsetY,
                        pointPaint
                    )
                }

                HandLandmarker.HAND_CONNECTIONS.forEach { connection ->
                    val start = leftHandLandmarks.getOrNull(connection.start())
                    val end = leftHandLandmarks.getOrNull(connection.end())
                    if (start != null && end != null) {
                        canvas.drawLine(
                            start.x() * imageWidth * scaleFactor + offsetX,
                            start.y() * imageHeight * scaleFactor + offsetY,
                            end.x() * imageWidth * scaleFactor + offsetX,
                            end.y() * imageHeight * scaleFactor + offsetY,
                            leftHandLinePaint
                        )
                    }
                }
            }

            // 4. Draw Right Hand Landmarks & Connections
            val rightHandLandmarks = holisticResult.rightHandLandmarks()
            if (rightHandLandmarks.isNotEmpty()) {
                for (landmark in rightHandLandmarks) {
                    canvas.drawPoint(
                        landmark.x() * imageWidth * scaleFactor + offsetX,
                        landmark.y() * imageHeight * scaleFactor + offsetY,
                        pointPaint
                    )
                }

                HandLandmarker.HAND_CONNECTIONS.forEach { connection ->
                    val start = rightHandLandmarks.getOrNull(connection.start())
                    val end = rightHandLandmarks.getOrNull(connection.end())
                    if (start != null && end != null) {
                        canvas.drawLine(
                            start.x() * imageWidth * scaleFactor + offsetX,
                            start.y() * imageHeight * scaleFactor + offsetY,
                            end.x() * imageWidth * scaleFactor + offsetX,
                            end.y() * imageHeight * scaleFactor + offsetY,
                            rightHandLinePaint
                        )
                    }
                }
            }
        }
    }

    private fun drawPose(canvas: Canvas, poseLandmarks: List<NormalizedLandmark>) {
        if (poseLandmarks.isEmpty()) return
        val offsetX = (width - imageWidth * scaleFactor) / 2f
        val offsetY = (height - imageHeight * scaleFactor) / 2f
        for (landmark in poseLandmarks) {
            canvas.drawPoint(
                landmark.x() * imageWidth * scaleFactor + offsetX,
                landmark.y() * imageHeight * scaleFactor + offsetY,
                pointPaint
            )
        }
        PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
            val start = poseLandmarks.getOrNull(connection.start())
            val end = poseLandmarks.getOrNull(connection.end())
            if (start != null && end != null) {
                canvas.drawLine(
                    start.x() * imageWidth * scaleFactor + offsetX,
                    start.y() * imageHeight * scaleFactor + offsetY,
                    end.x() * imageWidth * scaleFactor + offsetX,
                    end.y() * imageHeight * scaleFactor + offsetY,
                    poseLinePaint
                )
            }
        }
    }

    fun setResults(
        holisticLandmarkerResults: HolisticLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = holisticLandmarkerResults
        poseOnly = null

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 19F
    }
}

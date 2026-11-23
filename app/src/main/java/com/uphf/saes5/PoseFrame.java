package com.uphf.saes5;

import androidx.annotation.NonNull;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.Collections;
import java.util.List;

/**
 * Résultat d'analyse d'une image pendant la séance, quel que soit le modèle utilisé (US-1.4).
 *
 * <p>Les modèles « lite » et « holistic » renvoient tous deux les 33 points BlazePose du
 * corps : c'est tout ce dont la séance a besoin, et les mouvements de référence déjà
 * enregistrés restent comparables.</p>
 */
public final class PoseFrame {
    private final List<NormalizedLandmark> poseLandmarks;
    private final long latencyMillis;
    private final int inputImageWidth;
    private final int inputImageHeight;

    public PoseFrame(List<NormalizedLandmark> poseLandmarks, long latencyMillis,
                     int inputImageWidth, int inputImageHeight) {
        this.poseLandmarks = poseLandmarks == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(poseLandmarks);
        this.latencyMillis = latencyMillis;
        this.inputImageWidth = inputImageWidth;
        this.inputImageHeight = inputImageHeight;
    }

    @NonNull
    public List<NormalizedLandmark> getPoseLandmarks() { return poseLandmarks; }
    /** Temps entre la capture de l'image et l'arrivée du résultat. */
    public long getLatencyMillis() { return latencyMillis; }
    public int getInputImageWidth() { return inputImageWidth; }
    public int getInputImageHeight() { return inputImageHeight; }
}

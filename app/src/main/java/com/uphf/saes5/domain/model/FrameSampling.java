package com.uphf.saes5.domain.model;

/**
 * Part des images de la caméra réellement analysées (US-1.4) : une sur {@code stride}.
 *
 * <p>Analyser moins d'images réduit le calcul sur les téléphones lents, au prix d'un suivi
 * moins fin.</p>
 */
public final class FrameSampling {

    /** Au plus une image analysée sur trois : au-delà, le mouvement n'est plus suivi. */
    public static final int MAX_FRAME_STRIDE = 3;

    private FrameSampling() {}

    public static int clampStride(int stride) {
        return Math.max(1, Math.min(MAX_FRAME_STRIDE, stride));
    }

    /** Vrai si l'image {@code frameIndex} (comptée depuis 0) doit être analysée. */
    public static boolean shouldAnalyze(long frameIndex, int stride) {
        return frameIndex % clampStride(stride) == 0;
    }
}

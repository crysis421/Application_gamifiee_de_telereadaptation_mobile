package com.uphf.saes5;

import java.util.ArrayList;
import java.util.List;

/**
 * Lecture en boucle d'un modèle de référence, pour l'animer à l'écran.
 *
 * <p>Les images du modèle sont espacées de {@code intervalMs} (100 ms à l'enregistrement) :
 * affichées telles quelles, elles sauteraient à dix images par seconde. {@link #frameAt}
 * interpole donc entre deux images voisines. Au bout du modèle, la lecture reprend au début
 * sans interpoler la dernière image vers la première : le modèle se termine par la pose de
 * départ, la boucle est donc continue.</p>
 *
 * <p>Le cadrage ({@link #minX} … {@link #maxY}) englobe toutes les images : le mannequin
 * garde la même taille et la même place pendant tout le mouvement.</p>
 *
 * <p>Sans dépendance Android, pour être testée sur la JVM.</p>
 */
final class ModelAnimation {

    /** Écart d'enregistrement par défaut, pour un modèle qui ne le précise pas. */
    static final int DEFAULT_INTERVAL_MS = 100;

    /** Points dessinés : tête (nez, oreilles), bras, tronc, jambes et pieds. */
    static final int[] DRAWN_POINTS = {
            0, 7, 8, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
    };

    private final List<float[]> frames = new ArrayList<>();
    private final long intervalMs;

    final float minX;
    final float minY;
    final float maxX;
    final float maxY;

    /**
     * @throws IllegalArgumentException si aucune image n'est exploitable
     */
    ModelAnimation(List<float[]> modelFrames, int intervalMs) {
        if (modelFrames != null) {
            for (float[] frame : modelFrames) {
                if (isDrawable(frame)) frames.add(frame);
            }
        }
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("Modèle sans image exploitable");
        }
        this.intervalMs = intervalMs > 0 ? intervalMs : DEFAULT_INTERVAL_MS;

        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE;
        float x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (float[] frame : frames) {
            for (int point : DRAWN_POINTS) {
                x0 = Math.min(x0, frame[2 * point]);
                x1 = Math.max(x1, frame[2 * point]);
                y0 = Math.min(y0, frame[2 * point + 1]);
                y1 = Math.max(y1, frame[2 * point + 1]);
            }
        }
        minX = x0;
        minY = y0;
        maxX = x1;
        maxY = y1;
    }

    /** Durée d'une boucle, en millisecondes. */
    long durationMs() {
        return frames.size() * intervalMs;
    }

    /** La pose à {@code elapsedMs} du début de la lecture, interpolée entre deux images. */
    float[] frameAt(long elapsedMs) {
        if (frames.size() == 1) return frames.get(0);
        long inLoop = Math.floorMod(elapsedMs, durationMs());
        int index = (int) (inLoop / intervalMs);
        float fraction = (inLoop % intervalMs) / (float) intervalMs;
        float[] from = frames.get(index);
        float[] to = frames.get(Math.min(index + 1, frames.size() - 1));
        float[] pose = new float[from.length];
        for (int i = 0; i < pose.length; i++) {
            pose[i] = from[i] + (to[i] - from[i]) * fraction;
        }
        return pose;
    }

    private static boolean isDrawable(float[] frame) {
        if (frame == null || frame.length < MovementMath.LANDMARKS * 2) return false;
        for (int point : DRAWN_POINTS) {
            if (!Float.isFinite(frame[2 * point]) || !Float.isFinite(frame[2 * point + 1])) {
                return false;
            }
        }
        return true;
    }
}

package com.uphf.saes5;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.Arrays;
import java.util.List;

public final class MovementMath {

    public static final int LANDMARKS = 33;
    // Distance moyenne (en longueurs de torse) à partir de laquelle le score tombe à 0.
    // À ajuster en testant : plus la valeur est grande, plus le score est indulgent.
    private static final float MAX_DISTANCE = 0.6f;

    private MovementMath() {}

    /** Renvoie [x0,y0,x1,y1,...] normalisé, ou null si la pose est inutilisable. */
    public static float[] normalize(List<NormalizedLandmark> pose, int imgW, int imgH) {
        if (pose == null || pose.size() < LANDMARKS) return null;

        // 11/12 = épaules, 23/24 = hanches
        float hipX = (pose.get(23).x() + pose.get(24).x()) / 2f * imgW;
        float hipY = (pose.get(23).y() + pose.get(24).y()) / 2f * imgH;
        float shX = (pose.get(11).x() + pose.get(12).x()) / 2f * imgW;
        float shY = (pose.get(11).y() + pose.get(12).y()) / 2f * imgH;

        float torso = (float) Math.hypot(shX - hipX, shY - hipY);
        if (torso < 1e-3f) return null;

        float[] out = new float[LANDMARKS * 2];
        for (int i = 0; i < LANDMARKS; i++) {
            out[2 * i] = round4((pose.get(i).x() * imgW - hipX) / torso);
            out[2 * i + 1] = round4((pose.get(i).y() * imgH - hipY) / torso);
        }
        return out;
    }

    private static float round4(float v) {
        return Math.round(v * 10000f) / 10000f;
    }

    private static float frameDistance(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < LANDMARKS; i++) {
            float dx = a[2 * i] - b[2 * i];
            float dy = a[2 * i + 1] - b[2 * i + 1];
            sum += (float) Math.sqrt(dx * dx + dy * dy);
        }
        return sum / LANDMARKS;
    }

    /** Distance DTW moyenne par pas du chemin d'alignement. */
    private static float averageDtwDistance(List<float[]> ref, List<float[]> att) {
        int n = ref.size();
        int m = att.size();
        float[][] cost = new float[n + 1][m + 1];
        int[][] len = new int[n + 1][m + 1];
        for (float[] row : cost) Arrays.fill(row, Float.POSITIVE_INFINITY);
        cost[0][0] = 0f;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                float d = frameDistance(ref.get(i - 1), att.get(j - 1));
                float best = cost[i - 1][j - 1];
                int bestLen = len[i - 1][j - 1];
                if (cost[i - 1][j] < best) {
                    best = cost[i - 1][j];
                    bestLen = len[i - 1][j];
                }
                if (cost[i][j - 1] < best) {
                    best = cost[i][j - 1];
                    bestLen = len[i][j - 1];
                }
                cost[i][j] = best + d;
                len[i][j] = bestLen + 1;
            }
        }
        return cost[n][m] / len[n][m];
    }

    /** Score de 0 à 100. */
    public static int score(List<float[]> reference, List<float[]> attempt) {
        if (reference == null || reference.isEmpty() || attempt == null || attempt.isEmpty()) {
            return 0;
        }
        float avg = averageDtwDistance(reference, attempt);
        float s = 100f * (1f - avg / MAX_DISTANCE);
        return Math.round(Math.max(0f, Math.min(100f, s)));
    }
}
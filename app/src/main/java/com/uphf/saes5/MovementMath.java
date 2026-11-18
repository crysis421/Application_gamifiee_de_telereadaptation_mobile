package com.uphf.saes5;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MovementMath {

    public static final int LANDMARKS = 33;
    public static final int JOINT_ANGLES = 8;
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

    /**
     * Extrait les angles gauche/droite des coudes, épaules, hanches et genoux.
     * Les angles rendent la comparaison indépendante de la position, de l'échelle
     * et d'un miroir horizontal de la caméra.
     */
    public static float[] angleFeatures(float[] normalizedPose) {
        if (normalizedPose == null || normalizedPose.length < LANDMARKS * 2) return null;
        for (int i = 0; i < LANDMARKS * 2; i++) {
            if (!Float.isFinite(normalizedPose[i])) return null;
        }

        int[][] joints = {
                {11, 13, 15}, {12, 14, 16}, // coudes
                {13, 11, 23}, {14, 12, 24}, // épaules
                {11, 23, 25}, {12, 24, 26}, // hanches
                {23, 25, 27}, {24, 26, 28}  // genoux
        };
        float[] features = new float[JOINT_ANGLES];
        for (int i = 0; i < joints.length; i++) {
            float angle = jointAngle(normalizedPose, joints[i][0], joints[i][1], joints[i][2]);
            if (!Float.isFinite(angle)) return null;
            features[i] = angle;
        }
        return features;
    }

    private static float jointAngle(float[] pose, int first, int vertex, int third) {
        float ax = pose[first * 2] - pose[vertex * 2];
        float ay = pose[first * 2 + 1] - pose[vertex * 2 + 1];
        float bx = pose[third * 2] - pose[vertex * 2];
        float by = pose[third * 2 + 1] - pose[vertex * 2 + 1];
        double lengthProduct = Math.hypot(ax, ay) * Math.hypot(bx, by);
        if (lengthProduct < 1e-6) return Float.NaN;
        double cosine = (ax * bx + ay * by) / lengthProduct;
        cosine = Math.max(-1d, Math.min(1d, cosine));
        return (float) Math.toDegrees(Math.acos(cosine));
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

    /**
     * Compare deux séquences par DTW sur leurs huit angles articulaires.
     * Les images incomplètes sont ignorées et le score reste compris entre 0 et 100.
     */
    public static int angleScore(List<float[]> reference, List<float[]> attempt) {
        List<float[]> referenceAngles = usableAngleFrames(reference);
        List<float[]> attemptAngles = usableAngleFrames(attempt);
        if (referenceAngles.isEmpty() || attemptAngles.isEmpty()) return 0;

        float averageDifference = averageAngleDtwDifference(referenceAngles, attemptAngles);
        float score = 100f * (1f - averageDifference / 180f);
        return Math.round(Math.max(0f, Math.min(100f, score)));
    }

    private static List<float[]> usableAngleFrames(List<float[]> frames) {
        List<float[]> usable = new ArrayList<>();
        if (frames == null) return usable;
        for (float[] frame : frames) {
            float[] angles = angleFeatures(frame);
            if (angles != null) usable.add(angles);
        }
        return usable;
    }

    private static float angleFrameDifference(float[] first, float[] second) {
        float sum = 0f;
        for (int i = 0; i < JOINT_ANGLES; i++) {
            sum += Math.abs(first[i] - second[i]);
        }
        return sum / JOINT_ANGLES;
    }

    private static float averageAngleDtwDifference(List<float[]> reference,
                                                   List<float[]> attempt) {
        int n = reference.size();
        int m = attempt.size();
        float[][] cost = new float[n + 1][m + 1];
        int[][] pathLength = new int[n + 1][m + 1];
        for (float[] row : cost) Arrays.fill(row, Float.POSITIVE_INFINITY);
        cost[0][0] = 0f;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                float best = cost[i - 1][j - 1];
                int bestLength = pathLength[i - 1][j - 1];
                if (cost[i - 1][j] < best) {
                    best = cost[i - 1][j];
                    bestLength = pathLength[i - 1][j];
                }
                if (cost[i][j - 1] < best) {
                    best = cost[i][j - 1];
                    bestLength = pathLength[i][j - 1];
                }
                cost[i][j] = best + angleFrameDifference(
                        reference.get(i - 1), attempt.get(j - 1));
                pathLength[i][j] = bestLength + 1;
            }
        }
        return cost[n][m] / pathLength[n][m];
    }
}

package com.uphf.saes5;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumule un nombre borné d'images et compare chaque séquence complète au modèle.
 * Cette classe ne dépend pas d'Android afin que son comportement temps réel reste testable.
 */
public final class MovementComparisonEngine {
    private static final int REARM_DEPARTURE_SCORE = 95;

    private final List<float[]> referenceAngles = new ArrayList<>();
    private final List<float[]> attemptAngles = new ArrayList<>();
    private final int threshold;
    private boolean armed = true;

    public MovementComparisonEngine(List<float[]> referenceFrames, int threshold) {
        if (referenceFrames != null) {
            for (float[] frame : referenceFrames) {
                float[] angles = MovementMath.angleFeatures(frame);
                if (angles != null) this.referenceAngles.add(angles);
            }
        }
        this.threshold = Math.max(1, Math.min(100, threshold));
    }

    public MovementComparisonResult addFrame(float[] frame) {
        float[] angles = MovementMath.angleFeatures(frame);
        if (referenceAngles.isEmpty() || angles == null) {
            return new MovementComparisonResult(0, 0, false, false, false);
        }

        int liveScore = bestReferencePostureScore(angles);
        if (!armed) {
            float[] endPose = referenceAngles.get(referenceAngles.size() - 1);
            int endPoseScore = MovementMath.angleFeatureScore(endPose, angles);
            if (endPoseScore >= REARM_DEPARTURE_SCORE) {
                return new MovementComparisonResult(liveScore, 0, false, false, false);
            }
            armed = true;
            attemptAngles.clear();
        }

        attemptAngles.add(angles);
        while (attemptAngles.size() > referenceAngles.size()) {
            attemptAngles.remove(0);
        }

        if (attemptAngles.size() < referenceAngles.size()) {
            return new MovementComparisonResult(liveScore, 0, false, false, false);
        }

        int movementScore = MovementMath.angleFeatureSequenceScore(
                referenceAngles, attemptAngles);
        boolean valid = movementScore >= threshold;
        if (valid) {
            armed = false;
            attemptAngles.clear();
        }
        return new MovementComparisonResult(liveScore, movementScore, true, valid, valid);
    }

    public void reset() {
        attemptAngles.clear();
        armed = true;
    }

    private int bestReferencePostureScore(float[] angles) {
        int best = 0;
        for (float[] referenceFrameAngles : referenceAngles) {
            best = Math.max(best,
                    MovementMath.angleFeatureScore(referenceFrameAngles, angles));
        }
        return best;
    }
}

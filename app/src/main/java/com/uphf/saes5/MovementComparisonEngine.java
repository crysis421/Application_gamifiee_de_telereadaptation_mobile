package com.uphf.saes5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumule un nombre borné d'images et compare chaque séquence complète au modèle.
 * Cette classe ne dépend pas d'Android afin que son comportement temps réel reste testable.
 */
public final class MovementComparisonEngine {
    private final List<float[]> referenceFrames = new ArrayList<>();
    private final List<float[]> attemptFrames = new ArrayList<>();
    private final int threshold;

    public MovementComparisonEngine(List<float[]> referenceFrames, int threshold) {
        if (referenceFrames != null) {
            for (float[] frame : referenceFrames) {
                if (MovementMath.angleFeatures(frame) != null) {
                    this.referenceFrames.add(frame.clone());
                }
            }
        }
        this.threshold = Math.max(1, Math.min(100, threshold));
    }

    public MovementComparisonResult addFrame(float[] frame) {
        if (referenceFrames.isEmpty() || MovementMath.angleFeatures(frame) == null) {
            return new MovementComparisonResult(0, 0, false, false, false);
        }

        int liveScore = bestReferencePostureScore(frame);
        attemptFrames.add(frame.clone());
        while (attemptFrames.size() > referenceFrames.size()) {
            attemptFrames.remove(0);
        }

        if (attemptFrames.size() < referenceFrames.size()) {
            return new MovementComparisonResult(liveScore, 0, false, false, false);
        }

        int movementScore = MovementMath.angleScore(referenceFrames, attemptFrames);
        boolean valid = movementScore >= threshold;
        if (valid) {
            // La prochaine image commence une nouvelle répétition et ne peut donc pas
            // réémettre immédiatement la validation de celle qui vient de se terminer.
            attemptFrames.clear();
        }
        return new MovementComparisonResult(liveScore, movementScore, true, valid, valid);
    }

    public void reset() {
        attemptFrames.clear();
    }

    private int bestReferencePostureScore(float[] frame) {
        int best = 0;
        List<float[]> attempt = Collections.singletonList(frame);
        for (float[] referenceFrame : referenceFrames) {
            int score = MovementMath.angleScore(
                    Collections.singletonList(referenceFrame), attempt);
            best = Math.max(best, score);
        }
        return best;
    }
}

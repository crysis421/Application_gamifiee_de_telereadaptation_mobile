package com.uphf.saes5;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumule un nombre borné d'images et compare chaque séquence complète au modèle.
 * Cette classe ne dépend pas d'Android afin que son comportement temps réel reste testable.
 *
 * <p>Une répétition n'est comptée que si la séquence ressemble au modèle <em>et</em> qu'elle
 * en parcourt les trois phases (US-1.3) : elle part de la pose de départ, atteint la pose
 * cible, puis revient à la pose finale. Ces phases sont jugées sur les seules articulations
 * que le modèle fait bouger : sur un squat, les coudes immobiles ne doivent pas suffire à
 * valider quelqu'un qui reste debout.</p>
 */
public final class MovementComparisonEngine {
    private static final int REARM_DEPARTURE_SCORE = 95;
    /** Amplitude, en degrés, à partir de laquelle une articulation fait partie du mouvement. */
    static final float MOVING_JOINT_MIN_RANGE = 20f;

    private final List<float[]> referenceAngles = new ArrayList<>();
    private final List<float[]> attemptAngles = new ArrayList<>();
    private final int threshold;
    /** Articulations qui bougent dans le modèle ; aucune pour un exercice statique (planche). */
    private final boolean[] movingJoints = new boolean[MovementMath.JOINT_ANGLES];
    private final boolean hasMovingJoints;
    private final float[] targetAngles;
    private float[] lastAngles;
    private boolean armed = true;

    public MovementComparisonEngine(List<float[]> referenceFrames, int threshold) {
        if (referenceFrames != null) {
            for (float[] frame : referenceFrames) {
                float[] angles = MovementMath.angleFeatures(frame);
                if (angles != null) this.referenceAngles.add(angles);
            }
        }
        this.threshold = Math.max(1, Math.min(100, threshold));
        this.hasMovingJoints = detectMovingJoints();
        this.targetAngles = findTargetPose();
    }

    public MovementComparisonResult addFrame(float[] frame) {
        float[] angles = MovementMath.angleFeatures(frame);
        if (referenceAngles.isEmpty() || angles == null) {
            return new MovementComparisonResult(0, 0, false, false, false);
        }

        int liveScore = bestReferencePostureScore(angles);
        float[] previous = lastAngles;
        lastAngles = angles;
        if (!armed) {
            if (MovementMath.angleFeatureScore(endPose(), angles) >= REARM_DEPARTURE_SCORE) {
                return new MovementComparisonResult(liveScore, 0, false, false, false);
            }
            armed = true;
            attemptAngles.clear();
            // Répétitions enchaînées : la pose de retour de la précédente sert de départ.
            if (previous != null && matchesPhase(startPose(), previous)) {
                attemptAngles.add(previous);
            }
        }

        attemptAngles.add(angles);
        while (attemptAngles.size() > referenceAngles.size()) {
            attemptAngles.remove(0);
        }

        if (attemptAngles.size() < referenceAngles.size() || !hasLeftStartPose()) {
            return new MovementComparisonResult(liveScore, 0, false, false, false);
        }

        int movementScore = MovementMath.angleFeatureSequenceScore(
                referenceAngles, attemptAngles);
        boolean valid = movementScore >= threshold && coversAllPhases();
        if (valid) {
            armed = false;
            attemptAngles.clear();
        }
        return new MovementComparisonResult(liveScore, movementScore, true, valid, valid);
    }

    public void reset() {
        attemptAngles.clear();
        lastAngles = null;
        armed = true;
    }

    private float[] startPose() {
        return referenceAngles.get(0);
    }

    private float[] endPose() {
        return referenceAngles.get(referenceAngles.size() - 1);
    }

    /** Départ, cible puis retour, dans cet ordre, sur la fenêtre courante. */
    private boolean coversAllPhases() {
        if (!hasMovingJoints) return true;
        int last = attemptAngles.size() - 1;
        if (!matchesPhase(startPose(), attemptAngles.get(0))) return false;
        if (!matchesPhase(endPose(), attemptAngles.get(last))) return false;
        for (float[] angles : attemptAngles) {
            if (matchesPhase(targetAngles, angles)) return true;
        }
        return false;
    }

    /**
     * Tant que l'utilisateur n'a pas quitté la pose de départ, il n'y a rien à juger : on
     * reste en analyse plutôt que d'afficher un mouvement « à corriger » à quelqu'un au repos.
     */
    private boolean hasLeftStartPose() {
        if (!hasMovingJoints) return true;
        for (float[] angles : attemptAngles) {
            if (!matchesPhase(startPose(), angles)) return true;
        }
        return false;
    }

    private boolean matchesPhase(float[] phase, float[] angles) {
        return MovementMath.angleFeatureScore(phase, angles, movingJoints) >= threshold;
    }

    private boolean detectMovingJoints() {
        if (referenceAngles.isEmpty()) return false;
        boolean any = false;
        for (int joint = 0; joint < MovementMath.JOINT_ANGLES; joint++) {
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;
            for (float[] angles : referenceAngles) {
                min = Math.min(min, angles[joint]);
                max = Math.max(max, angles[joint]);
            }
            movingJoints[joint] = max - min >= MOVING_JOINT_MIN_RANGE;
            any |= movingJoints[joint];
        }
        return any;
    }

    /** La pose du modèle la plus éloignée de la pose de départ : le point haut du mouvement. */
    private float[] findTargetPose() {
        if (referenceAngles.isEmpty()) return null;
        float[] target = startPose();
        int lowestScore = Integer.MAX_VALUE;
        for (float[] angles : referenceAngles) {
            int score = MovementMath.angleFeatureScore(startPose(), angles, movingJoints);
            if (score < lowestScore) {
                lowestScore = score;
                target = angles;
            }
        }
        return target;
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

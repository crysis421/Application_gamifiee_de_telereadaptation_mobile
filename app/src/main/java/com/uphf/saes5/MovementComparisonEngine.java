package com.uphf.saes5;

import java.util.ArrayList;
import java.util.List;

/**
 * Compare en continu la pose de l'utilisateur au modèle et découpe le flux en tentatives.
 * Cette classe ne dépend pas d'Android afin que son comportement temps réel reste testable.
 *
 * <p>Une répétition n'est comptée que si la tentative ressemble au modèle <em>et</em> qu'elle
 * en parcourt les trois phases (US-1.3) : elle part de la pose de départ, atteint la pose
 * cible, puis revient à la pose finale. Ces phases sont jugées sur les seules articulations
 * que le modèle fait bouger : sur un squat, les coudes immobiles ne doivent pas suffire à
 * valider quelqu'un qui reste debout.</p>
 *
 * <p>Une tentative commence quand l'utilisateur quitte la pose de départ et se termine quand
 * il rejoint la pose finale. Elle peut durer jusqu'à {@link #MAX_SLOWDOWN} fois le modèle :
 * en rééducation, on va souvent plus lentement que la personne qui l'a enregistré. Une
 * tentative terminée sans validation est signalée comme ratée (US-4.4).</p>
 *
 * <p>Un modèle sans articulation mobile (planche, gainage) n'a pas de phases : il est jugé
 * sur une fenêtre glissante de la longueur du modèle, et ne produit pas d'échec.</p>
 */
public final class MovementComparisonEngine {
    private static final int REARM_DEPARTURE_SCORE = 95;
    /** Amplitude, en degrés, à partir de laquelle une articulation fait partie du mouvement. */
    static final float MOVING_JOINT_MIN_RANGE = 20f;
    /** Une tentative peut durer jusqu'à ce multiple de la durée du modèle. */
    static final int MAX_SLOWDOWN = 3;

    private final List<float[]> referenceAngles = new ArrayList<>();
    private final List<float[]> attemptAngles = new ArrayList<>();
    private final int threshold;
    /** Articulations qui bougent dans le modèle ; aucune pour un exercice statique (planche). */
    private final boolean[] movingJoints = new boolean[MovementMath.JOINT_ANGLES];
    private final boolean hasMovingJoints;
    private final float[] targetAngles;
    private float[] lastAngles;
    private boolean armed = true;
    /** L'utilisateur a quitté la pose de départ : une tentative est en cours. */
    private boolean departed;

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
                return notReady(liveScore);
            }
            armed = true;
            departed = false;
            attemptAngles.clear();
            // Répétitions enchaînées : la pose de retour de la précédente sert de départ.
            if (previous != null && matchesPhase(startPose(), previous)) {
                attemptAngles.add(previous);
            }
        }

        return hasMovingJoints
                ? addPhasedFrame(angles, liveScore)
                : addStaticFrame(angles, liveScore);
    }

    /** Mouvement à phases : une tentative va de la pose de départ au retour en pose finale. */
    private MovementComparisonResult addPhasedFrame(float[] angles, int liveScore) {
        boolean atStart = matchesPhase(startPose(), angles);
        if (!departed) {
            if (atStart) {
                // Toujours au départ : seule la dernière image compte comme point d'ancrage.
                restartAttemptFrom(angles);
                return notReady(liveScore);
            }
            departed = true;
        }

        attemptAngles.add(angles);
        int maxFrames = referenceAngles.size() * MAX_SLOWDOWN;
        while (attemptAngles.size() > maxFrames) {
            attemptAngles.remove(0);
        }
        if (attemptAngles.size() < 2) {
            return notReady(liveScore);
        }

        int movementScore = MovementMath.angleFeatureSequenceScore(
                referenceAngles, attemptAngles);
        if (movementScore >= threshold && coversAllPhases()) {
            armed = false;
            departed = false;
            attemptAngles.clear();
            return new MovementComparisonResult(liveScore, movementScore, true, true, true);
        }

        // Revenu en pose finale sans validation : la tentative est terminée, et ratée.
        boolean failed = matchesPhase(endPose(), angles);
        if (failed || atStart) {
            restartAttemptFrom(angles);
        }
        return new MovementComparisonResult(liveScore, movementScore, true, false, false, failed);
    }

    /** Modèle statique : fenêtre glissante de la longueur du modèle, sans notion d'échec. */
    private MovementComparisonResult addStaticFrame(float[] angles, int liveScore) {
        attemptAngles.add(angles);
        while (attemptAngles.size() > referenceAngles.size()) {
            attemptAngles.remove(0);
        }
        if (attemptAngles.size() < referenceAngles.size()) {
            return notReady(liveScore);
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
        lastAngles = null;
        armed = true;
        departed = false;
    }

    private void restartAttemptFrom(float[] angles) {
        departed = false;
        attemptAngles.clear();
        attemptAngles.add(angles);
    }

    private static MovementComparisonResult notReady(int liveScore) {
        return new MovementComparisonResult(liveScore, 0, false, false, false);
    }

    private float[] startPose() {
        return referenceAngles.get(0);
    }

    private float[] endPose() {
        return referenceAngles.get(referenceAngles.size() - 1);
    }

    /** Départ, cible puis retour, dans cet ordre, sur la tentative courante. */
    private boolean coversAllPhases() {
        int last = attemptAngles.size() - 1;
        if (!matchesPhase(startPose(), attemptAngles.get(0))) return false;
        if (!matchesPhase(endPose(), attemptAngles.get(last))) return false;
        for (float[] angles : attemptAngles) {
            if (matchesPhase(targetAngles, angles)) return true;
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

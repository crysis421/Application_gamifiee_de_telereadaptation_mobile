package com.uphf.saes5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Retrouve les exercices du catalogue les plus proches d'un mouvement capté (US-6.3).
 *
 * <p>Chaque exercice est jugé sur <em>ses</em> articulations mobiles : un squat sur les
 * hanches et les genoux, un exercice de bras sur les coudes. Comparer les huit angles à
 * chaque fois noierait la différence sous les articulations immobiles, et tous les exercices
 * obtiendraient des scores voisins.</p>
 *
 * <p>Sans dépendance Android, pour rester testable.</p>
 */
public final class MovementMatcher {

    public static final int MAX_SUGGESTIONS = 3;

    private MovementMatcher() {}

    /** Un exercice et son mouvement de référence (images normalisées). */
    public static final class Candidate {
        private final String exerciseId;
        private final List<float[]> referenceFrames;

        public Candidate(String exerciseId, List<float[]> referenceFrames) {
            this.exerciseId = exerciseId;
            this.referenceFrames = referenceFrames;
        }
    }

    public static final class Match {
        private final String exerciseId;
        private final int score;

        Match(String exerciseId, int score) {
            this.exerciseId = exerciseId;
            this.score = score;
        }

        public String getExerciseId() { return exerciseId; }
        public int getScore() { return score; }
    }

    /** Vrai si le mouvement fait réellement bouger au moins une articulation. */
    public static boolean hasMovement(List<float[]> attemptFrames) {
        for (boolean moving : MovementMath.movingJoints(MovementMath.angleFrames(attemptFrames),
                MovementComparisonEngine.MOVING_JOINT_MIN_RANGE)) {
            if (moving) return true;
        }
        return false;
    }

    /**
     * Les exercices les plus proches, du plus ressemblant au moins ressemblant, au plus
     * {@link #MAX_SUGGESTIONS}. Vide si l'utilisateur n'a pas bougé : on ne propose pas un
     * exercice au hasard.
     */
    public static List<Match> rank(List<Candidate> candidates, List<float[]> attemptFrames) {
        List<Match> matches = new ArrayList<>();
        if (!hasMovement(attemptFrames)) return matches;

        List<float[]> attempt = MovementMath.angleFrames(attemptFrames);
        for (Candidate candidate : candidates) {
            List<float[]> reference = MovementMath.angleFrames(candidate.referenceFrames);
            if (reference.isEmpty()) continue;
            boolean[] joints = MovementMath.movingJoints(
                    reference, MovementComparisonEngine.MOVING_JOINT_MIN_RANGE);
            matches.add(new Match(candidate.exerciseId,
                    MovementMath.angleFeatureSequenceScore(reference, attempt, joints)));
        }
        Collections.sort(matches, (a, b) -> Integer.compare(b.score, a.score));
        return matches.size() > MAX_SUGGESTIONS
                ? new ArrayList<>(matches.subList(0, MAX_SUGGESTIONS))
                : matches;
    }
}

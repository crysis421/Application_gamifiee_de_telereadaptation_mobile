package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Retrouver un exercice à partir d'un mouvement capté (US-6.3). */
public class MovementMatcherTest {

    private static final List<MovementMatcher.Candidate> CATALOGUE = Arrays.asList(
            new MovementMatcher.Candidate("squat", squatMovement()),
            new MovementMatcher.Candidate("bras", armsMovement()),
            new MovementMatcher.Candidate("sans-modele", new ArrayList<>()));

    @Test
    public void aSquat_isRecognisedAsTheSquat() {
        List<MovementMatcher.Match> matches = MovementMatcher.rank(CATALOGUE, slowly(squatMovement()));

        assertEquals("squat", matches.get(0).getExerciseId());
    }

    @Test
    public void anArmMovement_isRecognisedAsTheArmExercise() {
        List<MovementMatcher.Match> matches = MovementMatcher.rank(CATALOGUE, slowly(armsMovement()));

        assertEquals("bras", matches.get(0).getExerciseId());
    }

    @Test
    public void exercisesWithoutAReference_areNeverProposed() {
        for (MovementMatcher.Match match : MovementMatcher.rank(CATALOGUE, squatMovement())) {
            assertTrue(!match.getExerciseId().equals("sans-modele"));
        }
    }

    @Test
    public void matches_areSortedFromClosestToFarthest() {
        List<MovementMatcher.Match> matches = MovementMatcher.rank(CATALOGUE, squatMovement());

        for (int i = 1; i < matches.size(); i++) {
            assertTrue(matches.get(i - 1).getScore() >= matches.get(i).getScore());
        }
    }

    @Test
    public void atMostThreeExercises_areProposed() {
        List<MovementMatcher.Candidate> many = new ArrayList<>();
        for (int i = 0; i < 6; i++) many.add(new MovementMatcher.Candidate("e" + i, squatMovement()));

        assertEquals(MovementMatcher.MAX_SUGGESTIONS,
                MovementMatcher.rank(many, squatMovement()).size());
    }

    @Test
    public void standingStill_proposesNothing() {
        List<float[]> still = Arrays.asList(standingPose(), standingPose(), standingPose());

        assertTrue(MovementMatcher.rank(CATALOGUE, still).isEmpty());
        assertTrue(!MovementMatcher.hasMovement(still));
    }

    private static List<float[]> slowly(List<float[]> movement) {
        List<float[]> slow = new ArrayList<>();
        for (float[] frame : movement) {
            slow.add(frame);
            slow.add(frame);
        }
        return slow;
    }

    private static List<float[]> squatMovement() {
        return Arrays.asList(standingPose(), squatPose(), standingPose());
    }

    private static List<float[]> armsMovement() {
        return Arrays.asList(standingPose(), armsBentPose(), standingPose());
    }

    private static float[] standingPose() {
        float[] pose = new float[MovementMath.LANDMARKS * 2];
        point(pose, 11, -1f, -2f);
        point(pose, 12, 1f, -2f);
        point(pose, 13, -2f, -1f);
        point(pose, 14, 2f, -1f);
        point(pose, 15, -2f, 0f);
        point(pose, 16, 2f, 0f);
        point(pose, 23, -.5f, 0f);
        point(pose, 24, .5f, 0f);
        point(pose, 25, -.5f, 2f);
        point(pose, 26, .5f, 2f);
        point(pose, 27, -.5f, 4f);
        point(pose, 28, .5f, 4f);
        return pose;
    }

    private static float[] squatPose() {
        float[] pose = standingPose();
        point(pose, 25, -2.5f, .5f);
        point(pose, 26, 2.5f, .5f);
        return pose;
    }

    private static float[] armsBentPose() {
        float[] pose = standingPose();
        point(pose, 15, -1f, 0f);
        point(pose, 16, 1f, 0f);
        return pose;
    }

    private static void point(float[] pose, int landmark, float x, float y) {
        pose[landmark * 2] = x;
        pose[landmark * 2 + 1] = y;
    }
}

package com.uphf.saes5.domain.movement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class MovementComparisonEngineTest {

    @Test
    public void addFrame_returnsImmediateBestPostureScore() {
        MovementComparisonEngine engine = new MovementComparisonEngine(reference(), 80);

        MovementComparisonResult result = engine.addFrame(bentPose());

        assertEquals(100, result.getLiveScore());
        assertFalse(result.isReady());
    }

    @Test
    public void addFrame_validatesCompleteMovementAtThreshold() {
        MovementComparisonEngine engine = new MovementComparisonEngine(reference(), 80);

        engine.addFrame(standingPose());
        MovementComparisonResult result = engine.addFrame(bentPose());

        assertTrue(result.isReady());
        assertTrue(result.isMovementValid());
        assertTrue(result.isRepetitionValidated());
        assertEquals(100, result.getMovementScore());
    }

    @Test
    public void addFrame_marksDissimilarMovementInvalid() {
        MovementComparisonEngine engine = new MovementComparisonEngine(reference(), 99);

        engine.addFrame(changedPose());
        MovementComparisonResult result = engine.addFrame(changedPose());

        assertTrue(result.isReady());
        assertFalse(result.isMovementValid());
        assertFalse(result.isRepetitionValidated());
        assertTrue(result.getMovementScore() < 99);
    }

    @Test
    public void addFrame_keepsOnlyReferenceLengthWindow() {
        MovementComparisonEngine engine = new MovementComparisonEngine(reference(), 100);

        engine.addFrame(changedPose());
        engine.addFrame(standingPose());
        MovementComparisonResult result = engine.addFrame(bentPose());

        assertEquals(100, result.getMovementScore());
        assertTrue(result.isRepetitionValidated());
    }

    @Test
    public void addFrame_emitsOneValidationThenStartsANewSequence() {
        MovementComparisonEngine engine = new MovementComparisonEngine(reference(), 80);

        engine.addFrame(standingPose());
        MovementComparisonResult firstSequence = engine.addFrame(bentPose());
        MovementComparisonResult nextFrame = engine.addFrame(standingPose());

        assertTrue(firstSequence.isRepetitionValidated());
        assertFalse(nextFrame.isReady());
        assertFalse(nextFrame.isRepetitionValidated());
    }

    @Test
    public void addFrame_doesNotRevalidateWhileUserStaysInTheEndPose() {
        MovementComparisonEngine engine = new MovementComparisonEngine(reference(), 80);
        engine.addFrame(standingPose());
        assertTrue(engine.addFrame(bentPose()).isRepetitionValidated());

        MovementComparisonResult heldPose = null;
        for (int i = 0; i < 6; i++) {
            heldPose = engine.addFrame(bentPose());
            assertFalse(heldPose.isRepetitionValidated());
        }
        assertFalse(heldPose.isReady());

        // Quitter réellement la pose finale réarme le moteur pour la répétition suivante.
        assertFalse(engine.addFrame(standingPose()).isRepetitionValidated());
        assertTrue(engine.addFrame(bentPose()).isRepetitionValidated());
    }

    @Test
    public void addFrame_ignoresMalformedPoseWithoutAdvancingSequence() {
        MovementComparisonEngine engine = new MovementComparisonEngine(reference(), 80);

        MovementComparisonResult malformed = engine.addFrame(new float[3]);
        MovementComparisonResult firstValid = engine.addFrame(standingPose());

        assertEquals(0, malformed.getLiveScore());
        assertFalse(malformed.isReady());
        assertFalse(firstValid.isReady());
    }

    @Test
    public void reset_discardsTheCurrentSequence() {
        MovementComparisonEngine engine = new MovementComparisonEngine(reference(), 80);
        engine.addFrame(standingPose());

        engine.reset();
        MovementComparisonResult result = engine.addFrame(bentPose());

        assertFalse(result.isReady());
        assertFalse(result.isRepetitionValidated());
    }

    @Test
    public void addFrame_doesNotCountAUserWhoStaysStill() {
        MovementComparisonEngine engine = new MovementComparisonEngine(squatReference(), 80);

        for (int i = 0; i < 10; i++) {
            MovementComparisonResult result = engine.addFrame(standingPose());
            assertFalse(result.isRepetitionValidated());
            assertFalse(result.isMovementValid());
        }
    }

    @Test
    public void addFrame_doesNotCountAMovementThatSkipsTheTargetPose() {
        MovementComparisonEngine engine = new MovementComparisonEngine(squatReference(), 80);

        engine.addFrame(standingPose());
        engine.addFrame(halfSquatPose());
        MovementComparisonResult result = engine.addFrame(standingPose());

        assertFalse(result.isRepetitionValidated());
    }

    @Test
    public void addFrame_doesNotCountAMovementWithoutTheReturn() {
        MovementComparisonEngine engine = new MovementComparisonEngine(squatReference(), 80);

        engine.addFrame(standingPose());
        engine.addFrame(squatPose());
        MovementComparisonResult result = engine.addFrame(squatPose());

        assertFalse(result.isRepetitionValidated());
    }

    @Test
    public void addFrame_countsStartTargetReturn() {
        MovementComparisonEngine engine = new MovementComparisonEngine(squatReference(), 80);

        engine.addFrame(standingPose());
        engine.addFrame(squatPose());
        MovementComparisonResult result = engine.addFrame(standingPose());

        assertTrue(result.isRepetitionValidated());
    }

    @Test
    public void addFrame_countsConsecutiveRepetitions() {
        MovementComparisonEngine engine = new MovementComparisonEngine(squatReference(), 80);
        int count = 0;

        for (int rep = 0; rep < 3; rep++) {
            if (engine.addFrame(standingPose()).isRepetitionValidated()) count++;
            if (engine.addFrame(squatPose()).isRepetitionValidated()) count++;
            if (engine.addFrame(standingPose()).isRepetitionValidated()) count++;
        }

        assertEquals(3, count);
    }

    @Test
    public void addFrame_countsARepetitionSlowerThanTheModel() {
        MovementComparisonEngine engine = new MovementComparisonEngine(squatReference(), 80);
        boolean validated = false;

        for (float[] pose : Arrays.asList(standingPose(), standingPose(), squatPose(),
                squatPose(), squatPose(), squatPose(), standingPose())) {
            validated |= engine.addFrame(pose).isRepetitionValidated();
        }

        assertTrue(validated);
    }

    @Test
    public void addFrame_reportsAnAttemptThatMissesTheTarget_asOneFailure() {
        MovementComparisonEngine engine = new MovementComparisonEngine(squatReference(), 80);
        int failures = 0;

        for (float[] pose : Arrays.asList(standingPose(), partialSquatPose(),
                partialSquatPose(), standingPose(), standingPose(), standingPose())) {
            MovementComparisonResult result = engine.addFrame(pose);
            assertFalse(result.isRepetitionValidated());
            if (result.isRepetitionFailed()) failures++;
        }

        assertEquals(1, failures);
    }

    @Test
    public void addFrame_neverReportsAFailure_forAUserWhoStaysStill() {
        MovementComparisonEngine engine = new MovementComparisonEngine(squatReference(), 80);

        for (int i = 0; i < 10; i++) {
            assertFalse(engine.addFrame(standingPose()).isRepetitionFailed());
        }
    }

    @Test
    public void addFrame_aValidRepetition_isNotAFailure() {
        MovementComparisonEngine engine = new MovementComparisonEngine(squatReference(), 80);

        engine.addFrame(standingPose());
        engine.addFrame(squatPose());
        MovementComparisonResult result = engine.addFrame(standingPose());

        assertTrue(result.isRepetitionValidated());
        assertFalse(result.isRepetitionFailed());
    }

    private static List<float[]> reference() {
        return Arrays.asList(standingPose(), bentPose());
    }

    /** Flexion nette, mais loin de la profondeur du squat de référence. */
    private static float[] partialSquatPose() {
        float[] pose = standingPose();
        point(pose, 25, -1.4f, 1.4f);
        point(pose, 26, 1.4f, 1.4f);
        return pose;
    }

    private static List<float[]> squatReference() {
        return Arrays.asList(standingPose(), squatPose(), standingPose());
    }

    /** Hanches et genoux fléchis, bras inchangés. */
    private static float[] squatPose() {
        float[] pose = standingPose();
        point(pose, 25, -2.5f, .5f);
        point(pose, 26, 2.5f, .5f);
        return pose;
    }

    /** À mi-chemin entre debout et le squat : la cible n'est pas atteinte. */
    private static float[] halfSquatPose() {
        float[] pose = standingPose();
        point(pose, 25, -1.2f, 1.6f);
        point(pose, 26, 1.2f, 1.6f);
        return pose;
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

    private static float[] bentPose() {
        float[] pose = standingPose();
        point(pose, 15, -1f, 0f);
        point(pose, 16, 1f, 0f);
        point(pose, 27, -2f, 2f);
        point(pose, 28, 2f, 2f);
        return pose;
    }

    private static float[] changedPose() {
        float[] pose = standingPose();
        point(pose, 15, -4f, -1f);
        point(pose, 16, 4f, -1f);
        point(pose, 27, -3f, 2f);
        point(pose, 28, 3f, 2f);
        return pose;
    }

    private static void point(float[] pose, int landmark, float x, float y) {
        pose[landmark * 2] = x;
        pose[landmark * 2 + 1] = y;
    }
}

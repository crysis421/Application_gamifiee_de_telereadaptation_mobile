package com.uphf.saes5;

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

    private static List<float[]> reference() {
        return Arrays.asList(standingPose(), bentPose());
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

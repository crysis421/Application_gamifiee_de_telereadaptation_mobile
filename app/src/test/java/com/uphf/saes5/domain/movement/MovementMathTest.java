package com.uphf.saes5.domain.movement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MovementMathTest {

    @Test
    public void angleFeatures_returnsEightJointAngles() {
        float[] features = MovementMath.angleFeatures(standingPose());

        assertNotNull(features);
        assertEquals(8, features.length);
    }

    @Test
    public void angleScore_identicalPosesScoreOneHundred() {
        List<float[]> movement = Arrays.asList(standingPose(), bentPose(), standingPose());

        assertEquals(100, MovementMath.angleScore(movement, movement));
    }

    @Test
    public void angleScore_ignoresTranslationScaleAndHorizontalMirroring() {
        float[] reference = bentPose();
        float[] transformed = transform(reference, 4.2f, 13f, -7f, true);

        assertEquals(100, MovementMath.angleScore(
                Collections.singletonList(reference),
                Collections.singletonList(transformed)));
    }

    @Test
    public void angleScore_detectsChangedElbowAndKneeAngles() {
        float[] reference = standingPose();
        float[] changed = standingPose();
        point(changed, 15, -1f, 0f); // poignet gauche : coude fléchi différemment
        point(changed, 27, -2f, 2f); // cheville gauche : genou fléchi

        int score = MovementMath.angleScore(
                Collections.singletonList(reference),
                Collections.singletonList(changed));

        assertTrue("Le score doit baisser quand les articulations changent", score < 95);
    }

    @Test
    public void angleScore_alignsTimeWarpedSequences() {
        float[] start = standingPose();
        float[] middle = bentPose();
        List<float[]> reference = Arrays.asList(start, middle, start);
        List<float[]> slowerAttempt = Arrays.asList(start, start, middle, middle, start, start);

        assertEquals(100, MovementMath.angleScore(reference, slowerAttempt));
    }

    @Test
    public void angleMath_ignoresUnusableFrames() {
        float[] invalidNumbers = standingPose();
        invalidNumbers[0] = Float.NaN;

        assertNull(MovementMath.angleFeatures(null));
        assertNull(MovementMath.angleFeatures(new float[10]));
        assertNull(MovementMath.angleFeatures(invalidNumbers));
        assertEquals(100, MovementMath.angleScore(
                Arrays.asList(null, standingPose()),
                Arrays.asList(new float[2], standingPose())));
        assertEquals(0, MovementMath.angleScore(
                Collections.singletonList(null),
                Collections.singletonList(standingPose())));
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

    private static float[] transform(float[] pose, float scale, float tx, float ty,
                                     boolean mirrorHorizontally) {
        float[] transformed = pose.clone();
        for (int i = 0; i < transformed.length; i += 2) {
            float x = transformed[i] * scale;
            transformed[i] = (mirrorHorizontally ? -x : x) + tx;
            transformed[i + 1] = transformed[i + 1] * scale + ty;
        }
        return transformed;
    }

    private static void point(float[] pose, int landmark, float x, float y) {
        pose[landmark * 2] = x;
        pose[landmark * 2 + 1] = y;
    }
}

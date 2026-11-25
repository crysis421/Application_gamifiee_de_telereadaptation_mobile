package com.uphf.saes5;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Lecture en boucle d'un modèle par le mannequin de démonstration. */
public class ModelAnimationTest {

    private static final float EPSILON = 1e-5f;

    /** Une image où tous les points valent {@code value}, sauf le nez placé en {@code noseX}. */
    private static float[] frame(float value, float noseX) {
        float[] frame = new float[MovementMath.LANDMARKS * 2];
        Arrays.fill(frame, value);
        frame[0] = noseX;
        return frame;
    }

    @Test
    public void frameAt_interpolatesBetweenNeighbouringFrames() {
        ModelAnimation animation = new ModelAnimation(
                Arrays.asList(frame(0f, 0f), frame(1f, 1f)), 100);

        assertEquals(0f, animation.frameAt(0)[2], EPSILON);
        assertEquals(0.25f, animation.frameAt(25)[2], EPSILON);
        assertEquals(0.5f, animation.frameAt(50)[2], EPSILON);
    }

    @Test
    public void frameAt_restartsFromTheFirstFrame_withoutBlendingTheLoopSeam() {
        ModelAnimation animation = new ModelAnimation(
                Arrays.asList(frame(0f, 0f), frame(1f, 1f)), 100);

        // Dernière image : tenue telle quelle, pas d'interpolation vers la première.
        assertEquals(1f, animation.frameAt(150)[2], EPSILON);
        // Une boucle dure 200 ms : on repart du début.
        assertEquals(0f, animation.frameAt(200)[2], EPSILON);
        assertEquals(0.5f, animation.frameAt(250)[2], EPSILON);
    }

    @Test
    public void bounds_coverTheWholeMovement() {
        ModelAnimation animation = new ModelAnimation(
                Arrays.asList(frame(-1f, -3f), frame(2f, 4f)), 100);

        assertEquals(-3f, animation.minX, EPSILON);
        assertEquals(4f, animation.maxX, EPSILON);
        assertEquals(-1f, animation.minY, EPSILON);
        assertEquals(2f, animation.maxY, EPSILON);
    }

    @Test
    public void unusableFramesAreSkipped_andMissingIntervalFallsBackToDefault() {
        float[] broken = frame(0f, Float.NaN);
        float[] valid = frame(0.5f, 0.5f);
        ModelAnimation animation = new ModelAnimation(Arrays.asList(broken, valid), 0);

        assertArrayEquals(valid, animation.frameAt(0), EPSILON);
        assertEquals(ModelAnimation.DEFAULT_INTERVAL_MS, animation.durationMs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void modelWithoutUsableFrame_isRejected() {
        List<float[]> none = Collections.singletonList(new float[4]);
        new ModelAnimation(none, 100);
    }
}

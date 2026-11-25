package com.uphf.saes5;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/** Le mouvement n'est jugé que si la plupart du corps est réellement vu. */
public class BodyVisibilityTest {

    private final float[] x = new float[MovementMath.LANDMARKS];
    private final float[] y = new float[MovementMath.LANDMARKS];
    private final float[] visibility = new float[MovementMath.LANDMARKS];

    /** Corps entier au centre de l'image, bien visible. */
    private void wholeBodyInFrame() {
        Arrays.fill(x, 0.5f);
        Arrays.fill(y, 0.5f);
        Arrays.fill(visibility, 0.95f);
    }

    private boolean visible() {
        return BodyVisibility.isMostlyVisible(x, y, visibility);
    }

    @Test
    public void wholeBodyInFrame_isJudged() {
        wholeBodyInFrame();
        assertTrue(visible());
    }

    @Test
    public void seatedInFrontOfThePhone_legsBelowTheImage_isNotJudged() {
        wholeBodyInFrame();
        // Genoux et chevilles devinés sous le bord de l'image.
        for (int point : new int[]{25, 26, 27, 28}) {
            y[point] = 1.3f;
            visibility[point] = 0.1f;
        }
        assertFalse(visible());
    }

    /** Relevé réel sur téléphone, personne assise : la cheville au bord de l'image ne compte pas. */
    @Test
    public void seated_ankleOnTheEdgeButNotSeen_isNotJudged() {
        wholeBodyInFrame();
        y[27] = 0.97f;
        visibility[27] = 0.17f;
        y[28] = 1.06f;
        visibility[28] = 0.40f;
        assertFalse(visible());
    }

    @Test
    public void oneKeyPointOutside_handRaisedAboveTheImage_isStillJudged() {
        wholeBodyInFrame();
        y[15] = -0.1f;
        assertTrue(visible());
    }

    @Test
    public void bothHandsOutside_isNotJudged() {
        wholeBodyInFrame();
        y[15] = -0.1f;
        y[16] = -0.1f;
        assertFalse(visible());
    }

    @Test
    public void sideView_hiddenFarLimbs_isStillJudged() {
        wholeBodyInFrame();
        // De profil : bras et jambe du côté caché peu visibles, le reste net.
        for (int point : new int[]{12, 14, 16, 24, 26, 28}) {
            visibility[point] = 0.15f;
        }
        assertTrue(visible());
    }

    @Test
    public void bodyInFrameButBarelySeen_isNotJudged() {
        wholeBodyInFrame();
        Arrays.fill(visibility, 0.3f);
        assertFalse(visible());
    }

    @Test
    public void modelWithoutVisibility_reliesOnFramingOnly() {
        wholeBodyInFrame();
        Arrays.fill(visibility, Float.NaN);
        assertTrue(visible());
    }

    @Test
    public void pointOnTheImageEdge_countsAsInside() {
        wholeBodyInFrame();
        y[27] = 1.01f;
        y[28] = 1.01f;
        assertTrue(visible());
    }
}

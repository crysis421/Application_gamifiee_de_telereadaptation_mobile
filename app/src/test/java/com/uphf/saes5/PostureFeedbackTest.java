package com.uphf.saes5;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Couleur du squelette selon la posture instantanée (US-3.2). */
public class PostureFeedbackTest {

    @Test
    public void startsNeutral_beforeAnyPose() {
        assertEquals(PostureFeedback.State.NEUTRAL, new PostureFeedback(80).getState());
    }

    @Test
    public void postureAtThreshold_isCorrect() {
        PostureFeedback feedback = new PostureFeedback(80);

        assertEquals(PostureFeedback.State.CORRECT, feedback.update(80));
    }

    @Test
    public void postureClearlyBelowThreshold_isToCorrect() {
        PostureFeedback feedback = new PostureFeedback(80);

        assertEquals(PostureFeedback.State.TO_CORRECT, feedback.update(60));
    }

    @Test
    public void scoreJustBelowThreshold_keepsThePreviousState() {
        PostureFeedback feedback = new PostureFeedback(80);
        feedback.update(90);

        // Dans la marge : on ne bascule pas au rouge pour un point de score.
        assertEquals(PostureFeedback.State.CORRECT, feedback.update(78));
        assertEquals(PostureFeedback.State.TO_CORRECT, feedback.update(70));
        // Et on ne revient au vert qu'au seuil franchi.
        assertEquals(PostureFeedback.State.TO_CORRECT, feedback.update(78));
        assertEquals(PostureFeedback.State.CORRECT, feedback.update(80));
    }

    @Test
    public void firstScoreInsideTheMargin_isToCorrect() {
        assertEquals(PostureFeedback.State.TO_CORRECT, new PostureFeedback(80).update(78));
    }

    @Test
    public void lostBody_returnsToNeutral() {
        PostureFeedback feedback = new PostureFeedback(80);
        feedback.update(95);

        feedback.reset();

        assertEquals(PostureFeedback.State.NEUTRAL, feedback.getState());
    }
}

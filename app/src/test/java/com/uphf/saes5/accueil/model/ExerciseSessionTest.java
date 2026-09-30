package com.uphf.saes5.accueil.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Statistiques affichées sur l'écran de récapitulatif. */
public class ExerciseSessionTest {

    @Test
    public void successRate_isZero_whenNoAttempt() {
        ExerciseSession session = new ExerciseSession("id", 0, 0, 0L);

        assertEquals(0, session.getSuccessRate());
        assertEquals(0, session.getTotalAttempts());
    }

    @Test
    public void successRate_isHundred_whenNoFailure() {
        ExerciseSession session = new ExerciseSession("id", 12, 0, 60_000L);

        assertEquals(100, session.getSuccessRate());
    }

    @Test
    public void successRate_isRoundedToNearestInteger() {
        // 10 validées sur 12 tentatives = 83,33 % -> 83
        ExerciseSession session = new ExerciseSession("id", 10, 2, 60_000L);

        assertEquals(83, session.getSuccessRate());
        assertEquals(12, session.getTotalAttempts());
    }

    @Test
    public void averageTimePerRep_isZero_whenNoValidatedRep() {
        ExerciseSession session = new ExerciseSession("id", 0, 5, 60_000L);

        assertEquals(0L, session.getAverageMillisPerRep());
    }

    @Test
    public void averageTimePerRep_dividesTotalDurationByValidatedReps() {
        ExerciseSession session = new ExerciseSession("id", 10, 0, 60_000L);

        assertEquals(6_000L, session.getAverageMillisPerRep());
    }

    @Test
    public void goal_isReached_whenValidatedRepsMeetTarget() {
        ExerciseSession session = new ExerciseSession("id", 12, 3, 60_000L);

        assertTrue(session.isGoalReached(12));
        assertTrue(session.isGoalReached(10));
        assertFalse(session.isGoalReached(15));
    }
}

package com.uphf.saes5.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Statistiques affichées sur l'écran de récapitulatif. */
public class SessionResultTest {

    @Test
    public void successRate_isZero_whenNoAttempt() {
        SessionResult session = new SessionResult("id", 0, 0, 0L);

        assertEquals(0, session.getSuccessRate());
        assertEquals(0, session.getTotalAttempts());
    }

    @Test
    public void successRate_isHundred_whenNoFailure() {
        SessionResult session = new SessionResult("id", 12, 0, 60_000L);

        assertEquals(100, session.getSuccessRate());
    }

    @Test
    public void successRate_isRoundedToNearestInteger() {
        // 10 validées sur 12 tentatives = 83,33 % -> 83
        SessionResult session = new SessionResult("id", 10, 2, 60_000L);

        assertEquals(83, session.getSuccessRate());
        assertEquals(12, session.getTotalAttempts());
    }

    @Test
    public void averageTimePerRep_isZero_whenNoValidatedRep() {
        SessionResult session = new SessionResult("id", 0, 5, 60_000L);

        assertEquals(0L, session.getAverageMillisPerRep());
    }

    @Test
    public void averageTimePerRep_dividesTotalDurationByValidatedReps() {
        SessionResult session = new SessionResult("id", 10, 0, 60_000L);

        assertEquals(6_000L, session.getAverageMillisPerRep());
    }

    @Test
    public void goal_isReached_whenValidatedRepsMeetTarget() {
        SessionResult session = new SessionResult("id", 12, 3, 60_000L);

        assertTrue(session.isGoalReached(12));
        assertTrue(session.isGoalReached(10));
        assertFalse(session.isGoalReached(15));
    }
}

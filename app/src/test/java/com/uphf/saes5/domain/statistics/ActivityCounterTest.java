package com.uphf.saes5.domain.statistics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.uphf.saes5.domain.model.SessionEntry;

import java.util.Arrays;
import java.util.Collections;

/** Nombre d'exercices réalisés, affiché sur l'accueil (US-5.2). */
public class ActivityCounterTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long NOW = 400 * DAY;

    private static SessionEntry at(long timestamp, int validatedReps) {
        return new SessionEntry(timestamp, "squat", "Squat", validatedReps, 0, 10, 1_000,
                SessionEntry.SCORE_UNAVAILABLE, "", "");
    }

    @Test
    public void nothingDone_isZero() {
        ActivityCounter.Counts counts = ActivityCounter.count(Collections.emptyList(), NOW);

        assertEquals(0, counts.getTotal());
        assertEquals(0, counts.getLastSevenDays());
    }

    @Test
    public void everyFinishedExercise_countsOnce() {
        ActivityCounter.Counts counts = ActivityCounter.count(
                Arrays.asList(at(NOW - DAY, 4), at(NOW - 2 * DAY, 10), at(NOW, 1)), NOW);

        assertEquals(3, counts.getTotal());
    }

    @Test
    public void aSessionWithoutValidatedRepetition_isNotARealisedExercise() {
        ActivityCounter.Counts counts = ActivityCounter.count(
                Arrays.asList(at(NOW, 0), at(NOW, 2)), NOW);

        assertEquals(1, counts.getTotal());
    }

    @Test
    public void anAbandonedSession_isNotARealisedExercise() {
        SessionEntry abandoned = new SessionEntry(NOW, "squat", "Squat", 4, 0, 10, 1_000,
                SessionEntry.SCORE_UNAVAILABLE, "", "", "", -1, -1, true);

        assertEquals(0, ActivityCounter.count(Collections.singletonList(abandoned), NOW).getTotal());
    }

    @Test
    public void theWeek_keepsOnlyTheLastSevenDays() {
        ActivityCounter.Counts counts = ActivityCounter.count(Arrays.asList(
                at(NOW - 8 * DAY, 3),
                at(NOW - 7 * DAY, 3),
                at(NOW - 6 * DAY, 3),
                at(NOW, 3)), NOW);

        assertEquals(4, counts.getTotal());
        assertEquals(3, counts.getLastSevenDays());
    }
}

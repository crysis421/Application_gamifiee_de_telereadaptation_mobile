package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Les statistiques viennent des vraies séances, plus de données en dur. */
public class StatisticsRepositoryTest {

    private static SessionEntry entry(String id, int validated, int failed, int similarity) {
        return new SessionEntry(1_000L, id, "Nom " + id, validated, failed, 10, 60_000L,
                similarity, "", "");
    }

    @Test
    public void anEmptyLog_meansNoStatistics() {
        assertTrue(StatisticsRepository.toStatisticsSessions(
                Collections.emptyList()).isEmpty());
    }

    @Test
    public void quality_isTheSimilarity_whenTheCameraMeasuredIt() {
        List<ExerciseSession> sessions = StatisticsRepository.toStatisticsSessions(
                Collections.singletonList(entry("squat", 8, 2, 87)));

        assertEquals(87, sessions.get(0).getQualityScore());
        assertEquals("squat", sessions.get(0).getExerciseId());
    }

    @Test
    public void quality_isTheSuccessRate_forAManualSession() {
        List<ExerciseSession> sessions = StatisticsRepository.toStatisticsSessions(
                Collections.singletonList(entry("squat", 3, 1, SessionEntry.SCORE_UNAVAILABLE)));

        assertEquals(75, sessions.get(0).getQualityScore());
    }

    @Test
    public void aSessionWithoutAnyAttempt_isIgnored() {
        assertTrue(StatisticsRepository.toStatisticsSessions(Collections.singletonList(
                entry("squat", 0, 0, SessionEntry.SCORE_UNAVAILABLE))).isEmpty());
    }

    @Test
    public void exercises_comeFromTheCatalogue_andKeepDeletedOnesWithSessions() {
        List<Exercise> catalogue = Collections.singletonList(
                new Exercise("squat", "Squat", "Jambes", "Facile", 5, "Aucun", 3));
        List<SessionEntry> log = Arrays.asList(
                entry("squat", 5, 0, 90), entry("ancien", 2, 0, 70));

        List<StatisticsExercise> exercises =
                StatisticsRepository.toStatisticsExercises(catalogue, log);

        assertEquals(2, exercises.size());
        assertEquals("Jambes", exercises.get(0).getMuscleGroup());
        assertEquals("Nom ancien", exercises.get(1).getName());
        assertEquals("", exercises.get(1).getMuscleGroup());
    }
}

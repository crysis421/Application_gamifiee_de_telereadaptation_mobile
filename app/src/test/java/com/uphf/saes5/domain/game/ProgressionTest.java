package com.uphf.saes5.domain.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.SessionEntry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Rangs par groupe musculaire et étoiles par exercice (US-4.5). */
public class ProgressionTest {

    private static final List<Exercise> CATALOGUE = Arrays.asList(
            new Exercise("squat", "Squat", "Jambes", "Facile", 5, "Aucun", 3),
            new Exercise("fentes", "Fentes", "Jambes", "Facile", 6, "Aucun", 2),
            new Exercise("pompes", "Pompes", "Pectoraux", "Moyen", 7, "Tapis", 4));

    private static SessionEntry withPoints(String id, int points) {
        return new SessionEntry(0, id, id, 5, 0, 10, 60_000, 80, "", "", "", -1, points);
    }

    @Test
    public void sessionPoints_areTheArcadePoints_whenTheyWereRecorded() {
        assertEquals(420, Progression.sessionPoints(withPoints("squat", 420)));
    }

    @Test
    public void sessionPoints_areRebuiltFromRepetitions_forOlderSessions() {
        SessionEntry old = new SessionEntry(0, "squat", "Squat", 3, 1, 10, 60_000, 80, "", "");

        // Trois succès à 80 % de précision, sans multiplicateur.
        assertEquals(240, Progression.sessionPoints(old));
    }

    @Test
    public void stars_followThePointThresholds() {
        assertEquals(0, Progression.starsFor(Progression.STAR_THRESHOLDS[0] - 1));
        assertEquals(1, Progression.starsFor(Progression.STAR_THRESHOLDS[0]));
        assertEquals(2, Progression.starsFor(Progression.STAR_THRESHOLDS[1]));
        assertEquals(Progression.MAX_STARS, Progression.starsFor(1_000_000));
    }

    @Test
    public void rank_followsThePointThresholds() {
        assertEquals(Progression.Rank.NOVICE, Progression.Rank.forPoints(0));
        assertEquals(Progression.Rank.BRONZE, Progression.Rank.forPoints(Progression.Rank.BRONZE.getThreshold()));
        assertEquals(Progression.Rank.PLATINUM, Progression.Rank.forPoints(1_000_000));
        assertNull(Progression.Rank.PLATINUM.next());
    }

    @Test
    public void muscleRank_accumulatesThePointsOfEveryExerciseOfThatMuscle() {
        Progression.Summary summary = Progression.calculate(CATALOGUE, Arrays.asList(
                withPoints("squat", 300), withPoints("fentes", 250), withPoints("pompes", 100)));

        Progression.MuscleRank legs = summary.muscle("Jambes");
        assertEquals(550, legs.getPoints());
        assertEquals(Progression.Rank.BRONZE, legs.getRank());
        assertEquals(Progression.Rank.SILVER.getThreshold() - 550, legs.getPointsToNextRank());
    }

    @Test
    public void aMuscleWithoutSession_isNovice() {
        Progression.Summary summary = Progression.calculate(CATALOGUE, Collections.emptyList());

        assertEquals(Progression.Rank.NOVICE, summary.muscle("Dos").getRank());
        assertEquals(0, summary.muscle("Dos").getPoints());
    }

    @Test
    public void stars_areCountedPerExercise_andAddUp() {
        Progression.Summary summary = Progression.calculate(CATALOGUE, Arrays.asList(
                withPoints("squat", Progression.STAR_THRESHOLDS[1]),
                withPoints("pompes", Progression.STAR_THRESHOLDS[0])));

        assertEquals(2, summary.exercise("squat").getStars());
        assertEquals(1, summary.exercise("pompes").getStars());
        assertEquals(3, summary.getTotalStars());
    }

    @Test
    public void exercisesWithoutPoints_areListedLast_withNoStar() {
        Progression.Summary summary = Progression.calculate(CATALOGUE,
                Collections.singletonList(withPoints("pompes", 50)));

        assertEquals("pompes", summary.getExercises().get(0).getExerciseId());
        assertEquals(0, summary.exercise("squat").getStars());
        assertTrue(summary.exercise("squat").getPointsToNextStar() > 0);
    }
}

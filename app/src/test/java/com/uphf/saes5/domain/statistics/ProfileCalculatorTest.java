package com.uphf.saes5.domain.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.uphf.saes5.domain.game.MuscleProgress;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ProfileCalculatorTest {
    private static final StatisticsExercise SQUAT =
            new StatisticsExercise("squat", "Squat", "Jambes");

    @Test
    public void calculate_countsPerfectSessionsAsStarsAndSortsHistoryNewestFirst() {
        ProfileSummary result = ProfileCalculator.calculate(
                Collections.singletonList(SQUAT),
                Arrays.asList(
                        session("squat", 2_000, 89),
                        session("squat", 3_000, 90),
                        session("squat", 1_000, 100),
                        session("unknown", 4_000, 100)));

        assertEquals(2, result.getStarCount());
        assertEquals(3, result.getHistory().size());
        assertEquals(Arrays.asList(3_000L, 2_000L, 1_000L), result.getHistory().stream()
                .map(ProfileHistoryEntry::getCompletedAtEpochMillis)
                .collect(Collectors.toList()));
        assertTrue(result.getHistory().get(0).earnedStar());
    }

    @Test
    public void muscleProgress_usesExistingRankThresholds() {
        assertEquals(MuscleProgress.TO_IMPROVE, MuscleProgress.fromScore(39));
        assertEquals(MuscleProgress.PROGRESSING, MuscleProgress.fromScore(40));
        assertEquals(MuscleProgress.PROGRESSING, MuscleProgress.fromScore(69));
        assertEquals(MuscleProgress.MASTERED, MuscleProgress.fromScore(70));
    }

    @Test
    public void calculate_preservesMuscleRowsEvenWithoutHistory() {
        List<StatisticsExercise> exercises = Collections.singletonList(SQUAT);
        ProfileSummary result = ProfileCalculator.calculate(exercises, Collections.emptyList());

        assertEquals(5, result.getMuscleStatistics().size());
        assertTrue(result.getHistory().isEmpty());
        assertEquals(0, result.getStarCount());
    }

    private static ScoredSession session(String exerciseId, long completedAt, int score) {
        return new ScoredSession(exerciseId, completedAt, score);
    }
}

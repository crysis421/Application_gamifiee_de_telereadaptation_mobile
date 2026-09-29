package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StatisticsCalculatorTest {
    private static final StatisticsExercise SQUAT =
            new StatisticsExercise("squat", "Squat", "Jambes");

    @Test
    public void calculate_countsScoresAtOrAboveNinetyAsPerfect() {
        StatisticsSummary result = StatisticsCalculator.calculate(
                Collections.singletonList(SQUAT),
                Arrays.asList(session("squat", 89), session("squat", 90), session("squat", 100)));

        assertEquals(67, result.getGlobalPerfectPercentage());
        assertEquals(67, result.getExerciseStatistics().get(0).getPerfectPercentage());
    }

    @Test
    public void calculate_roundsHalfUpForAverageScores() {
        StatisticsSummary result = StatisticsCalculator.calculate(
                Collections.singletonList(SQUAT),
                Arrays.asList(session("squat", 50), session("squat", 51)));

        assertEquals(51, result.getExerciseStatistics().get(0).getAverageScore());
        assertEquals(51, muscle(result, "Jambes").getAverageScore());
    }

    @Test
    public void exerciseSession_clampsScoresToValidPercentageRange() {
        assertEquals(0, session("squat", -5).getQualityScore());
        assertEquals(100, session("squat", 140).getQualityScore());
    }

    @Test
    public void calculate_ignoresSessionsForUnknownExercises() {
        StatisticsSummary result = StatisticsCalculator.calculate(
                Collections.singletonList(SQUAT),
                Arrays.asList(session("unknown", 100), session("squat", 40)));

        assertEquals(0, result.getGlobalPerfectPercentage());
        assertEquals(1, result.getExerciseStatistics().get(0).getSessionCount());
        assertEquals(40, result.getExerciseStatistics().get(0).getAverageScore());
    }

    @Test
    public void calculate_returnsNoDataMusclesForEmptyHistory() {
        StatisticsSummary result = StatisticsCalculator.calculate(
                Collections.singletonList(SQUAT), Collections.emptyList());

        assertEquals(0, result.getGlobalPerfectPercentage());
        assertEquals(0, result.getExerciseStatistics().size());
        assertEquals(5, result.getMuscleStatistics().size());
        for (MuscleStatistic statistic : result.getMuscleStatistics()) {
            assertFalse(statistic.hasData());
        }
    }

    @Test
    public void calculate_keepsMusclesInBodyDisplayOrder() {
        StatisticsSummary result = StatisticsCalculator.calculate(
                Collections.singletonList(SQUAT), Collections.emptyList());
        List<String> names = result.getMuscleStatistics().stream()
                .map(MuscleStatistic::getMuscleGroup)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("Dos", "Pectoraux", "Bras", "Tronc", "Jambes"), names);
    }

    private static ExerciseSession session(String exerciseId, int score) {
        return new ExerciseSession(exerciseId, 1_700_000_000_000L, score);
    }

    private static MuscleStatistic muscle(StatisticsSummary summary, String name) {
        return summary.getMuscleStatistics().stream()
                .filter(statistic -> statistic.getMuscleGroup().equals(name))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }
}

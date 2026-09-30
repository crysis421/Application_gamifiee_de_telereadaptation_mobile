package com.uphf.saes5;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StatisticsCalculator {
    private StatisticsCalculator() {}

    public static StatisticsSummary calculate(List<StatisticsExercise> exercises,
                                               List<ExerciseSession> sessions) {
        Map<String, StatisticsExercise> exerciseById = new LinkedHashMap<>();
        for (StatisticsExercise exercise : exercises) {
            exerciseById.put(exercise.getId(), exercise);
        }

        Map<String, List<Integer>> scoresByExercise = new LinkedHashMap<>();
        Map<String, List<Integer>> scoresByMuscle = new LinkedHashMap<>();
        for (String group : MuscleGroups.ALL) {
            scoresByMuscle.put(group, new ArrayList<>());
        }

        int validSessionCount = 0;
        int perfectSessionCount = 0;
        for (ExerciseSession session : sessions) {
            StatisticsExercise exercise = exerciseById.get(session.getExerciseId());
            if (exercise == null) continue;

            int score = session.getQualityScore();
            scoresByExercise.computeIfAbsent(exercise.getId(), ignored -> new ArrayList<>()).add(score);
            List<Integer> muscleScores = scoresByMuscle.get(exercise.getMuscleGroup());
            if (muscleScores != null) muscleScores.add(score);
            validSessionCount++;
            if (score >= 90) perfectSessionCount++;
        }

        List<ExerciseStatistic> exerciseStatistics = new ArrayList<>();
        for (StatisticsExercise exercise : exercises) {
            List<Integer> scores = scoresByExercise.get(exercise.getId());
            if (scores == null || scores.isEmpty()) continue;
            exerciseStatistics.add(new ExerciseStatistic(
                    exercise.getId(),
                    exercise.getName(),
                    exercise.getMuscleGroup(),
                    roundedAverage(scores),
                    roundedPercentage(countPerfect(scores), scores.size()),
                    scores.size()));
        }

        List<MuscleStatistic> muscleStatistics = new ArrayList<>();
        for (String group : MuscleGroups.ALL) {
            List<Integer> scores = scoresByMuscle.get(group);
            boolean hasData = scores != null && !scores.isEmpty();
            muscleStatistics.add(new MuscleStatistic(
                    group,
                    hasData ? roundedAverage(scores) : 0,
                    hasData ? scores.size() : 0,
                    hasData));
        }

        return new StatisticsSummary(
                roundedPercentage(perfectSessionCount, validSessionCount),
                exerciseStatistics,
                muscleStatistics);
    }

    private static int countPerfect(List<Integer> scores) {
        int count = 0;
        for (int score : scores) if (score >= 90) count++;
        return count;
    }

    private static int roundedAverage(List<Integer> scores) {
        long total = 0;
        for (int score : scores) total += score;
        return Math.round((float) total / scores.size());
    }

    private static int roundedPercentage(int part, int total) {
        if (total == 0) return 0;
        return Math.round((part * 100f) / total);
    }
}

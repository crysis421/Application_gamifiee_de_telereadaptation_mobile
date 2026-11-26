package com.uphf.saes5.domain.statistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProfileCalculator {
    private ProfileCalculator() {}

    public static ProfileSummary calculate(List<StatisticsExercise> exercises,
                                           List<ScoredSession> sessions) {
        Map<String, StatisticsExercise> exerciseById = new LinkedHashMap<>();
        for (StatisticsExercise exercise : exercises) {
            exerciseById.put(exercise.getId(), exercise);
        }

        List<ProfileHistoryEntry> history = new ArrayList<>();
        int starCount = 0;
        for (ScoredSession session : sessions) {
            StatisticsExercise exercise = exerciseById.get(session.getExerciseId());
            if (exercise == null) continue;

            ProfileHistoryEntry entry = new ProfileHistoryEntry(
                    exercise.getName(),
                    exercise.getMuscleGroup(),
                    session.getCompletedAtEpochMillis(),
                    session.getQualityScore());
            history.add(entry);
            if (entry.earnedStar()) starCount++;
        }
        history.sort(Comparator.comparingLong(ProfileHistoryEntry::getCompletedAtEpochMillis).reversed());

        StatisticsSummary statistics = StatisticsCalculator.calculate(exercises, sessions);
        return new ProfileSummary(statistics.getMuscleStatistics(), history, starCount);
    }
}

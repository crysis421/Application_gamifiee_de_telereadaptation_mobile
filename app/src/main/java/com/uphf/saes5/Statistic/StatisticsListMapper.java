package com.uphf.saes5;

import java.util.ArrayList;
import java.util.List;

public final class StatisticsListMapper {
    private StatisticsListMapper() {}

    public static List<StatisticListItem> fromMuscles(List<MuscleStatistic> statistics) {
        List<StatisticListItem> items = new ArrayList<>();
        for (MuscleStatistic statistic : statistics) {
            items.add(new StatisticListItem(
                    statistic.getMuscleGroup(), null, statistic.getAverageScore(), null,
                    statistic.getSessionCount(), statistic.hasData()));
        }
        return items;
    }

    public static List<StatisticListItem> fromExercises(List<ExerciseStatistic> statistics) {
        List<StatisticListItem> items = new ArrayList<>();
        for (ExerciseStatistic statistic : statistics) {
            items.add(new StatisticListItem(
                    statistic.getExerciseName(), statistic.getMuscleGroup(),
                    statistic.getAverageScore(), statistic.getPerfectPercentage(),
                    statistic.getSessionCount(), true));
        }
        return items;
    }
}

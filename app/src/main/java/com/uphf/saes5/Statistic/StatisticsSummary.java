package com.uphf.saes5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StatisticsSummary {
    private final int globalPerfectPercentage;
    private final List<ExerciseStatistic> exerciseStatistics;
    private final List<MuscleStatistic> muscleStatistics;

    public StatisticsSummary(int globalPerfectPercentage,
                             List<ExerciseStatistic> exerciseStatistics,
                             List<MuscleStatistic> muscleStatistics) {
        this.globalPerfectPercentage = globalPerfectPercentage;
        this.exerciseStatistics = Collections.unmodifiableList(new ArrayList<>(exerciseStatistics));
        this.muscleStatistics = Collections.unmodifiableList(new ArrayList<>(muscleStatistics));
    }

    public int getGlobalPerfectPercentage() { return globalPerfectPercentage; }
    public List<ExerciseStatistic> getExerciseStatistics() { return exerciseStatistics; }
    public List<MuscleStatistic> getMuscleStatistics() { return muscleStatistics; }
}

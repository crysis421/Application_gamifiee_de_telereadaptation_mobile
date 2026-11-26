package com.uphf.saes5.ui.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.uphf.saes5.domain.statistics.ExerciseStatistic;
import com.uphf.saes5.domain.statistics.StatisticsSummary;

public final class StatisticsScreenState {
    public enum DisplayMode { MUSCLES, EXERCISES }

    private final StatisticsSummary summary;
    private final DisplayMode displayMode;
    private final String selectedMuscle;
    private final List<ExerciseStatistic> visibleExerciseStatistics;

    public StatisticsScreenState(StatisticsSummary summary,
                                 DisplayMode displayMode,
                                 String selectedMuscle,
                                 List<ExerciseStatistic> visibleExerciseStatistics) {
        this.summary = summary;
        this.displayMode = displayMode;
        this.selectedMuscle = selectedMuscle;
        this.visibleExerciseStatistics = Collections.unmodifiableList(
                new ArrayList<>(visibleExerciseStatistics));
    }

    public StatisticsSummary getSummary() { return summary; }
    public DisplayMode getDisplayMode() { return displayMode; }
    public String getSelectedMuscle() { return selectedMuscle; }
    public List<ExerciseStatistic> getVisibleExerciseStatistics() {
        return visibleExerciseStatistics;
    }
}

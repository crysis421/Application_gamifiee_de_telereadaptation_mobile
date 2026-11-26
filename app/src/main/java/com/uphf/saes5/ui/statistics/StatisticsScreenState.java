package com.uphf.saes5.ui.statistics;

import androidx.annotation.Nullable;

import com.uphf.saes5.domain.statistics.ExerciseStatistic;
import com.uphf.saes5.domain.statistics.StatisticsSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ce que l'écran Statistiques affiche, calculé par {@link StatisticsViewModel}. Immuable. */
public final class StatisticsScreenState {

    public enum DisplayMode { MUSCLES, EXERCISES }

    /** Titre de la liste, selon le mode et la sélection d'un muscle. */
    public enum Heading { MUSCLES_ALL, MUSCLE_SELECTED, EXERCISES_ALL, EXERCISES_FILTERED }

    private final StatisticsSummary summary;
    private final DisplayMode displayMode;
    @Nullable private final String selectedMuscle;
    private final List<ExerciseStatistic> visibleExerciseStatistics;
    private final List<StatisticListItem> items;
    private final Heading heading;
    private final boolean statisticsTabVisible;

    public StatisticsScreenState(StatisticsSummary summary,
                                 DisplayMode displayMode,
                                 @Nullable String selectedMuscle,
                                 List<ExerciseStatistic> visibleExerciseStatistics,
                                 List<StatisticListItem> items,
                                 Heading heading,
                                 boolean statisticsTabVisible) {
        this.summary = summary;
        this.displayMode = displayMode;
        this.selectedMuscle = selectedMuscle;
        this.visibleExerciseStatistics = Collections.unmodifiableList(
                new ArrayList<>(visibleExerciseStatistics));
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.heading = heading;
        this.statisticsTabVisible = statisticsTabVisible;
    }

    public StatisticsSummary getSummary() { return summary; }
    public DisplayMode getDisplayMode() { return displayMode; }
    /** Le muscle touché sur le corps, ou {@code null} : pas de filtre. */
    @Nullable public String getSelectedMuscle() { return selectedMuscle; }
    public List<ExerciseStatistic> getVisibleExerciseStatistics() {
        return visibleExerciseStatistics;
    }
    /** Les lignes de la liste, prêtes pour l'adapter. */
    public List<StatisticListItem> getItems() { return items; }
    public Heading getHeading() { return heading; }
    public boolean isStatisticsTabVisible() { return statisticsTabVisible; }
}

package com.uphf.saes5.ui.statistics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import com.uphf.saes5.data.session.StatisticsRepository;
import com.uphf.saes5.domain.statistics.ExerciseStatistic;
import com.uphf.saes5.domain.statistics.ScoredSession;
import com.uphf.saes5.domain.statistics.StatisticsCalculator;
import com.uphf.saes5.domain.statistics.StatisticsExercise;
import com.uphf.saes5.domain.statistics.StatisticsSummary;

public class StatisticsViewModel extends ViewModel {
    private final MutableLiveData<StatisticsScreenState> state = new MutableLiveData<>();
    private StatisticsSummary summary;
    private StatisticsScreenState.DisplayMode displayMode = StatisticsScreenState.DisplayMode.MUSCLES;
    private String selectedMuscle;

    public StatisticsViewModel() {
        this(StatisticsRepository.getExercises(), StatisticsRepository.getAll());
    }

    /** Données fournies directement : sert aux tests, qui n'ont pas de journal sur disque. */
    StatisticsViewModel(List<StatisticsExercise> exercises, List<ScoredSession> sessions) {
        summary = StatisticsCalculator.calculate(exercises, sessions);
        publishState();
    }

    /** Relit le journal : une séance a pu se terminer depuis l'ouverture de l'écran. */
    public void refresh() {
        refresh(StatisticsRepository.getExercises(), StatisticsRepository.getAll());
    }

    void refresh(List<StatisticsExercise> exercises, List<ScoredSession> sessions) {
        summary = StatisticsCalculator.calculate(exercises, sessions);
        publishState();
    }

    public LiveData<StatisticsScreenState> getState() {
        return state;
    }

    public void selectMuscle(String muscleGroup) {
        selectedMuscle = muscleGroup != null && muscleGroup.equals(selectedMuscle)
                ? null
                : muscleGroup;
        publishState();
    }

    public void showMuscleMode() {
        displayMode = StatisticsScreenState.DisplayMode.MUSCLES;
        publishState();
    }

    public void showExerciseMode() {
        displayMode = StatisticsScreenState.DisplayMode.EXERCISES;
        publishState();
    }

    private void publishState() {
        List<ExerciseStatistic> visibleExercises = new ArrayList<>();
        for (ExerciseStatistic statistic : summary.getExerciseStatistics()) {
            if (selectedMuscle == null || statistic.getMuscleGroup().equals(selectedMuscle)) {
                visibleExercises.add(statistic);
            }
        }
        state.setValue(new StatisticsScreenState(
                summary, displayMode, selectedMuscle, visibleExercises));
    }
}

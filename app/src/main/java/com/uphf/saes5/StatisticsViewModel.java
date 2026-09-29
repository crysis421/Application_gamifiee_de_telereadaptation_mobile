package com.uphf.saes5;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class StatisticsViewModel extends ViewModel {
    private final MutableLiveData<StatisticsScreenState> state = new MutableLiveData<>();
    private final StatisticsSummary summary;
    private StatisticsScreenState.DisplayMode displayMode = StatisticsScreenState.DisplayMode.MUSCLES;
    private String selectedMuscle;

    public StatisticsViewModel() {
        summary = StatisticsCalculator.calculate(
                ExerciseRepository.getAll(), StatisticsRepository.getAll());
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

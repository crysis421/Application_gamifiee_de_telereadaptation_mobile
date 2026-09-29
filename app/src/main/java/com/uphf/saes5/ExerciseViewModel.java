package com.uphf.saes5;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ExerciseViewModel extends ViewModel {
    private final MutableLiveData<List<Exercise>> filteredExercises = new MutableLiveData<>();

    private String muscle = "";
    private String difficulty = "";
    private Integer maxDuration = null;
    private String equipment = "";

    public ExerciseViewModel() {
        applyFilters();
    }

    public LiveData<List<Exercise>> getExercises() {
        return filteredExercises;
    }

    public void filter(String muscle, String difficulty, Integer maxDuration, String equipment) {
        this.muscle = muscle;
        this.difficulty = difficulty;
        this.maxDuration = maxDuration;
        this.equipment = equipment;
        applyFilters();
    }

    public void reset() {
        filter("", "", null, "");
    }

    public void refresh() {
        applyFilters();
    }

    private void applyFilters() {
        List<Exercise> result = new ArrayList<>();
        for (Exercise exercise : ExerciseRepository.getAll()) {
            if (!muscle.isEmpty() && !exercise.getMuscleGroup().equalsIgnoreCase(muscle)) continue;
            if (!difficulty.isEmpty() && !exercise.getDifficulty().equalsIgnoreCase(difficulty)) continue;
            if (maxDuration != null && exercise.getDurationMinutes() > maxDuration) continue;
            if (!equipment.isEmpty() && !exercise.getEquipment().equalsIgnoreCase(equipment)) continue;
            result.add(exercise);
        }
        filteredExercises.setValue(result);
    }
}

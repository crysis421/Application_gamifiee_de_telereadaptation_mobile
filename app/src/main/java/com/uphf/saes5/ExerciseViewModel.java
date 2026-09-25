package com.uphf.saes5;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ExerciseViewModel extends ViewModel {
    private final List<Exercise> allExercises = ExerciseRepository.getAll();
    private final MutableLiveData<List<Exercise>> filteredExercises = new MutableLiveData<>();

    public ExerciseViewModel() {
        filteredExercises.setValue(new ArrayList<>(allExercises));
    }

    public LiveData<List<Exercise>> getExercises() {
        return filteredExercises;
    }

    public void filter(String muscle, String difficulty, Integer maxDuration, String equipment) {
        List<Exercise> result = new ArrayList<>();
        for (Exercise exercise : allExercises) {
            if (!muscle.isEmpty() && !exercise.getMuscleGroup().equalsIgnoreCase(muscle)) continue;
            if (!difficulty.isEmpty() && !exercise.getDifficulty().equalsIgnoreCase(difficulty)) continue;
            if (maxDuration != null && exercise.getDurationMinutes() > maxDuration) continue;
            if (!equipment.isEmpty() && !exercise.getEquipment().equalsIgnoreCase(equipment)) continue;
            result.add(exercise);
        }
        filteredExercises.setValue(result);
    }

    public void reset() {
        filteredExercises.setValue(new ArrayList<>(allExercises));
    }
}

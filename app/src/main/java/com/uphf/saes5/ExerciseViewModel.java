package com.uphf.saes5;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExerciseViewModel extends ViewModel {
    private final MutableLiveData<List<Exercise>> exercises = new MutableLiveData<>();
    private final List<Exercise> all = new ArrayList<>();

    public ExerciseViewModel() {
        all.addAll(ExerciseRepository.getAllExercises());
        exercises.setValue(new ArrayList<>(all));
    }
    public LiveData<List<Exercise>> getExercises() {
        return exercises;
    }

    public void filter(String muscleGroup, String difficulty, Integer maxDuration, String equipment) {
        List<Exercise> filtered = new ArrayList<>();
        for (Exercise e : all) {
            if (muscleGroup != null && !muscleGroup.isEmpty() && !e.getMuscleGroup().equalsIgnoreCase(muscleGroup)) continue;
            if (difficulty != null && !difficulty.isEmpty() && !e.getDifficulty().equalsIgnoreCase(difficulty)) continue;
            if (maxDuration != null && maxDuration > 0 && e.getDurationMinutes() > maxDuration) continue;
            if (equipment != null && !equipment.isEmpty() && !e.getEquipment().equalsIgnoreCase(equipment)) continue;
            filtered.add(e);
        }
        exercises.setValue(filtered);
    }

    public void resetFilters() {
        exercises.setValue(new ArrayList<>(all));
    }
}
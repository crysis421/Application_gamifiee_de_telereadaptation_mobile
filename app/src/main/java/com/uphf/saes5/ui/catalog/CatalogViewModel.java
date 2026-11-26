package com.uphf.saes5.ui.catalog;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.ui.common.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue d'exercices : filtres (muscle, difficulté, durée maximale, équipement) et
 * suppression. Les filtres survivent à la rotation avec le ViewModel.
 */
public final class CatalogViewModel extends ViewModel {

    /** Événements ponctuels du catalogue. */
    public enum CatalogEvent { DELETED }

    private final ExerciseRepository exercises;
    private final GameSettingsRepository gameSettings;
    private final MutableLiveData<CatalogUiState> state = new MutableLiveData<>();
    private final MutableLiveData<Event<CatalogEvent>> events = new MutableLiveData<>();

    private String muscle = "";
    private String difficulty = "";
    @Nullable private Integer maxDuration;
    private String equipment = "";

    public CatalogViewModel(ExerciseRepository exercises, GameSettingsRepository gameSettings) {
        this.exercises = exercises;
        this.gameSettings = gameSettings;
        applyFilters();
    }

    public LiveData<CatalogUiState> getState() {
        return state;
    }

    public LiveData<Event<CatalogEvent>> getEvents() {
        return events;
    }

    /** Un filtre vide (ou {@code null} pour la durée) ne filtre rien. */
    public void filter(String muscle, String difficulty, @Nullable Integer maxDuration,
                       String equipment) {
        this.muscle = muscle;
        this.difficulty = difficulty;
        this.maxDuration = maxDuration;
        this.equipment = equipment;
        applyFilters();
    }

    public void reset() {
        filter("", "", null, "");
    }

    /** Reflète les créations, modifications et suppressions faites sur d'autres écrans. */
    public void refresh() {
        applyFilters();
    }

    /** Retire l'exercice du catalogue (après confirmation par l'écran). */
    public void delete(String exerciseId) {
        exercises.remove(exerciseId);
        applyFilters();
        events.setValue(new Event<>(CatalogEvent.DELETED));
    }

    private void applyFilters() {
        List<Exercise> result = new ArrayList<>();
        for (Exercise exercise : exercises.getAll()) {
            if (!muscle.isEmpty() && !exercise.getMuscleGroup().equalsIgnoreCase(muscle)) continue;
            if (!difficulty.isEmpty() && !exercise.getDifficulty().equalsIgnoreCase(difficulty)) continue;
            if (maxDuration != null && exercise.getDurationMinutes() > maxDuration) continue;
            if (!equipment.isEmpty() && !exercise.getEquipment().equalsIgnoreCase(equipment)) continue;
            result.add(exercise);
        }
        state.setValue(new CatalogUiState(result,
                gameSettings.isEnabled(GameFeature.STARS),
                gameSettings.isEnabled(GameFeature.STATISTICS)));
    }
}

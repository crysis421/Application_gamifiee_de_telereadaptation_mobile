package com.uphf.saes5.ui.catalog;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.GameFeature;

/**
 * Fiche d'un exercice du catalogue, avant de le lancer.
 *
 * <p>Rien n'y change pendant l'affichage : l'exercice est lu une fois, sans LiveData.</p>
 */
public final class ExerciseDetailViewModel extends ViewModel {

    @Nullable private final Exercise exercise;
    private final boolean statisticsTabVisible;

    public ExerciseDetailViewModel(String exerciseId, ExerciseRepository exercises,
                                   GameSettingsRepository gameSettings) {
        this.exercise = exercises.findById(exerciseId);
        this.statisticsTabVisible = gameSettings.isEnabled(GameFeature.STATISTICS);
    }

    /** L'exercice, ou {@code null} s'il n'est plus au catalogue. */
    @Nullable
    public Exercise getExercise() {
        return exercise;
    }

    public boolean isStatisticsTabVisible() {
        return statisticsTabVisible;
    }
}

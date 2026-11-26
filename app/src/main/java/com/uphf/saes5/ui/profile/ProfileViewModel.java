package com.uphf.saes5.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.session.StatisticsRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.game.Progression;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.statistics.ProfileCalculator;

/**
 * Profil : rangs par muscle et étoiles par exercice (US-4.5), puis historique des séances.
 *
 * <p>Tout est calculé à partir du journal des séances.</p>
 */
public final class ProfileViewModel extends ViewModel {

    private final ExerciseRepository exercises;
    private final SessionRepository sessions;
    private final StatisticsRepository statistics;
    private final GameSettingsRepository gameSettings;
    private final MutableLiveData<ProfileUiState> state = new MutableLiveData<>();

    public ProfileViewModel(ExerciseRepository exercises, SessionRepository sessions,
                            StatisticsRepository statistics, GameSettingsRepository gameSettings) {
        this.exercises = exercises;
        this.sessions = sessions;
        this.statistics = statistics;
        this.gameSettings = gameSettings;
    }

    public LiveData<ProfileUiState> getState() {
        return state;
    }

    /** Relit le journal : l'onglet est réutilisé, une séance a pu se terminer entre-temps. */
    public void refresh() {
        boolean progressionVisible = gameSettings.isEnabled(GameFeature.STARS);
        state.setValue(new ProfileUiState(
                progressionVisible,
                progressionVisible
                        ? Progression.calculate(exercises.getAll(), sessions.readAll())
                        : null,
                ProfileCalculator.calculate(statistics.getExercises(), statistics.getSessions()),
                gameSettings.isEnabled(GameFeature.STATISTICS)));
    }
}

package com.uphf.saes5.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.activity.StepSource;
import com.uphf.saes5.data.home.DailyExerciseRepository;
import com.uphf.saes5.data.home.FavoritesRepository;
import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.settings.ConsentRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.statistics.ActivityCounter;

import java.time.Clock;

/**
 * Accueil : exercice du jour (US-4.2), pas du jour (US-5.1), exercices réalisés (US-5.2), et
 * raccourcis, chacun masquable par le chercheur (US-4.1, US-5.4).
 *
 * <p>Tout ce qui est affiché se décide ici ; l'écran ne fait qu'appliquer l'état. Un
 * élément désactivé disparaît entièrement : la condition de test doit être réellement
 * comparable, sans emplacement vide ni onglet mort.</p>
 */
public final class HomeViewModel extends ViewModel {

    private final GameSettingsRepository gameSettings;
    private final ConsentRepository consent;
    private final DailyExerciseRepository dailyExercise;
    private final FavoritesRepository favorites;
    private final SessionRepository sessions;
    private final StepSource stepSource;
    private final Clock clock;

    private final MutableLiveData<HomeUiState> state = new MutableLiveData<>();

    @Nullable
    private Exercise exerciseOfTheDay;
    /** Carte repliée par défaut ; l'état survit à la rotation avec le ViewModel. */
    private boolean detailsExpanded;
    private HomeUiState.StepsStatus stepsStatus = HomeUiState.StepsStatus.LOADING;
    private int steps;

    public HomeViewModel(GameSettingsRepository gameSettings, ConsentRepository consent,
                         DailyExerciseRepository dailyExercise, FavoritesRepository favorites,
                         SessionRepository sessions, StepSource stepSource, Clock clock) {
        this.gameSettings = gameSettings;
        this.consent = consent;
        this.dailyExercise = dailyExercise;
        this.favorites = favorites;
        this.sessions = sessions;
        this.stepSource = stepSource;
        this.clock = clock;
    }

    public LiveData<HomeUiState> getState() {
        return state;
    }

    /** US-9.1 : rien ne démarre, caméra comprise, avant l'accord éclairé du participant. */
    public boolean isConsentRequired() {
        return consent.isRequired();
    }

    /**
     * Relit tout : à chaque retour sur l'accueil, les réglages, le catalogue et les séances
     * ont pu changer sur un autre écran.
     */
    public void refresh() {
        exerciseOfTheDay = gameSettings.isEnabled(GameFeature.DAILY_EXERCISE)
                ? dailyExercise.exerciseOfTheDay()
                : null;
        publish();
        refreshSteps();
    }

    /** Relit les pas : au retour, ou une fois la permission accordée. */
    public void refreshSteps() {
        if (!gameSettings.isEnabled(GameFeature.HOME_STEPS)) {
            // Bloc masqué (US-5.4) : inutile de lire les pas, ni d'inviter à la permission.
            stepsStatus = HomeUiState.StepsStatus.HIDDEN;
            publish();
            return;
        }
        stepSource.readToday(new StepSource.Callback() {
            @Override
            public void onSteps(int count) {
                stepsStatus = HomeUiState.StepsStatus.VALUE;
                steps = count;
                publish();
            }

            @Override
            public void onUnavailable(@NonNull StepSource.Availability reason) {
                stepsStatus = reason == StepSource.Availability.PERMISSION_MISSING
                        ? HomeUiState.StepsStatus.PERMISSION_MISSING
                        : HomeUiState.StepsStatus.UNSUPPORTED;
                publish();
            }
        });
    }

    public void toggleFavorite() {
        if (exerciseOfTheDay == null) return;
        favorites.toggleFavorite(exerciseOfTheDay.getId());
        publish();
    }

    public void toggleDetails() {
        detailsExpanded = !detailsExpanded;
        publish();
    }

    private void publish() {
        boolean dailyEnabled = gameSettings.isEnabled(GameFeature.DAILY_EXERCISE);
        boolean hasExercise = exerciseOfTheDay != null;
        ActivityCounter.Counts done = ActivityCounter.count(sessions.readAll(), clock.millis());
        state.setValue(new HomeUiState(
                gameSettings.isEnabled(GameFeature.STATISTICS),
                gameSettings.isEnabled(GameFeature.HOME_CATALOG),
                hasExercise,
                // Élément désactivé : ni carte, ni message « catalogue vide ».
                dailyEnabled && !hasExercise,
                exerciseOfTheDay,
                gameSettings.isEnabled(GameFeature.FAVORITES),
                hasExercise && favorites.isFavorite(exerciseOfTheDay.getId()),
                dailyExercise.isCompletedToday(),
                detailsExpanded,
                gameSettings.isEnabled(GameFeature.HOME_EXERCISES_DONE),
                done.getTotal(),
                done.getLastSevenDays(),
                stepsStatus,
                steps));
    }
}

package com.uphf.saes5.ui.summary;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.home.FavoritesRepository;
import com.uphf.saes5.data.session.SessionRecorder;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.SessionResult;
import com.uphf.saes5.ui.common.Event;

/**
 * Récapitulatif de fin d'exercice : statistiques de la séance, mise en favori, et journal.
 *
 * <p>C'est ici que la séance terminée est journalisée pour la recherche (US-7.1), avec les
 * réglages actifs et le nom de la condition de test : sans eux, une mesure ne peut pas être
 * rattachée à la configuration qui l'a produite.</p>
 */
public final class SummaryViewModel extends ViewModel {

    private final SummaryArgs args;
    private final FavoritesRepository favorites;
    private final GameSettingsRepository gameSettings;
    private final SessionRecorder recorder;
    @Nullable private final Exercise exercise;
    private final MutableLiveData<SummaryUiState> state = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> favoriteChanges = new MutableLiveData<>();
    private boolean recorded;

    public SummaryViewModel(SummaryArgs args, ExerciseRepository exercises,
                            FavoritesRepository favorites, GameSettingsRepository gameSettings,
                            SessionRecorder recorder) {
        this.args = args;
        this.favorites = favorites;
        this.gameSettings = gameSettings;
        this.recorder = recorder;
        this.exercise = exercises.findById(args.getExerciseId());
        publish();
    }

    public LiveData<SummaryUiState> getState() {
        return state;
    }

    /** Nouvel état favori après un toucher du cœur, pour le message de confirmation. */
    public LiveData<Event<Boolean>> getFavoriteChanges() {
        return favoriteChanges;
    }

    /**
     * Journalise la séance, une seule fois.
     *
     * <p>Une rotation garde ce ViewModel ; un écran restauré après l'arrêt du processus en
     * recrée un, mais avec {@code firstCreation} faux : la séance est déjà dans le journal.</p>
     *
     * @param firstCreation vrai à la toute première ouverture de l'écran
     */
    public void onCreated(boolean firstCreation) {
        if (!firstCreation || recorded || exercise == null) return;
        recorded = true;
        // Une séance manuelle garde un score « indisponible » plutôt que zéro, pour ne pas se
        // confondre avec une comparaison réellement mesurée.
        recorder.record(exercise, args.getValidatedReps(), args.getFailedReps(),
                args.getDurationMillis(), args.getSimilarityScore(),
                args.getAverageLatencyMillis(), args.getArcadePoints(), false);
    }

    public void toggleFavorite() {
        if (exercise == null) return;
        favoriteChanges.setValue(new Event<>(favorites.toggleFavorite(exercise.getId())));
        publish();
    }

    private void publish() {
        state.setValue(new SummaryUiState(
                exercise,
                new SessionResult(args.getExerciseId(), args.getValidatedReps(),
                        args.getFailedReps(), args.getDurationMillis()),
                args.getSimilarityScore(),
                args.getArcadePoints(),
                gameSettings.isEnabled(GameFeature.FAVORITES),
                exercise != null && favorites.isFavorite(exercise.getId())));
    }
}

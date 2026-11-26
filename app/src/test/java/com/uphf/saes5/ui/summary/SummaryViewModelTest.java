package com.uphf.saes5.ui.summary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.data.home.FavoritesRepository;
import com.uphf.saes5.data.session.SessionRecorder;
import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.settings.AnalysisSettingsRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.testing.FileAssetSource;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.io.File;
import java.time.Clock;

/** Récapitulatif : la séance est journalisée exactement une fois, même si l'écran est recréé. */
public class SummaryViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private GameSettingsRepository gameSettings;
    private ExerciseRepository exercises;
    private FavoritesRepository favorites;
    private SessionRepository sessions;
    private SessionRecorder recorder;

    @Before
    public void setUp() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        gameSettings = new GameSettingsRepository(store);
        exercises = new ExerciseRepository(store, new MovementRepository(
                new File(folder.getRoot(), "movements"), FileAssetSource.appAssets()));
        exercises.initialize();
        favorites = new FavoritesRepository(store);
        sessions = new SessionRepository(new File(folder.getRoot(), "sessions.json"),
                new File(folder.getRoot(), "exports"), Clock.systemUTC());
        recorder = new SessionRecorder(sessions, gameSettings,
                new AnalysisSettingsRepository(store), Clock.systemUTC());
    }

    private SummaryViewModel viewModel(String exerciseId, int points) {
        SummaryArgs args = new SummaryArgs(exerciseId, 12, 2, 90_000L, 85, 40, points);
        return new SummaryViewModel(args, exercises, favorites, gameSettings, recorder);
    }

    @Test
    public void theSession_isRecordedOnce_evenIfTheScreenIsRecreated() {
        SummaryViewModel viewModel = viewModel("base_squat", 900);

        viewModel.onCreated(true);
        viewModel.onCreated(true);

        assertEquals(1, sessions.count());
        assertEquals(85, sessions.readAll().get(0).getSimilarityScore());
    }

    @Test
    public void aScreenRestoredAfterTheProcessDied_doesNotRecordAgain() {
        viewModel("base_squat", 900).onCreated(false);

        assertEquals(0, sessions.count());
    }

    @Test
    public void anExerciseNoLongerInTheCatalogue_closesTheScreen_withoutRecording() {
        SummaryViewModel viewModel = viewModel("supprimé", 900);
        viewModel.onCreated(true);

        assertNull(viewModel.getState().getValue().getExercise());
        assertEquals(0, sessions.count());
    }

    @Test
    public void theGoal_isReached_whenTheTargetIsMet() {
        SummaryUiState state = viewModel("base_squat", 900).getState().getValue();

        assertTrue(state.isGoalReached());
        assertTrue(state.isPointsVisible());
        assertEquals(14, state.getResult().getTotalAttempts());
    }

    @Test
    public void favorites_andPoints_followTheirSwitches() {
        gameSettings.setEnabled(GameFeature.FAVORITES, false);

        SummaryUiState state =
                viewModel("base_squat", SessionEntry.POINTS_UNAVAILABLE).getState().getValue();

        assertFalse(state.isFavoriteVisible());
        assertFalse(state.isPointsVisible());
    }

    @Test
    public void toggleFavorite_reportsTheNewState() {
        SummaryViewModel viewModel = viewModel("base_squat", 900);

        viewModel.toggleFavorite();

        assertTrue(viewModel.getState().getValue().isFavorite());
        assertEquals(Boolean.TRUE, viewModel.getFavoriteChanges().getValue().getContentIfNotHandled());
    }
}

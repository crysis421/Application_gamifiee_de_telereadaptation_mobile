package com.uphf.saes5.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.activity.StepSource;
import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.data.home.DailyExerciseRepository;
import com.uphf.saes5.data.home.FavoritesRepository;
import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.settings.ConsentRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.testing.FileAssetSource;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Accueil : ce qui est affiché selon le catalogue, les séances et les réglages. */
public class HomeViewModelTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private InMemoryPreferenceStore store;
    private GameSettingsRepository gameSettings;
    private ConsentRepository consent;
    private ExerciseRepository exercises;
    private SessionRepository sessions;
    private FakeSteps steps;

    /** Pas du jour factices : répond tout de suite, et compte les lectures. */
    private static final class FakeSteps implements StepSource {
        int reads;

        @Override
        public void readToday(@NonNull Callback callback) {
            reads++;
            callback.onSteps(1234);
        }
    }

    @Before
    public void setUp() {
        store = new InMemoryPreferenceStore();
        gameSettings = new GameSettingsRepository(store);
        consent = new ConsentRepository(store, clock);
        consent.accept();
        exercises = new ExerciseRepository(store, new MovementRepository(
                new File(folder.getRoot(), "movements"), FileAssetSource.appAssets()));
        exercises.initialize();
        sessions = new SessionRepository(new File(folder.getRoot(), "sessions.json"),
                new File(folder.getRoot(), "exports"), clock);
        steps = new FakeSteps();
    }

    private HomeViewModel viewModel() {
        HomeViewModel viewModel = new HomeViewModel(gameSettings, consent,
                new DailyExerciseRepository(store, exercises, clock),
                new FavoritesRepository(store), sessions, steps, clock);
        viewModel.refresh();
        return viewModel;
    }

    private static HomeUiState state(HomeViewModel viewModel) {
        return viewModel.getState().getValue();
    }

    @Test
    public void everythingIsShown_whenNothingIsDisabled() {
        HomeUiState state = state(viewModel());

        assertTrue(state.isDailyCardVisible());
        assertNotNull(state.getDailyExercise());
        assertFalse(state.isEmptyCatalogVisible());
        assertTrue(state.isFavoriteVisible());
        assertTrue(state.isStatisticsVisible());
        assertTrue(state.isCatalogButtonVisible());
        assertEquals(HomeUiState.StepsStatus.VALUE, state.getStepsStatus());
        assertEquals(1234, state.getSteps());
    }

    @Test
    public void disabledDailyExercise_hidesTheCard_andTheEmptyCatalogueMessage() {
        gameSettings.setEnabled(GameFeature.DAILY_EXERCISE, false);

        HomeUiState state = state(viewModel());

        assertFalse(state.isDailyCardVisible());
        assertFalse(state.isEmptyCatalogVisible());
    }

    @Test
    public void anEmptyCatalogue_saysSo() {
        for (Exercise exercise : exercises.getAll()) exercises.remove(exercise.getId());

        HomeUiState state = state(viewModel());

        assertFalse(state.isDailyCardVisible());
        assertTrue(state.isEmptyCatalogVisible());
    }

    @Test
    public void disabledElements_areHidden() {
        gameSettings.setEnabled(GameFeature.FAVORITES, false);
        gameSettings.setEnabled(GameFeature.STATISTICS, false);
        gameSettings.setEnabled(GameFeature.HOME_CATALOG, false);
        gameSettings.setEnabled(GameFeature.HOME_EXERCISES_DONE, false);

        HomeUiState state = state(viewModel());

        assertFalse(state.isFavoriteVisible());
        assertFalse(state.isStatisticsVisible());
        assertFalse(state.isCatalogButtonVisible());
        assertFalse(state.isExercisesDoneTileVisible());
    }

    @Test
    public void hiddenStepsTile_doesNotReadTheSteps() {
        gameSettings.setEnabled(GameFeature.HOME_STEPS, false);

        HomeUiState state = state(viewModel());

        assertEquals(HomeUiState.StepsStatus.HIDDEN, state.getStepsStatus());
        assertEquals(0, steps.reads);
    }

    @Test
    public void favorite_togglesOnAndOff() {
        HomeViewModel viewModel = viewModel();

        viewModel.toggleFavorite();
        assertTrue(state(viewModel).isFavorite());

        viewModel.toggleFavorite();
        assertFalse(state(viewModel).isFavorite());
    }

    @Test
    public void details_areCollapsedFirst_thenExpanded() {
        HomeViewModel viewModel = viewModel();
        assertFalse(state(viewModel).isDetailsExpanded());

        viewModel.toggleDetails();

        assertTrue(state(viewModel).isDetailsExpanded());
    }

    @Test
    public void consent_isRequired_untilAccepted() {
        consent.withdraw();

        assertTrue(viewModel().isConsentRequired());
    }

    @Test
    public void onlySessionsWithAValidatedRepetition_countAsDone() {
        sessions.record(new SessionEntry(NOW.toEpochMilli(), "base_squat", "Squat", 3, 0, 12,
                60_000L, SessionEntry.SCORE_UNAVAILABLE, "", ""));
        sessions.record(new SessionEntry(NOW.toEpochMilli(), "base_squat", "Squat", 0, 2, 12,
                60_000L, SessionEntry.SCORE_UNAVAILABLE, "", ""));

        HomeUiState state = state(viewModel());

        assertEquals(1, state.getExercisesDoneTotal());
        assertEquals(1, state.getExercisesDoneLastSevenDays());
    }
}

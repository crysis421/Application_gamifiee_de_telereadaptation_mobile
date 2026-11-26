package com.uphf.saes5.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.session.StatisticsRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.game.Progression;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.testing.FileAssetSource;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.io.File;
import java.time.Clock;

/** Profil : progression (rangs et étoiles) et historique, tirés du journal des séances. */
public class ProfileViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private GameSettingsRepository gameSettings;
    private ExerciseRepository exercises;
    private SessionRepository sessions;

    @Before
    public void setUp() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        gameSettings = new GameSettingsRepository(store);
        exercises = new ExerciseRepository(store, new MovementRepository(
                new File(folder.getRoot(), "movements"), FileAssetSource.appAssets()));
        exercises.initialize();
        sessions = new SessionRepository(new File(folder.getRoot(), "sessions.json"),
                new File(folder.getRoot(), "exports"), Clock.systemUTC());
        sessions.record(new SessionEntry(1_000L, "base_squat", "Squat", 10, 0, 12, 60_000L,
                90, "", "", "", SessionEntry.LATENCY_UNAVAILABLE, 800, false));
    }

    private ProfileUiState state() {
        ProfileViewModel viewModel = new ProfileViewModel(exercises, sessions,
                new StatisticsRepository(exercises, sessions), gameSettings);
        viewModel.refresh();
        return viewModel.getState().getValue();
    }

    @Test
    public void progression_isComputedFromTheJournal() {
        ProfileUiState state = state();

        assertTrue(state.isProgressionVisible());
        assertNotNull(state.getProgression());
        assertEquals(Progression.calculate(exercises.getAll(), sessions.readAll()).getTotalStars(),
                state.getProgression().getTotalStars());
        assertEquals(1, state.getHistory().getHistory().size());
    }

    @Test
    public void disabledStars_hideTheProgression_butKeepTheHistory() {
        gameSettings.setEnabled(GameFeature.STARS, false);

        ProfileUiState state = state();

        assertFalse(state.isProgressionVisible());
        assertNull(state.getProgression());
        assertEquals(1, state.getHistory().getHistory().size());
    }

    @Test
    public void disabledStatistics_hideTheStatisticsTab() {
        gameSettings.setEnabled(GameFeature.STATISTICS, false);

        assertFalse(state().isStatisticsTabVisible());
    }
}

package com.uphf.saes5.ui.catalog;

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
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.testing.FileAssetSource;
import com.uphf.saes5.testing.InMemoryPreferenceStore;
import com.uphf.saes5.ui.common.Event;

import java.io.File;

/** Catalogue : filtres, suppression, étoiles. */
public class CatalogViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private GameSettingsRepository gameSettings;
    private ExerciseRepository exercises;

    @Before
    public void setUp() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        gameSettings = new GameSettingsRepository(store);
        exercises = new ExerciseRepository(store, new MovementRepository(
                new File(folder.getRoot(), "movements"), FileAssetSource.appAssets()));
        exercises.initialize();
    }

    private CatalogViewModel viewModel() {
        return new CatalogViewModel(exercises, gameSettings);
    }

    @Test
    public void filteringByMuscle_keepsOnlyThatMuscle_andResetShowsEverything() {
        CatalogViewModel viewModel = viewModel();

        viewModel.filter("Jambes", "", null, "");
        for (Exercise exercise : viewModel.getState().getValue().getExercises()) {
            assertEquals("Jambes", exercise.getMuscleGroup());
        }
        assertEquals(3, viewModel.getState().getValue().getExercises().size());

        viewModel.reset();
        assertEquals(exercises.getAll().size(), viewModel.getState().getValue().getExercises().size());
    }

    @Test
    public void deleting_removesTheExercise_andTellsTheScreenOnce() {
        CatalogViewModel viewModel = viewModel();

        viewModel.delete("base_squat");

        assertNull(exercises.findById("base_squat"));
        for (Exercise exercise : viewModel.getState().getValue().getExercises()) {
            assertFalse("base_squat".equals(exercise.getId()));
        }
        Event<CatalogViewModel.CatalogEvent> event = viewModel.getEvents().getValue();
        assertEquals(CatalogViewModel.CatalogEvent.DELETED, event.getContentIfNotHandled());
        assertNull(event.getContentIfNotHandled());
    }

    @Test
    public void stars_followTheGameSetting() {
        assertTrue(viewModel().getState().getValue().isStarsVisible());

        gameSettings.setEnabled(GameFeature.STARS, false);

        assertFalse(viewModel().getState().getValue().isStarsVisible());
    }
}

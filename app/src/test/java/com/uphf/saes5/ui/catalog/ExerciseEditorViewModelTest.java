package com.uphf.saes5.ui.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.Movement;
import com.uphf.saes5.testing.FileAssetSource;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.io.File;
import java.util.ArrayList;

/** Création et modification d'un exercice : validation du formulaire et modèle obligatoire. */
public class ExerciseEditorViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private ExerciseRepository exercises;
    private MovementRepository movements;

    @Before
    public void setUp() {
        movements = new MovementRepository(
                new File(folder.getRoot(), "movements"), FileAssetSource.appAssets());
        exercises = new ExerciseRepository(new InMemoryPreferenceStore(), movements);
        exercises.initialize();
    }

    private static ExerciseForm form(String name, String duration, String equipment) {
        return new ExerciseForm(name, duration, "10", equipment, "Consigne.", "Bras", "Facile",
                2.4f, 75);
    }

    private void recordModel(String name) throws Exception {
        Movement movement = new Movement();
        movement.name = name;
        movement.frames = new ArrayList<>();
        movements.save(movement);
    }

    private static ExerciseEditorViewModel.EditorEvent lastEvent(ExerciseEditorViewModel viewModel) {
        return viewModel.getEvents().getValue().getContentIfNotHandled();
    }

    @Test
    public void anEmptyName_isRefused() {
        ExerciseEditorViewModel viewModel = new ExerciseEditorViewModel(null, null, exercises, movements);
        int before = exercises.getAll().size();

        viewModel.save(form("  ", "5", ""));

        assertEquals(ExerciseEditorViewModel.EditorEvent.NAME_REQUIRED, lastEvent(viewModel));
        assertEquals(before, exercises.getAll().size());
    }

    @Test
    public void aZeroDuration_isRefused() {
        ExerciseEditorViewModel viewModel = new ExerciseEditorViewModel(null, null, exercises, movements);

        viewModel.save(form("Curl", "0", ""));

        assertEquals(ExerciseEditorViewModel.EditorEvent.DURATION_INVALID, lastEvent(viewModel));
    }

    @Test
    public void creatingWithoutARecordedModel_isRefused() {
        ExerciseEditorViewModel viewModel = new ExerciseEditorViewModel(null, null, exercises, movements);

        viewModel.save(form("Curl", "5", ""));

        assertEquals(ExerciseEditorViewModel.EditorEvent.MODEL_MISSING, lastEvent(viewModel));
        assertFalse(viewModel.getState().getValue().isReferenceAvailable());
    }

    @Test
    public void creatingWithARecordedModel_addsTheExercise() throws Exception {
        ExerciseEditorViewModel viewModel = new ExerciseEditorViewModel(null, null, exercises, movements);
        recordModel(viewModel.getDraftId());
        viewModel.refreshReference();

        viewModel.save(form("Curl", "5", ""));

        assertEquals(ExerciseEditorViewModel.EditorEvent.CREATED, lastEvent(viewModel));
        Exercise created = exercises.findById(viewModel.getDraftId());
        assertNotNull(created);
        assertEquals("Aucun", created.getEquipment());
        assertEquals(2, created.getStars());
        assertEquals(75, created.getSimilarityThreshold());
        assertEquals(viewModel.getDraftId(), created.getReferenceMovementId());
    }

    @Test
    public void editing_keepsTheIdentifier() {
        ExerciseEditorViewModel viewModel =
                new ExerciseEditorViewModel("base_squat", null, exercises, movements);
        assertTrue(viewModel.getState().getValue().isEditing());

        viewModel.save(form("Squat lent", "8", "Chaise"));

        assertEquals(ExerciseEditorViewModel.EditorEvent.UPDATED, lastEvent(viewModel));
        assertEquals("Squat lent", exercises.findById("base_squat").getName());
    }

    @Test
    public void editingAnExerciseNoLongerInTheCatalogue_closesTheForm() {
        ExerciseEditorViewModel viewModel =
                new ExerciseEditorViewModel("supprimé", null, exercises, movements);

        assertEquals(ExerciseEditorViewModel.EditorEvent.NO_LONGER_AVAILABLE, lastEvent(viewModel));
    }

    @Test
    public void aRestoredDraft_keepsItsIdentifier() {
        ExerciseEditorViewModel viewModel =
                new ExerciseEditorViewModel(null, "custom_x", exercises, movements);

        assertEquals("custom_x", viewModel.getDraftId());
        assertEquals("custom_x", viewModel.getState().getValue().getReferenceMovementId());
    }
}

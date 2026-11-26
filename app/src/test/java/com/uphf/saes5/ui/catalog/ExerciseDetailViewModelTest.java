package com.uphf.saes5.ui.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.testing.FileAssetSource;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.io.File;

/** Fiche d'un exercice du catalogue. */
public class ExerciseDetailViewModelTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private ExerciseRepository exercises;
    private final GameSettingsRepository gameSettings =
            new GameSettingsRepository(new InMemoryPreferenceStore());

    @Before
    public void setUp() {
        exercises = new ExerciseRepository(new InMemoryPreferenceStore(), new MovementRepository(
                new File(folder.getRoot(), "movements"), FileAssetSource.appAssets()));
        exercises.initialize();
    }

    @Test
    public void aKnownExercise_isShown() {
        assertEquals("Squat",
                new ExerciseDetailViewModel("base_squat", exercises, gameSettings).getExercise().getName());
    }

    @Test
    public void disabledStatistics_hideTheStatisticsTab() {
        gameSettings.setEnabled(GameFeature.STATISTICS, false);

        assertFalse(new ExerciseDetailViewModel("base_squat", exercises, gameSettings)
                .isStatisticsTabVisible());
    }

    @Test
    public void anExerciseNoLongerInTheCatalogue_isNull() {
        assertNull(new ExerciseDetailViewModel("supprimé", exercises, gameSettings).getExercise());
    }
}

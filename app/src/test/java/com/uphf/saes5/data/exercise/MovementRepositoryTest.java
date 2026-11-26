package com.uphf.saes5.data.exercise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.domain.exercise.BaseExercises;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.Movement;
import com.uphf.saes5.testing.FileAssetSource;

import java.io.File;
import java.util.ArrayList;

/** Modèles de mouvement enregistrés sur le téléphone, et démonstrations livrées. */
public class MovementRepositoryTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private File directory;
    private MovementRepository movements;

    @Before
    public void setUp() {
        directory = new File(folder.getRoot(), "movements");
        movements = new MovementRepository(directory, FileAssetSource.appAssets());
    }

    private static Movement movement(String name, int frames) {
        Movement movement = new Movement();
        movement.name = name;
        movement.sampleIntervalMs = 100;
        movement.durationSec = 6;
        movement.landmarkCount = 33;
        movement.frames = new ArrayList<>();
        for (int i = 0; i < frames; i++) movement.frames.add(new float[66]);
        return movement;
    }

    @Test
    public void saveThenLoad_returnsTheSameMovement() throws Exception {
        movements.save(movement("squat_lent", 25));

        Movement loaded = movements.load("squat_lent");

        assertNotNull(loaded);
        assertEquals("squat_lent", loaded.name);
        assertEquals(25, loaded.frames.size());
        assertTrue(movements.exists("squat_lent"));
    }

    @Test
    public void aNameWithForbiddenCharacters_isSavedAndFoundUnderTheSameFile() throws Exception {
        // Même assainissement qu'avant le remaniement : un modèle déjà enregistré reste trouvé.
        movements.save(movement("a/b?", 1));

        assertTrue(movements.exists("a/b?"));
        assertTrue(new File(directory, "ab.json").isFile());
    }

    @Test
    public void unknownOrBlankName_doesNotExist() {
        assertFalse(movements.exists("absent"));
        assertFalse(movements.exists("  "));
        assertFalse(movements.exists(null));
        assertNull(movements.load("absent"));
    }

    @Test
    public void baseExercise_hasItsDemo_andAnExerciseWithoutOne_hasNone() {
        Exercise squat = BaseExercises.all().get(1);

        Movement demo = movements.loadDemo(squat);

        assertNotNull(demo);
        assertFalse(demo.frames.isEmpty());
        assertNull(movements.loadDemo(
                new Exercise("custom_1", "Mon exercice", "Bras", "Facile", 5, "Aucun", 3)));
    }

    @Test
    public void usability_needsAtLeastOneFrameWithJointAngles() {
        assertFalse(MovementRepository.isUsable(null));
        assertFalse(MovementRepository.isUsable(movement("vide", 0)));
        assertTrue(MovementRepository.isUsable(
                movements.loadAsset(BaseExercises.modelAssetFor(BaseExercises.all().get(0)))));
    }
}

package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Exercices de base et leurs modèles calculés.
 *
 * <p>Chaque modèle est rejoué dans le vrai moteur de comparaison, plus lentement et bruité
 * comme le serait une pose détectée par la caméra : il doit compter une répétition, et rien
 * quand on reste immobile. C'est ce qui garantit qu'un modèle calculé est utilisable, et pas
 * seulement lisible.</p>
 */
public class BaseExercisesTest {

    private static final File ASSETS = new File("src/main/assets/" + BaseExercises.ASSET_DIRECTORY);

    private static Movement model(Exercise exercise) throws Exception {
        File file = new File(ASSETS, exercise.getReferenceMovementId() + ".json");
        assertTrue("modèle manquant : " + file, file.isFile());
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Movement.class);
        }
    }

    private static Movement read(File file) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Movement.class);
        }
    }

    @Test
    public void everyBaseExercise_hasADrawableDemo_standingOnTheGround() throws Exception {
        for (Exercise exercise : BaseExercises.all()) {
            File file = new File("src/main/assets/" + BaseExercises.demoAssetFor(exercise));
            assertTrue("démonstration manquante : " + file, file.isFile());
            Movement demo = read(file);

            // Le mannequin sait l'animer...
            new ModelAnimation(demo.frames, demo.sampleIntervalMs);
            // ...et il est posé au sol : à chaque image, son point le plus bas est au niveau 0.
            for (float[] frame : demo.frames) {
                float lowest = -Float.MAX_VALUE;
                for (int i = 1; i < frame.length; i += 2) lowest = Math.max(lowest, frame[i]);
                assertEquals(exercise.getId() + " : corps décollé du sol", 0f, lowest, 1e-3f);
            }
        }
    }

    @Test
    public void demo_isFoundById_thenByName_andAbsentOtherwise() {
        Exercise base = BaseExercises.all().get(0);
        assertEquals(BaseExercises.DEMO_DIRECTORY + "/" + base.getId() + ".json",
                BaseExercises.demoAssetFor(base));

        // Un « squat » enregistré par l'utilisateur avant les exercices de base.
        Exercise userSquat = new Exercise("squat", " SQUAT ", "Jambes", "Facile", 5, "Aucun", 3);
        assertEquals(BaseExercises.DEMO_DIRECTORY + "/base_squat.json",
                BaseExercises.demoAssetFor(userSquat));

        Exercise custom = new Exercise("custom_1", "Mon exercice", "Bras", "Facile", 5, "Aucun", 3);
        assertNull(BaseExercises.demoAssetFor(custom));
    }

    @Test
    public void everyBaseExercise_hasAReadableModel() throws Exception {
        for (Exercise exercise : BaseExercises.all()) {
            Movement movement = model(exercise);
            assertNotNull(exercise.getId(), movement);
            assertEquals(exercise.getId(), movement.name);
            assertTrue(exercise.getId(), movement.frames.size() >= 20);
            for (float[] frame : movement.frames) {
                assertNotNull(exercise.getId() + " : image sans angles exploitables",
                        MovementMath.angleFeatures(frame));
            }
        }
    }

    @Test
    public void baseExercises_coverEveryMuscleGroupAtEveryDifficulty() {
        Set<String> seen = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (Exercise exercise : BaseExercises.all()) {
            seen.add(exercise.getMuscleGroup() + "/" + exercise.getDifficulty());
            assertTrue("identifiant en double : " + exercise.getId(), ids.add(exercise.getId()));
        }
        for (String muscle : new String[]{"Jambes", "Pectoraux", "Bras", "Tronc", "Dos"}) {
            for (String difficulty : new String[]{"Facile", "Moyen", "Difficile"}) {
                assertTrue(muscle + "/" + difficulty, seen.contains(muscle + "/" + difficulty));
            }
        }
    }

    @Test
    public void everyModel_countsASlowNoisyRepetition() throws Exception {
        for (Exercise exercise : BaseExercises.all()) {
            List<float[]> reference = model(exercise).frames;
            MovementComparisonEngine engine = new MovementComparisonEngine(
                    reference, exercise.getSimilarityThreshold());

            List<float[]> attempt = new ArrayList<>();
            boolean holdPosture = isHeldPosture(reference);
            int passes = holdPosture ? 2 : 1;
            for (int pass = 0; pass < passes; pass++) {
                attempt.addAll(slowedDown(reference));
            }
            int validated = countValidated(engine, noisy(attempt, exercise.getId().hashCode()));

            if (holdPosture) {
                assertTrue(exercise.getId() + " : posture tenue non reconnue", validated >= 1);
            } else {
                assertEquals(exercise.getId() + " : répétitions comptées",
                        expectedRepetitions(exercise), validated);
            }
        }
    }

    @Test
    public void everyMovingModel_countsNothing_whenStandingStillAtTheStart() throws Exception {
        for (Exercise exercise : BaseExercises.all()) {
            List<float[]> reference = model(exercise).frames;
            if (isHeldPosture(reference)) continue;
            MovementComparisonEngine engine = new MovementComparisonEngine(
                    reference, exercise.getSimilarityThreshold());

            List<float[]> still = new ArrayList<>();
            for (int i = 0; i < 60; i++) still.add(reference.get(0));

            assertEquals(exercise.getId(), 0, countValidated(engine, noisy(still, 7)));
        }
    }

    /**
     * Répétitions attendues pour une lecture complète du modèle. Les fentes alternées
     * enchaînent une fente de chaque côté et le moteur compte chaque fente, comme le dit la
     * consigne ; les montées de genoux, elles, ne comptent qu'une fois les deux genoux levés.
     */
    private static int expectedRepetitions(Exercise exercise) {
        return "base_fentes_alternees".equals(exercise.getId()) ? 2 : 1;
    }

    private static int countValidated(MovementComparisonEngine engine, List<float[]> frames) {
        int validated = 0;
        for (float[] frame : frames) {
            if (engine.addFrame(frame).isRepetitionValidated()) validated++;
        }
        return validated;
    }

    /** Posture tenue (planche) : aucune articulation ne bouge assez pour former des phases. */
    private static boolean isHeldPosture(List<float[]> frames) {
        float[] min = null, max = null;
        for (float[] frame : frames) {
            float[] angles = MovementMath.angleFeatures(frame);
            if (min == null) {
                min = angles.clone();
                max = angles.clone();
            }
            for (int i = 0; i < angles.length; i++) {
                min[i] = Math.min(min[i], angles[i]);
                max[i] = Math.max(max[i], angles[i]);
            }
        }
        for (int i = 0; i < min.length; i++) {
            if (max[i] - min[i] >= MovementComparisonEngine.MOVING_JOINT_MIN_RANGE) return false;
        }
        return true;
    }

    /** Une fois et demie plus lent : en rééducation, on va moins vite que le modèle. */
    private static List<float[]> slowedDown(List<float[]> frames) {
        List<float[]> slow = new ArrayList<>();
        for (int i = 0; i < frames.size(); i++) {
            slow.add(frames.get(i));
            if (i % 2 == 0) slow.add(frames.get(i));
        }
        return slow;
    }

    /** Bruit de détection : chaque point bouge d'au plus 3 % d'une longueur de torse. */
    private static List<float[]> noisy(List<float[]> frames, long seed) {
        Random random = new Random(seed);
        List<float[]> out = new ArrayList<>();
        for (float[] frame : frames) {
            float[] copy = frame.clone();
            for (int i = 0; i < copy.length; i++) {
                copy[i] += (random.nextFloat() - 0.5f) * 0.06f;
            }
            out.add(copy);
        }
        return out;
    }
}

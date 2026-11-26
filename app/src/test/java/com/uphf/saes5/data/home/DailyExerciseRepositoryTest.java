package com.uphf.saes5.data.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.testing.FileAssetSource;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Exercice du jour : figé pour la journée, et réalisé une fois par jour (US-4.2). */
public class DailyExerciseRepositoryTest {

    private static final Instant MONDAY = Instant.parse("2026-11-23T10:00:00Z");
    private static final Instant TUESDAY = Instant.parse("2026-11-24T10:00:00Z");

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private InMemoryPreferenceStore store;
    private ExerciseRepository exercises;

    @Before
    public void setUp() {
        store = new InMemoryPreferenceStore();
        exercises = new ExerciseRepository(store, new MovementRepository(
                new File(folder.getRoot(), "movements"), FileAssetSource.appAssets()));
        exercises.initialize();
    }

    private DailyExerciseRepository on(Instant day) {
        return new DailyExerciseRepository(store, exercises, Clock.fixed(day, ZoneOffset.UTC));
    }

    @Test
    public void theExerciseOfTheDay_staysTheSame_evenIfTheCatalogueChanges() {
        Exercise first = on(MONDAY).exerciseOfTheDay();
        exercises.add(new Exercise("custom_1", "Nouveau", "Bras", "Facile", 5, "Aucun", 3));

        Exercise again = on(MONDAY).exerciseOfTheDay();

        assertNotNull(first);
        assertEquals(first.getId(), again.getId());
        assertEquals("2026-11-23", store.getString("daily_exercise_day", null));
    }

    @Test
    public void aDeletedExerciseOfTheDay_isReplaced() {
        Exercise first = on(MONDAY).exerciseOfTheDay();
        exercises.remove(first.getId());

        Exercise replaced = on(MONDAY).exerciseOfTheDay();

        assertNotNull(replaced);
        assertFalse(first.getId().equals(replaced.getId()));
    }

    @Test
    public void anEmptyCatalogue_hasNoExerciseOfTheDay() {
        for (Exercise exercise : exercises.getAll()) exercises.remove(exercise.getId());

        assertNull(on(MONDAY).exerciseOfTheDay());
    }

    @Test
    public void completion_countsForTheDayOnly() {
        assertFalse(on(MONDAY).isCompletedToday());

        on(MONDAY).markCompletedToday();

        assertTrue(on(MONDAY).isCompletedToday());
        assertFalse(on(TUESDAY).isCompletedToday());
    }
}

package com.uphf.saes5.accueil.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.uphf.saes5.Exercise;
import com.uphf.saes5.ExerciseRepository;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tirage de l'exercice du jour parmi le catalogue (US-4.2). */
public class DailyExerciseProviderTest {

    private static Exercise exercise(String id) {
        return new Exercise(id, id, "Jambes", "Facile", 5, "Aucun", 3);
    }

    @Test
    public void pick_isNull_whenCatalogueIsEmpty() {
        assertNull(DailyExerciseProvider.pickForDay(Collections.emptyList(), "2026-09-30"));
        assertNull(DailyExerciseProvider.pickForDay(null, "2026-09-30"));
    }

    @Test
    public void pick_isStable_forTheSameDay() {
        List<Exercise> catalogue = ExerciseRepository.getAll();

        Exercise first = DailyExerciseProvider.pickForDay(catalogue, "2026-09-30");
        Exercise second = DailyExerciseProvider.pickForDay(catalogue, "2026-09-30");

        assertEquals(first.getId(), second.getId());
    }

    @Test
    public void pick_comesFromTheCatalogue() {
        List<Exercise> catalogue = ExerciseRepository.getAll();
        Set<String> catalogueIds = new HashSet<>();
        for (Exercise candidate : catalogue) {
            catalogueIds.add(candidate.getId());
        }

        // Un mois de tirages : aucun ne doit sortir du catalogue.
        for (int day = 1; day <= 31; day++) {
            Exercise picked = DailyExerciseProvider.pickForDay(
                    catalogue, String.format("2026-10-%02d", day));
            assertTrue("jour " + day, catalogueIds.contains(picked.getId()));
        }
    }

    @Test
    public void pick_canReturnAnExerciseCreatedByTheUser() {
        // Un exercice créé depuis le catalogue doit pouvoir devenir l'exercice du jour.
        Exercise custom = exercise("custom_rowing_maison");
        List<Exercise> catalogue = new ArrayList<>();
        catalogue.add(custom);

        assertSame(custom, DailyExerciseProvider.pickForDay(catalogue, "2026-09-30"));
    }

    @Test
    public void pick_variesAcrossDays() {
        List<Exercise> catalogue = ExerciseRepository.getAll();
        Set<String> picked = new HashSet<>();

        for (int day = 1; day <= 31; day++) {
            picked.add(DailyExerciseProvider.pickForDay(
                    catalogue, String.format("2026-10-%02d", day)).getId());
        }

        assertTrue("le tirage doit changer d'un jour à l'autre", picked.size() > 1);
    }
}

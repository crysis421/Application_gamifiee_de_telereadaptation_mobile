package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.util.List;

/**
 * Modification et suppression d'un exercice du catalogue.
 *
 * <p>Le dépôt est un singleton statique partagé par toute l'application : chaque test ne touche
 * qu'à son propre exercice et le retire ensuite, pour ne pas perturber les autres.</p>
 */
public class ExerciseRepositoryTest {

    private static final String ID = "test_exercice_temporaire";

    @After
    public void cleanUp() {
        ExerciseRepository.remove(ID);
    }

    private static Exercise sample(String name, int stars) {
        return new Exercise(ID, name, "Jambes", "Facile", 5, "Aucun", stars,
                "Consigne d'origine.", 10);
    }

    @Test
    public void update_replacesEveryField_andKeepsPosition() {
        ExerciseRepository.add(sample("Avant", 2));
        int positionBefore = indexOf(ID);

        boolean updated = ExerciseRepository.update(new Exercise(
                ID, "Après", "Dos", "Difficile", 9, "Élastique", 5,
                "Nouvelle consigne.", 20));

        assertTrue(updated);
        Exercise result = ExerciseRepository.findById(ID);
        assertNotNull(result);
        assertEquals("Après", result.getName());
        assertEquals("Dos", result.getMuscleGroup());
        assertEquals("Difficile", result.getDifficulty());
        assertEquals(9, result.getDurationMinutes());
        assertEquals("Élastique", result.getEquipment());
        assertEquals(5, result.getStars());
        assertEquals("Nouvelle consigne.", result.getDescription());
        assertEquals(20, result.getTargetReps());
        assertEquals("l'exercice ne doit pas changer de place", positionBefore, indexOf(ID));
    }

    @Test
    public void update_keepsTheSameId_soFavoritesKeepPointingToIt() {
        ExerciseRepository.add(sample("Avant", 2));

        ExerciseRepository.update(new Exercise(
                ID, "Après", "Dos", "Difficile", 9, "Élastique", 5, "", 20));

        // Les favoris et l'exercice du jour stockent un identifiant : il doit survivre.
        assertNotNull(ExerciseRepository.findById(ID));
        assertEquals(1, countOccurrences(ID));
    }

    @Test
    public void update_isIgnored_whenExerciseIsNotInTheCatalogue() {
        int sizeBefore = ExerciseRepository.getAll().size();

        boolean updated = ExerciseRepository.update(sample("Fantôme", 1));

        assertFalse(updated);
        assertEquals(sizeBefore, ExerciseRepository.getAll().size());
    }

    @Test
    public void remove_takesTheExerciseOut() {
        ExerciseRepository.add(sample("À supprimer", 3));
        int sizeWithIt = ExerciseRepository.getAll().size();

        boolean removed = ExerciseRepository.remove(ID);

        assertTrue(removed);
        assertNull(ExerciseRepository.findById(ID));
        assertEquals(sizeWithIt - 1, ExerciseRepository.getAll().size());
    }

    @Test
    public void remove_returnsFalse_whenAlreadyGone() {
        assertFalse(ExerciseRepository.remove(ID));
        assertFalse(ExerciseRepository.remove(null));
    }

    private int indexOf(String id) {
        List<Exercise> all = ExerciseRepository.getAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    private int countOccurrences(String id) {
        int count = 0;
        for (Exercise exercise : ExerciseRepository.getAll()) {
            if (exercise.getId().equals(id)) count++;
        }
        return count;
    }
}

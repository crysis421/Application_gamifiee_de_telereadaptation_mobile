package com.uphf.saes5.accueil.data;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.uphf.saes5.accueil.model.Exercise;

/**
 * Catalogue bouchon, en dur et en mémoire.
 *
 * <p>TODO (US-2.1 / US-6.1) : à remplacer par le vrai catalogue une fois la base de données
 * disponible. Le seul point à modifier est {@link ExerciseRepositoryProvider}.</p>
 *
 * <p>Les exercices proposés sont volontairement centrés sur le haut du corps : l'utilisateur
 * reste ainsi plus près de l'écran, ce qui améliore la visibilité et le suivi (US-3.4).</p>
 */
public class FakeExerciseRepository implements ExerciseRepository {

    private final List<Exercise> exercises = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
            new Exercise(
                    "elevations-laterales",
                    "Élévations latérales",
                    "Debout, bras le long du corps. Monte les bras tendus sur les côtés jusqu'à "
                            + "l'horizontale, puis redescends lentement.",
                    "Épaules",
                    "Aucun",
                    Exercise.DIFFICULTY_EASY,
                    12,
                    90),
            new Exercise(
                    "flexions-bras",
                    "Flexions des bras",
                    "Coudes collés au buste, remonte les avant-bras jusqu'aux épaules, "
                            + "puis redescends en contrôlant le mouvement.",
                    "Biceps",
                    "Aucun",
                    Exercise.DIFFICULTY_EASY,
                    15,
                    90),
            new Exercise(
                    "rotations-epaules",
                    "Rotations d'épaules",
                    "Bras écartés à l'horizontale, effectue de larges cercles vers l'avant "
                            + "puis vers l'arrière.",
                    "Épaules",
                    "Aucun",
                    Exercise.DIFFICULTY_EASY,
                    20,
                    60),
            new Exercise(
                    "developpe-militaire",
                    "Développé au-dessus de la tête",
                    "Mains à hauteur d'épaules, pousse les bras vers le plafond jusqu'à "
                            + "l'extension complète, puis redescends.",
                    "Épaules",
                    "Aucun",
                    Exercise.DIFFICULTY_MEDIUM,
                    12,
                    120),
            new Exercise(
                    "inclinaisons-laterales",
                    "Inclinaisons latérales du buste",
                    "Debout, jambes écartées. Incline le buste sur le côté en gardant le dos "
                            + "droit, puis reviens au centre.",
                    "Abdominaux",
                    "Aucun",
                    Exercise.DIFFICULTY_MEDIUM,
                    16,
                    120),
            new Exercise(
                    "squats",
                    "Squats",
                    "Pieds écartés à la largeur des épaules, descends en fléchissant les genoux "
                            + "en gardant le dos droit, puis remonte.",
                    "Jambes",
                    "Aucun",
                    Exercise.DIFFICULTY_HARD,
                    10,
                    150)
    )));

    @Override
    public List<Exercise> getAll() {
        return exercises;
    }

    @Nullable
    @Override
    public Exercise findById(String id) {
        if (id == null) {
            return null;
        }
        for (Exercise exercise : exercises) {
            if (id.equals(exercise.getId())) {
                return exercise;
            }
        }
        return null;
    }
}

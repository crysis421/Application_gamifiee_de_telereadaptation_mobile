package com.uphf.saes5.data.home;

import androidx.annotation.Nullable;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.storage.PreferenceStore;
import com.uphf.saes5.domain.exercise.DailyExercisePicker;
import com.uphf.saes5.domain.model.Exercise;

import java.time.Clock;
import java.util.List;

/**
 * Exercice du jour (US-4.2) : lequel est mis en avant, et s'il a été réalisé aujourd'hui.
 *
 * <p>Le choix est <em>figé pour la journée</em> : une fois tiré, l'identifiant retenu est
 * enregistré avec la date. Tant que la journée ne change pas, c'est ce même exercice qui est
 * renvoyé, et il survit à la fermeture de l'application.</p>
 *
 * <p>C'est ce qui le rend insensible aux modifications du catalogue : le tirage initial dépend
 * du nombre d'exercices et de leur ordre, donc sans cette mémoire, créer ou supprimer un
 * exercice en cours de journée changeait l'exercice mis en avant.</p>
 *
 * <p>Le tirage est relancé dans deux cas seulement : le jour a changé, ou l'exercice retenu
 * n'est plus au catalogue — il a été supprimé entre-temps.</p>
 */
public final class DailyExerciseRepository {

    private static final String KEY_DAY = "daily_exercise_day";
    private static final String KEY_EXERCISE_ID = "daily_exercise_id";
    private static final String KEY_LAST_COMPLETED_DAY = "daily_exercise_last_completed_day";

    private final PreferenceStore store;
    private final ExerciseRepository exercises;
    private final Clock clock;

    public DailyExerciseRepository(PreferenceStore store, ExerciseRepository exercises,
                                   Clock clock) {
        this.store = store;
        this.exercises = exercises;
        this.clock = clock;
    }

    /** L'exercice mis en avant aujourd'hui, ou {@code null} si le catalogue est vide. */
    @Nullable
    public Exercise exerciseOfTheDay() {
        List<Exercise> catalogue = exercises.getAll();
        if (catalogue.isEmpty()) {
            return null;
        }

        String today = DailyExercisePicker.dayKey(clock);
        if (today.equals(store.getString(KEY_DAY, null))) {
            Exercise retained = exercises.findById(store.getString(KEY_EXERCISE_ID, null));
            if (retained != null) {
                return retained;
            }
            // L'exercice du jour a été supprimé du catalogue : on en retire un autre.
        }

        Exercise picked = DailyExercisePicker.pickForDay(catalogue, today);
        store.putString(KEY_DAY, today);
        store.putString(KEY_EXERCISE_ID, picked.getId());
        return picked;
    }

    /** Vrai si un exercice a été réalisé aujourd'hui (au moins une répétition validée). */
    public boolean isCompletedToday() {
        return DailyExercisePicker.dayKey(clock).equals(store.getString(KEY_LAST_COMPLETED_DAY, null));
    }

    public void markCompletedToday() {
        store.putString(KEY_LAST_COMPLETED_DAY, DailyExercisePicker.dayKey(clock));
    }
}

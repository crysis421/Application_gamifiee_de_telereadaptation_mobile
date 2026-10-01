package com.uphf.saes5;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Catalogue d'exercices.
 *
 * <p>Le contenu est conservé d'une ouverture à l'autre dans les SharedPreferences, sérialisé en
 * JSON. C'est volontairement sommaire : ce stockage tient le temps que la base de données
 * arrive (voir le remaniement « base de données et modèle de données » du backlog), et seule
 * cette classe sera à remplacer le jour venu.</p>
 *
 * <p>{@link #init(Context)} doit être appelé au démarrage de l'application. Sans lui — en test
 * unitaire par exemple — le catalogue fonctionne en mémoire avec les exercices d'origine et
 * n'écrit rien.</p>
 */
public final class ExerciseRepository {

    private static final String TAG = "ExerciseRepository";
    private static final String PREFS_NAME = "exergame_prefs";
    private static final String KEY_EXERCISES = "catalogue_exercises";

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_MUSCLE = "muscleGroup";
    private static final String FIELD_DIFFICULTY = "difficulty";
    private static final String FIELD_DURATION = "durationMinutes";
    private static final String FIELD_EQUIPMENT = "equipment";
    private static final String FIELD_STARS = "stars";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_TARGET_REPS = "targetReps";

    @Nullable
    private static SharedPreferences preferences;

    /** Liste modifiable, amorcée avec le catalogue d'origine puis remplacée par init(). */
    private static final List<Exercise> exercises = new ArrayList<>(defaults());

    private ExerciseRepository() {}

    /** Le catalogue livré avec l'application, utilisé à la première ouverture. */
    private static List<Exercise> defaults() {
        return Arrays.asList(
                new Exercise("squat", "Squat", "Jambes", "Facile", 5, "Aucun", 3,
                        "Pieds écartés à la largeur des épaules, descends en fléchissant les genoux "
                                + "en gardant le dos droit, puis remonte.", 10),
                new Exercise("pompes", "Pompes", "Pectoraux", "Moyen", 7, "Tapis", 4,
                        "Mains au sol à l'aplomb des épaules, corps gainé. Descends jusqu'à frôler "
                                + "le sol, puis pousse pour remonter.", 12),
                new Exercise("fentes", "Fentes", "Jambes", "Facile", 6, "Aucun", 2,
                        "Avance une jambe et fléchis les deux genoux jusqu'à l'angle droit, puis "
                                + "reviens en poussant sur la jambe avant. Alterne à chaque répétition.", 14),
                new Exercise("planche", "Planche", "Tronc", "Difficile", 4, "Aucun", 5,
                        "En appui sur les avant-bras et la pointe des pieds, garde le corps aligné "
                                + "des épaules aux talons sans creuser le dos.", 8),
                new Exercise("rowing", "Rowing élastique", "Dos", "Moyen", 8, "Élastique", 1,
                        "Élastique sous les pieds, buste légèrement penché. Tire les coudes vers "
                                + "l'arrière en serrant les omoplates, puis relâche en contrôlant.", 12)
        );
    }

    /**
     * Branche le catalogue sur le stockage et recharge son contenu.
     *
     * <p>Appelé une fois au démarrage, depuis {@link SaeS5Application}.</p>
     */
    public static synchronized void init(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String stored = preferences.getString(KEY_EXERCISES, null);
        if (stored == null) {
            // Première ouverture : on écrit le catalogue d'origine.
            save();
            return;
        }
        List<Exercise> loaded = deserialize(stored);
        if (loaded != null) {
            exercises.clear();
            exercises.addAll(loaded);
        }
    }

    public static synchronized List<Exercise> getAll() {
        return new ArrayList<>(exercises);
    }

    public static synchronized void add(Exercise exercise) {
        exercises.add(exercise);
        save();
    }

    /**
     * Remplace l'exercice portant le même identifiant, en conservant sa place dans la liste.
     *
     * @return {@code false} si l'exercice n'est plus au catalogue, auquel cas rien n'est modifié.
     */
    public static synchronized boolean update(Exercise exercise) {
        if (exercise == null) {
            return false;
        }
        for (int i = 0; i < exercises.size(); i++) {
            if (exercises.get(i).getId().equals(exercise.getId())) {
                exercises.set(i, exercise);
                save();
                return true;
            }
        }
        return false;
    }

    /**
     * Retire un exercice du catalogue.
     *
     * @return {@code false} s'il n'y était déjà plus.
     */
    public static synchronized boolean remove(String id) {
        if (id == null) {
            return false;
        }
        for (int i = 0; i < exercises.size(); i++) {
            if (id.equals(exercises.get(i).getId())) {
                exercises.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    /** L'exercice portant cet identifiant, ou {@code null} s'il n'est plus au catalogue. */
    @Nullable
    public static synchronized Exercise findById(String id) {
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

    public static String newId() {
        return "custom_" + UUID.randomUUID();
    }

    /** Rétablit le catalogue d'origine. Exposé pour les tests et un éventuel « réinitialiser ». */
    @VisibleForTesting
    public static synchronized void resetToDefaults() {
        exercises.clear();
        exercises.addAll(defaults());
        save();
    }

    /** Écrit le catalogue. Sans effet tant que {@link #init(Context)} n'a pas été appelé. */
    private static void save() {
        if (preferences == null) {
            return;
        }
        preferences.edit().putString(KEY_EXERCISES, serialize(exercises)).apply();
    }

    @VisibleForTesting
    static String serialize(List<Exercise> list) {
        JSONArray array = new JSONArray();
        for (Exercise exercise : list) {
            JSONObject object = new JSONObject();
            try {
                object.put(FIELD_ID, exercise.getId());
                object.put(FIELD_NAME, exercise.getName());
                object.put(FIELD_MUSCLE, exercise.getMuscleGroup());
                object.put(FIELD_DIFFICULTY, exercise.getDifficulty());
                object.put(FIELD_DURATION, exercise.getDurationMinutes());
                object.put(FIELD_EQUIPMENT, exercise.getEquipment());
                object.put(FIELD_STARS, exercise.getStars());
                object.put(FIELD_DESCRIPTION, exercise.getDescription());
                object.put(FIELD_TARGET_REPS, exercise.getTargetReps());
            } catch (JSONException e) {
                Log.w(TAG, "Exercice non enregistré : " + exercise.getId(), e);
                continue;
            }
            array.put(object);
        }
        return array.toString();
    }

    /**
     * Relit un catalogue enregistré.
     *
     * @return {@code null} si le contenu est illisible — on garde alors ce qui est en mémoire
     *         plutôt que de présenter un catalogue vide.
     */
    @Nullable
    @VisibleForTesting
    static List<Exercise> deserialize(String stored) {
        List<Exercise> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(stored);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                result.add(new Exercise(
                        object.getString(FIELD_ID),
                        object.getString(FIELD_NAME),
                        object.optString(FIELD_MUSCLE, ""),
                        object.optString(FIELD_DIFFICULTY, ""),
                        object.optInt(FIELD_DURATION, 1),
                        object.optString(FIELD_EQUIPMENT, "Aucun"),
                        object.optInt(FIELD_STARS, 1),
                        object.optString(FIELD_DESCRIPTION, ""),
                        object.optInt(FIELD_TARGET_REPS, Exercise.DEFAULT_TARGET_REPS)));
            }
        } catch (JSONException e) {
            Log.w(TAG, "Catalogue enregistré illisible, il est ignoré", e);
            return null;
        }
        return result;
    }
}

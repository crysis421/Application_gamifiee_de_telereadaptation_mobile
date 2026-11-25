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
 * <p>Un exercice n'existe qu'avec son modèle de référence : c'est à lui que le mouvement est
 * comparé. Le catalogue démarre donc vide — aucun modèle ne peut être livré avec
 * l'application — et ne se remplit que d'exercices créés avec leur modèle (voir
 * {@code CreateExerciseActivity}).</p>
 *
 * <p>{@link #init(Context)} doit être appelé au démarrage de l'application. Sans lui — en test
 * unitaire par exemple — le catalogue fonctionne en mémoire et n'écrit rien.</p>
 */
public final class ExerciseRepository {

    private static final String TAG = "ExerciseRepository";
    private static final String PREFS_NAME = "exergame_prefs";
    private static final String KEY_EXERCISES = "catalogue_exercises";
    /** Posée une fois les exercices sans modèle retirés des catalogues déjà enregistrés. */
    private static final String KEY_WITHOUT_MODEL_REMOVED = "catalogue_without_model_removed";

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_MUSCLE = "muscleGroup";
    private static final String FIELD_DIFFICULTY = "difficulty";
    private static final String FIELD_DURATION = "durationMinutes";
    private static final String FIELD_EQUIPMENT = "equipment";
    private static final String FIELD_STARS = "stars";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_TARGET_REPS = "targetReps";
    private static final String FIELD_REFERENCE_MOVEMENT = "referenceMovementId";
    private static final String FIELD_SIMILARITY_THRESHOLD = "similarityThreshold";

    @Nullable
    private static SharedPreferences preferences;

    /** Liste modifiable, vide au départ puis remplie par init(). */
    private static final List<Exercise> exercises = new ArrayList<>();

    private ExerciseRepository() {}

    /**
     * Branche le catalogue sur le stockage et recharge son contenu.
     *
     * <p>Appelé une fois au démarrage, depuis {@link SaeS5Application}.</p>
     */
    public static synchronized void init(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String stored = preferences.getString(KEY_EXERCISES, null);
        if (stored != null) {
            List<Exercise> loaded = deserialize(stored);
            if (loaded != null) {
                exercises.clear();
                exercises.addAll(loaded);
            }
        }
        removeExercisesWithoutModelOnce(context);
    }

    /**
     * Retire, une seule fois par appareil, les exercices enregistrés avant que le modèle ne
     * devienne obligatoire : les cinq exercices autrefois livrés avec l'application, et ceux
     * créés sans enregistrer de mouvement. Ceux dont le modèle a été enregistré entre-temps
     * sont conservés.
     */
    private static void removeExercisesWithoutModelOnce(Context context) {
        if (preferences.getBoolean(KEY_WITHOUT_MODEL_REMOVED, false)) {
            return;
        }
        for (int i = exercises.size() - 1; i >= 0; i--) {
            if (!MovementStorage.exists(context, exercises.get(i).getReferenceMovementId())) {
                Log.i(TAG, "Exercice sans modèle retiré : " + exercises.get(i).getId());
                exercises.remove(i);
            }
        }
        save();
        preferences.edit().putBoolean(KEY_WITHOUT_MODEL_REMOVED, true).apply();
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
                object.put(FIELD_REFERENCE_MOVEMENT, exercise.getReferenceMovementId());
                object.put(FIELD_SIMILARITY_THRESHOLD, exercise.getSimilarityThreshold());
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
                String id = object.getString(FIELD_ID);
                result.add(new Exercise(
                        id,
                        object.getString(FIELD_NAME),
                        object.optString(FIELD_MUSCLE, ""),
                        object.optString(FIELD_DIFFICULTY, ""),
                        object.optInt(FIELD_DURATION, 1),
                        object.optString(FIELD_EQUIPMENT, "Aucun"),
                        object.optInt(FIELD_STARS, 1),
                        object.optString(FIELD_DESCRIPTION, ""),
                        object.optInt(FIELD_TARGET_REPS, Exercise.DEFAULT_TARGET_REPS),
                        object.optString(FIELD_REFERENCE_MOVEMENT, id),
                        object.optInt(FIELD_SIMILARITY_THRESHOLD,
                                Exercise.DEFAULT_SIMILARITY_THRESHOLD)));
            }
        } catch (JSONException e) {
            Log.w(TAG, "Catalogue enregistré illisible, il est ignoré", e);
            return null;
        }
        return result;
    }
}

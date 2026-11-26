package com.uphf.saes5.data.exercise;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.uphf.saes5.data.storage.PreferenceStore;
import com.uphf.saes5.domain.exercise.BaseExercises;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.Movement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Catalogue d'exercices.
 *
 * <p>Le contenu est conservé d'une ouverture à l'autre dans les préférences, sérialisé en
 * JSON. C'est volontairement sommaire : ce stockage tient le temps que la base de données
 * arrive, et seule cette classe sera à remplacer le jour venu.</p>
 *
 * <p>Un exercice n'existe qu'avec son modèle de référence : c'est à lui que le mouvement est
 * comparé. Le catalogue démarre avec les exercices de base ({@link BaseExercises}), livrés
 * avec leur modèle, puis ne s'enrichit que d'exercices créés avec le leur.</p>
 *
 * <p>{@link #initialize()} est appelé une fois, à la création du conteneur de
 * l'application.</p>
 */
public final class ExerciseRepository {

    private static final String TAG = "ExerciseRepository";
    private static final String KEY_EXERCISES = "catalogue_exercises";
    /** Posée une fois les exercices sans modèle retirés des catalogues déjà enregistrés. */
    private static final String KEY_WITHOUT_MODEL_REMOVED = "catalogue_without_model_removed";
    /** Posée une fois les exercices de base installés : un exercice supprimé ne revient pas. */
    private static final String KEY_BASE_INSTALLED = "catalogue_base_exercises_installed";

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

    private final PreferenceStore store;
    private final MovementRepository movements;
    private final List<Exercise> exercises = new ArrayList<>();

    public ExerciseRepository(PreferenceStore store, MovementRepository movements) {
        this.store = store;
        this.movements = movements;
    }

    /** Recharge le catalogue enregistré, puis applique les deux mises à niveau « une fois ». */
    public synchronized void initialize() {
        String stored = store.getString(KEY_EXERCISES, null);
        if (stored != null) {
            List<Exercise> loaded = deserialize(stored);
            if (loaded != null) {
                exercises.clear();
                exercises.addAll(loaded);
            }
        }
        removeExercisesWithoutModelOnce();
        installBaseExercisesOnce();
    }

    /**
     * Ajoute, une seule fois par appareil, les exercices de base et leur modèle.
     *
     * <p>Un exercice du même identifiant ou du même nom est laissé tel quel, avec le modèle que
     * l'utilisateur lui a peut-être enregistré. Le modèle est copié avant l'exercice : si la
     * copie échoue, l'exercice n'est pas ajouté.</p>
     */
    private void installBaseExercisesOnce() {
        if (store.getBoolean(KEY_BASE_INSTALLED, false)) {
            return;
        }
        for (Exercise base : BaseExercises.all()) {
            if (isAlreadyInCatalogue(base)) {
                continue;
            }
            if (!movements.exists(base.getReferenceMovementId())) {
                Movement model = movements.loadAsset(BaseExercises.modelAssetFor(base));
                try {
                    if (model == null) throw new IOException("modèle illisible");
                    movements.save(model);
                } catch (IOException e) {
                    Log.w(TAG, "Exercice de base non installé : " + base.getId(), e);
                    continue;
                }
            }
            exercises.add(base);
        }
        save();
        store.putBoolean(KEY_BASE_INSTALLED, true);
    }

    private boolean isAlreadyInCatalogue(Exercise candidate) {
        for (Exercise exercise : exercises) {
            if (exercise.getId().equals(candidate.getId())
                    || exercise.getName().trim().equalsIgnoreCase(candidate.getName().trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retire, une seule fois par appareil, les exercices enregistrés avant que le modèle ne
     * devienne obligatoire. Ceux dont le modèle a été enregistré entre-temps sont conservés.
     */
    private void removeExercisesWithoutModelOnce() {
        if (store.getBoolean(KEY_WITHOUT_MODEL_REMOVED, false)) {
            return;
        }
        for (int i = exercises.size() - 1; i >= 0; i--) {
            if (!movements.exists(exercises.get(i).getReferenceMovementId())) {
                Log.i(TAG, "Exercice sans modèle retiré : " + exercises.get(i).getId());
                exercises.remove(i);
            }
        }
        save();
        store.putBoolean(KEY_WITHOUT_MODEL_REMOVED, true);
    }

    public synchronized List<Exercise> getAll() {
        return new ArrayList<>(exercises);
    }

    public synchronized void add(Exercise exercise) {
        exercises.add(exercise);
        save();
    }

    /**
     * Remplace l'exercice portant le même identifiant, en conservant sa place dans la liste.
     *
     * @return {@code false} si l'exercice n'est plus au catalogue, auquel cas rien n'est modifié.
     */
    public synchronized boolean update(Exercise exercise) {
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
    public synchronized boolean remove(String id) {
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
    public synchronized Exercise findById(String id) {
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

    /** Identifiant d'un nouvel exercice créé par l'utilisateur. */
    public String newId() {
        return "custom_" + UUID.randomUUID();
    }

    private void save() {
        store.putString(KEY_EXERCISES, serialize(exercises));
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

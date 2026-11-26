package com.uphf.saes5.ui.catalog;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.ui.common.Event;

/**
 * Création d'un exercice, et modification d'un exercice existant.
 *
 * <p>En création, l'exercice reçoit un identifiant de brouillon dès l'ouverture : c'est sous
 * ce nom que le modèle de référence est enregistré. Un exercice n'existe qu'avec son modèle :
 * la création attend qu'il ait été enregistré.</p>
 *
 * <p>En modification, l'identifiant est conservé : les favoris et l'exercice du jour qui le
 * référencent continuent de pointer sur le bon exercice.</p>
 */
public final class ExerciseEditorViewModel extends ViewModel {

    /** Issue d'un enregistrement, ou raison de fermer le formulaire. */
    public enum EditorEvent {
        NAME_REQUIRED, DURATION_INVALID, TARGET_REPS_INVALID, MODEL_MISSING,
        CREATED, UPDATED,
        /** Modification demandée sur un exercice qui n'est plus au catalogue. */
        NO_LONGER_AVAILABLE
    }

    private static final String DEFAULT_EQUIPMENT = "Aucun";

    private final ExerciseRepository exercises;
    private final MovementRepository movements;
    @Nullable private final Exercise edited;
    private final String draftId;
    private final String referenceMovementId;
    private final MutableLiveData<ExerciseEditorUiState> state = new MutableLiveData<>();
    private final MutableLiveData<Event<EditorEvent>> events = new MutableLiveData<>();

    /**
     * @param editedId        l'exercice à modifier, ou {@code null} pour en créer un
     * @param restoredDraftId identifiant du brouillon d'avant la recréation de l'écran, ou
     *                        {@code null} : le modèle déjà enregistré reste rattaché
     */
    public ExerciseEditorViewModel(@Nullable String editedId, @Nullable String restoredDraftId,
                                   ExerciseRepository exercises, MovementRepository movements) {
        this.exercises = exercises;
        this.movements = movements;
        this.edited = exercises.findById(editedId);
        if (edited != null) {
            draftId = edited.getId();
            referenceMovementId = edited.getReferenceMovementId();
        } else {
            draftId = restoredDraftId != null ? restoredDraftId : exercises.newId();
            referenceMovementId = draftId;
        }
        refreshReference();
        // Sans ce garde-fou, le formulaire basculerait silencieusement en création et
        // enregistrerait un doublon.
        if (editedId != null && edited == null) {
            events.setValue(new Event<>(EditorEvent.NO_LONGER_AVAILABLE));
        }
    }

    public LiveData<ExerciseEditorUiState> getState() {
        return state;
    }

    public LiveData<Event<EditorEvent>> getEvents() {
        return events;
    }

    /** À conserver avec l'état de l'écran, pour {@code restoredDraftId}. */
    public String getDraftId() {
        return draftId;
    }

    /** Relit la présence du modèle : au retour de l'écran d'enregistrement. */
    public void refreshReference() {
        state.setValue(new ExerciseEditorUiState(
                edited != null,
                edited,
                referenceMovementId,
                movements.exists(referenceMovementId),
                edited != null ? edited.getSimilarityThreshold()
                        : Exercise.DEFAULT_SIMILARITY_THRESHOLD));
    }

    /** Valide le formulaire puis ajoute ou remplace l'exercice. */
    public void save(ExerciseForm form) {
        String name = form.name.trim();
        if (name.isEmpty()) {
            emit(EditorEvent.NAME_REQUIRED);
            return;
        }
        int duration = parsePositive(form.durationText);
        if (duration <= 0) {
            emit(EditorEvent.DURATION_INVALID);
            return;
        }
        int targetReps = parsePositive(form.targetRepsText);
        if (targetReps <= 0) {
            emit(EditorEvent.TARGET_REPS_INVALID);
            return;
        }
        if (edited == null && !movements.exists(referenceMovementId)) {
            emit(EditorEvent.MODEL_MISSING);
            return;
        }

        String equipment = form.equipment.trim();
        Exercise result = new Exercise(
                draftId,
                name,
                form.muscle,
                form.difficulty,
                duration,
                equipment.isEmpty() ? DEFAULT_EQUIPMENT : equipment,
                Math.max(1, Math.round(form.rating)),
                form.description.trim(),
                targetReps,
                referenceMovementId,
                form.threshold);

        if (edited != null) {
            exercises.update(result);
            emit(EditorEvent.UPDATED);
        } else {
            exercises.add(result);
            emit(EditorEvent.CREATED);
        }
    }

    private void emit(EditorEvent event) {
        events.setValue(new Event<>(event));
    }

    /** Valeur entière saisie, ou 0 si le champ est vide ou illisible. */
    private static int parsePositive(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

package com.uphf.saes5.ui.catalog;

import androidx.annotation.Nullable;

import com.uphf.saes5.domain.model.Exercise;

/** État du formulaire d'exercice, calculé par {@link ExerciseEditorViewModel}. Immuable. */
public final class ExerciseEditorUiState {

    private final boolean editing;
    @Nullable private final Exercise initial;
    private final String referenceMovementId;
    private final boolean referenceAvailable;
    private final int threshold;

    ExerciseEditorUiState(boolean editing, @Nullable Exercise initial, String referenceMovementId,
                          boolean referenceAvailable, int threshold) {
        this.editing = editing;
        this.initial = initial;
        this.referenceMovementId = referenceMovementId;
        this.referenceAvailable = referenceAvailable;
        this.threshold = threshold;
    }

    /** Modification d'un exercice existant, plutôt que création. */
    public boolean isEditing() { return editing; }
    /** L'exercice modifié, pour préremplir le formulaire ; {@code null} en création. */
    @Nullable public Exercise getInitial() { return initial; }
    /** Nom sous lequel le modèle de référence est enregistré. */
    public String getReferenceMovementId() { return referenceMovementId; }
    /** Le modèle a été enregistré : on peut le tester, et créer l'exercice. */
    public boolean isReferenceAvailable() { return referenceAvailable; }
    /** Seuil de similarité initial du curseur. */
    public int getThreshold() { return threshold; }
}

package com.uphf.saes5.accueil.data;

import androidx.annotation.Nullable;

import java.util.List;

import com.uphf.saes5.accueil.model.Exercise;

/**
 * Accès au catalogue d'exercices.
 *
 * <p>Seule cette interface est utilisée par les écrans. L'implémentation réelle (base de
 * données, US-6.1) pourra donc remplacer {@link FakeExerciseRepository} sans toucher à l'UI.</p>
 */
public interface ExerciseRepository {

    /** Tous les exercices du catalogue. Jamais {@code null}, éventuellement vide. */
    List<Exercise> getAll();

    /** L'exercice portant cet identifiant, ou {@code null} s'il n'existe pas. */
    @Nullable
    Exercise findById(String id);
}

package com.uphf.saes5.accueil.data;

/**
 * Point d'accès unique au catalogue.
 *
 * <p>TODO (US-6.1) : quand le vrai catalogue sera disponible, il suffira de changer
 * l'implémentation instanciée ici — aucun écran n'est à modifier.</p>
 */
public final class ExerciseRepositoryProvider {

    private static ExerciseRepository instance;

    private ExerciseRepositoryProvider() {
    }

    public static synchronized ExerciseRepository get() {
        if (instance == null) {
            instance = new FakeExerciseRepository();
        }
        return instance;
    }
}

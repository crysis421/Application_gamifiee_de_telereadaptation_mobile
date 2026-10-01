package com.uphf.saes5;

import android.app.Application;

/**
 * Point d'entrée de l'application.
 *
 * <p>Sert à brancher le catalogue sur son stockage avant qu'un écran ne l'interroge : sans ça,
 * les exercices créés ou modifiés seraient perdus à la fermeture.</p>
 */
public class SaeS5Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ExerciseRepository.init(this);
    }
}

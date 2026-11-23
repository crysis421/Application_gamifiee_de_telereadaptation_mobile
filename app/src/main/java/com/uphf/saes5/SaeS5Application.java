package com.uphf.saes5;

import android.app.Application;

/**
 * Point d'entrée de l'application.
 *
 * <p>Sert à brancher le catalogue sur son stockage avant qu'un écran ne l'interroge : sans ça,
 * les exercices créés ou modifiés seraient perdus à la fermeture.</p>
 *
 * <p>Idem pour les réglages de gamification (US-4.1), lus dès le premier écran.</p>
 */
public class SaeS5Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ExerciseRepository.init(this);
        GameSettings.init(this);
        AnalysisSettings.init(this);
        SessionLog.init(this);
    }
}

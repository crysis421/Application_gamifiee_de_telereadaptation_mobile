package com.uphf.saes5;

import android.app.Application;
import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.session.SessionLog;
import com.uphf.saes5.data.settings.AnalysisSettings;
import com.uphf.saes5.data.settings.Consent;
import com.uphf.saes5.data.settings.GameSettings;

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
        Consent.init(this);
        SessionLog.init(this);
    }
}

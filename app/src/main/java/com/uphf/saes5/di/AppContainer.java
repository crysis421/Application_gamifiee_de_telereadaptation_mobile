package com.uphf.saes5.di;

import android.content.Context;

import com.uphf.saes5.SaeS5Application;
import com.uphf.saes5.data.session.SessionRecorder;
import com.uphf.saes5.data.settings.AnalysisSettingsRepository;
import com.uphf.saes5.data.settings.ConsentRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.data.settings.TestConditionRepository;
import com.uphf.saes5.data.storage.PreferenceStore;
import com.uphf.saes5.data.storage.SharedPreferencesStore;

import java.time.Clock;

/**
 * Crée une seule fois chaque dépôt de l'application et les met à disposition des écrans.
 *
 * <p>C'est l'injection de dépendances, écrite à la main : un ViewModel reçoit ses dépôts par
 * son constructeur (voir {@link ViewModelFactory}), sans aller les chercher dans un état
 * global. Les tests construisent les mêmes classes avec des stockages en mémoire.</p>
 */
public final class AppContainer {

    /** Nom historique du fichier de préférences : le changer ferait perdre les réglages. */
    private static final String PREFERENCES_NAME = "exergame_prefs";

    public final Clock clock = Clock.systemDefaultZone();
    public final GameSettingsRepository gameSettings;
    public final TestConditionRepository testConditions;
    public final AnalysisSettingsRepository analysisSettings;
    public final ConsentRepository consent;
    public final SessionRecorder sessionRecorder;

    public AppContainer(Context context) {
        Context app = context.getApplicationContext();
        PreferenceStore preferences = new SharedPreferencesStore(
                app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE));

        gameSettings = new GameSettingsRepository(preferences);
        testConditions = new TestConditionRepository(preferences, gameSettings);
        analysisSettings = new AnalysisSettingsRepository(preferences);
        consent = new ConsentRepository(preferences, clock);
        sessionRecorder = new SessionRecorder(gameSettings, analysisSettings, clock);
    }

    /** Le conteneur de l'application, depuis n'importe quel écran. */
    public static AppContainer from(Context context) {
        return ((SaeS5Application) context.getApplicationContext()).getContainer();
    }
}

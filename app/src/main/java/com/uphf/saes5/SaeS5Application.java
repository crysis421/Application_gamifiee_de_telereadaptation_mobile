package com.uphf.saes5;

import android.app.Application;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.session.SessionLog;
import com.uphf.saes5.di.AppContainer;

/**
 * Point d'entrée de l'application : crée le conteneur de dépendances avant le premier écran.
 */
public class SaeS5Application extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        ExerciseRepository.init(this);
        SessionLog.init(this);
    }

    public AppContainer getContainer() {
        return container;
    }
}

package com.uphf.saes5;

import android.app.Application;

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
    }

    public AppContainer getContainer() {
        return container;
    }
}

package com.uphf.saes5.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Modèle de détection de pose utilisé pendant les séances (US-1.4). */
public enum AnalysisModel {
    /** BlazePose « lite » : corps seul, pensé pour le mobile. Par défaut. */
    LITE("lite"),
    /** Holistic : corps, mains et visage. Plus précis, plus lent. */
    FULL("full");

    private final String key;

    AnalysisModel(String key) {
        this.key = key;
    }

    /** Clé stable, enregistrée dans les réglages et dans les journaux. */
    public String getKey() {
        return key;
    }

    /** Une valeur inconnue (ancienne version, faute de frappe) retombe sur le modèle léger. */
    @NonNull
    public static AnalysisModel fromKey(@Nullable String key) {
        for (AnalysisModel model : values()) {
            if (model.key.equals(key)) return model;
        }
        return LITE;
    }
}

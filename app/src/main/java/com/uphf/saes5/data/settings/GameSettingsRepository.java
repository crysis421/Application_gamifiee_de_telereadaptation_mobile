package com.uphf.saes5.data.settings;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.uphf.saes5.data.storage.PreferenceStore;
import com.uphf.saes5.domain.model.GameFeature;

/**
 * Interrupteurs des éléments de gamification (US-4.1) et nom de la condition de test (US-7.3).
 *
 * <p>Chaque élément de jeu a son interrupteur, persisté et appliqué à toute la session. Tous
 * sont actifs par défaut : l'application se comporte normalement tant que le chercheur n'a
 * rien désactivé.</p>
 */
public final class GameSettingsRepository {

    private static final String KEY_PREFIX = "setting_";
    private static final String KEY_CONDITION_NAME = "condition_name";

    private final PreferenceStore store;

    public GameSettingsRepository(PreferenceStore store) {
        this.store = store;
    }

    /** Vrai tant que l'élément n'a pas été désactivé : l'absence de réglage n'éteint rien. */
    public boolean isEnabled(@NonNull GameFeature feature) {
        return store.getBoolean(KEY_PREFIX + feature.getKey(), true);
    }

    public void setEnabled(@NonNull GameFeature feature, boolean enabled) {
        store.putBoolean(KEY_PREFIX + feature.getKey(), enabled);
    }

    /**
     * Le nom donné à la condition de test courante, ou une chaîne vide.
     *
     * <p>Il accompagne chaque séance journalisée : la combinaison de réglages est déjà dans
     * {@link #snapshot()}, ce nom sert à la désigner dans l'analyse.</p>
     */
    public String getConditionName() {
        return store.getString(KEY_CONDITION_NAME, "");
    }

    public void setConditionName(String name) {
        store.putString(KEY_CONDITION_NAME, name == null ? "" : name.trim());
    }

    /**
     * La configuration active, sous une forme stable destinée aux journaux (US-7.1) et à
     * l'identification d'une condition de test (US-7.3).
     *
     * <p>Exemple : {@code daily_exercise=1;favorites=0;stars=1;statistics=1;sounds=1}. Les
     * éléments sont toujours dans l'ordre de {@link GameFeature}, pour que deux sessions
     * restent comparables.</p>
     */
    public String snapshot() {
        return snapshotOf(GameFeature.values());
    }

    @VisibleForTesting
    String snapshotOf(GameFeature[] features) {
        StringBuilder result = new StringBuilder();
        for (GameFeature feature : features) {
            if (result.length() > 0) {
                result.append(';');
            }
            result.append(feature.getKey()).append('=').append(isEnabled(feature) ? '1' : '0');
        }
        return result.toString();
    }
}

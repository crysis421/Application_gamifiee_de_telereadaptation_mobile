package com.uphf.saes5.data.settings;

import androidx.annotation.VisibleForTesting;

import com.uphf.saes5.data.storage.PreferenceStore;

import java.time.Clock;

/**
 * Consentement éclairé du participant (US-9.1).
 *
 * <p>Le consentement porte sur une version de la notice : si ce que l'application collecte
 * change, {@link #CURRENT_VERSION} augmente et la notice est présentée de nouveau.</p>
 */
public final class ConsentRepository {

    /** À incrémenter à chaque changement de fond de la notice. */
    public static final int CURRENT_VERSION = 1;
    @VisibleForTesting
    static final int NONE = 0;

    private static final String KEY_VERSION = "consent_version";
    private static final String KEY_AT = "consent_at";

    private final PreferenceStore store;
    private final Clock clock;

    public ConsentRepository(PreferenceStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /** Vrai si la notice actuelle n'a pas encore été acceptée. */
    public boolean isRequired() {
        return isRequired(store.getInt(KEY_VERSION, NONE), CURRENT_VERSION);
    }

    @VisibleForTesting
    static boolean isRequired(int acceptedVersion, int currentVersion) {
        return acceptedVersion < currentVersion;
    }

    public void accept() {
        store.putInt(KEY_VERSION, CURRENT_VERSION);
        store.putLong(KEY_AT, clock.millis());
    }

    /** Date d'acceptation, ou 0 si la notice n'a jamais été acceptée. */
    public long acceptedAt() {
        return store.getLong(KEY_AT, 0L);
    }

    /** Retrait du consentement : la notice sera de nouveau demandée au prochain lancement. */
    public void withdraw() {
        store.remove(KEY_VERSION, KEY_AT);
    }
}

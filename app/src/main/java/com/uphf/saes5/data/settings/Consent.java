package com.uphf.saes5.data.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * Consentement éclairé du participant (US-9.1).
 *
 * <p>Le consentement porte sur une version de la notice : si ce que l'application collecte
 * change, {@link #CURRENT_VERSION} augmente et la notice est présentée de nouveau.</p>
 */
public final class Consent {

    /** À incrémenter à chaque changement de fond de la notice. */
    public static final int CURRENT_VERSION = 1;
    static final int NONE = 0;

    private static final String PREFS_NAME = "exergame_prefs";
    private static final String KEY_VERSION = "consent_version";
    private static final String KEY_AT = "consent_at";

    @Nullable
    private static SharedPreferences preferences;

    private Consent() {}

    public static synchronized void init(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Vrai si la notice actuelle n'a pas encore été acceptée. */
    public static boolean isRequired() {
        if (preferences == null) return false;
        return isRequired(preferences.getInt(KEY_VERSION, NONE), CURRENT_VERSION);
    }

    static boolean isRequired(int acceptedVersion, int currentVersion) {
        return acceptedVersion < currentVersion;
    }

    public static void accept() {
        if (preferences == null) return;
        preferences.edit()
                .putInt(KEY_VERSION, CURRENT_VERSION)
                .putLong(KEY_AT, System.currentTimeMillis())
                .apply();
    }

    /** Date d'acceptation, ou 0 si la notice n'a jamais été acceptée. */
    public static long acceptedAt() {
        if (preferences == null) return 0L;
        return preferences.getLong(KEY_AT, 0L);
    }

    /** Retrait du consentement : la notice sera de nouveau demandée au prochain lancement. */
    public static void withdraw() {
        if (preferences == null) return;
        preferences.edit().remove(KEY_VERSION).remove(KEY_AT).apply();
    }
}

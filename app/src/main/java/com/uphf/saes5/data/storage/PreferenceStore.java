package com.uphf.saes5.data.storage;

import androidx.annotation.Nullable;

import java.util.Set;

/**
 * Stockage clé-valeur des réglages et petits états de l'application.
 *
 * <p>Les dépôts n'utilisent que cette interface : dans l'application, elle s'appuie sur les
 * SharedPreferences ({@link SharedPreferencesStore}) ; en test, sur une table en mémoire. Les
 * dépôts restent ainsi testables sans Android.</p>
 *
 * <p>Chaque écriture est appliquée immédiatement.</p>
 */
public interface PreferenceStore {

    String getString(String key, @Nullable String def);

    /** Écrit la valeur, ou retire la clé si elle est {@code null}. */
    void putString(String key, @Nullable String value);

    boolean getBoolean(String key, boolean def);

    void putBoolean(String key, boolean value);

    int getInt(String key, int def);

    void putInt(String key, int value);

    long getLong(String key, long def);

    void putLong(String key, long value);

    /** Copie non modifiable de l'ensemble, vide si la clé est absente. */
    Set<String> getStringSet(String key);

    void putStringSet(String key, Set<String> value);

    void remove(String... keys);
}

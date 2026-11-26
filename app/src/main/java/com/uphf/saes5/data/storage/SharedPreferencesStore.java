package com.uphf.saes5.data.storage;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** {@link PreferenceStore} adossé aux SharedPreferences d'Android. */
public final class SharedPreferencesStore implements PreferenceStore {

    private final SharedPreferences preferences;

    public SharedPreferencesStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getString(String key, @Nullable String def) {
        return preferences.getString(key, def);
    }

    @Override
    public void putString(String key, @Nullable String value) {
        if (value == null) {
            preferences.edit().remove(key).apply();
        } else {
            preferences.edit().putString(key, value).apply();
        }
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return preferences.getBoolean(key, def);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    @Override
    public int getInt(String key, int def) {
        return preferences.getInt(key, def);
    }

    @Override
    public void putInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    @Override
    public long getLong(String key, long def) {
        return preferences.getLong(key, def);
    }

    @Override
    public void putLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }

    @Override
    public Set<String> getStringSet(String key) {
        // Copie défensive : l'ensemble renvoyé par SharedPreferences ne doit jamais être modifié.
        return Collections.unmodifiableSet(
                new HashSet<>(preferences.getStringSet(key, Collections.emptySet())));
    }

    @Override
    public void putStringSet(String key, Set<String> value) {
        preferences.edit().putStringSet(key, new HashSet<>(value)).apply();
    }

    @Override
    public void remove(String... keys) {
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : keys) editor.remove(key);
        editor.apply();
    }
}

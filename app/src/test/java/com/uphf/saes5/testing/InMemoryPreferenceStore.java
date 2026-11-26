package com.uphf.saes5.testing;

import androidx.annotation.Nullable;

import com.uphf.saes5.data.storage.PreferenceStore;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Préférences en mémoire, pour tester les dépôts sans Android. */
public final class InMemoryPreferenceStore implements PreferenceStore {

    private final Map<String, Object> values = new HashMap<>();

    @Override
    public String getString(String key, @Nullable String def) {
        Object value = values.get(key);
        return value instanceof String ? (String) value : def;
    }

    @Override
    public void putString(String key, @Nullable String value) {
        if (value == null) values.remove(key);
        else values.put(key, value);
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : def;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        values.put(key, value);
    }

    @Override
    public int getInt(String key, int def) {
        Object value = values.get(key);
        return value instanceof Integer ? (Integer) value : def;
    }

    @Override
    public void putInt(String key, int value) {
        values.put(key, value);
    }

    @Override
    public long getLong(String key, long def) {
        Object value = values.get(key);
        return value instanceof Long ? (Long) value : def;
    }

    @Override
    public void putLong(String key, long value) {
        values.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key) {
        Object value = values.get(key);
        return value instanceof Set
                ? Collections.unmodifiableSet(new HashSet<>((Set<String>) value))
                : Collections.emptySet();
    }

    @Override
    public void putStringSet(String key, Set<String> value) {
        values.put(key, new HashSet<>(value));
    }

    @Override
    public void remove(String... keys) {
        for (String key : keys) values.remove(key);
    }

    /** Vrai si la clé a été écrite : sert à vérifier qu'un dépôt écrit bien sous l'ancienne clé. */
    public boolean contains(String key) {
        return values.containsKey(key);
    }
}

package com.uphf.saes5.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import com.uphf.saes5.data.settings.GameSettings;

/**
 * Une condition de test : une combinaison nommée d'éléments de jeu et de blocs de l'accueil
 * (US-7.3).
 *
 * <p>Le chercheur la compose une fois, l'enregistre, puis la réapplique d'un toucher avant
 * chaque participant, sans risquer d'oublier un interrupteur. Son nom accompagne ensuite
 * chaque séance journalisée.</p>
 *
 * <p>Les valeurs sont rangées par clé stable ({@link GameSettings.Feature#getKey()}), comme
 * dans les journaux : renommer une constante Java ne casse pas les conditions enregistrées.
 * Un interrupteur ajouté après l'enregistrement reprend sa valeur par défaut (activé), pour
 * que réappliquer une condition donne toujours le même résultat.</p>
 */
public final class TestCondition {

    /** Reçoit les valeurs à appliquer : {@code GameSettings::setEnabled} dans l'application. */
    public interface Setter {
        void set(GameSettings.Feature feature, boolean enabled);
    }

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<TestCondition>>() {}.getType();

    private final String name;
    private final Map<String, Boolean> features;

    private TestCondition(String name, Map<String, Boolean> features) {
        this.name = name;
        this.features = features;
    }

    /** Photographie la configuration courante sous un nom. */
    public static TestCondition capture(@NonNull String name,
                                        @NonNull Predicate<GameSettings.Feature> isEnabled) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (GameSettings.Feature feature : GameSettings.Feature.values()) {
            values.put(feature.getKey(), isEnabled.test(feature));
        }
        return new TestCondition(name.trim(), values);
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled(GameSettings.Feature feature) {
        Boolean value = features == null ? null : features.get(feature.getKey());
        return value == null || value;
    }

    public void applyTo(@NonNull Setter setter) {
        for (GameSettings.Feature feature : GameSettings.Feature.values()) {
            setter.set(feature, isEnabled(feature));
        }
    }

    /** Ajoute la condition, ou remplace celle qui porte déjà ce nom (casse ignorée). */
    public static List<TestCondition> save(List<TestCondition> conditions,
                                           TestCondition condition) {
        List<TestCondition> result = remove(conditions, condition.getName());
        result.add(condition);
        result.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    public static List<TestCondition> remove(List<TestCondition> conditions, String name) {
        List<TestCondition> result = new ArrayList<>();
        for (TestCondition condition : conditions) {
            if (!condition.getName().equalsIgnoreCase(name)) result.add(condition);
        }
        return result;
    }

    public static String toJson(List<TestCondition> conditions) {
        return GSON.toJson(conditions, LIST_TYPE);
    }

    /** Liste vide si le texte est absent ou illisible : on ne bloque jamais l'écran. */
    public static List<TestCondition> fromJson(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            List<TestCondition> read = GSON.fromJson(json, LIST_TYPE);
            if (read == null) return new ArrayList<>();
            List<TestCondition> valid = new ArrayList<>();
            for (TestCondition condition : read) {
                if (condition != null && condition.name != null) valid.add(condition);
            }
            return valid;
        } catch (JsonParseException | IllegalStateException e) {
            return new ArrayList<>(Collections.emptyList());
        }
    }
}

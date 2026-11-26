package com.uphf.saes5.domain.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Une condition de test : une combinaison nommée d'éléments de jeu et de blocs de l'accueil
 * (US-7.3).
 *
 * <p>Le chercheur la compose une fois, l'enregistre, puis la réapplique d'un toucher avant
 * chaque participant, sans risquer d'oublier un interrupteur. Son nom accompagne ensuite
 * chaque séance journalisée.</p>
 *
 * <p>Les valeurs sont rangées par clé stable ({@link GameFeature#getKey()}), comme dans les
 * journaux : renommer une constante Java ne casse pas les conditions enregistrées. Un
 * interrupteur ajouté après l'enregistrement reprend sa valeur par défaut (activé), pour que
 * réappliquer une condition donne toujours le même résultat.</p>
 */
public final class TestCondition {

    /** Reçoit les valeurs à appliquer, élément par élément. */
    public interface Setter {
        void set(GameFeature feature, boolean enabled);
    }

    // Noms de champs relus par Gson dans les conditions déjà enregistrées : ne pas renommer.
    private final String name;
    private final Map<String, Boolean> features;

    private TestCondition(String name, Map<String, Boolean> features) {
        this.name = name;
        this.features = features;
    }

    /** Photographie la configuration courante sous un nom. */
    public static TestCondition capture(@NonNull String name,
                                        @NonNull Predicate<GameFeature> isEnabled) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (GameFeature feature : GameFeature.values()) {
            values.put(feature.getKey(), isEnabled.test(feature));
        }
        return new TestCondition(name.trim(), values);
    }

    /** Nom de la condition, ou {@code null} pour une condition enregistrée illisible. */
    public String getName() {
        return name;
    }

    public boolean isEnabled(GameFeature feature) {
        Boolean value = features == null ? null : features.get(feature.getKey());
        return value == null || value;
    }

    public void applyTo(@NonNull Setter setter) {
        for (GameFeature feature : GameFeature.values()) {
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
}

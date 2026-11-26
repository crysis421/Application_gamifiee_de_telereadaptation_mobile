package com.uphf.saes5.data.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import com.uphf.saes5.data.storage.PreferenceStore;
import com.uphf.saes5.domain.model.TestCondition;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Conditions de test enregistrées (US-7.3), conservées en JSON dans les préférences.
 *
 * <p>Enregistrer photographie les interrupteurs de {@link GameSettingsRepository} ;
 * appliquer les remet tous dans l'état photographié et reprend le nom pour les journaux.</p>
 */
public final class TestConditionRepository {

    private static final String KEY_SAVED_CONDITIONS = "saved_conditions";
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<TestCondition>>() {}.getType();

    private final PreferenceStore store;
    private final GameSettingsRepository gameSettings;

    public TestConditionRepository(PreferenceStore store, GameSettingsRepository gameSettings) {
        this.store = store;
        this.gameSettings = gameSettings;
    }

    /** Conditions enregistrées, par ordre alphabétique. */
    public List<TestCondition> getSaved() {
        return fromJson(store.getString(KEY_SAVED_CONDITIONS, null));
    }

    /** Enregistre la configuration courante sous ce nom, en remplaçant un homonyme. */
    public void saveCurrent(@NonNull String name) {
        TestCondition condition = TestCondition.capture(name, gameSettings::isEnabled);
        store.putString(KEY_SAVED_CONDITIONS, toJson(TestCondition.save(getSaved(), condition)));
        gameSettings.setConditionName(condition.getName());
    }

    /** Applique tous les interrupteurs de la condition, et reprend son nom pour les journaux. */
    public void apply(@NonNull TestCondition condition) {
        condition.applyTo(gameSettings::setEnabled);
        gameSettings.setConditionName(condition.getName());
    }

    public void delete(@NonNull String name) {
        store.putString(KEY_SAVED_CONDITIONS, toJson(TestCondition.remove(getSaved(), name)));
    }

    @VisibleForTesting
    static String toJson(List<TestCondition> conditions) {
        return GSON.toJson(conditions, LIST_TYPE);
    }

    /** Liste vide si le texte est absent ou illisible : on ne bloque jamais l'écran. */
    @VisibleForTesting
    static List<TestCondition> fromJson(@Nullable String json) {
        List<TestCondition> valid = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return valid;
        try {
            List<TestCondition> read = GSON.fromJson(json, LIST_TYPE);
            if (read == null) return valid;
            for (TestCondition condition : read) {
                if (condition != null && condition.getName() != null) valid.add(condition);
            }
            return valid;
        } catch (JsonParseException | IllegalStateException e) {
            return new ArrayList<>();
        }
    }
}

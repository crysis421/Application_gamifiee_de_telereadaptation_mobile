package com.uphf.saes5.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.time.Clock;

/**
 * Les dépôts relisent les réglages déjà enregistrés sur les téléphones, sous leurs clés
 * d'origine : un remaniement ne doit faire perdre ni condition de test, ni consentement.
 */
public class PreferenceKeysCompatibilityTest {

    @Test
    public void settingsStoredByThePreviousVersion_areReadBack() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        store.putBoolean("setting_stars", false);
        store.putString("condition_name", "A");
        store.putString("saved_conditions",
                "[{\"name\":\"A\",\"features\":{\"stars\":false}}]");
        store.putString("analysis_model", "full");
        store.putInt("analysis_frame_stride", 2);
        store.putInt("consent_version", 1);
        store.putLong("consent_at", 123L);

        GameSettingsRepository game = new GameSettingsRepository(store);
        TestConditionRepository conditions = new TestConditionRepository(store, game);
        AnalysisSettingsRepository analysis = new AnalysisSettingsRepository(store);
        ConsentRepository consent = new ConsentRepository(store, Clock.systemUTC());

        assertFalse(game.isEnabled(GameFeature.STARS));
        assertTrue(game.isEnabled(GameFeature.SOUNDS));
        assertEquals("A", game.getConditionName());
        assertEquals(1, conditions.getSaved().size());
        assertEquals(AnalysisModel.FULL, analysis.getModel());
        assertEquals(2, analysis.getFrameStride());
        assertFalse(consent.isRequired());
        assertEquals(123L, consent.acceptedAt());
        assertEquals("daily_exercise=1;favorites=1;stars=0;statistics=1;sounds=1;arcade=1;"
                + "home_steps=1;home_exercises_done=1;home_catalog=1", game.snapshot());
    }

    @Test
    public void settingsWrittenNow_useThePreviousKeys() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        GameSettingsRepository game = new GameSettingsRepository(store);

        game.setEnabled(GameFeature.ARCADE, false);
        new AnalysisSettingsRepository(store).setFrameStride(3);
        new ConsentRepository(store, Clock.systemUTC()).accept();

        assertTrue(store.contains("setting_arcade"));
        assertEquals(3, store.getInt("analysis_frame_stride", 0));
        assertEquals(1, store.getInt("consent_version", 0));
    }
}

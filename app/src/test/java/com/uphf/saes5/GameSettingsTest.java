package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration des éléments de gamification (US-4.1).
 *
 * <p>Sans {@code init(Context)}, les réglages ne sont adossés à aucun stockage : tout est
 * actif. C'est ce que ces tests vérifient, ainsi que la stabilité du format de la condition
 * de test, dont dépend la comparabilité des sessions (US-7.3).</p>
 */
public class GameSettingsTest {

    @Test
    public void everyFeature_isEnabled_whenNoSettingWasStored() {
        for (GameSettings.Feature feature : GameSettings.Feature.values()) {
            assertTrue(feature.name(), GameSettings.isEnabled(feature));
        }
    }

    @Test
    public void snapshot_listsEveryFeature_inDeclarationOrder() {
        assertEquals(
                "daily_exercise=1;favorites=1;stars=1;statistics=1;sounds=1;arcade=1",
                GameSettings.snapshot());
    }

    @Test
    public void snapshot_isEmpty_whenThereIsNothingToReport() {
        assertEquals("", GameSettings.snapshotOf(new GameSettings.Feature[0]));
    }

    @Test
    public void featureKeys_areUniqueAndStable() {
        // Les clés partent dans les journaux : un doublon rendrait une session illisible.
        Set<String> keys = new HashSet<>();
        for (GameSettings.Feature feature : GameSettings.Feature.values()) {
            assertTrue("clé en double : " + feature.getKey(), keys.add(feature.getKey()));
            assertTrue("clé vide : " + feature.name(), !feature.getKey().isEmpty());
        }
        assertEquals(GameSettings.Feature.values().length, keys.size());
    }

    @Test
    public void everyFeature_carriesALabelAndAnExplanation() {
        // L'écran de réglages se construit à partir de l'énumération : une ressource
        // manquante s'y traduirait par une ligne vide.
        for (GameSettings.Feature feature : GameSettings.Feature.values()) {
            assertTrue(feature.name() + " sans libellé", feature.getLabelRes() != 0);
            assertTrue(feature.name() + " sans explication", feature.getHelpRes() != 0);
        }
    }
}

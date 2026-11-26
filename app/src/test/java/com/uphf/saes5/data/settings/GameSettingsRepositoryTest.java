package com.uphf.saes5.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.testing.InMemoryPreferenceStore;
import com.uphf.saes5.ui.common.FeatureLabels;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration des éléments de gamification (US-4.1).
 *
 * <p>Tant que rien n'est enregistré, tout est actif. Ces tests vérifient aussi la stabilité
 * du format de la condition de test, dont dépend la comparabilité des sessions (US-7.3).</p>
 */
public class GameSettingsRepositoryTest {

    private final GameSettingsRepository settings =
            new GameSettingsRepository(new InMemoryPreferenceStore());

    @Test
    public void everyFeature_isEnabled_whenNoSettingWasStored() {
        for (GameFeature feature : GameFeature.values()) {
            assertTrue(feature.name(), settings.isEnabled(feature));
        }
    }

    @Test
    public void aDisabledFeature_staysDisabled() {
        settings.setEnabled(GameFeature.SOUNDS, false);

        assertFalse(settings.isEnabled(GameFeature.SOUNDS));
        assertTrue(settings.isEnabled(GameFeature.STARS));
    }

    @Test
    public void snapshot_listsEveryFeature_inDeclarationOrder() {
        assertEquals(
                "daily_exercise=1;favorites=1;stars=1;statistics=1;sounds=1;arcade=1;"
                        + "home_steps=1;home_exercises_done=1;home_catalog=1",
                settings.snapshot());
    }

    @Test
    public void snapshot_isEmpty_whenThereIsNothingToReport() {
        assertEquals("", settings.snapshotOf(new GameFeature[0]));
    }

    @Test
    public void conditionName_isTrimmed_andEmptyByDefault() {
        assertEquals("", settings.getConditionName());

        settings.setConditionName("  A - sans son ");

        assertEquals("A - sans son", settings.getConditionName());
    }

    @Test
    public void featureKeys_areUniqueAndStable() {
        // Les clés partent dans les journaux : un doublon rendrait une session illisible.
        Set<String> keys = new HashSet<>();
        for (GameFeature feature : GameFeature.values()) {
            assertTrue("clé en double : " + feature.getKey(), keys.add(feature.getKey()));
            assertTrue("clé vide : " + feature.name(), !feature.getKey().isEmpty());
        }
        assertEquals(GameFeature.values().length, keys.size());
    }

    @Test
    public void homeBlocks_areInTheHomeSection_andGameElementsInTheGameSection() {
        assertEquals(GameFeature.Group.HOME, GameFeature.HOME_STEPS.getGroup());
        assertEquals(GameFeature.Group.HOME, GameFeature.HOME_EXERCISES_DONE.getGroup());
        assertEquals(GameFeature.Group.HOME, GameFeature.HOME_CATALOG.getGroup());
        assertEquals(GameFeature.Group.GAME, GameFeature.ARCADE.getGroup());
    }

    @Test
    public void everyFeature_carriesALabelAndAnExplanation() {
        // L'écran de réglages se construit à partir de l'énumération : une ressource
        // manquante s'y traduirait par une ligne vide.
        for (GameFeature feature : GameFeature.values()) {
            assertTrue(feature.name() + " sans libellé", FeatureLabels.label(feature) != 0);
            assertTrue(feature.name() + " sans explication", FeatureLabels.help(feature) != 0);
        }
    }
}

package com.uphf.saes5.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.TestCondition;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Conditions de test enregistrées puis réappliquées (US-7.3). */
public class TestConditionRepositoryTest {

    private final InMemoryPreferenceStore store = new InMemoryPreferenceStore();
    private final GameSettingsRepository gameSettings = new GameSettingsRepository(store);
    private final TestConditionRepository conditions =
            new TestConditionRepository(store, gameSettings);

    private static TestCondition withoutSounds(String name) {
        return TestCondition.capture(name, feature -> feature != GameFeature.SOUNDS);
    }

    @Test
    public void savingThenApplying_restoresEverySwitch_andTheName() {
        gameSettings.setEnabled(GameFeature.STARS, false);
        conditions.saveCurrent("A");
        gameSettings.setEnabled(GameFeature.STARS, true);
        gameSettings.setConditionName("B");

        conditions.apply(conditions.getSaved().get(0));

        assertFalse(gameSettings.isEnabled(GameFeature.STARS));
        assertEquals("A", gameSettings.getConditionName());
    }

    @Test
    public void delete_removesOnlyThatCondition() {
        conditions.saveCurrent("A");
        conditions.saveCurrent("B");

        conditions.delete("a");

        assertEquals(1, conditions.getSaved().size());
        assertEquals("B", conditions.getSaved().get(0).getName());
    }

    @Test
    public void aFeatureAddedAfterTheConditionWasSaved_isApplied_enabled() {
        // Condition enregistrée avant l'existence d'un interrupteur : il reprend sa valeur par
        // défaut, et non celle qui traînait, pour que la condition reste reproductible.
        TestCondition old = TestConditionRepository.fromJson(
                "[{\"name\":\"Ancienne\",\"features\":{\"sounds\":false}}]").get(0);
        Map<GameFeature, Boolean> applied = new EnumMap<>(GameFeature.class);

        old.applyTo(applied::put);

        assertFalse(applied.get(GameFeature.SOUNDS));
        assertTrue(applied.get(GameFeature.HOME_STEPS));
    }

    @Test
    public void json_roundTrip_keepsNamesAndValues() {
        List<TestCondition> saved = new ArrayList<>();
        saved.add(withoutSounds("A - sans son"));
        saved.add(TestCondition.capture("B - tout", feature -> true));

        List<TestCondition> read =
                TestConditionRepository.fromJson(TestConditionRepository.toJson(saved));

        assertEquals(2, read.size());
        assertEquals("A - sans son", read.get(0).getName());
        assertFalse(read.get(0).isEnabled(GameFeature.SOUNDS));
        assertTrue(read.get(1).isEnabled(GameFeature.SOUNDS));
    }

    @Test
    public void unreadableJson_meansNoCondition() {
        assertTrue(TestConditionRepository.fromJson("pas du json").isEmpty());
        assertTrue(TestConditionRepository.fromJson(null).isEmpty());
    }
}

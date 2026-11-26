package com.uphf.saes5.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.uphf.saes5.data.settings.GameSettings;

/** Conditions de test nommées, enregistrées et réappliquées (US-7.3). */
public class TestConditionTest {

    private static TestCondition withoutSounds(String name) {
        return TestCondition.capture(name,
                feature -> feature != GameSettings.Feature.SOUNDS);
    }

    @Test
    public void capture_recordsEveryFeature() {
        TestCondition condition = withoutSounds("B");

        assertEquals("B", condition.getName());
        assertFalse(condition.isEnabled(GameSettings.Feature.SOUNDS));
        assertTrue(condition.isEnabled(GameSettings.Feature.ARCADE));
    }

    @Test
    public void apply_setsEveryFeature() {
        Map<GameSettings.Feature, Boolean> applied = new EnumMap<>(GameSettings.Feature.class);

        withoutSounds("B").applyTo(applied::put);

        assertEquals(GameSettings.Feature.values().length, applied.size());
        assertFalse(applied.get(GameSettings.Feature.SOUNDS));
        assertTrue(applied.get(GameSettings.Feature.FAVORITES));
    }

    @Test
    public void aFeatureAddedAfterTheConditionWasSaved_isApplied_enabled() {
        // Condition enregistrée avant l'existence d'un interrupteur : il reprend sa valeur par
        // défaut, et non celle qui traînait, pour que la condition reste reproductible.
        TestCondition old = TestCondition.fromJson(
                "[{\"name\":\"Ancienne\",\"features\":{\"sounds\":false}}]").get(0);
        Map<GameSettings.Feature, Boolean> applied = new EnumMap<>(GameSettings.Feature.class);

        old.applyTo(applied::put);

        assertFalse(applied.get(GameSettings.Feature.SOUNDS));
        assertTrue(applied.get(GameSettings.Feature.HOME_STEPS));
    }

    @Test
    public void json_roundTrip_keepsNamesAndValues() {
        List<TestCondition> conditions = new ArrayList<>();
        conditions.add(withoutSounds("A - sans son"));
        conditions.add(TestCondition.capture("B - tout", feature -> true));

        List<TestCondition> read = TestCondition.fromJson(TestCondition.toJson(conditions));

        assertEquals(2, read.size());
        assertEquals("A - sans son", read.get(0).getName());
        assertFalse(read.get(0).isEnabled(GameSettings.Feature.SOUNDS));
        assertTrue(read.get(1).isEnabled(GameSettings.Feature.SOUNDS));
    }

    @Test
    public void unreadableJson_meansNoCondition() {
        assertTrue(TestCondition.fromJson("pas du json").isEmpty());
        assertTrue(TestCondition.fromJson(null).isEmpty());
    }

    @Test
    public void savingUnderAnExistingName_replacesIt() {
        List<TestCondition> conditions = new ArrayList<>(
                Collections.singletonList(TestCondition.capture("A", feature -> true)));

        List<TestCondition> saved = TestCondition.save(conditions, withoutSounds("a"));

        assertEquals(1, saved.size());
        assertFalse(saved.get(0).isEnabled(GameSettings.Feature.SOUNDS));
    }

    @Test
    public void conditions_areKeptInAlphabeticalOrder() {
        List<TestCondition> saved = TestCondition.save(
                TestCondition.save(new ArrayList<>(), withoutSounds("B")), withoutSounds("A"));

        assertEquals("A", saved.get(0).getName());
        assertEquals("B", saved.get(1).getName());
    }

    @Test
    public void remove_dropsOnlyThatCondition() {
        List<TestCondition> conditions = TestCondition.save(
                TestCondition.save(new ArrayList<>(), withoutSounds("A")), withoutSounds("B"));

        List<TestCondition> left = TestCondition.remove(conditions, "A");

        assertEquals(1, left.size());
        assertEquals("B", left.get(0).getName());
    }
}

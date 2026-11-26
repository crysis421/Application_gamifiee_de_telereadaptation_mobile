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

/** Conditions de test nommées, enregistrées et réappliquées (US-7.3). */
public class TestConditionTest {

    private static TestCondition withoutSounds(String name) {
        return TestCondition.capture(name,
                feature -> feature != GameFeature.SOUNDS);
    }

    @Test
    public void capture_recordsEveryFeature() {
        TestCondition condition = withoutSounds("B");

        assertEquals("B", condition.getName());
        assertFalse(condition.isEnabled(GameFeature.SOUNDS));
        assertTrue(condition.isEnabled(GameFeature.ARCADE));
    }

    @Test
    public void apply_setsEveryFeature() {
        Map<GameFeature, Boolean> applied = new EnumMap<>(GameFeature.class);

        withoutSounds("B").applyTo(applied::put);

        assertEquals(GameFeature.values().length, applied.size());
        assertFalse(applied.get(GameFeature.SOUNDS));
        assertTrue(applied.get(GameFeature.FAVORITES));
    }

    @Test
    public void savingUnderAnExistingName_replacesIt() {
        List<TestCondition> conditions = new ArrayList<>(
                Collections.singletonList(TestCondition.capture("A", feature -> true)));

        List<TestCondition> saved = TestCondition.save(conditions, withoutSounds("a"));

        assertEquals(1, saved.size());
        assertFalse(saved.get(0).isEnabled(GameFeature.SOUNDS));
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

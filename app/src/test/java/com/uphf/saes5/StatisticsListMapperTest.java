package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StatisticsListMapperTest {
    @Test
    public void fromMuscles_preservesProgressCountsAndNoDataState() {
        List<StatisticListItem> result = StatisticsListMapper.fromMuscles(Arrays.asList(
                new MuscleStatistic("Jambes", 68, 6, true),
                new MuscleStatistic("Bras", 0, 0, false)));

        assertEquals("Jambes", result.get(0).getTitle());
        assertEquals(68, result.get(0).getScore());
        assertEquals(6, result.get(0).getSessionCount());
        assertTrue(result.get(0).hasData());
        assertNull(result.get(0).getPerfectPercentage());
        assertFalse(result.get(1).hasData());
    }

    @Test
    public void fromExercises_preservesAveragePerfectRateAndSessionCount() {
        ExerciseStatistic squat = new ExerciseStatistic(
                "squat", "Squat", "Jambes", 92, 67, 3);

        StatisticListItem result = StatisticsListMapper.fromExercises(
                Collections.singletonList(squat)).get(0);

        assertEquals("Squat", result.getTitle());
        assertEquals("Jambes", result.getSubtitle());
        assertEquals(92, result.getScore());
        assertEquals(Integer.valueOf(67), result.getPerfectPercentage());
        assertEquals(3, result.getSessionCount());
        assertTrue(result.hasData());
    }
}

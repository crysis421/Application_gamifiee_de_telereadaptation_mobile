package com.uphf.saes5.data.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/** Les pas affichés sont ceux du jour (US-5.1) : la lecture part de minuit. */
public class StepCounterTest {

    @Test
    public void startOfDay_isMidnightOfTheSameDay() {
        Calendar afternoon = Calendar.getInstance();
        afternoon.set(2026, Calendar.NOVEMBER, 23, 16, 42, 7);

        long midnight = StepCounter.startOfDay(afternoon.getTimeInMillis());

        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(midnight);
        assertEquals(23, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertTrue(midnight <= afternoon.getTimeInMillis());
    }

    @Test
    public void startOfDay_atMidnight_isItself() {
        Calendar midnight = Calendar.getInstance();
        midnight.set(2026, Calendar.NOVEMBER, 23, 0, 0, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        assertEquals(midnight.getTimeInMillis(),
                StepCounter.startOfDay(midnight.getTimeInMillis()));
    }
}

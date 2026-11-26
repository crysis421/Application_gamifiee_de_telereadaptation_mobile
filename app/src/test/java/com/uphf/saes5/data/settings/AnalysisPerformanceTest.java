package com.uphf.saes5.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.domain.movement.LatencyTracker;

/** Réglages d'analyse et mesure de latence (US-1.4). */
public class AnalysisPerformanceTest {

    @Test
    public void strideOne_analyzesEveryFrame() {
        for (long frame = 0; frame < 5; frame++) {
            assertTrue(AnalysisSettings.shouldAnalyze(frame, 1));
        }
    }

    @Test
    public void strideThree_analyzesOneFrameOutOfThree() {
        int analyzed = 0;
        for (long frame = 0; frame < 9; frame++) {
            if (AnalysisSettings.shouldAnalyze(frame, 3)) analyzed++;
        }
        assertEquals(3, analyzed);
    }

    @Test
    public void invalidStride_fallsBackToEveryFrame() {
        assertTrue(AnalysisSettings.shouldAnalyze(1, 0));
        assertEquals(1, AnalysisSettings.clampStride(0));
        assertEquals(AnalysisSettings.MAX_FRAME_STRIDE,
                AnalysisSettings.clampStride(AnalysisSettings.MAX_FRAME_STRIDE + 4));
    }

    @Test
    public void defaults_areTheLightModelOnEveryFrame_withoutStorage() {
        assertEquals(AnalysisSettings.Model.LITE, AnalysisSettings.getModel());
        assertEquals(1, AnalysisSettings.getFrameStride());
        assertEquals("model=lite;frame_stride=1", AnalysisSettings.snapshot());
    }

    @Test
    public void unknownModelKey_fallsBackToLite() {
        assertEquals(AnalysisSettings.Model.LITE, AnalysisSettings.Model.fromKey("inconnu"));
        assertEquals(AnalysisSettings.Model.FULL, AnalysisSettings.Model.fromKey("full"));
    }

    @Test
    public void latency_isUnavailableBeforeAnyMeasure() {
        LatencyTracker tracker = new LatencyTracker();

        assertEquals(LatencyTracker.UNAVAILABLE, tracker.getAverageMillis());
        assertFalse(tracker.isAboveTarget());
    }

    @Test
    public void latency_averagesTheMeasures() {
        LatencyTracker tracker = new LatencyTracker();
        tracker.record(40);
        tracker.record(60);
        tracker.record(81);

        assertEquals(60, tracker.getAverageMillis());
        assertEquals(81, tracker.getLastMillis());
        assertFalse(tracker.isAboveTarget());
    }

    @Test
    public void latency_aboveTheTarget_isFlagged() {
        LatencyTracker tracker = new LatencyTracker();
        tracker.record(150);
        tracker.record(130);

        assertTrue(tracker.isAboveTarget());
    }

    @Test
    public void sessionEntry_withoutCamera_hasNoLatencyNorAnalysisSettings() {
        SessionEntry manual = new SessionEntry(0, "squat", "Squat", 3, 1, 10, 1_000,
                SessionEntry.SCORE_UNAVAILABLE, "", "");

        assertFalse(manual.hasLatency());
        assertEquals("", manual.getAnalysisSettings());
    }

    @Test
    public void sessionEntry_keepsTheMeasuredLatency() {
        SessionEntry tracked = new SessionEntry(0, "squat", "Squat", 3, 1, 10, 1_000, 85,
                "", "", "model=lite;frame_stride=2", 48);

        assertTrue(tracked.hasLatency());
        assertEquals(48, tracked.getAverageLatencyMillis());
        assertEquals("model=lite;frame_stride=2", tracked.getAnalysisSettings());
    }

    @Test
    public void latency_ignoresNegativeMeasures() {
        LatencyTracker tracker = new LatencyTracker();
        tracker.record(-5);

        assertEquals(LatencyTracker.UNAVAILABLE, tracker.getAverageMillis());
    }
}

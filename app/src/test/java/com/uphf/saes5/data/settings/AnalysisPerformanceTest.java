package com.uphf.saes5.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.FrameSampling;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.domain.movement.LatencyTracker;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

/** Réglages d'analyse et mesure de latence (US-1.4). */
public class AnalysisPerformanceTest {

    private final AnalysisSettingsRepository settings =
            new AnalysisSettingsRepository(new InMemoryPreferenceStore());

    @Test
    public void strideOne_analyzesEveryFrame() {
        for (long frame = 0; frame < 5; frame++) {
            assertTrue(FrameSampling.shouldAnalyze(frame, 1));
        }
    }

    @Test
    public void strideThree_analyzesOneFrameOutOfThree() {
        int analyzed = 0;
        for (long frame = 0; frame < 9; frame++) {
            if (FrameSampling.shouldAnalyze(frame, 3)) analyzed++;
        }
        assertEquals(3, analyzed);
    }

    @Test
    public void invalidStride_fallsBackToEveryFrame() {
        assertTrue(FrameSampling.shouldAnalyze(1, 0));
        assertEquals(1, FrameSampling.clampStride(0));
        assertEquals(FrameSampling.MAX_FRAME_STRIDE,
                FrameSampling.clampStride(FrameSampling.MAX_FRAME_STRIDE + 4));
    }

    @Test
    public void defaults_areTheLightModelOnEveryFrame_whenNothingIsStored() {
        assertEquals(AnalysisModel.LITE, settings.getModel());
        assertEquals(1, settings.getFrameStride());
        assertEquals("model=lite;frame_stride=1", settings.snapshot());
    }

    @Test
    public void unknownModelKey_fallsBackToLite() {
        assertEquals(AnalysisModel.LITE, AnalysisModel.fromKey("inconnu"));
        assertEquals(AnalysisModel.FULL, AnalysisModel.fromKey("full"));
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

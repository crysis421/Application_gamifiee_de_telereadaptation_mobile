package com.uphf.saes5.data.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.uphf.saes5.data.settings.AnalysisSettingsRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Ce qui accompagne chaque séance journalisée (US-7.1). */
public class SessionRecorderTest {

    private static final long NOW = 1_700_000_000_000L;

    private final InMemoryPreferenceStore store = new InMemoryPreferenceStore();
    private final GameSettingsRepository gameSettings = new GameSettingsRepository(store);
    private final AnalysisSettingsRepository analysisSettings =
            new AnalysisSettingsRepository(store);
    private final SessionRecorder recorder = new SessionRecorder(
            gameSettings, analysisSettings, Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC));

    private static final Exercise SQUAT =
            new Exercise("squat", "Squat", "Jambes", "Facile", 5, "Aucun", 3, "", 12);

    @Test
    public void aFinishedSession_carriesTheExerciseAndAllActiveSettings() {
        SessionEntry entry = recorder.build(SQUAT, 9, 2, 60_000, 84, 45, 1_200, false);

        assertEquals("squat", entry.getExerciseId());
        assertEquals(NOW, entry.getTimestampMillis());
        assertEquals(12, entry.getTargetReps());
        assertEquals(84, entry.getSimilarityScore());
        assertEquals(gameSettings.snapshot(), entry.getSettings());
        assertEquals(analysisSettings.snapshot(), entry.getAnalysisSettings());
        assertEquals(1_200, entry.getArcadePoints());
        assertFalse(entry.isAbandoned());
    }

    @Test
    public void anAbandonedSession_isMarkedSo() {
        assertTrue(recorder.build(SQUAT, 1, 0, 5_000,
                SessionEntry.SCORE_UNAVAILABLE, SessionEntry.LATENCY_UNAVAILABLE,
                SessionEntry.POINTS_UNAVAILABLE, true).isAbandoned());
    }

    @Test
    public void withoutCamera_noAnalysisSettingsAreRecorded() {
        SessionEntry manual = recorder.build(SQUAT, 3, 0, 5_000,
                SessionEntry.SCORE_UNAVAILABLE, SessionEntry.LATENCY_UNAVAILABLE,
                SessionEntry.POINTS_UNAVAILABLE, false);

        assertEquals("", manual.getAnalysisSettings());
        assertFalse(manual.hasLatency());
    }
}

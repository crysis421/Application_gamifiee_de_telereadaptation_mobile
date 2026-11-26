package com.uphf.saes5.data.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.uphf.saes5.data.settings.AnalysisSettings;
import com.uphf.saes5.data.settings.GameSettings;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.SessionEntry;

/** Ce qui accompagne chaque séance journalisée (US-7.1). */
public class SessionRecorderTest {

    private static final Exercise SQUAT =
            new Exercise("squat", "Squat", "Jambes", "Facile", 5, "Aucun", 3, "", 12);

    @Test
    public void aFinishedSession_carriesTheExerciseAndAllActiveSettings() {
        SessionEntry entry = SessionRecorder.build(SQUAT, 9, 2, 60_000, 84, 45, 1_200, false);

        assertEquals("squat", entry.getExerciseId());
        assertEquals(12, entry.getTargetReps());
        assertEquals(84, entry.getSimilarityScore());
        assertEquals(GameSettings.snapshot(), entry.getSettings());
        assertEquals(AnalysisSettings.snapshot(), entry.getAnalysisSettings());
        assertEquals(1_200, entry.getArcadePoints());
        assertFalse(entry.isAbandoned());
    }

    @Test
    public void anAbandonedSession_isMarkedSo() {
        assertTrue(SessionRecorder.build(SQUAT, 1, 0, 5_000,
                SessionEntry.SCORE_UNAVAILABLE, SessionEntry.LATENCY_UNAVAILABLE,
                SessionEntry.POINTS_UNAVAILABLE, true).isAbandoned());
    }

    @Test
    public void withoutCamera_noAnalysisSettingsAreRecorded() {
        SessionEntry manual = SessionRecorder.build(SQUAT, 3, 0, 5_000,
                SessionEntry.SCORE_UNAVAILABLE, SessionEntry.LATENCY_UNAVAILABLE,
                SessionEntry.POINTS_UNAVAILABLE, false);

        assertEquals("", manual.getAnalysisSettings());
        assertFalse(manual.hasLatency());
    }
}

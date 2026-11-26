package com.uphf.saes5.data.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.domain.model.SessionEntry;

import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Journal des séances : calculs et sélection par période (US-7.1 / US-7.2).
 *
 * <p>L'écriture du fichier est vérifiée dans un dossier temporaire.</p>
 */
public class SessionRepositoryTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private static final long JOUR = 24L * 60 * 60 * 1000;

    private static SessionEntry at(long timestamp) {
        return new SessionEntry(timestamp, "squat", "Squat", 10, 2, 10, 60_000L,
                SessionEntry.SCORE_UNAVAILABLE, "daily_exercise=1", "A");
    }

    @Test
    public void period_keepsOnlyEntriesInsideTheRange() {
        List<SessionEntry> entries = Arrays.asList(at(1_000), at(5_000), at(9_000));

        List<SessionEntry> kept = SessionRepository.filterByPeriod(entries, 2_000, 8_000);

        assertEquals(1, kept.size());
        assertEquals(5_000, kept.get(0).getTimestampMillis());
    }

    @Test
    public void period_boundsAreInclusive() {
        List<SessionEntry> entries = Arrays.asList(at(2_000), at(8_000));

        assertEquals(2, SessionRepository.filterByPeriod(entries, 2_000, 8_000).size());
    }

    @Test
    public void period_keepsEverything_whenUnbounded() {
        List<SessionEntry> entries = Arrays.asList(at(1), at(JOUR * 400));

        assertEquals(2, SessionRepository.filterByPeriod(entries, 0, Long.MAX_VALUE).size());
    }

    @Test
    public void period_onAnEmptyLog_yieldsNothing() {
        assertTrue(SessionRepository.filterByPeriod(new ArrayList<>(), 0, Long.MAX_VALUE).isEmpty());
    }

    @Test
    public void successRate_isRounded_andZeroWithoutAttempt() {
        SessionEntry none = new SessionEntry(0, "id", "nom", 0, 0, 10, 0,
                SessionEntry.SCORE_UNAVAILABLE, "", "");
        assertEquals(0, none.getSuccessRate());
        assertEquals(0, none.getTotalAttempts());

        // 10 validées sur 12 tentatives = 83,33 % -> 83
        SessionEntry mixed = new SessionEntry(0, "id", "nom", 10, 2, 10, 0,
                SessionEntry.SCORE_UNAVAILABLE, "", "");
        assertEquals(83, mixed.getSuccessRate());
        assertEquals(12, mixed.getTotalAttempts());
    }

    @Test
    public void goal_isReached_whenValidatedRepsMeetTarget() {
        assertTrue(new SessionEntry(0, "id", "nom", 10, 0, 10, 0, 0, "", "").isGoalReached());
        assertFalse(new SessionEntry(0, "id", "nom", 9, 0, 10, 0, 0, "", "").isGoalReached());
        // Objectif inconnu : on ne peut pas prétendre qu'il est atteint.
        assertFalse(new SessionEntry(0, "id", "nom", 9, 0, 0, 0, 0, "", "").isGoalReached());
    }

    @Test
    public void missingSimilarityScore_isDistinguishableFromZero() {
        // Un score absent ne doit pas se lire comme un mouvement parfaitement raté.
        assertFalse(new SessionEntry(0, "id", "nom", 1, 0, 1, 0,
                SessionEntry.SCORE_UNAVAILABLE, "", "").hasSimilarityScore());
        assertTrue(new SessionEntry(0, "id", "nom", 1, 0, 1, 0, 0, "", "").hasSimilarityScore());
    }

    @Test
    public void measuredSimilarityScore_isPreservedAndBoundedAsAPercentage() {
        SessionEntry measured = new SessionEntry(0, "id", "nom", 1, 0, 1, 0,
                87, "", "");
        SessionEntry aboveRange = new SessionEntry(0, "id", "nom", 1, 0, 1, 0,
                140, "", "");
        SessionEntry belowRange = new SessionEntry(0, "id", "nom", 1, 0, 1, 0,
                -20, "", "");

        assertTrue(measured.hasSimilarityScore());
        assertEquals(87, measured.getSimilarityScore());
        assertEquals(100, aboveRange.getSimilarityScore());
        assertEquals(0, belowRange.getSimilarityScore());
    }

    @Test
    public void nullTextFields_becomeEmptyStrings() {
        // Le journal ne doit jamais contenir la chaîne "null".
        SessionEntry entry = new SessionEntry(0, "id", "nom", 1, 0, 1, 0, 0, null, null);
        assertEquals("", entry.getSettings());
        assertEquals("", entry.getConditionName());
    }
    @Test
    public void recordedSessions_areReadBack_inOrder_andCleared() {
        SessionRepository sessions = repository();
        sessions.record(at(1_000));
        sessions.record(at(2_000));

        List<SessionEntry> read = repository().readAll();

        assertEquals(2, read.size());
        assertEquals(1_000, read.get(0).getTimestampMillis());
        assertEquals("A", read.get(1).getConditionName());
        assertEquals(2, sessions.count());

        sessions.clear();

        assertEquals(0, sessions.count());
    }

    @Test
    public void export_writesTheChosenFormat_inTheExportFolder() throws Exception {
        SessionRepository sessions = repository();
        sessions.record(at(1_000));

        File csv = sessions.writeExport(0, Long.MAX_VALUE, SessionRepository.ExportFormat.CSV);

        assertNotNull(csv);
        assertTrue(csv.getName().endsWith(".csv"));
        assertEquals(new File(folder.getRoot(), "exports"), csv.getParentFile());
        assertEquals("text/csv", SessionRepository.ExportFormat.CSV.getMimeType());
    }

    private SessionRepository repository() {
        return new SessionRepository(new File(folder.getRoot(), "sessions.json"),
                new File(folder.getRoot(), "exports"), Clock.systemUTC());
    }
}

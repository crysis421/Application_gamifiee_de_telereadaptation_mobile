package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

/** Export du journal lisible directement par un tableur (US-7.2). */
public class SessionCsvTest {

    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    private static SessionEntry entry(String name, int similarity, String condition) {
        return new SessionEntry(1_795_018_629_000L, "fentes", name, 9, 3, 14, 27_196L,
                similarity, "daily_exercise=1;sounds=1", condition,
                "model=lite;frame_stride=1", 48, 1_250, false);
    }

    private static String[] lines(String csv) {
        return csv.substring(1).split("\r\n");
    }

    /** Découpe une ligne comme un tableur : un « ; » entre guillemets ne sépare pas. */
    private static java.util.List<String> cells(String row) {
        java.util.List<String> cells = new java.util.ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < row.length() && row.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ';' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    @Test
    public void startsWithAByteOrderMark_soThatExcelReadsTheAccents() {
        assertEquals('﻿', SessionCsv.toCsv(Collections.emptyList(), PARIS).charAt(0));
    }

    @Test
    public void anEmptyLog_stillHasTheHeader() {
        String[] lines = lines(SessionCsv.toCsv(Collections.emptyList(), PARIS));

        assertEquals(1, lines.length);
        assertTrue(lines[0].startsWith("date;exerciseId;exerciseName;validatedReps;failedReps"));
    }

    @Test
    public void oneRowPerSession_withTheSameColumnCountAsTheHeader() {
        String[] lines = lines(SessionCsv.toCsv(
                Arrays.asList(entry("Fentes", 87, "A"), entry("Fentes", 90, "B")), PARIS));

        assertEquals(3, lines.length);
        assertEquals(SessionCsv.HEADER.length, cells(lines[0]).size());
        assertEquals(SessionCsv.HEADER.length, cells(lines[1]).size());
        assertEquals(SessionCsv.HEADER.length, cells(lines[2]).size());
    }

    @Test
    public void valuesAreWrittenInHeaderOrder() {
        String row = lines(SessionCsv.toCsv(
                Collections.singletonList(entry("Fentes", 87, "A")), PARIS))[1];

        assertEquals(Arrays.asList("2026-11-18 17:17:09", "fentes", "Fentes", "9", "3", "14",
                "75", "0", "27196", "87", "daily_exercise=1;sounds=1", "A",
                "model=lite;frame_stride=1", "48", "1250", "0"), cells(row));
    }

    @Test
    public void aMissingMeasure_isAnEmptyCell_notAZero() {
        SessionEntry manual = new SessionEntry(0, "squat", "Squat", 1, 0, 10, 1_000,
                SessionEntry.SCORE_UNAVAILABLE, "", "");
        String row = lines(SessionCsv.toCsv(Collections.singletonList(manual), PARIS))[1];
        java.util.List<String> cells = cells(row);

        assertEquals("", cells.get(SessionCsv.column("similarityScore")));
        assertEquals("", cells.get(SessionCsv.column("averageLatencyMillis")));
        assertEquals("", cells.get(SessionCsv.column("arcadePoints")));
    }

    @Test
    public void cellsContainingTheSeparatorOrQuotes_areQuoted() {
        assertEquals("\"a;b\"", SessionCsv.escape("a;b"));
        assertEquals("\"dit \"\"bonjour\"\"\"", SessionCsv.escape("dit \"bonjour\""));
        assertEquals("\"deux\nlignes\"", SessionCsv.escape("deux\nlignes"));
        assertEquals("simple", SessionCsv.escape("simple"));
    }

    @Test
    public void theActiveSettings_stayInOneCell() {
        // Les réglages contiennent des « ; » : sans guillemets, ils déborderaient sur
        // plusieurs colonnes.
        String row = lines(SessionCsv.toCsv(
                Collections.singletonList(entry("Fentes", 87, "A")), PARIS))[1];

        assertTrue(row, row.contains(";\"daily_exercise=1;sounds=1\";"));
    }
}

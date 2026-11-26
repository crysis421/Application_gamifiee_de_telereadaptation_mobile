package com.uphf.saes5.data.session;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import com.uphf.saes5.domain.model.SessionEntry;

/**
 * Le journal des séances au format CSV, ouvrable tel quel dans un tableur (US-7.2).
 *
 * <p>Choix faits pour Excel en français, l'outil le plus probable du chercheur :</p>
 * <ul>
 *   <li>séparateur « ; » (la virgule y est le séparateur décimal) ;</li>
 *   <li>UTF-8 précédé d'un BOM, sans quoi les accents sont mal lus ;</li>
 *   <li>fins de ligne CRLF, comme le veut la RFC 4180.</li>
 * </ul>
 *
 * <p>Une mesure absente (score, latence, points) est une case vide, jamais un zéro : un zéro
 * fausserait les moyennes. Les colonnes reprennent les clés du JSON, pour passer d'un format à
 * l'autre sans table de correspondance.</p>
 */
public final class SessionCsv {

    static final String[] HEADER = {
            "date", "exerciseId", "exerciseName", "validatedReps", "failedReps", "targetReps",
            "successRatePercent", "goalReached", "durationMillis", "similarityScore",
            "activeSettings", "conditionName", "analysisSettings", "averageLatencyMillis",
            "arcadePoints", "abandoned"
    };

    private static final char SEPARATOR = ';';
    private static final String NEWLINE = "\r\n";
    private static final char BOM = '﻿';

    private SessionCsv() {}

    public static String toCsv(List<SessionEntry> entries, TimeZone timeZone) {
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        date.setTimeZone(timeZone);

        StringBuilder csv = new StringBuilder().append(BOM);
        appendRow(csv, Arrays.asList(HEADER));
        for (SessionEntry entry : entries) {
            appendRow(csv, Arrays.asList(
                    date.format(new Date(entry.getTimestampMillis())),
                    entry.getExerciseId(),
                    entry.getExerciseName(),
                    String.valueOf(entry.getValidatedReps()),
                    String.valueOf(entry.getFailedReps()),
                    String.valueOf(entry.getTargetReps()),
                    String.valueOf(entry.getSuccessRate()),
                    flag(entry.isGoalReached()),
                    String.valueOf(entry.getDurationMillis()),
                    entry.hasSimilarityScore() ? String.valueOf(entry.getSimilarityScore()) : "",
                    entry.getSettings(),
                    entry.getConditionName(),
                    entry.getAnalysisSettings(),
                    entry.hasLatency() ? String.valueOf(entry.getAverageLatencyMillis()) : "",
                    entry.hasArcadePoints() ? String.valueOf(entry.getArcadePoints()) : "",
                    flag(entry.isAbandoned())));
        }
        return csv.toString();
    }

    /** Index d'une colonne dans {@link #HEADER}. */
    static int column(String name) {
        return Arrays.asList(HEADER).indexOf(name);
    }

    private static void appendRow(StringBuilder csv, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) csv.append(SEPARATOR);
            csv.append(escape(cells.get(i)));
        }
        csv.append(NEWLINE);
    }

    /** Met une case entre guillemets si elle contient un séparateur, un guillemet ou un saut. */
    static String escape(String cell) {
        if (cell == null) return "";
        boolean quote = cell.indexOf(SEPARATOR) >= 0 || cell.indexOf('"') >= 0
                || cell.indexOf('\n') >= 0 || cell.indexOf('\r') >= 0;
        if (!quote) return cell;
        return '"' + cell.replace("\"", "\"\"") + '"';
    }

    private static String flag(boolean value) {
        return value ? "1" : "0";
    }
}

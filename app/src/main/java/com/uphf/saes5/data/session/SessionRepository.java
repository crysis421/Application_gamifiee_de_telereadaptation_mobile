package com.uphf.saes5.data.session;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.uphf.saes5.domain.model.SessionEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

/**
 * Journal des séances d'exercice (US-7.1) et export (US-7.2).
 *
 * <p>Une séance terminée est ajoutée au fichier {@code sessions.json}, dans le stockage privé
 * de l'application : rien n'est publié, aucune permission n'est nécessaire, et le fichier
 * disparaît avec l'application.</p>
 *
 * <p>Seules les données listées par {@link SessionEntry} sont conservées. Aucune image ni
 * vidéo n'est écrite, conformément à la contrainte de l'US.</p>
 */
public final class SessionRepository {

    private static final String TAG = "SessionRepository";

    private static final String F_TIMESTAMP = "timestamp";
    private static final String F_TIMESTAMP_MILLIS = "timestampMillis";
    private static final String F_EXERCISE_ID = "exerciseId";
    private static final String F_EXERCISE_NAME = "exerciseName";
    private static final String F_VALIDATED = "validatedReps";
    private static final String F_FAILED = "failedReps";
    private static final String F_TARGET = "targetReps";
    private static final String F_SUCCESS_RATE = "successRatePercent";
    private static final String F_GOAL_REACHED = "goalReached";
    private static final String F_DURATION = "durationMillis";
    private static final String F_SCORE = "similarityScore";
    private static final String F_SETTINGS = "activeSettings";
    private static final String F_CONDITION = "conditionName";
    // US-1.4. Clés ajoutées, jamais renommées : les anciennes entrées restent lisibles.
    private static final String F_ANALYSIS = "analysisSettings";
    private static final String F_LATENCY = "averageLatencyMillis";
    private static final String F_POINTS = "arcadePoints";
    private static final String F_ABANDONED = "abandoned";

    /** Horodatage lisible, en plus des millisecondes, pour qui ouvre le fichier à la main. */
    private static final SimpleDateFormat READABLE =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    private final File file;
    private final File exportDirectory;
    private final Clock clock;

    /**
     * @param file            le journal ({@code sessions.json} dans le stockage privé)
     * @param exportDirectory dossier des fichiers d'export partagés (cache de l'application)
     */
    public SessionRepository(File file, File exportDirectory, Clock clock) {
        this.file = file;
        this.exportDirectory = exportDirectory;
        this.clock = clock;
    }

    /**
     * Ajoute une séance au journal.
     *
     * <p>Une erreur d'écriture est consignée sans être levée : journaliser ne doit jamais faire
     * échouer une fin de séance.</p>
     */
    public synchronized void record(@NonNull SessionEntry entry) {
        List<SessionEntry> all = readAll();
        all.add(entry);
        write(all);
    }

    /** Toutes les séances journalisées, de la plus ancienne à la plus récente. */
    public synchronized List<SessionEntry> readAll() {
        List<SessionEntry> result = new ArrayList<>();
        if (!file.exists()) {
            return result;
        }
        String content = read();
        if (content.isEmpty()) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(content);
            for (int i = 0; i < array.length(); i++) {
                result.add(fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // Journal illisible : on ne le vide pas, on refuse juste de l'interpréter.
            Log.w(TAG, "Journal illisible, il est ignoré", e);
            return new ArrayList<>();
        }
        return result;
    }

    /**
     * Le journal au format JSON, limité à une période (US-7.2).
     *
     * @param fromMillis début inclus, ou 0 pour ne pas borner
     * @param toMillis   fin incluse, ou {@link Long#MAX_VALUE} pour ne pas borner
     */
    public synchronized String exportJson(long fromMillis, long toMillis) {
        return toJsonArray(filterByPeriod(readAll(), fromMillis, toMillis));
    }

    /**
     * Ne garde que les séances comprises dans la période.
     *
     * <p>Séparé de la lecture du fichier pour être vérifiable en test unitaire.</p>
     */
    @VisibleForTesting
    static List<SessionEntry> filterByPeriod(List<SessionEntry> entries, long fromMillis, long toMillis) {
        List<SessionEntry> result = new ArrayList<>();
        for (SessionEntry entry : entries) {
            long at = entry.getTimestampMillis();
            if (at >= fromMillis && at <= toMillis) {
                result.add(entry);
            }
        }
        return result;
    }

    /** Formats d'export (US-7.2) : tableur pour lire et calculer, JSON pour un script. */
    public enum ExportFormat {
        CSV("csv", "text/csv"),
        JSON("json", "application/json");

        private final String extension;
        private final String mimeType;

        ExportFormat(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public String getMimeType() { return mimeType; }
    }

    /** Écrit l'export dans un fichier partageable, et le renvoie. */
    @Nullable
    public synchronized File writeExport(long fromMillis, long toMillis, ExportFormat format) {
        if (!exportDirectory.exists() && !exportDirectory.mkdirs()) {
            Log.w(TAG, "Dossier d'export impossible à créer");
            return null;
        }
        File target = new File(exportDirectory, "sessions-" + READABLE.format(new Date(clock.millis()))
                .replace(':', '-') + "." + format.extension);
        String content = format == ExportFormat.CSV
                ? SessionCsv.toCsv(filterByPeriod(readAll(), fromMillis, toMillis),
                        TimeZone.getDefault())
                : exportJson(fromMillis, toMillis);
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            return target;
        } catch (IOException e) {
            Log.w(TAG, "Export impossible", e);
            return null;
        }
    }

    /** Efface tout le journal : retrait du consentement du participant (US-9.1). */
    public synchronized void clear() {
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Journal impossible à effacer");
        }
    }

    /** Nombre de séances journalisées. */
    public synchronized int count() {
        return readAll().size();
    }

    // --- sérialisation ----------------------------------------------------

    private static String toJsonArray(List<SessionEntry> entries) {
        JSONArray array = new JSONArray();
        for (SessionEntry entry : entries) {
            JSONObject object = toJson(entry);
            if (object != null) {
                array.put(object);
            }
        }
        try {
            // Indenté : le fichier doit rester lisible tel quel par le chercheur.
            return array.toString(2);
        } catch (JSONException e) {
            Log.w(TAG, "Indentation impossible, écriture sur une ligne", e);
            return array.toString();
        }
    }

    @Nullable
    private static JSONObject toJson(SessionEntry entry) {
        JSONObject object = new JSONObject();
        try {
            object.put(F_TIMESTAMP, READABLE.format(new Date(entry.getTimestampMillis())));
            object.put(F_TIMESTAMP_MILLIS, entry.getTimestampMillis());
            object.put(F_EXERCISE_ID, entry.getExerciseId());
            object.put(F_EXERCISE_NAME, entry.getExerciseName());
            object.put(F_VALIDATED, entry.getValidatedReps());
            object.put(F_FAILED, entry.getFailedReps());
            object.put(F_TARGET, entry.getTargetReps());
            object.put(F_SUCCESS_RATE, entry.getSuccessRate());
            object.put(F_GOAL_REACHED, entry.isGoalReached());
            object.put(F_DURATION, entry.getDurationMillis());
            // Null explicite plutôt que 0 : un score absent ne doit pas se confondre avec un
            // score nul lors de l'analyse.
            object.put(F_SCORE, entry.hasSimilarityScore() ? entry.getSimilarityScore() : JSONObject.NULL);
            object.put(F_SETTINGS, entry.getSettings());
            object.put(F_CONDITION, entry.getConditionName());
            object.put(F_ANALYSIS, entry.getAnalysisSettings());
            object.put(F_LATENCY, entry.hasLatency() ? entry.getAverageLatencyMillis() : JSONObject.NULL);
            object.put(F_POINTS, entry.hasArcadePoints() ? entry.getArcadePoints() : JSONObject.NULL);
            object.put(F_ABANDONED, entry.isAbandoned());
            return object;
        } catch (JSONException e) {
            Log.w(TAG, "Séance non journalisée", e);
            return null;
        }
    }

    private static SessionEntry fromJson(JSONObject object) throws JSONException {
        return new SessionEntry(
                object.optLong(F_TIMESTAMP_MILLIS, 0L),
                object.optString(F_EXERCISE_ID, ""),
                object.optString(F_EXERCISE_NAME, ""),
                object.optInt(F_VALIDATED, 0),
                object.optInt(F_FAILED, 0),
                object.optInt(F_TARGET, 0),
                object.optLong(F_DURATION, 0L),
                object.isNull(F_SCORE)
                        ? SessionEntry.SCORE_UNAVAILABLE
                        : object.optInt(F_SCORE, SessionEntry.SCORE_UNAVAILABLE),
                object.optString(F_SETTINGS, ""),
                object.optString(F_CONDITION, ""),
                object.optString(F_ANALYSIS, ""),
                object.isNull(F_LATENCY)
                        ? SessionEntry.LATENCY_UNAVAILABLE
                        : object.optInt(F_LATENCY, SessionEntry.LATENCY_UNAVAILABLE),
                object.isNull(F_POINTS)
                        ? SessionEntry.POINTS_UNAVAILABLE
                        : object.optInt(F_POINTS, SessionEntry.POINTS_UNAVAILABLE),
                // Absent des entrées antérieures : elles sont toutes passées par le récap.
                object.optBoolean(F_ABANDONED, false));
    }

    // --- fichier ----------------------------------------------------------

    private String read() {
        try (Scanner scanner = new Scanner(file, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (IOException e) {
            Log.w(TAG, "Lecture du journal impossible", e);
            return "";
        }
    }

    private void write(List<SessionEntry> entries) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(toJsonArray(entries).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "Écriture du journal impossible", e);
        }
    }
}

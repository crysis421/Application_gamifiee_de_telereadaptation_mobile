package com.uphf.saes5.data.exercise;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import com.uphf.saes5.data.storage.AssetSource;
import com.uphf.saes5.domain.exercise.BaseExercises;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.Movement;
import com.uphf.saes5.domain.movement.MovementMath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modèles de mouvement : ceux enregistrés sur le téléphone (un fichier JSON par modèle) et
 * ceux livrés avec l'application (modèles et démonstrations des exercices de base).
 */
public final class MovementRepository {

    private static final String TAG = "MovementRepository";

    private final Gson gson = new Gson();
    private final File directory;
    private final AssetSource assets;

    /**
     * @param directory dossier des modèles enregistrés ({@code files/movements})
     * @param assets    fichiers livrés avec l'application
     */
    public MovementRepository(File directory, AssetSource assets) {
        this.directory = directory;
        this.assets = assets;
    }

    public synchronized void save(Movement movement) throws IOException {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(fileFor(movement.name)), StandardCharsets.UTF_8)) {
            gson.toJson(movement, writer);
        }
    }

    /** Le modèle enregistré sous ce nom, ou {@code null} s'il est absent ou illisible. */
    @Nullable
    public synchronized Movement load(String name) {
        File file = fileFor(name);
        if (!file.exists()) return null;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Movement.class);
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Modèle illisible : " + name, e);
            return null;
        }
    }

    public synchronized boolean exists(@Nullable String name) {
        return name != null && !name.trim().isEmpty() && fileFor(name).isFile();
    }

    /** Noms des modèles enregistrés, par ordre alphabétique. */
    public synchronized List<String> list() {
        List<String> names = new ArrayList<>();
        File[] files = directory().listFiles((dir, file) -> file.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                names.add(fileName.substring(0, fileName.length() - ".json".length()));
            }
        }
        Collections.sort(names);
        return names;
    }

    /** Un modèle livré avec l'application, ou {@code null} s'il est illisible. */
    @Nullable
    public Movement loadAsset(String path) {
        try (Reader reader = new InputStreamReader(assets.open(path), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Movement.class);
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Asset illisible : " + path, e);
            return null;
        }
    }

    /**
     * La démonstration montrée par le mannequin pour cet exercice, ou {@code null} s'il n'en a
     * pas : l'écran montre alors le modèle de l'exercice lui-même.
     */
    @Nullable
    public Movement loadDemo(Exercise exercise) {
        String path = BaseExercises.demoAssetFor(exercise);
        return path == null ? null : loadAsset(path);
    }

    /** Vrai si au moins une image du modèle donne des angles articulaires comparables. */
    public static boolean isUsable(@Nullable Movement movement) {
        if (movement == null || movement.frames == null) return false;
        for (float[] frame : movement.frames) {
            if (MovementMath.angleFeatures(frame) != null) return true;
        }
        return false;
    }

    private File fileFor(String name) {
        return new File(directory(), safe(name) + ".json");
    }

    private File directory() {
        if (!directory.exists() && !directory.mkdirs()) {
            Log.w(TAG, "Dossier des modèles impossible à créer");
        }
        return directory;
    }

    /** Même assainissement depuis la première version : les fichiers existants restent trouvés. */
    private static String safe(String name) {
        String cleaned = name.replaceAll("[^\\p{L}\\p{N}_ -]", "").trim();
        return cleaned.isEmpty() ? "mouvement" : cleaned;
    }
}

package com.uphf.saes5;

import android.content.Context;

import com.google.gson.Gson;

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

public final class MovementStorage {

    private static final Gson gson = new Gson();

    private MovementStorage() {}

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "movements");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static String safe(String name) {
        String s = name.replaceAll("[^\\p{L}\\p{N}_ -]", "").trim();
        return s.isEmpty() ? "mouvement" : s;
    }

    public static void save(Context c, Movement m) throws IOException {
        File f = new File(dir(c), safe(m.name) + ".json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            gson.toJson(m, w);
        }
    }

    public static Movement load(Context c, String name) {
        File f = new File(dir(c), safe(name) + ".json");
        if (!f.exists()) return null;
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            return gson.fromJson(r, Movement.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> list(Context c) {
        List<String> names = new ArrayList<>();
        File[] files = dir(c).listFiles((d, n) -> n.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                String n = f.getName();
                names.add(n.substring(0, n.length() - 5));
            }
        }
        Collections.sort(names);
        return names;
    }
}
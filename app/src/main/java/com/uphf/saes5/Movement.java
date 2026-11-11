package com.uphf.saes5;

import java.util.List;

public class Movement {
    public String name;
    public int sampleIntervalMs;
    public int durationSec;
    public int landmarkCount;
    // une entrée par image : [x0, y0, x1, y1, ...] (champ vectoriel normalisé)
    public List<float[]> frames;
}
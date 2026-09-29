package com.uphf.saes5;

public final class MuscleBodyGeometry {
    private MuscleBodyGeometry() {}

    public static String groupAt(float normalizedX, float normalizedY) {
        if (inside(normalizedX, normalizedY, 0.30f, 0.16f, 0.70f, 0.24f)) return "Dos";
        if (inside(normalizedX, normalizedY, 0.34f, 0.24f, 0.66f, 0.36f)) return "Pectoraux";
        if (inside(normalizedX, normalizedY, 0.16f, 0.23f, 0.34f, 0.56f)
                || inside(normalizedX, normalizedY, 0.66f, 0.23f, 0.84f, 0.56f)) return "Bras";
        if (inside(normalizedX, normalizedY, 0.36f, 0.36f, 0.64f, 0.59f)) return "Tronc";
        if (inside(normalizedX, normalizedY, 0.34f, 0.59f, 0.66f, 0.95f)) return "Jambes";
        return null;
    }

    private static boolean inside(float x, float y,
                                  float left, float top, float right, float bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }
}

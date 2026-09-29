package com.uphf.saes5;

public final class MuscleBodyGeometry {
    private MuscleBodyGeometry() {}

    public static String groupAt(float normalizedX, float normalizedY) {
        if (inside(normalizedX, normalizedY, .20f, .27f, .36f, .35f)) return "Pectoraux";
        if (inside(normalizedX, normalizedY, .21f, .35f, .35f, .54f)) return "Tronc";
        if (inside(normalizedX, normalizedY, .08f, .28f, .18f, .52f)
                || inside(normalizedX, normalizedY, .38f, .28f, .48f, .52f)
                || inside(normalizedX, normalizedY, .52f, .28f, .62f, .52f)
                || inside(normalizedX, normalizedY, .82f, .28f, .92f, .52f)) return "Bras";
        if (inside(normalizedX, normalizedY, .64f, .24f, .80f, .44f)) return "Dos";
        if (inside(normalizedX, normalizedY, .20f, .57f, .27f, .92f)
                || inside(normalizedX, normalizedY, .29f, .57f, .36f, .92f)
                || inside(normalizedX, normalizedY, .64f, .56f, .71f, .92f)
                || inside(normalizedX, normalizedY, .73f, .56f, .80f, .92f)) return "Jambes";
        return null;
    }

    private static boolean inside(float x, float y,
                                  float left, float top, float right, float bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }
}

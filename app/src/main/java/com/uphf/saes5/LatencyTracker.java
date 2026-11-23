package com.uphf.saes5;

/**
 * Mesure la latence de l'analyse de pose pendant une séance (US-1.4).
 *
 * <p>La latence d'une image est le temps entre sa capture et l'arrivée du résultat de
 * MediaPipe : c'est le retard que l'utilisateur perçoit entre son geste et le squelette.</p>
 */
public final class LatencyTracker {

    /** Seuil de latence perçue visé par l'US-1.4. */
    public static final int TARGET_MILLIS = 100;
    public static final int UNAVAILABLE = -1;

    private long total;
    private int count;
    private int last = UNAVAILABLE;

    public void record(long latencyMillis) {
        if (latencyMillis < 0) return;
        total += latencyMillis;
        count++;
        last = (int) Math.min(Integer.MAX_VALUE, latencyMillis);
    }

    /** Moyenne arrondie, ou {@link #UNAVAILABLE} si rien n'a été mesuré. */
    public int getAverageMillis() {
        if (count == 0) return UNAVAILABLE;
        return Math.round((float) total / count);
    }

    public int getLastMillis() {
        return last;
    }

    public boolean isAboveTarget() {
        int average = getAverageMillis();
        return average != UNAVAILABLE && average > TARGET_MILLIS;
    }
}

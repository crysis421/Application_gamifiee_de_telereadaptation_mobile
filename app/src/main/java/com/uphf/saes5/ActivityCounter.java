package com.uphf.saes5;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Nombre d'exercices réalisés (US-5.2), tiré du journal des séances.
 *
 * <p>Même règle que l'exercice du jour (US-4.2) : une séance n'est réalisée que si au moins une
 * répétition a été validée. Ouvrir un exercice puis le quitter ne compte pas.</p>
 */
public final class ActivityCounter {

    private static final long SEVEN_DAYS = TimeUnit.DAYS.toMillis(7);

    private ActivityCounter() {}

    public static Counts count(List<SessionEntry> log, long nowMillis) {
        int total = 0;
        int week = 0;
        for (SessionEntry entry : log) {
            // Abandonnée (US-7.1) : journalisée pour l'étude, mais pas « réalisée ».
            if (entry.getValidatedReps() <= 0 || entry.isAbandoned()) continue;
            total++;
            if (entry.getTimestampMillis() >= nowMillis - SEVEN_DAYS) week++;
        }
        return new Counts(total, week);
    }

    public static final class Counts {
        private final int total;
        private final int lastSevenDays;

        Counts(int total, int lastSevenDays) {
            this.total = total;
            this.lastSevenDays = lastSevenDays;
        }

        public int getTotal() { return total; }
        public int getLastSevenDays() { return lastSevenDays; }
    }
}

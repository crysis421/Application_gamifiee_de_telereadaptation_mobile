package com.uphf.saes5.domain.game;

import com.uphf.saes5.domain.model.SessionEntry;

/**
 * Points et multiplicateurs du mode arcade (US-4.4).
 *
 * <ul>
 *   <li>Une répétition validée rapporte autant de points que sa précision (0 à 100), plus un
 *   bonus si elle est quasi parfaite : c'est ce qui pousse à viser les 100 %.</li>
 *   <li>Le multiplicateur monte d'un cran tous les {@link #SUCCESSES_PER_LEVEL} succès
 *   consécutifs, jusqu'à {@link #MAX_MULTIPLIER}.</li>
 *   <li>Un mouvement raté le remet à 1 (aspect « punitif »), sans retirer de points.</li>
 * </ul>
 *
 * <p>Sans dépendance Android, pour rester testable.</p>
 */
public final class ArcadeScore {

    /** Points d'une répétition sans précision mesurée (séance manuelle). */
    static final int BASE_POINTS = 100;
    static final int PERFECT_PRECISION = 95;
    static final int PERFECT_BONUS = 50;
    static final int SUCCESSES_PER_LEVEL = 3;
    static final int MAX_MULTIPLIER = 4;

    private int total;
    private int streak;
    private int multiplier = 1;

    /**
     * Une répétition validée.
     *
     * @param precision score de similarité de la répétition, ou
     *                  {@link SessionEntry#SCORE_UNAVAILABLE}
     * @return les points gagnés, multiplicateur compris
     */
    public int onSuccess(int precision) {
        int points;
        if (precision < 0) {
            points = BASE_POINTS;
        } else {
            int clamped = Math.min(100, precision);
            points = clamped + (clamped >= PERFECT_PRECISION ? PERFECT_BONUS : 0);
        }
        int gained = points * multiplier;
        total += gained;
        streak++;
        multiplier = Math.min(MAX_MULTIPLIER, 1 + streak / SUCCESSES_PER_LEVEL);
        return gained;
    }

    /** Un mouvement raté : la série est rompue. */
    public void onFailure() {
        streak = 0;
        multiplier = 1;
    }

    public int getTotal() { return total; }
    public int getMultiplier() { return multiplier; }
    public int getStreak() { return streak; }
}

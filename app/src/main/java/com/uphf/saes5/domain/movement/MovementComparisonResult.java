package com.uphf.saes5.domain.movement;

/** Résultat immuable produit après chaque image analysée. */
public final class MovementComparisonResult {
    private final int liveScore;
    private final int movementScore;
    private final boolean ready;
    private final boolean movementValid;
    private final boolean repetitionValidated;
    private final boolean repetitionFailed;

    MovementComparisonResult(int liveScore, int movementScore, boolean ready,
                             boolean movementValid, boolean repetitionValidated) {
        this(liveScore, movementScore, ready, movementValid, repetitionValidated, false);
    }

    MovementComparisonResult(int liveScore, int movementScore, boolean ready,
                             boolean movementValid, boolean repetitionValidated,
                             boolean repetitionFailed) {
        this.liveScore = liveScore;
        this.movementScore = movementScore;
        this.ready = ready;
        this.movementValid = movementValid;
        this.repetitionValidated = repetitionValidated;
        this.repetitionFailed = repetitionFailed;
    }

    /**
     * Vrai une seule fois quand une tentative s'achève sans être validée : l'utilisateur a
     * quitté la pose de départ puis y est revenu sans reproduire le modèle (US-4.4).
     */
    public boolean isRepetitionFailed() {
        return repetitionFailed;
    }

    public int getLiveScore() {
        return liveScore;
    }

    public int getMovementScore() {
        return movementScore;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isMovementValid() {
        return movementValid;
    }

    /** Vrai une seule fois quand la séquence complète franchit le seuil. */
    public boolean isRepetitionValidated() {
        return repetitionValidated;
    }
}

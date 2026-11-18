package com.uphf.saes5;

/** Résultat immuable produit après chaque image analysée. */
public final class MovementComparisonResult {
    private final int liveScore;
    private final int movementScore;
    private final boolean ready;
    private final boolean movementValid;
    private final boolean repetitionValidated;

    MovementComparisonResult(int liveScore, int movementScore, boolean ready,
                             boolean movementValid, boolean repetitionValidated) {
        this.liveScore = liveScore;
        this.movementScore = movementScore;
        this.ready = ready;
        this.movementValid = movementValid;
        this.repetitionValidated = repetitionValidated;
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

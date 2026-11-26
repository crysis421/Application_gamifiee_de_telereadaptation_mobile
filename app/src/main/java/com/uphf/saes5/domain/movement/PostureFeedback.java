package com.uphf.saes5.domain.movement;

/**
 * Décide de la couleur du squelette pendant la séance (US-3.2).
 *
 * <p>Le jugement porte sur la posture <em>instantanée</em>, comparée à la pose du modèle la
 * plus proche : vert si elle atteint le seuil de l'exercice, rouge sinon. On ne s'appuie pas
 * sur la validation de la répétition, qui n'est connue qu'en fin de mouvement : pendant la
 * descente d'un bon squat, le squelette serait rouge.</p>
 *
 * <p>Une marge sous le seuil évite que la couleur clignote quand le score oscille autour.</p>
 */
public final class PostureFeedback {

    public enum State { NEUTRAL, CORRECT, TO_CORRECT }

    /** Points de score sous le seuil pendant lesquels on garde la couleur précédente. */
    static final int HYSTERESIS = 5;

    private final int threshold;
    private State state = State.NEUTRAL;

    public PostureFeedback(int threshold) {
        this.threshold = threshold;
    }

    public State update(int liveScore) {
        if (liveScore >= threshold) {
            state = State.CORRECT;
        } else if (liveScore < threshold - HYSTERESIS || state == State.NEUTRAL) {
            state = State.TO_CORRECT;
        }
        return state;
    }

    /** Corps perdu de vue : plus rien à juger. */
    public void reset() {
        state = State.NEUTRAL;
    }

    public State getState() {
        return state;
    }
}

package com.uphf.saes5.ui.exercise;

import androidx.annotation.Nullable;

import com.uphf.saes5.ui.summary.SummaryArgs;

/**
 * Ce qui vient de se passer pendant la séance : de quoi jouer un son, animer le compteur,
 * afficher le bandeau, ou changer d'écran. Traité une seule fois par l'écran.
 */
public final class ExerciseEvent {

    public enum Kind {
        REP_VALIDATED,
        REP_FAILED,
        /** Objectif atteint avec la caméra : la séance se termine seule après un délai. */
        GOAL_REACHED,
        /** Séance terminée : ouvrir le récapitulatif. */
        FINISHED,
        /** Séance quittée et journalisée comme abandonnée. */
        ABANDONED
    }

    private final Kind kind;
    private final int pointsGained;
    @Nullable private final SummaryArgs summary;

    private ExerciseEvent(Kind kind, int pointsGained, @Nullable SummaryArgs summary) {
        this.kind = kind;
        this.pointsGained = pointsGained;
        this.summary = summary;
    }

    static ExerciseEvent of(Kind kind) {
        return new ExerciseEvent(kind, -1, null);
    }

    /** @param pointsGained points gagnés, ou -1 si le mode arcade est désactivé */
    static ExerciseEvent repValidated(int pointsGained) {
        return new ExerciseEvent(Kind.REP_VALIDATED, pointsGained, null);
    }

    static ExerciseEvent finished(SummaryArgs summary) {
        return new ExerciseEvent(Kind.FINISHED, -1, summary);
    }

    public Kind getKind() { return kind; }
    /** Points de la répétition validée, ou -1 si le mode arcade est désactivé. */
    public int getPointsGained() { return pointsGained; }
    /** Mesures de la séance, pour {@link Kind#FINISHED}. */
    @Nullable public SummaryArgs getSummary() { return summary; }
}

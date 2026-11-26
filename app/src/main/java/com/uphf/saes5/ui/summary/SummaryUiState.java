package com.uphf.saes5.ui.summary;

import androidx.annotation.Nullable;

import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.SessionResult;

/** Ce que le récapitulatif affiche, calculé par {@link SummaryViewModel}. Immuable. */
public final class SummaryUiState {

    @Nullable private final Exercise exercise;
    private final SessionResult result;
    private final int similarity;
    private final int points;
    private final boolean favoriteVisible;
    private final boolean favorite;

    SummaryUiState(@Nullable Exercise exercise, SessionResult result, int similarity, int points,
                   boolean favoriteVisible, boolean favorite) {
        this.exercise = exercise;
        this.result = result;
        this.similarity = similarity;
        this.points = points;
        this.favoriteVisible = favoriteVisible;
        this.favorite = favorite;
    }

    /** L'exercice, ou {@code null} s'il n'est plus au catalogue : l'écran se ferme. */
    @Nullable public Exercise getExercise() { return exercise; }
    public SessionResult getResult() { return result; }
    public boolean isGoalReached() {
        return exercise != null && result.isGoalReached(exercise.getTargetReps());
    }
    /** Score moyen de ressemblance, ou {@code SessionEntry.SCORE_UNAVAILABLE} (séance manuelle). */
    public int getSimilarity() { return similarity; }
    public int getPoints() { return points; }
    /** Points absents si le mode arcade était désactivé pendant la séance (US-4.4). */
    public boolean isPointsVisible() { return points >= 0; }
    public boolean isFavoriteVisible() { return favoriteVisible; }
    public boolean isFavorite() { return favorite; }
}

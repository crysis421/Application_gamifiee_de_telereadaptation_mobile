package com.uphf.saes5.ui.profile;

import androidx.annotation.Nullable;

import com.uphf.saes5.domain.game.Progression;
import com.uphf.saes5.domain.statistics.ProfileSummary;

/** Ce que le profil affiche, calculé par {@link ProfileViewModel}. Immuable. */
public final class ProfileUiState {

    private final boolean progressionVisible;
    @Nullable private final Progression.Summary progression;
    private final ProfileSummary history;
    private final boolean statisticsTabVisible;

    ProfileUiState(boolean progressionVisible, @Nullable Progression.Summary progression,
                   ProfileSummary history, boolean statisticsTabVisible) {
        this.progressionVisible = progressionVisible;
        this.progression = progression;
        this.history = history;
        this.statisticsTabVisible = statisticsTabVisible;
    }

    /** Rangs et étoiles (US-4.5), masqués avec l'élément de jeu « Rangs et étoiles ». */
    public boolean isProgressionVisible() { return progressionVisible; }
    /** La progression, ou {@code null} quand elle est masquée : elle n'est pas calculée. */
    @Nullable public Progression.Summary getProgression() { return progression; }
    /** Historique des séances, toujours affiché. */
    public ProfileSummary getHistory() { return history; }
    public boolean isStatisticsTabVisible() { return statisticsTabVisible; }
}

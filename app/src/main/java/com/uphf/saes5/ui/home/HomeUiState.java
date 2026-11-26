package com.uphf.saes5.ui.home;

import androidx.annotation.Nullable;

import com.uphf.saes5.domain.model.Exercise;

/**
 * Ce que l'accueil affiche, calculé par {@link HomeViewModel}.
 *
 * <p>Immuable : chaque changement produit un nouvel état, que l'écran affiche tel quel. Un
 * élément de jeu désactivé y est simplement « non visible » : l'écran n'a pas à connaître les
 * réglages.</p>
 */
public final class HomeUiState {

    /** Ce que la tuile des pas peut montrer (US-5.1). */
    public enum StepsStatus {
        /** Tuile masquée (US-5.4) : les pas ne sont pas lus. */
        HIDDEN,
        LOADING,
        VALUE,
        /** Il manque la permission : la tuile invite à l'accorder d'un toucher. */
        PERMISSION_MISSING,
        /** Appareil sans Play services compatibles : la tuile le dit, plutôt qu'un zéro. */
        UNSUPPORTED
    }

    private final boolean statisticsVisible;
    private final boolean catalogButtonVisible;
    private final boolean dailyCardVisible;
    private final boolean emptyCatalogVisible;
    @Nullable private final Exercise dailyExercise;
    private final boolean favoriteVisible;
    private final boolean favorite;
    private final boolean doneToday;
    private final boolean detailsExpanded;
    private final boolean exercisesDoneTileVisible;
    private final int exercisesDoneTotal;
    private final int exercisesDoneLastSevenDays;
    private final StepsStatus stepsStatus;
    private final int steps;

    HomeUiState(boolean statisticsVisible, boolean catalogButtonVisible,
                boolean dailyCardVisible, boolean emptyCatalogVisible,
                @Nullable Exercise dailyExercise, boolean favoriteVisible, boolean favorite,
                boolean doneToday, boolean detailsExpanded, boolean exercisesDoneTileVisible,
                int exercisesDoneTotal, int exercisesDoneLastSevenDays,
                StepsStatus stepsStatus, int steps) {
        this.statisticsVisible = statisticsVisible;
        this.catalogButtonVisible = catalogButtonVisible;
        this.dailyCardVisible = dailyCardVisible;
        this.emptyCatalogVisible = emptyCatalogVisible;
        this.dailyExercise = dailyExercise;
        this.favoriteVisible = favoriteVisible;
        this.favorite = favorite;
        this.doneToday = doneToday;
        this.detailsExpanded = detailsExpanded;
        this.exercisesDoneTileVisible = exercisesDoneTileVisible;
        this.exercisesDoneTotal = exercisesDoneTotal;
        this.exercisesDoneLastSevenDays = exercisesDoneLastSevenDays;
        this.stepsStatus = stepsStatus;
        this.steps = steps;
    }

    /** Bouton et onglet Statistiques (US-4.1). */
    public boolean isStatisticsVisible() { return statisticsVisible; }
    public boolean isCatalogButtonVisible() { return catalogButtonVisible; }
    /** Carte « Exercice du jour » (US-4.2). */
    public boolean isDailyCardVisible() { return dailyCardVisible; }
    /** Message « catalogue vide », à la place de la carte. */
    public boolean isEmptyCatalogVisible() { return emptyCatalogVisible; }
    @Nullable public Exercise getDailyExercise() { return dailyExercise; }
    public boolean isFavoriteVisible() { return favoriteVisible; }
    public boolean isFavorite() { return favorite; }
    /** Un exercice a déjà été réalisé aujourd'hui. */
    public boolean isDoneToday() { return doneToday; }
    public boolean isDetailsExpanded() { return detailsExpanded; }
    public boolean isStepsTileVisible() { return stepsStatus != StepsStatus.HIDDEN; }
    public boolean isExercisesDoneTileVisible() { return exercisesDoneTileVisible; }
    /** Exercices réalisés au total (US-5.2). */
    public int getExercisesDoneTotal() { return exercisesDoneTotal; }
    public int getExercisesDoneLastSevenDays() { return exercisesDoneLastSevenDays; }
    public StepsStatus getStepsStatus() { return stepsStatus; }
    /** Pas du jour, si {@link #getStepsStatus()} vaut {@link StepsStatus#VALUE}. */
    public int getSteps() { return steps; }
}

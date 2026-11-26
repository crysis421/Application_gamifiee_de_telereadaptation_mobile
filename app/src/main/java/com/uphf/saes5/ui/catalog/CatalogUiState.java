package com.uphf.saes5.ui.catalog;

import com.uphf.saes5.domain.model.Exercise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ce que le catalogue affiche, calculé par {@link CatalogViewModel}. Immuable. */
public final class CatalogUiState {

    private final List<Exercise> exercises;
    private final boolean starsVisible;
    private final boolean statisticsTabVisible;

    CatalogUiState(List<Exercise> exercises, boolean starsVisible, boolean statisticsTabVisible) {
        this.exercises = Collections.unmodifiableList(new ArrayList<>(exercises));
        this.starsVisible = starsVisible;
        this.statisticsTabVisible = statisticsTabVisible;
    }

    /** Les exercices qui passent les filtres, dans l'ordre du catalogue. */
    public List<Exercise> getExercises() { return exercises; }
    /** Étoiles de difficulté, élément de jeu désactivable (US-4.1). */
    public boolean isStarsVisible() { return starsVisible; }
    public boolean isStatisticsTabVisible() { return statisticsTabVisible; }
}

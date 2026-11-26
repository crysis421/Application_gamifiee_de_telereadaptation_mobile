package com.uphf.saes5.ui.common;

import androidx.annotation.StringRes;

import com.uphf.saes5.R;
import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.GameFeature;

/**
 * Libellés affichés des réglages.
 *
 * <p>Ils vivent côté interface : le modèle ({@link GameFeature}, {@link AnalysisModel}) ne
 * connaît que des clés stables, sans ressource Android.</p>
 */
public final class FeatureLabels {

    private FeatureLabels() {}

    @StringRes
    public static int label(GameFeature feature) {
        switch (feature) {
            case DAILY_EXERCISE: return R.string.setting_daily_exercise;
            case FAVORITES: return R.string.setting_favorites;
            case STARS: return R.string.setting_stars;
            case STATISTICS: return R.string.setting_statistics;
            case SOUNDS: return R.string.setting_sounds;
            case ARCADE: return R.string.setting_arcade;
            case HOME_STEPS: return R.string.setting_home_steps;
            case HOME_EXERCISES_DONE: return R.string.setting_home_exercises_done;
            case HOME_CATALOG: return R.string.setting_home_catalog;
            default: throw new IllegalArgumentException("Élément sans libellé : " + feature);
        }
    }

    @StringRes
    public static int help(GameFeature feature) {
        switch (feature) {
            case DAILY_EXERCISE: return R.string.setting_daily_exercise_help;
            case FAVORITES: return R.string.setting_favorites_help;
            case STARS: return R.string.setting_stars_help;
            case STATISTICS: return R.string.setting_statistics_help;
            case SOUNDS: return R.string.setting_sounds_help;
            case ARCADE: return R.string.setting_arcade_help;
            case HOME_STEPS: return R.string.setting_home_steps_help;
            case HOME_EXERCISES_DONE: return R.string.setting_home_exercises_done_help;
            case HOME_CATALOG: return R.string.setting_home_catalog_help;
            default: throw new IllegalArgumentException("Élément sans explication : " + feature);
        }
    }

    @StringRes
    public static int label(AnalysisModel model) {
        return model == AnalysisModel.FULL ? R.string.analysis_model_full : R.string.analysis_model_lite;
    }
}

package com.uphf.saes5;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * Réglages de l'analyse de pose (US-1.4) : modèle utilisé et nombre d'images analysées.
 *
 * <p>Séparés de {@link GameSettings} : ce ne sont pas des éléments de jeu, et ils ne doivent
 * pas changer la forme de {@link GameSettings#snapshot()}, déjà présente dans les journaux.
 * Ils sont journalisés à part, via {@link #snapshot()}.</p>
 *
 * <p>Stockés dans les mêmes SharedPreferences que les autres réglages.</p>
 */
public final class AnalysisSettings {

    public enum Model {
        /** BlazePose « lite » : corps seul, pensé pour le mobile. Par défaut. */
        LITE("lite", R.string.analysis_model_lite),
        /** Holistic : corps, mains et visage. Plus précis, plus lent. */
        FULL("full", R.string.analysis_model_full);

        private final String key;
        @StringRes private final int labelRes;

        Model(String key, @StringRes int labelRes) {
            this.key = key;
            this.labelRes = labelRes;
        }

        public String getKey() { return key; }
        @StringRes public int getLabelRes() { return labelRes; }

        /** Une valeur inconnue (ancienne version, faute de frappe) retombe sur le modèle léger. */
        @NonNull
        public static Model fromKey(@Nullable String key) {
            for (Model model : values()) {
                if (model.key.equals(key)) return model;
            }
            return LITE;
        }
    }

    public static final int MAX_FRAME_STRIDE = 3;

    private static final String PREFS_NAME = "exergame_prefs";
    private static final String KEY_MODEL = "analysis_model";
    private static final String KEY_FRAME_STRIDE = "analysis_frame_stride";

    @Nullable
    private static SharedPreferences preferences;

    private AnalysisSettings() {}

    /** Branche les réglages sur leur stockage. Appelé au démarrage, depuis SaeS5Application. */
    public static synchronized void init(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static Model getModel() {
        if (preferences == null) return Model.LITE;
        return Model.fromKey(preferences.getString(KEY_MODEL, Model.LITE.getKey()));
    }

    public static void setModel(@NonNull Model model) {
        if (preferences == null) return;
        preferences.edit().putString(KEY_MODEL, model.getKey()).apply();
    }

    /** Une image analysée sur {@code n} : 1 analyse tout, 3 en saute deux sur trois. */
    public static int getFrameStride() {
        if (preferences == null) return 1;
        return clampStride(preferences.getInt(KEY_FRAME_STRIDE, 1));
    }

    public static void setFrameStride(int stride) {
        if (preferences == null) return;
        preferences.edit().putInt(KEY_FRAME_STRIDE, clampStride(stride)).apply();
    }

    static int clampStride(int stride) {
        return Math.max(1, Math.min(MAX_FRAME_STRIDE, stride));
    }

    /** Vrai si l'image {@code frameIndex} (comptée depuis 0) doit être analysée. */
    public static boolean shouldAnalyze(long frameIndex, int stride) {
        return frameIndex % clampStride(stride) == 0;
    }

    /** Forme stable journalisée avec chaque séance, par exemple {@code model=lite;frame_stride=2}. */
    @NonNull
    public static String snapshot() {
        return "model=" + getModel().getKey() + ";frame_stride=" + getFrameStride();
    }
}

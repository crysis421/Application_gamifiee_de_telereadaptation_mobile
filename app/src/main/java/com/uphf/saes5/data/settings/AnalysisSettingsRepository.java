package com.uphf.saes5.data.settings;

import androidx.annotation.NonNull;

import com.uphf.saes5.data.storage.PreferenceStore;
import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.FrameSampling;

/**
 * Réglages de l'analyse de pose (US-1.4) : modèle utilisé et part d'images analysées.
 *
 * <p>Séparés des éléments de jeu ({@link GameSettingsRepository}) : ce ne sont pas des
 * éléments de jeu, et ils ne doivent pas changer la forme de leur instantané, déjà présent
 * dans les journaux. Ils sont journalisés à part, via {@link #snapshot()}.</p>
 */
public final class AnalysisSettingsRepository {

    private static final String KEY_MODEL = "analysis_model";
    private static final String KEY_FRAME_STRIDE = "analysis_frame_stride";

    private final PreferenceStore store;

    public AnalysisSettingsRepository(PreferenceStore store) {
        this.store = store;
    }

    @NonNull
    public AnalysisModel getModel() {
        return AnalysisModel.fromKey(store.getString(KEY_MODEL, AnalysisModel.LITE.getKey()));
    }

    public void setModel(@NonNull AnalysisModel model) {
        store.putString(KEY_MODEL, model.getKey());
    }

    /** Une image analysée sur {@code n} : 1 analyse tout, 3 en saute deux sur trois. */
    public int getFrameStride() {
        return FrameSampling.clampStride(store.getInt(KEY_FRAME_STRIDE, 1));
    }

    public void setFrameStride(int stride) {
        store.putInt(KEY_FRAME_STRIDE, FrameSampling.clampStride(stride));
    }

    /** Forme stable journalisée avec chaque séance, par exemple {@code model=lite;frame_stride=2}. */
    @NonNull
    public String snapshot() {
        return "model=" + getModel().getKey() + ";frame_stride=" + getFrameStride();
    }
}

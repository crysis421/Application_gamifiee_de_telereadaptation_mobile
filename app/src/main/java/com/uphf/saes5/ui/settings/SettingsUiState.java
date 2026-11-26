package com.uphf.saes5.ui.settings;

import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.GameFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ce que l'écran Réglages affiche, calculé par {@link SettingsViewModel}. Immuable. */
public final class SettingsUiState {

    /** Un interrupteur de l'écran : l'élément, et s'il est actif. */
    public static final class FeatureToggle {
        private final GameFeature feature;
        private final boolean enabled;

        FeatureToggle(GameFeature feature, boolean enabled) {
            this.feature = feature;
            this.enabled = enabled;
        }

        public GameFeature getFeature() { return feature; }
        public boolean isEnabled() { return enabled; }
    }

    private final List<FeatureToggle> features;
    private final AnalysisModel model;
    private final int frameStride;
    private final String conditionName;
    private final int sessionCount;
    private final String snapshot;

    SettingsUiState(List<FeatureToggle> features, AnalysisModel model, int frameStride,
                    String conditionName, int sessionCount, String snapshot) {
        this.features = Collections.unmodifiableList(new ArrayList<>(features));
        this.model = model;
        this.frameStride = frameStride;
        this.conditionName = conditionName;
        this.sessionCount = sessionCount;
        this.snapshot = snapshot;
    }

    /** Tous les éléments, dans l'ordre de {@link GameFeature}. */
    public List<FeatureToggle> getFeatures() { return features; }
    public AnalysisModel getModel() { return model; }
    public int getFrameStride() { return frameStride; }
    public String getConditionName() { return conditionName; }
    /** Séances journalisées, à exporter. */
    public int getSessionCount() { return sessionCount; }
    /** La configuration telle qu'elle sera journalisée (US-7.3). */
    public String getSnapshot() { return snapshot; }
}

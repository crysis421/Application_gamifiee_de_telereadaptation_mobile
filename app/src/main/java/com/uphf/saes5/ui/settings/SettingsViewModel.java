package com.uphf.saes5.ui.settings;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.settings.AnalysisSettingsRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.data.settings.TestConditionRepository;
import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.TestCondition;
import com.uphf.saes5.ui.common.Event;

import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Réglages du chercheur : éléments de gamification (US-4.1), analyse de pose (US-1.4),
 * conditions de test nommées (US-7.3) et export du journal (US-7.2).
 */
public final class SettingsViewModel extends ViewModel {

    /** Message à afficher, ou fichier à partager. */
    public static final class SettingsEvent {

        public enum Kind {
            CONDITION_NAME_REQUIRED, CONDITION_SAVED, CONDITION_APPLIED,
            EXPORT_EMPTY, EXPORT_FAILED, EXPORT_READY
        }

        private final Kind kind;
        @Nullable private final String name;
        @Nullable private final File file;
        @Nullable private final String mimeType;

        private SettingsEvent(Kind kind, @Nullable String name, @Nullable File file,
                              @Nullable String mimeType) {
            this.kind = kind;
            this.name = name;
            this.file = file;
            this.mimeType = mimeType;
        }

        public Kind getKind() { return kind; }
        /** Nom de la condition enregistrée ou appliquée. */
        @Nullable public String getName() { return name; }
        /** Fichier exporté, pour {@link Kind#EXPORT_READY}. */
        @Nullable public File getFile() { return file; }
        @Nullable public String getMimeType() { return mimeType; }
    }

    private final GameSettingsRepository gameSettings;
    private final TestConditionRepository conditions;
    private final AnalysisSettingsRepository analysisSettings;
    private final SessionRepository sessions;
    private final Clock clock;
    private final MutableLiveData<SettingsUiState> state = new MutableLiveData<>();
    private final MutableLiveData<Event<SettingsEvent>> events = new MutableLiveData<>();

    public SettingsViewModel(GameSettingsRepository gameSettings, TestConditionRepository conditions,
                             AnalysisSettingsRepository analysisSettings, SessionRepository sessions,
                             Clock clock) {
        this.gameSettings = gameSettings;
        this.conditions = conditions;
        this.analysisSettings = analysisSettings;
        this.sessions = sessions;
        this.clock = clock;
        publish();
    }

    public LiveData<SettingsUiState> getState() {
        return state;
    }

    public LiveData<Event<SettingsEvent>> getEvents() {
        return events;
    }

    /** Relit les réglages et le nombre de séances : au retour sur l'écran. */
    public void refresh() {
        publish();
    }

    public void setFeatureEnabled(GameFeature feature, boolean enabled) {
        gameSettings.setEnabled(feature, enabled);
        publish();
    }

    public void setModel(AnalysisModel model) {
        analysisSettings.setModel(model);
        publish();
    }

    public void setFrameStride(int stride) {
        analysisSettings.setFrameStride(stride);
        publish();
    }

    /** Nom de la condition de test, enregistré à chaque frappe. */
    public void setConditionName(String name) {
        gameSettings.setConditionName(name);
        publish();
    }

    /** Enregistre la configuration courante sous le nom saisi. */
    public void saveCondition() {
        String name = gameSettings.getConditionName();
        if (name.isEmpty()) {
            emit(SettingsEvent.Kind.CONDITION_NAME_REQUIRED, null, null, null);
            return;
        }
        conditions.saveCurrent(name);
        emit(SettingsEvent.Kind.CONDITION_SAVED, name, null, null);
    }

    /** Conditions enregistrées, par ordre alphabétique, pour les boîtes de choix. */
    public List<TestCondition> getSavedConditions() {
        return conditions.getSaved();
    }

    /** Réapplique d'un toucher une condition enregistrée, avant chaque participant. */
    public void applyCondition(TestCondition condition) {
        conditions.apply(condition);
        publish();
        emit(SettingsEvent.Kind.CONDITION_APPLIED, condition.getName(), null, null);
    }

    public void deleteCondition(String name) {
        conditions.delete(name);
    }

    /**
     * Produit l'export du journal (US-7.2). Exporter un journal vide n'aurait aucun sens : on
     * le dit plutôt que de partager un fichier sans séance.
     *
     * @param days nombre de jours exportés, ou 0 pour tout l'historique
     */
    public void export(int days, SessionRepository.ExportFormat format) {
        if (sessions.count() == 0) {
            emit(SettingsEvent.Kind.EXPORT_EMPTY, null, null, null);
            return;
        }
        long from = days == 0 ? 0L : clock.millis() - TimeUnit.DAYS.toMillis(days);
        File exported = sessions.writeExport(from, Long.MAX_VALUE, format);
        if (exported == null) {
            emit(SettingsEvent.Kind.EXPORT_FAILED, null, null, null);
        } else {
            emit(SettingsEvent.Kind.EXPORT_READY, null, exported, format.getMimeType());
        }
    }

    private void emit(SettingsEvent.Kind kind, @Nullable String name, @Nullable File file,
                      @Nullable String mimeType) {
        events.setValue(new Event<>(new SettingsEvent(kind, name, file, mimeType)));
    }

    private void publish() {
        List<SettingsUiState.FeatureToggle> toggles = new ArrayList<>();
        for (GameFeature feature : GameFeature.values()) {
            toggles.add(new SettingsUiState.FeatureToggle(feature, gameSettings.isEnabled(feature)));
        }
        state.setValue(new SettingsUiState(toggles, analysisSettings.getModel(),
                analysisSettings.getFrameStride(), gameSettings.getConditionName(),
                sessions.count(), gameSettings.snapshot()));
    }
}

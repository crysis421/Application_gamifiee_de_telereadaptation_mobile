package com.uphf.saes5.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.settings.AnalysisSettingsRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.data.settings.TestConditionRepository;
import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.io.File;
import java.time.Clock;

/** Réglages : interrupteurs, analyse, conditions de test et export du journal. */
public class SettingsViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private GameSettingsRepository gameSettings;
    private SessionRepository sessions;
    private SettingsViewModel viewModel;

    @Before
    public void setUp() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        gameSettings = new GameSettingsRepository(store);
        sessions = new SessionRepository(new File(folder.getRoot(), "sessions.json"),
                new File(folder.getRoot(), "exports"), Clock.systemUTC());
        viewModel = new SettingsViewModel(gameSettings,
                new TestConditionRepository(store, gameSettings),
                new AnalysisSettingsRepository(store), sessions, Clock.systemUTC());
    }

    private SettingsUiState state() {
        return viewModel.getState().getValue();
    }

    private SettingsViewModel.SettingsEvent lastEvent() {
        return viewModel.getEvents().getValue().getContentIfNotHandled();
    }

    @Test
    public void everyFeature_isListed_andSwitchingOneShowsInTheSnapshot() {
        assertEquals(GameFeature.values().length, state().getFeatures().size());

        viewModel.setFeatureEnabled(GameFeature.SOUNDS, false);

        assertTrue(state().getSnapshot().contains("sounds=0"));
        assertFalse(state().getFeatures().get(GameFeature.SOUNDS.ordinal()).isEnabled());
    }

    @Test
    public void analysisSettings_areKept() {
        viewModel.setModel(AnalysisModel.FULL);
        viewModel.setFrameStride(2);

        assertEquals(AnalysisModel.FULL, state().getModel());
        assertEquals(2, state().getFrameStride());
    }

    @Test
    public void savingAConditionWithoutAName_isRefused() {
        viewModel.saveCondition();

        assertEquals(SettingsViewModel.SettingsEvent.Kind.CONDITION_NAME_REQUIRED,
                lastEvent().getKind());
    }

    @Test
    public void applyingASavedCondition_restoresItsSwitchesAndName() {
        viewModel.setConditionName("A");
        viewModel.setFeatureEnabled(GameFeature.STARS, false);
        viewModel.saveCondition();
        viewModel.setFeatureEnabled(GameFeature.STARS, true);
        viewModel.setConditionName("B");

        viewModel.applyCondition(viewModel.getSavedConditions().get(0));

        assertFalse(gameSettings.isEnabled(GameFeature.STARS));
        assertEquals("A", state().getConditionName());
        SettingsViewModel.SettingsEvent event = lastEvent();
        assertEquals(SettingsViewModel.SettingsEvent.Kind.CONDITION_APPLIED, event.getKind());
        assertEquals("A", event.getName());
    }

    @Test
    public void exportingAnEmptyJournal_isRefused() {
        viewModel.export(0, SessionRepository.ExportFormat.CSV);

        assertEquals(SettingsViewModel.SettingsEvent.Kind.EXPORT_EMPTY, lastEvent().getKind());
    }

    @Test
    public void exportingTheJournal_producesAFileToShare() {
        sessions.record(new SessionEntry(System.currentTimeMillis(), "base_squat", "Squat", 3, 0,
                12, 60_000L, SessionEntry.SCORE_UNAVAILABLE, "", ""));
        viewModel.refresh();

        viewModel.export(7, SessionRepository.ExportFormat.CSV);

        SettingsViewModel.SettingsEvent event = lastEvent();
        assertEquals(SettingsViewModel.SettingsEvent.Kind.EXPORT_READY, event.getKind());
        assertNotNull(event.getFile());
        assertEquals("text/csv", event.getMimeType());
        assertEquals(1, state().getSessionCount());
    }
}

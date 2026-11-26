package com.uphf.saes5.ui.recording;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.testing.FileAssetSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Enregistrement d'un modèle : compte à rebours, captation, résultat — et rotation. */
public class MovementRecordingViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private MovementRepository movements;
    private long now;

    @Before
    public void setUp() {
        movements = new MovementRepository(
                new File(folder.getRoot(), "movements"), FileAssetSource.appAssets());
    }

    private MovementRecordingViewModel viewModel(MovementRecordingViewModel.Mode mode) {
        return new MovementRecordingViewModel(mode, "mon_squat", movements, () -> now);
    }

    /** Un corps debout, de face : épaules au-dessus des hanches. */
    private static List<NormalizedLandmark> pose() {
        List<NormalizedLandmark> pose = new ArrayList<>();
        for (int i = 0; i < 33; i++) {
            float x = 0.5f + (i % 2 == 0 ? -0.05f : 0.05f);
            float y = i < 11 ? 0.2f : i < 23 ? 0.35f + 0.01f * i : 0.6f + 0.01f * i;
            pose.add(NormalizedLandmark.create(x, y, 0f, Optional.of(1f), Optional.of(1f)));
        }
        return pose;
    }

    /** Avance l'horloge, en fournissant une pose toutes les 100 ms si demandé. */
    private void advance(MovementRecordingViewModel viewModel, long millis, boolean withPose) {
        for (long t = 0; t < millis; t += 100) {
            now += 100;
            if (withPose) viewModel.onPoseLandmarks(pose(), 640, 480);
            viewModel.onTick();
        }
    }

    private static RecordingUiState state(MovementRecordingViewModel viewModel) {
        return viewModel.getState().getValue();
    }

    @Test
    public void start_countsDownFromThree_thenRecords() {
        MovementRecordingViewModel viewModel = viewModel(MovementRecordingViewModel.Mode.RECORD);

        viewModel.start();
        assertEquals(RecordingUiState.Phase.COUNTDOWN, state(viewModel).getPhase());
        assertEquals(3, state(viewModel).getSecondsLeft());
        assertFalse(state(viewModel).isActionVisible());

        advance(viewModel, 3_000, false);
        assertEquals(RecordingUiState.Phase.RECORDING, state(viewModel).getPhase());
    }

    @Test
    public void aFullRecording_isSaved() {
        MovementRecordingViewModel viewModel = viewModel(MovementRecordingViewModel.Mode.RECORD);
        viewModel.start();
        advance(viewModel, 3_000, false);

        advance(viewModel, 6_000, true);

        RecordingUiState state = state(viewModel);
        assertEquals(RecordingUiState.Phase.DONE, state.getPhase());
        assertEquals(RecordingUiState.Result.SAVED, state.getResult());
        assertTrue(state.getFrameCount() >= 20);
        assertTrue(state.isActionVisible());
        assertTrue(movements.exists("mon_squat"));
    }

    @Test
    public void tooFewFrames_areNotSaved() {
        MovementRecordingViewModel viewModel = viewModel(MovementRecordingViewModel.Mode.RECORD);
        viewModel.start();
        advance(viewModel, 3_000, false);
        advance(viewModel, 1_000, true);

        advance(viewModel, 5_000, false);

        assertEquals(RecordingUiState.Result.TOO_FEW_FRAMES, state(viewModel).getResult());
        assertFalse(movements.exists("mon_squat"));
    }

    @Test
    public void comparingWithoutAModel_isKnownBeforeTheCameraStarts() {
        // L'écran doit se fermer sans ouvrir la caméra ni demander la permission.
        assertTrue(viewModel(MovementRecordingViewModel.Mode.COMPARE).isMovementMissing());
        assertFalse(viewModel(MovementRecordingViewModel.Mode.RECORD).isMovementMissing());
    }

    @Test
    public void theRecordingInProgress_survivesTheScreenBeingRecreated() {
        // L'Activity recréée après une rotation retrouve la même instance : seul l'appel à
        // start() par la nouvelle caméra ne doit pas relancer le compte à rebours.
        MovementRecordingViewModel viewModel = viewModel(MovementRecordingViewModel.Mode.RECORD);
        viewModel.start();
        advance(viewModel, 3_000, false);
        advance(viewModel, 2_000, true);
        int framesBefore = state(viewModel).getFrameCount();

        viewModel.onCameraReady();
        advance(viewModel, 100, true);

        assertEquals(RecordingUiState.Phase.RECORDING, state(viewModel).getPhase());
        assertTrue(state(viewModel).getFrameCount() > framesBefore);
    }

    @Test
    public void anAnalysisError_makesRecordingUnavailable() {
        MovementRecordingViewModel viewModel = viewModel(MovementRecordingViewModel.Mode.RECORD);
        viewModel.start();

        viewModel.onAnalysisError("GPU indisponible");

        assertEquals(RecordingUiState.Phase.UNAVAILABLE, state(viewModel).getPhase());
        assertEquals("GPU indisponible", state(viewModel).getError());
        assertFalse(state(viewModel).isActionVisible());
    }
}

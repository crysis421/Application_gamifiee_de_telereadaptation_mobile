package com.uphf.saes5.ui.exercise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.data.exercise.MovementRepository;
import com.uphf.saes5.data.home.DailyExerciseRepository;
import com.uphf.saes5.data.session.SessionRecorder;
import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.settings.AnalysisSettingsRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.exercise.BaseExercises;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.Movement;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.testing.FileAssetSource;
import com.uphf.saes5.testing.InMemoryPreferenceStore;
import com.uphf.saes5.ui.camera.PoseFrame;
import com.uphf.saes5.ui.summary.SummaryArgs;

import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Séance d'exercice : comptage par la caméra, mode manuel, objectif atteint, abandon, et état
 * conservé quand l'écran est recréé (rotation).
 */
public class ExerciseSessionViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private GameSettingsRepository gameSettings;
    private SessionRepository sessions;
    private SessionRecorder recorder;
    private DailyExerciseRepository daily;
    private Movement squatModel;
    private long now = 1_000_000L;
    /** Événements reçus, dans l'ordre, comme les consommerait l'écran. */
    private final List<ExerciseEvent> events = new ArrayList<>();

    @Before
    public void setUp() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        gameSettings = new GameSettingsRepository(store);
        MovementRepository movements = new MovementRepository(
                new File(folder.getRoot(), "movements"), FileAssetSource.appAssets());
        ExerciseRepository exercises = new ExerciseRepository(store, movements);
        exercises.initialize();
        sessions = new SessionRepository(new File(folder.getRoot(), "sessions.json"),
                new File(folder.getRoot(), "exports"), Clock.systemUTC());
        recorder = new SessionRecorder(sessions, gameSettings,
                new AnalysisSettingsRepository(store), Clock.systemUTC());
        daily = new DailyExerciseRepository(store, exercises, Clock.systemUTC());
        squatModel = movements.loadAsset("movements/base_squat.json");
    }

    private static Exercise squat(int targetReps) {
        Exercise base = BaseExercises.all().get(1);
        return new Exercise(base.getId(), base.getName(), base.getMuscleGroup(),
                base.getDifficulty(), base.getDurationMinutes(), base.getEquipment(),
                base.getStars(), base.getDescription(), targetReps,
                base.getReferenceMovementId(), base.getSimilarityThreshold());
    }

    private ExerciseSessionViewModel viewModel(Exercise exercise, Movement reference) {
        ExerciseSessionViewModel viewModel = new ExerciseSessionViewModel(exercise, reference,
                null, gameSettings, recorder, daily, () -> now, Runnable::run, Runnable::run);
        viewModel.getEvents().observeForever(event -> {
            ExerciseEvent content = event.getContentIfNotHandled();
            if (content != null) events.add(content);
        });
        return viewModel;
    }

    private static ExerciseUiState state(ExerciseSessionViewModel viewModel) {
        return viewModel.getState().getValue();
    }

    /** Une image du modèle, replacée dans l'image de la caméra comme la verrait MediaPipe. */
    private static PoseFrame frameOf(float[] modelFrame, boolean legsVisible) {
        List<NormalizedLandmark> pose = new ArrayList<>();
        for (int i = 0; i < 33; i++) {
            float x = 0.5f + modelFrame[2 * i] * 0.12f;
            float y = 0.4f + modelFrame[2 * i + 1] * 0.12f;
            float visibility = legsVisible || i < 25 ? 1f : 0f;
            pose.add(NormalizedLandmark.create(x, y, 0f,
                    Optional.of(visibility), Optional.of(visibility)));
        }
        return new PoseFrame(pose, 40, 1000, 1000);
    }

    /** Joue le modèle une fois et demie plus lentement, bruité, une image toutes les 100 ms. */
    private void playSquat(ExerciseSessionViewModel viewModel) {
        Random random = new Random(42);
        int index = 0;
        for (float[] frame : squatModel.frames) {
            int copies = index++ % 2 == 0 ? 2 : 1;
            for (int c = 0; c < copies; c++) {
                float[] noisy = frame.clone();
                for (int i = 0; i < noisy.length; i++) {
                    noisy[i] += (random.nextFloat() - 0.5f) * 0.06f;
                }
                now += 100;
                viewModel.onPoseFrame(frameOf(noisy, true));
            }
        }
    }

    @Test
    public void withoutAModel_theSessionIsManual() {
        ExerciseSessionViewModel viewModel = viewModel(squat(12), null);

        assertTrue(state(viewModel).isManualMode());
        assertEquals(ExerciseUiState.Status.MANUAL_REFERENCE_MISSING, state(viewModel).getStatus());
        assertNull(state(viewModel).getPreview());

        viewModel.onManualValidate();

        assertEquals(1, state(viewModel).getValidatedReps());
        assertEquals(ExerciseEvent.Kind.REP_VALIDATED, events.get(0).getKind());
    }

    @Test
    public void aSquatFilmedByTheCamera_countsOneRepetition() {
        ExerciseSessionViewModel viewModel = viewModel(squat(12), squatModel);
        assertFalse(state(viewModel).isManualMode());
        assertNotNull(state(viewModel).getPreview());

        viewModel.onCameraStarted();
        playSquat(viewModel);

        assertEquals(1, state(viewModel).getValidatedReps());
        assertTrue(state(viewModel).getAverageLatency() >= 0);
    }

    @Test
    public void legsOutOfTheFrame_countNothing() {
        ExerciseSessionViewModel viewModel = viewModel(squat(12), squatModel);
        viewModel.onCameraStarted();

        for (float[] frame : squatModel.frames) {
            now += 100;
            viewModel.onPoseFrame(frameOf(frame, false));
        }

        assertEquals(ExerciseUiState.Status.BODY_NOT_VISIBLE, state(viewModel).getStatus());
        assertEquals(0, state(viewModel).getValidatedReps());
    }

    @Test
    public void reachingTheGoal_finishesOnce_afterTheDelay() {
        ExerciseSessionViewModel viewModel = viewModel(squat(1), squatModel);
        viewModel.onCameraStarted();

        playSquat(viewModel);

        assertTrue(state(viewModel).getGoalReachedAtElapsed() >= 0);
        assertEquals(ExerciseEvent.Kind.GOAL_REACHED, events.get(events.size() - 1).getKind());

        viewModel.onAutoFinishDue();
        viewModel.onAutoFinishDue();
        viewModel.onFinishRequested();

        long finished = events.stream().filter(e -> e.getKind() == ExerciseEvent.Kind.FINISHED).count();
        assertEquals(1, finished);
        SummaryArgs summary = events.get(events.size() - 1).getSummary();
        assertEquals(1, summary.getValidatedReps());
        assertTrue(daily.isCompletedToday());
    }

    @Test
    public void quitting_recordsAnAbandonedSession() {
        ExerciseSessionViewModel viewModel = viewModel(squat(12), null);
        viewModel.onManualValidate();

        viewModel.onQuitConfirmed();

        assertEquals(1, sessions.count());
        assertTrue(sessions.readAll().get(0).isAbandoned());
        assertEquals(ExerciseEvent.Kind.ABANDONED, events.get(events.size() - 1).getKind());
    }

    @Test
    public void finishingWithoutAnyRepetition_doesNotCompleteTheDay() {
        ExerciseSessionViewModel viewModel = viewModel(squat(12), null);

        viewModel.onFinishRequested();

        assertFalse(daily.isCompletedToday());
        assertEquals(SessionEntry.SCORE_UNAVAILABLE,
                events.get(events.size() - 1).getSummary().getSimilarityScore());
    }

    @Test
    public void disabledArcade_hidesTheScore_andRecordsNoPoints() {
        gameSettings.setEnabled(GameFeature.ARCADE, false);
        ExerciseSessionViewModel viewModel = viewModel(squat(12), null);
        viewModel.onManualValidate();

        viewModel.onFinishRequested();

        assertFalse(state(viewModel).isArcadeVisible());
        assertEquals(-1, events.get(0).getPointsGained());
        assertEquals(SessionEntry.POINTS_UNAVAILABLE,
                events.get(events.size() - 1).getSummary().getArcadePoints());
    }

    @Test
    public void refusingTheCamera_switchesToManualMode() {
        ExerciseSessionViewModel viewModel = viewModel(squat(12), squatModel);

        viewModel.onCameraPermission(false);

        assertTrue(state(viewModel).isManualMode());
        assertEquals(ExerciseUiState.Status.MANUAL_PERMISSION_REFUSED, state(viewModel).getStatus());
    }

    @Test
    public void theSessionSurvivesTheScreenBeingRecreated() {
        // Rotation : l'Activity est recréée, le ViewModel non. La caméra est rebranchée et
        // l'événement déjà traité n'est pas rejoué au nouvel observateur.
        ExerciseSessionViewModel viewModel = viewModel(squat(12), null);
        long started = state(viewModel).getStartedAtElapsed();
        viewModel.onManualValidate();
        viewModel.onManualValidate();

        List<ExerciseEvent> replayed = new ArrayList<>();
        viewModel.getEvents().observeForever(event -> {
            ExerciseEvent content = event.getContentIfNotHandled();
            if (content != null) replayed.add(content);
        });

        assertEquals(2, state(viewModel).getValidatedReps());
        assertEquals(started, state(viewModel).getStartedAtElapsed());
        assertTrue(replayed.isEmpty());
    }
}

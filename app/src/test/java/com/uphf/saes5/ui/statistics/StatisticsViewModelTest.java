package com.uphf.saes5.ui.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.uphf.saes5.domain.statistics.ExerciseStatistic;
import com.uphf.saes5.domain.statistics.ScoredSession;
import com.uphf.saes5.domain.statistics.StatisticsExercise;

public class StatisticsViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private StatisticsViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new StatisticsViewModel(EXERCISES, SESSIONS);
    }

    @Test
    public void refresh_keepsTheSelection_andShowsTheNewSessions() {
        viewModel.showExerciseMode();
        viewModel.refresh(EXERCISES, Arrays.asList(session("squat", 0, 100)));

        StatisticsScreenState state = viewModel.getState().getValue();
        assertEquals(StatisticsScreenState.DisplayMode.EXERCISES, state.getDisplayMode());
        assertEquals(1, state.getVisibleExerciseStatistics().size());
    }

    // Anciennes données en dur du dépôt, conservées comme jeu d'essai.
    private static final List<StatisticsExercise> EXERCISES = Arrays.asList(
            new StatisticsExercise("squat", "Squat", "Jambes"),
            new StatisticsExercise("pompes", "Pompes", "Pectoraux"),
            new StatisticsExercise("fentes", "Fentes", "Jambes"),
            new StatisticsExercise("planche", "Planche", "Tronc"),
            new StatisticsExercise("rowing", "Rowing", "Dos"));

    private static final List<ScoredSession> SESSIONS = Arrays.asList(
            session("squat", 18, 95), session("squat", 12, 88), session("squat", 5, 92),
            session("pompes", 17, 72), session("pompes", 10, 68), session("pompes", 3, 90),
            session("fentes", 16, 45), session("fentes", 9, 52), session("fentes", 2, 38),
            session("planche", 15, 82), session("planche", 8, 91), session("planche", 1, 87),
            session("rowing", 14, 22), session("rowing", 7, 30), session("rowing", 0, 38));

    private static ScoredSession session(String exerciseId, int daysAgo, int score) {
        return new ScoredSession(exerciseId, 1_796_083_200_000L - daysAgo * 86_400_000L, score);
    }

    @Test
    public void initialState_showsMusclesWithoutSelection() {
        StatisticsScreenState state = viewModel.getState().getValue();

        assertEquals(StatisticsScreenState.DisplayMode.MUSCLES, state.getDisplayMode());
        assertNull(state.getSelectedMuscle());
    }

    @Test
    public void showExerciseMode_changesTheDisplayedMode() {
        viewModel.showExerciseMode();

        assertEquals(StatisticsScreenState.DisplayMode.EXERCISES,
                viewModel.getState().getValue().getDisplayMode());
    }

    @Test
    public void selectMuscle_filtersExercisesToThatGroup() {
        viewModel.selectMuscle("Jambes");

        StatisticsScreenState state = viewModel.getState().getValue();
        List<String> names = state.getVisibleExerciseStatistics().stream()
                .map(ExerciseStatistic::getExerciseName)
                .collect(Collectors.toList());
        assertEquals("Jambes", state.getSelectedMuscle());
        assertEquals(Arrays.asList("Squat", "Fentes"), names);
    }

    @Test
    public void selectMuscle_clearsFilterWhenSelectedAgain() {
        viewModel.selectMuscle("Jambes");
        viewModel.selectMuscle("Jambes");

        StatisticsScreenState state = viewModel.getState().getValue();
        assertNull(state.getSelectedMuscle());
        assertEquals(5, state.getVisibleExerciseStatistics().size());
    }

    @Test
    public void selectMuscle_keepsEmptyMuscleSelectionWithNoExerciseRows() {
        viewModel.selectMuscle("Bras");

        StatisticsScreenState state = viewModel.getState().getValue();
        assertEquals("Bras", state.getSelectedMuscle());
        assertEquals(0, state.getVisibleExerciseStatistics().size());
    }
}

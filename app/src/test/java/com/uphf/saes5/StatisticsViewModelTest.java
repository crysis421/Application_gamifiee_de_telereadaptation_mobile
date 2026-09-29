package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StatisticsViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private StatisticsViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new StatisticsViewModel();
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

package com.uphf.saes5.ui.statistics;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.uphf.saes5.R;
import com.uphf.saes5.databinding.ActivityStatisticsBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.ui.common.TabActivity;

/**
 * Statistiques (US-5.3) : vue du MVVM, elle affiche l'état de {@link StatisticsViewModel}
 * et lui transmet la sélection d'un muscle et le changement de mode.
 */
public class StatisticsActivity extends TabActivity {

    private ActivityStatisticsBinding binding;
    private StatisticsViewModel viewModel;
    private final StatisticsListAdapter adapter = new StatisticsListAdapter();

    @Override
    protected int tabId() {
        return R.id.nav_statistics;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = AppContainer.from(this);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(StatisticsViewModel.class,
                () -> new StatisticsViewModel(container.statisticsRepository, container.gameSettings)))
                .get(StatisticsViewModel.class);

        binding.statisticsRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.statisticsRecycler.setAdapter(adapter);
        binding.statisticsRecycler.setNestedScrollingEnabled(false);

        binding.statisticsMuscleBody.setOnMuscleSelectedListener(viewModel::selectMuscle);
        binding.statisticsModeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.statistics_mode_muscles) viewModel.showMuscleMode();
            if (checkedId == R.id.statistics_mode_exercises) viewModel.showExerciseMode();
        });

        setupTabs(findViewById(R.id.bottom_nav));
        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // L'onglet est réutilisé (singleTop) : sans relecture, il afficherait les chiffres
        // d'avant la dernière séance.
        viewModel.refresh();
    }

    private void render(StatisticsScreenState state) {
        showStatisticsTab(state.isStatisticsTabVisible());
        binding.statisticsGlobalValue.setText(getString(
                R.string.statistics_percentage, state.getSummary().getGlobalPerfectPercentage()));
        binding.statisticsMuscleBody.setMuscleStatistics(state.getSummary().getMuscleStatistics());
        binding.statisticsMuscleBody.setSelectedMuscle(state.getSelectedMuscle());

        int checkedId = state.getDisplayMode() == StatisticsScreenState.DisplayMode.MUSCLES
                ? R.id.statistics_mode_muscles
                : R.id.statistics_mode_exercises;
        if (binding.statisticsModeGroup.getCheckedButtonId() != checkedId) {
            binding.statisticsModeGroup.check(checkedId);
        }
        binding.statisticsDetailHeading.setText(heading(state));

        adapter.submitList(state.getItems());
        boolean isEmpty = state.getItems().isEmpty();
        binding.statisticsEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.statisticsRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private String heading(StatisticsScreenState state) {
        switch (state.getHeading()) {
            case MUSCLE_SELECTED:
                return getString(R.string.statistics_selected_muscle_heading, state.getSelectedMuscle());
            case EXERCISES_ALL:
                return getString(R.string.statistics_exercise_heading);
            case EXERCISES_FILTERED:
                return getString(R.string.statistics_filtered_heading, state.getSelectedMuscle());
            default:
                return getString(R.string.statistics_muscle_heading);
        }
    }
}

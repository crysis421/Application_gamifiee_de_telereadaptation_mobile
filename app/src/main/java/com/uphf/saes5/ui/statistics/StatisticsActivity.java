package com.uphf.saes5.ui.statistics;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import com.uphf.saes5.R;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.statistics.MuscleStatistic;
import com.uphf.saes5.ui.common.Navigation;
import com.uphf.saes5.ui.common.TabSwipe;
import com.uphf.saes5.ui.widget.MuscleBodyView;

import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {
    private StatisticsViewModel viewModel;
    private StatisticsListAdapter adapter;
    private MuscleBodyView muscleBody;
    private MaterialButtonToggleGroup modeGroup;
    private TextView globalValue;
    private TextView detailHeading;
    private TextView emptyState;
    private RecyclerView recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        viewModel = new ViewModelProvider(this, ViewModelFactory.of(StatisticsViewModel.class,
                () -> new StatisticsViewModel(AppContainer.from(this).statisticsRepository)))
                .get(StatisticsViewModel.class);
        globalValue = findViewById(R.id.statistics_global_value);
        detailHeading = findViewById(R.id.statistics_detail_heading);
        emptyState = findViewById(R.id.statistics_empty_state);
        muscleBody = findViewById(R.id.statistics_muscle_body);
        modeGroup = findViewById(R.id.statistics_mode_group);
        recycler = findViewById(R.id.statistics_recycler);

        adapter = new StatisticsListAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.setNestedScrollingEnabled(false);

        muscleBody.setOnMuscleSelectedListener(viewModel::selectMuscle);
        modeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.statistics_mode_muscles) viewModel.showMuscleMode();
            if (checkedId == R.id.statistics_mode_exercises) viewModel.showExerciseMode();
        });

        viewModel.getState().observe(this, this::render);
        Navigation.setupTabs(this, findViewById(R.id.bottom_nav), R.id.nav_statistics);
        Navigation.setStatisticsVisible(findViewById(R.id.bottom_nav),
                AppContainer.from(this).gameSettings.isEnabled(GameFeature.STATISTICS));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // L'onglet est réutilisé (singleTop) : sans relecture, il afficherait les chiffres
        // d'avant la dernière séance.
        viewModel.refresh();
    }

    private void render(StatisticsScreenState state) {
        globalValue.setText(getString(
                R.string.statistics_percentage, state.getSummary().getGlobalPerfectPercentage()));
        muscleBody.setMuscleStatistics(state.getSummary().getMuscleStatistics());
        muscleBody.setSelectedMuscle(state.getSelectedMuscle());

        List<StatisticListItem> items;
        int checkedId;
        if (state.getDisplayMode() == StatisticsScreenState.DisplayMode.MUSCLES) {
            checkedId = R.id.statistics_mode_muscles;
            items = StatisticsListMapper.fromMuscles(
                    visibleMuscles(state.getSummary().getMuscleStatistics(), state.getSelectedMuscle()));
            detailHeading.setText(state.getSelectedMuscle() == null
                    ? getString(R.string.statistics_muscle_heading)
                    : getString(R.string.statistics_selected_muscle_heading, state.getSelectedMuscle()));
        } else {
            checkedId = R.id.statistics_mode_exercises;
            items = StatisticsListMapper.fromExercises(state.getVisibleExerciseStatistics());
            detailHeading.setText(state.getSelectedMuscle() == null
                    ? getString(R.string.statistics_exercise_heading)
                    : getString(R.string.statistics_filtered_heading, state.getSelectedMuscle()));
        }
        if (modeGroup.getCheckedButtonId() != checkedId) modeGroup.check(checkedId);

        adapter.submitList(items);
        boolean isEmpty = items.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private List<MuscleStatistic> visibleMuscles(List<MuscleStatistic> statistics,
                                                 String selectedMuscle) {
        if (selectedMuscle == null) return statistics;
        List<MuscleStatistic> result = new ArrayList<>();
        for (MuscleStatistic statistic : statistics) {
            if (selectedMuscle.equals(statistic.getMuscleGroup())) result.add(statistic);
        }
        return result;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Balayer vers la gauche ou la droite change d'onglet (voir TabSwipe).
        return TabSwipe.dispatch(this, event) || super.dispatchTouchEvent(event);
    }
}

package com.uphf.saes5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;

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

        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
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
        configureNavigation();
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

    private void configureNavigation() {
        BottomNavigationView navigation = findViewById(R.id.bottom_nav);
        navigation.setSelectedItemId(R.id.nav_statistics);
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                Navigation.openTab(this, MainActivity.class);
                finish();
                return true;
            }
            if (item.getItemId() == R.id.nav_catalog) {
                Navigation.openTab(this, ExerciseCatalogActivity.class);
                finish();
                return true;
            }
            return item.getItemId() == R.id.nav_statistics;
        });
    }
}

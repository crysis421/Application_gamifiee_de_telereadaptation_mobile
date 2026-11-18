package com.uphf.saes5;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ProfileSummary summary = ProfileCalculator.calculate(
                StatisticsRepository.getExercises(), StatisticsRepository.getAll());
        ((TextView) findViewById(R.id.profile_star_count)).setText(
                getString(R.string.profile_stars_count, summary.getStarCount()));
        renderMuscleRanks(summary);
        renderHistory(summary);
        configureNavigation();
    }

    private void renderMuscleRanks(ProfileSummary summary) {
        LinearLayout container = findViewById(R.id.profile_muscle_ranks);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (MuscleStatistic statistic : summary.getMuscleStatistics()) {
            View row = inflater.inflate(R.layout.item_profile_muscle, container, false);
            ((TextView) row.findViewById(R.id.profile_muscle_name))
                    .setText(statistic.getMuscleGroup());

            TextView rank = row.findViewById(R.id.profile_muscle_rank);
            TextView score = row.findViewById(R.id.profile_muscle_score);
            TextView sessions = row.findViewById(R.id.profile_muscle_sessions);
            if (!statistic.hasData()) {
                rank.setText(R.string.legend_no_data);
                score.setVisibility(View.GONE);
                sessions.setVisibility(View.GONE);
            } else {
                rank.setText(rankLabel(MuscleProgress.fromScore(statistic.getAverageScore())));
                score.setText(getString(R.string.statistics_score, statistic.getAverageScore()));
                sessions.setText(getResources().getQuantityString(
                        R.plurals.statistics_sessions,
                        statistic.getSessionCount(),
                        statistic.getSessionCount()));
            }
            container.addView(row);
        }
    }

    private int rankLabel(MuscleProgress progress) {
        switch (progress) {
            case TO_IMPROVE:
                return R.string.legend_to_improve;
            case PROGRESSING:
                return R.string.legend_progressing;
            default:
                return R.string.legend_mastered;
        }
    }

    private void renderHistory(ProfileSummary summary) {
        TextView emptyState = findViewById(R.id.profile_history_empty);
        RecyclerView history = findViewById(R.id.profile_history_list);
        if (summary.getHistory().isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            history.setVisibility(View.GONE);
            return;
        }

        history.setLayoutManager(new LinearLayoutManager(this));
        ProfileHistoryAdapter adapter = new ProfileHistoryAdapter();
        adapter.submitList(summary.getHistory());
        history.setAdapter(adapter);
        history.setNestedScrollingEnabled(false);
    }

    private void configureNavigation() {
        BottomNavigationView navigation = findViewById(R.id.bottom_nav);
        Navigation.applyGameSettings(navigation);
        navigation.setSelectedItemId(R.id.nav_profile);
        // Navigation.openTab plutôt que startActivity + finish : les onglets se réutilisent
        // au lieu de s'empiler, et le catalogue devient atteignable depuis le profil.
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                Navigation.openTab(this, MainActivity.class);
            } else if (item.getItemId() == R.id.nav_statistics) {
                Navigation.openTab(this, StatisticsActivity.class);
            } else if (item.getItemId() == R.id.nav_catalog) {
                Navigation.openTab(this, ExerciseCatalogActivity.class);
            }
            return true;
        });
    }
}

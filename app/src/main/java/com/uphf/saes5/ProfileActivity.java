package com.uphf.saes5;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Profil : rangs par muscle et étoiles par exercice (US-4.5), puis historique des séances.
 *
 * <p>Tout est calculé à partir du journal des séances. L'écran est relu à chaque retour, car
 * l'onglet est réutilisé (singleTop) et une séance a pu se terminer entre-temps.</p>
 */
public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        render();
        configureNavigation();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        render();
    }

    private void render() {
        boolean progressionEnabled = GameSettings.isEnabled(GameSettings.Feature.STARS);
        findViewById(R.id.profile_progression)
                .setVisibility(progressionEnabled ? View.VISIBLE : View.GONE);
        if (progressionEnabled) {
            Progression.Summary progression = Progression.calculate(
                    ExerciseRepository.getAll(), SessionLog.readAll());
            ((TextView) findViewById(R.id.profile_star_count)).setText(
                    getString(R.string.profile_stars_count, progression.getTotalStars()));
            renderMuscleRanks(progression);
            renderExerciseStars(progression);
        }

        renderHistory(ProfileCalculator.calculate(
                StatisticsRepository.getExercises(), StatisticsRepository.getAll()));
    }

    private void renderMuscleRanks(Progression.Summary progression) {
        LinearLayout container = findViewById(R.id.profile_muscle_ranks);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Progression.MuscleRank muscle : progression.getMuscles()) {
            View row = inflater.inflate(R.layout.item_profile_muscle, container, false);
            ((TextView) row.findViewById(R.id.profile_muscle_name))
                    .setText(muscle.getMuscleGroup());
            ((TextView) row.findViewById(R.id.profile_muscle_rank))
                    .setText(rankLabel(muscle.getRank()));
            ((TextView) row.findViewById(R.id.profile_muscle_score))
                    .setText(getString(R.string.profile_points, muscle.getPoints()));

            Progression.Rank next = muscle.getRank().next();
            ((TextView) row.findViewById(R.id.profile_muscle_sessions)).setText(next == null
                    ? getString(R.string.profile_max_rank)
                    : getString(R.string.profile_next_rank,
                            muscle.getPointsToNextRank(), getString(rankLabel(next))));
            container.addView(row);
        }
    }

    private void renderExerciseStars(Progression.Summary progression) {
        LinearLayout container = findViewById(R.id.profile_exercise_stars);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Progression.ExerciseStars exercise : progression.getExercises()) {
            // Même carte que les muscles : nom, étoiles, points et prochain palier.
            View row = inflater.inflate(R.layout.item_profile_muscle, container, false);
            ((TextView) row.findViewById(R.id.profile_muscle_name)).setText(exercise.getName());
            ((TextView) row.findViewById(R.id.profile_muscle_rank))
                    .setText(starLine(exercise.getStars()));
            ((TextView) row.findViewById(R.id.profile_muscle_score))
                    .setText(getString(R.string.profile_points, exercise.getPoints()));
            ((TextView) row.findViewById(R.id.profile_muscle_sessions)).setText(
                    exercise.getPointsToNextStar() == 0
                            ? getString(R.string.profile_all_stars)
                            : getString(R.string.profile_next_star,
                                    exercise.getPointsToNextStar()));
            container.addView(row);
        }
    }

    private static String starLine(int stars) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < Progression.MAX_STARS; i++) line.append(i < stars ? '★' : '☆');
        return line.toString();
    }

    @StringRes
    private static int rankLabel(Progression.Rank rank) {
        switch (rank) {
            case BRONZE: return R.string.rank_bronze;
            case SILVER: return R.string.rank_silver;
            case GOLD: return R.string.rank_gold;
            case PLATINUM: return R.string.rank_platinum;
            default: return R.string.rank_novice;
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

        emptyState.setVisibility(View.GONE);
        history.setVisibility(View.VISIBLE);
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

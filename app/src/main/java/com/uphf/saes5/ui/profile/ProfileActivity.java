package com.uphf.saes5.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.uphf.saes5.R;
import com.uphf.saes5.databinding.ActivityProfileBinding;
import com.uphf.saes5.databinding.ItemProfileMuscleBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.game.Progression;
import com.uphf.saes5.domain.statistics.ProfileSummary;
import com.uphf.saes5.ui.common.TabActivity;

/**
 * Profil : rangs par muscle et étoiles par exercice (US-4.5), puis historique des séances.
 *
 * <p>Vue du MVVM : elle affiche l'état de {@link ProfileViewModel}. L'état est relu à chaque
 * retour, car l'onglet est réutilisé (singleTop) et une séance a pu se terminer entre-temps.</p>
 */
public class ProfileActivity extends TabActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;
    private final ProfileHistoryAdapter historyAdapter = new ProfileHistoryAdapter();

    @Override
    protected int tabId() {
        return R.id.nav_profile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.profileHistoryList.setLayoutManager(new LinearLayoutManager(this));
        binding.profileHistoryList.setAdapter(historyAdapter);
        binding.profileHistoryList.setNestedScrollingEnabled(false);

        AppContainer container = AppContainer.from(this);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(ProfileViewModel.class,
                () -> new ProfileViewModel(container.exerciseRepository,
                        container.sessionRepository, container.statisticsRepository,
                        container.gameSettings)))
                .get(ProfileViewModel.class);
        setupTabs(findViewById(R.id.bottom_nav));
        viewModel.getState().observe(this, this::render);
        viewModel.refresh();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        viewModel.refresh();
    }

    private void render(ProfileUiState state) {
        showStatisticsTab(state.isStatisticsTabVisible());
        binding.profileProgression.setVisibility(
                state.isProgressionVisible() ? View.VISIBLE : View.GONE);
        Progression.Summary progression = state.getProgression();
        if (progression != null) {
            binding.profileStarCount.setText(
                    getString(R.string.profile_stars_count, progression.getTotalStars()));
            renderMuscleRanks(progression);
            renderExerciseStars(progression);
        }
        renderHistory(state.getHistory());
    }

    private void renderMuscleRanks(Progression.Summary progression) {
        binding.profileMuscleRanks.removeAllViews();
        for (Progression.MuscleRank muscle : progression.getMuscles()) {
            Progression.Rank next = muscle.getRank().next();
            addCard(binding.profileMuscleRanks, muscle.getMuscleGroup(),
                    getString(rankLabel(muscle.getRank())), muscle.getPoints(),
                    next == null
                            ? getString(R.string.profile_max_rank)
                            : getString(R.string.profile_next_rank,
                                    muscle.getPointsToNextRank(), getString(rankLabel(next))));
        }
    }

    private void renderExerciseStars(Progression.Summary progression) {
        binding.profileExerciseStars.removeAllViews();
        for (Progression.ExerciseStars exercise : progression.getExercises()) {
            addCard(binding.profileExerciseStars, exercise.getName(),
                    starLine(exercise.getStars()), exercise.getPoints(),
                    exercise.getPointsToNextStar() == 0
                            ? getString(R.string.profile_all_stars)
                            : getString(R.string.profile_next_star, exercise.getPointsToNextStar()));
        }
    }

    /** Une carte de progression : nom, rang ou étoiles, points et prochain palier. */
    private void addCard(LinearLayout container, String name, String badge, int points,
                         String nextStep) {
        ItemProfileMuscleBinding card =
                ItemProfileMuscleBinding.inflate(getLayoutInflater(), container, false);
        card.profileMuscleName.setText(name);
        card.profileMuscleRank.setText(badge);
        card.profileMuscleScore.setText(getString(R.string.profile_points, points));
        card.profileMuscleSessions.setText(nextStep);
        container.addView(card.getRoot());
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
        boolean empty = summary.getHistory().isEmpty();
        binding.profileHistoryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.profileHistoryList.setVisibility(empty ? View.GONE : View.VISIBLE);
        historyAdapter.submitList(summary.getHistory());
    }
}

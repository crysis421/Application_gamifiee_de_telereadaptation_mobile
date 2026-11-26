package com.uphf.saes5.ui.summary;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import com.uphf.saes5.R;
import com.uphf.saes5.databinding.ActivitySummaryBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.domain.model.SessionResult;
import com.uphf.saes5.ui.common.EdgeToEdgeSupport;
import com.uphf.saes5.ui.common.ExerciseFormatter;
import com.uphf.saes5.ui.exercise.ExerciseActivity;

/**
 * Récapitulatif affiché à la fin d'un exercice : statistiques de la séance et mise en favori.
 *
 * <p>Vue du MVVM : {@link SummaryViewModel} journalise la séance et prépare l'affichage.</p>
 */
public class SummaryActivity extends AppCompatActivity {

    // Noms d'extras des versions précédentes de l'écran.
    private static final String EXTRA_EXERCISE_ID = "extra_exercise_id";
    private static final String EXTRA_VALIDATED_REPS = "extra_validated_reps";
    private static final String EXTRA_FAILED_REPS = "extra_failed_reps";
    private static final String EXTRA_DURATION_MILLIS = "extra_duration_millis";
    private static final String EXTRA_SIMILARITY_SCORE = "extra_similarity_score";
    private static final String EXTRA_AVERAGE_LATENCY = "extra_average_latency";
    private static final String EXTRA_ARCADE_POINTS = "extra_arcade_points";

    private ActivitySummaryBinding binding;
    private SummaryViewModel viewModel;

    public static Intent newIntent(Context context, SummaryArgs args) {
        return new Intent(context, SummaryActivity.class)
                .putExtra(EXTRA_EXERCISE_ID, args.getExerciseId())
                .putExtra(EXTRA_VALIDATED_REPS, args.getValidatedReps())
                .putExtra(EXTRA_FAILED_REPS, args.getFailedReps())
                .putExtra(EXTRA_DURATION_MILLIS, args.getDurationMillis())
                .putExtra(EXTRA_SIMILARITY_SCORE, args.getSimilarityScore())
                .putExtra(EXTRA_AVERAGE_LATENCY, args.getAverageLatencyMillis())
                .putExtra(EXTRA_ARCADE_POINTS, args.getArcadePoints());
    }

    private static SummaryArgs argsOf(Intent intent) {
        return new SummaryArgs(
                intent.getStringExtra(EXTRA_EXERCISE_ID),
                intent.getIntExtra(EXTRA_VALIDATED_REPS, 0),
                intent.getIntExtra(EXTRA_FAILED_REPS, 0),
                intent.getLongExtra(EXTRA_DURATION_MILLIS, 0L),
                intent.getIntExtra(EXTRA_SIMILARITY_SCORE, SessionEntry.SCORE_UNAVAILABLE),
                intent.getIntExtra(EXTRA_AVERAGE_LATENCY, SessionEntry.LATENCY_UNAVAILABLE),
                intent.getIntExtra(EXTRA_ARCADE_POINTS, SessionEntry.POINTS_UNAVAILABLE));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        SummaryArgs args = argsOf(getIntent());
        AppContainer container = AppContainer.from(this);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(SummaryViewModel.class,
                () -> new SummaryViewModel(args, container.exerciseRepository,
                        container.favoritesRepository, container.gameSettings,
                        container.sessionRecorder)))
                .get(SummaryViewModel.class);

        Exercise exercise = viewModel.getState().getValue().getExercise();
        if (exercise == null) {
            finish();
            return;
        }
        // Une séance n'est journalisée qu'une fois : après une rotation, ou une restauration de
        // l'écran, elle est déjà dans le journal.
        viewModel.onCreated(savedInstanceState == null);

        binding = ActivitySummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeSupport.applySystemBarInsets(binding.main);

        binding.favoriteButton.setOnClickListener(view -> viewModel.toggleFavorite());
        binding.restartButton.setOnClickListener(view -> {
            startActivity(ExerciseActivity.newIntent(this, exercise.getId()));
            finish();
        });
        binding.backHomeButton.setOnClickListener(view -> finish());

        viewModel.getState().observe(this, this::render);
        viewModel.getFavoriteChanges().observe(this, event -> {
            Boolean favorite = event.getContentIfNotHandled();
            if (favorite == null) return;
            Snackbar.make(binding.getRoot(),
                    favorite ? R.string.summary_favorite_added : R.string.summary_favorite_removed,
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    private void render(SummaryUiState state) {
        Exercise exercise = state.getExercise();
        SessionResult result = state.getResult();
        binding.exerciseName.setText(exercise.getName());

        boolean goalReached = state.isGoalReached();
        binding.goalBadge.setVisibility(goalReached ? View.VISIBLE : View.GONE);
        binding.goalMissed.setVisibility(goalReached ? View.GONE : View.VISIBLE);
        binding.goalMissed.setText(getString(R.string.summary_goal_missed, exercise.getTargetReps()));

        binding.statReps.statValue.setText(String.valueOf(result.getValidatedReps()));
        binding.statReps.statLabel.setText(R.string.summary_stat_reps);

        binding.statSuccessRate.statValue.setText(
                getString(R.string.summary_percent, result.getSuccessRate()));
        binding.statSuccessRate.statLabel.setText(R.string.summary_stat_success_rate);

        binding.statTotalTime.statValue.setText(
                ExerciseFormatter.formatDuration(result.getDurationMillis()));
        binding.statTotalTime.statLabel.setText(R.string.summary_stat_total_time);

        binding.statAverageTime.statValue.setText(
                ExerciseFormatter.formatDuration(result.getAverageMillisPerRep()));
        binding.statAverageTime.statLabel.setText(R.string.summary_stat_average_time);

        binding.statFailedReps.statValue.setText(String.valueOf(result.getFailedReps()));
        binding.statFailedReps.statLabel.setText(R.string.summary_stat_failed_reps);

        binding.statSimilarity.statValue.setText(
                state.getSimilarity() == SessionEntry.SCORE_UNAVAILABLE
                        ? getString(R.string.summary_similarity_unavailable)
                        : getString(R.string.summary_percent, state.getSimilarity()));
        binding.statSimilarity.statLabel.setText(R.string.summary_stat_similarity);

        // US-4.4 : absent si l'élément de jeu était désactivé pendant la séance.
        binding.statPoints.getRoot().setVisibility(state.isPointsVisible() ? View.VISIBLE : View.GONE);
        binding.statPoints.statValue.setText(String.valueOf(state.getPoints()));
        binding.statPoints.statLabel.setText(R.string.summary_stat_points);

        // Les favoris sont un élément de jeu désactivable (US-4.1).
        binding.favoriteButton.setVisibility(state.isFavoriteVisible() ? View.VISIBLE : View.GONE);
        binding.favoriteButton.setText(state.isFavorite()
                ? R.string.summary_remove_favorite
                : R.string.summary_add_favorite);
        binding.favoriteButton.setIconResource(
                state.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }
}

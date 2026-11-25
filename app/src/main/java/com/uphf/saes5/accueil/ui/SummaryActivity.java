package com.uphf.saes5.accueil.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import com.uphf.saes5.R;
import com.uphf.saes5.ExerciseRepository;
import com.uphf.saes5.GameSettings;
import com.uphf.saes5.SessionEntry;
import com.uphf.saes5.SessionLog;
import com.uphf.saes5.accueil.data.FavoritesRepository;
import com.uphf.saes5.databinding.ActivitySummaryBinding;
import com.uphf.saes5.Exercise;
import com.uphf.saes5.accueil.model.ExerciseSession;

/**
 * Récapitulatif affiché à la fin d'un exercice : statistiques de la session et mise en favori.
 *
 * <p>Les favoris enregistrés ici seront repris par le catalogue (US-6.1) pour retrouver ses
 * exercices préférés.</p>
 */
public class SummaryActivity extends AppCompatActivity {

    private static final String EXTRA_EXERCISE_ID = "extra_exercise_id";
    private static final String EXTRA_VALIDATED_REPS = "extra_validated_reps";
    private static final String EXTRA_FAILED_REPS = "extra_failed_reps";
    private static final String EXTRA_DURATION_MILLIS = "extra_duration_millis";
    private static final String EXTRA_SIMILARITY_SCORE = "extra_similarity_score";

    private ActivitySummaryBinding binding;
    private FavoritesRepository favoritesRepository;
    private Exercise exercise;

    public static Intent newIntent(Context context,
                                   String exerciseId,
                                   int validatedReps,
                                   int failedReps,
                                   long durationMillis,
                                   int similarityScore) {
        Intent intent = new Intent(context, SummaryActivity.class);
        intent.putExtra(EXTRA_EXERCISE_ID, exerciseId);
        intent.putExtra(EXTRA_VALIDATED_REPS, validatedReps);
        intent.putExtra(EXTRA_FAILED_REPS, failedReps);
        intent.putExtra(EXTRA_DURATION_MILLIS, durationMillis);
        intent.putExtra(EXTRA_SIMILARITY_SCORE, similarityScore);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        String exerciseId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
        exercise = ExerciseRepository.findById(exerciseId);
        if (exercise == null) {
            finish();
            return;
        }

        binding = ActivitySummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeSupport.applySystemBarInsets(binding.main);

        favoritesRepository = new FavoritesRepository(this);

        ExerciseSession session = new ExerciseSession(
                exerciseId,
                getIntent().getIntExtra(EXTRA_VALIDATED_REPS, 0),
                getIntent().getIntExtra(EXTRA_FAILED_REPS, 0),
                getIntent().getLongExtra(EXTRA_DURATION_MILLIS, 0L));
        int similarityScore = getIntent().getIntExtra(
                EXTRA_SIMILARITY_SCORE, SessionEntry.SCORE_UNAVAILABLE);

        bindSession(session, similarityScore);
        if (GameSettings.isEnabled(GameSettings.Feature.STARS)) {
            int stars = session.earnedStars(exercise.getTargetReps());
            binding.sessionStars.setText("★★★".substring(0, stars) + "☆☆☆".substring(stars));
            binding.sessionStars.setContentDescription(getString(R.string.session_stars, stars));
            binding.sessionStars.setVisibility(View.VISIBLE);
            if (savedInstanceState == null && android.animation.ValueAnimator.areAnimatorsEnabled()) {
                binding.sessionStars.setAlpha(0f);
                binding.sessionStars.setScaleX(0.85f);
                binding.sessionStars.setScaleY(0.85f);
                binding.sessionStars.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(450).start();
            }
        }
        bindFavoriteButton();

        // Une séance n'est journalisée qu'une fois : après une rotation, l'écran est recréé
        // avec les mêmes extras et l'écrirait une seconde fois.
        if (savedInstanceState == null) {
            logSession(session, similarityScore);
        }

        binding.restartButton.setOnClickListener(view -> {
            startActivity(ExerciseActivity.newIntent(this, exercise.getId()));
            finish();
        });
        binding.backHomeButton.setOnClickListener(view -> finish());
    }

    private void bindSession(ExerciseSession session, int similarityScore) {
        binding.exerciseName.setText(exercise.getName());

        boolean goalReached = session.isGoalReached(exercise.getTargetReps());
        binding.goalBadge.setVisibility(goalReached ? View.VISIBLE : View.GONE);
        binding.goalMissed.setVisibility(goalReached ? View.GONE : View.VISIBLE);
        binding.goalMissed.setText(getString(R.string.summary_goal_missed, exercise.getTargetReps()));

        binding.statReps.statValue.setText(String.valueOf(session.getValidatedReps()));
        binding.statReps.statLabel.setText(R.string.summary_stat_reps);

        binding.statSuccessRate.statValue.setText(
                getString(R.string.summary_percent, session.getSuccessRate()));
        binding.statSuccessRate.statLabel.setText(R.string.summary_stat_success_rate);

        binding.statTotalTime.statValue.setText(
                ExerciseFormatter.formatDuration(session.getDurationMillis()));
        binding.statTotalTime.statLabel.setText(R.string.summary_stat_total_time);

        binding.statAverageTime.statValue.setText(
                ExerciseFormatter.formatDuration(session.getAverageMillisPerRep()));
        binding.statAverageTime.statLabel.setText(R.string.summary_stat_average_time);

        binding.statFailedReps.statValue.setText(String.valueOf(session.getFailedReps()));
        binding.statFailedReps.statLabel.setText(R.string.summary_stat_failed_reps);

        binding.statSimilarity.statValue.setText(similarityScore == SessionEntry.SCORE_UNAVAILABLE
                ? getString(R.string.summary_similarity_unavailable)
                : getString(R.string.summary_percent, similarityScore));
        binding.statSimilarity.statLabel.setText(R.string.summary_stat_similarity);
    }

    /**
     * Enregistre la séance dans le journal de recherche (US-7.1).
     *
     * <p>Les réglages actifs et le nom de la condition de test accompagnent la séance : sans
     * eux, une mesure ne peut pas être rattachée à la configuration qui l'a produite.</p>
     *
     * <p>Une séance manuelle conserve explicitement un score indisponible plutôt que zéro, afin
     * de ne pas se confondre avec une comparaison réellement mesurée.</p>
     */
    private void logSession(ExerciseSession session, int similarityScore) {
        SessionLog.record(new SessionEntry(
                System.currentTimeMillis(),
                exercise.getId(),
                exercise.getName(),
                session.getValidatedReps(),
                session.getFailedReps(),
                exercise.getTargetReps(),
                session.getDurationMillis(),
                similarityScore,
                GameSettings.snapshot(),
                GameSettings.getConditionName()));
    }

    private void bindFavoriteButton() {
        // Les favoris sont un élément de jeu désactivable (US-4.1).
        if (!GameSettings.isEnabled(GameSettings.Feature.FAVORITES)) {
            binding.favoriteButton.setVisibility(View.GONE);
            return;
        }
        binding.favoriteButton.setVisibility(View.VISIBLE);
        refreshFavoriteButton();
        binding.favoriteButton.setOnClickListener(view -> {
            boolean favorite = favoritesRepository.toggleFavorite(exercise.getId());
            refreshFavoriteButton();
            Snackbar.make(binding.getRoot(),
                    favorite ? R.string.summary_favorite_added : R.string.summary_favorite_removed,
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    private void refreshFavoriteButton() {
        boolean favorite = favoritesRepository.isFavorite(exercise.getId());
        binding.favoriteButton.setText(
                favorite ? R.string.summary_remove_favorite : R.string.summary_add_favorite);
        binding.favoriteButton.setIconResource(
                favorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }
}

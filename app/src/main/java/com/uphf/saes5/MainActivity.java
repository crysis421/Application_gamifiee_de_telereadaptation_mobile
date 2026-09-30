package com.uphf.saes5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.uphf.saes5.accueil.data.DailyExerciseProvider;
import com.uphf.saes5.accueil.data.DailyExerciseTracker;
import com.uphf.saes5.accueil.data.FavoritesRepository;
import com.uphf.saes5.accueil.ui.EdgeToEdgeSupport;
import com.uphf.saes5.accueil.ui.ExerciseActivity;
import com.uphf.saes5.accueil.ui.ExerciseFormatter;
import com.uphf.saes5.databinding.ActivityMainBinding;

/**
 * Accueil. Il porte la carte « Exercice du jour » (US-4.2) ainsi que les raccourcis vers les
 * statistiques et le catalogue ; les blocs « nombre de pas » (US-5.1) et « exercices réalisés »
 * (US-5.2) viendront s'y ajouter.
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_DETAILS_EXPANDED = "daily_details_expanded";

    private ActivityMainBinding binding;
    private FavoritesRepository favoritesRepository;
    private DailyExerciseTracker dailyExerciseTracker;

    @Nullable
    private Exercise exerciseOfTheDay;

    /** Carte « Exercice du jour » repliée par défaut : seul l'en-tête reste visible. */
    private boolean detailsExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        detailsExpanded = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_DETAILS_EXPANDED, false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeSupport.applySystemBarInsets(binding.main);

        binding.openStatisticsButton.setOnClickListener(view ->
                startActivity(new Intent(this, StatisticsActivity.class)));
        binding.openCatalogButton.setOnClickListener(view ->
                startActivity(new Intent(this, ExerciseCatalogActivity.class)));

        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_catalog) {
                startActivity(new Intent(this, ExerciseCatalogActivity.class));
            } else if (item.getItemId() == R.id.nav_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
            }
            return true;
        });

        favoritesRepository = new FavoritesRepository(this);
        dailyExerciseTracker = new DailyExerciseTracker(this);

        bindExerciseOfTheDay();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // La carte doit rester dans le même état après une rotation de l'écran.
        outState.putBoolean(STATE_DETAILS_EXPANDED, detailsExpanded);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // L'accueil reste l'onglet sélectionné au retour d'un autre écran.
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        // Le catalogue a pu changer entre-temps : un exercice créé devient éligible aussitôt.
        bindExerciseOfTheDay();
    }

    /**
     * Relit l'exercice du jour dans le catalogue et rafraîchit la carte.
     *
     * <p>Appelé aussi depuis {@code onResume} : l'état favori, la réalisation du jour et le
     * catalogue lui-même ont pu changer sur un autre écran.</p>
     */
    private void bindExerciseOfTheDay() {
        exerciseOfTheDay = DailyExerciseProvider.ofTheDay();

        boolean hasExercise = exerciseOfTheDay != null;
        binding.dailyExerciseCard.getRoot().setVisibility(hasExercise ? View.VISIBLE : View.GONE);
        binding.emptyCatalogMessage.setVisibility(hasExercise ? View.GONE : View.VISIBLE);
        if (!hasExercise) {
            return;
        }

        bindExercise(exerciseOfTheDay);
    }

    private void bindExercise(Exercise exercise) {
        binding.dailyExerciseCard.exerciseName.setText(exercise.getName());
        binding.dailyExerciseCard.exerciseMeta.setText(ExerciseFormatter.metaLine(this, exercise));
        binding.dailyExerciseCard.exerciseGoal.setText(
                getString(R.string.daily_exercise_goal, exercise.getTargetReps()));

        // Les exercices créés par l'utilisateur n'ont pas de consigne : on n'affiche pas un vide.
        binding.dailyExerciseCard.exerciseDescription.setText(exercise.getDescription());
        binding.dailyExerciseCard.exerciseDescription.setVisibility(
                exercise.hasDescription() ? View.VISIBLE : View.GONE);

        binding.dailyExerciseCard.favoriteButton.setOnClickListener(view -> {
            favoritesRepository.toggleFavorite(exercise.getId());
            refreshFavoriteIcon(exercise);
        });

        binding.dailyExerciseCard.startButton.setOnClickListener(view ->
                startActivity(ExerciseActivity.newIntent(this, exercise.getId())));

        // Toute la carte plie / déplie, le lien n'est que le repère visuel de l'action.
        binding.dailyExerciseCard.getRoot().setOnClickListener(view -> toggleDetails());
        binding.dailyExerciseCard.toggleDetailsButton.setOnClickListener(view -> toggleDetails());

        applyDetailsState();
        refreshFavoriteIcon(exercise);
        refreshCompletionState();
    }

    private void toggleDetails() {
        detailsExpanded = !detailsExpanded;
        applyDetailsState();
    }

    /**
     * Affiche ou masque le détail de la carte.
     *
     * <p>Il n'y a pas de chevron : c'est le libellé du lien qui porte l'information (« Voir le
     * détail » / « Réduire »), donc l'action reste explicite sans icône à interpréter. Le pliage
     * lui-même est animé par {@code animateLayoutChanges} côté layout.</p>
     */
    private void applyDetailsState() {
        binding.dailyExerciseCard.dailyDetails.setVisibility(detailsExpanded ? View.VISIBLE : View.GONE);
        binding.dailyExerciseCard.toggleDetailsButton.setText(detailsExpanded
                ? R.string.daily_exercise_collapse
                : R.string.daily_exercise_expand);
    }

    private void refreshFavoriteIcon(Exercise exercise) {
        boolean favorite = favoritesRepository.isFavorite(exercise.getId());
        binding.dailyExerciseCard.favoriteButton.setIconResource(
                favorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        binding.dailyExerciseCard.favoriteButton.setContentDescription(
                getString(favorite ? R.string.summary_remove_favorite : R.string.summary_add_favorite));
    }

    private void refreshCompletionState() {
        boolean doneToday = dailyExerciseTracker.isCompletedToday();
        binding.dailyExerciseCard.doneBadge.setVisibility(doneToday ? View.VISIBLE : View.GONE);
        binding.dailyExerciseCard.startButton.setText(
                doneToday ? R.string.daily_exercise_restart : R.string.daily_exercise_start);
    }
}

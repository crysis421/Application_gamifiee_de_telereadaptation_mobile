package com.uphf.saes5;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
 * statistiques et le catalogue, ainsi que le nombre de pas du jour (US-5.1) et le nombre
 * d'exercices réalisés (US-5.2).
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_DETAILS_EXPANDED = "daily_details_expanded";

    private ActivityMainBinding binding;
    private StepCounter stepCounter;
    /** Demandée seulement au toucher de la tuile des pas, jamais d'office au lancement. */
    private final ActivityResultLauncher<String> requestActivityRecognition =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> bindSteps());
    private FavoritesRepository favoritesRepository;
    private DailyExerciseTracker dailyExerciseTracker;
    private DailyExerciseProvider dailyExerciseProvider;

    @Nullable
    private Exercise exerciseOfTheDay;

    /** Carte « Exercice du jour » repliée par défaut : seul l'en-tête reste visible. */
    private boolean detailsExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // US-9.1 : rien ne démarre, caméra comprise, avant l'accord éclairé du participant.
        if (Consent.isRequired()) {
            startActivity(new Intent(this, ConsentActivity.class));
            finish();
            return;
        }
        EdgeToEdge.enable(this);

        detailsExpanded = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_DETAILS_EXPANDED, false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeSupport.applySystemBarInsets(binding.main);

        binding.openStatisticsButton.setOnClickListener(view ->
                Navigation.openTab(this, StatisticsActivity.class));
        binding.openCatalogButton.setOnClickListener(view ->
                Navigation.openTab(this, ExerciseCatalogActivity.class));
        binding.openSettingsButton.setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class)));

        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_catalog) {
                Navigation.openTab(this, ExerciseCatalogActivity.class);
            } else if (item.getItemId() == R.id.nav_statistics) {
                Navigation.openTab(this, StatisticsActivity.class);
            } else if (item.getItemId() == R.id.nav_profile) {
                Navigation.openTab(this, ProfileActivity.class);
            }
            return true;
        });

        favoritesRepository = new FavoritesRepository(this);
        stepCounter = new StepCounter(this);
        dailyExerciseTracker = new DailyExerciseTracker(this);
        dailyExerciseProvider = new DailyExerciseProvider(this);

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
        // Les réglages ont pu changer : on réapplique avant de reconstruire la carte.
        applyGameSettings();
        // Le catalogue a pu changer entre-temps : un exercice créé devient éligible aussitôt.
        bindExerciseOfTheDay();
        // Une séance a pu se terminer depuis le dernier passage sur l'accueil (US-5.2).
        bindExercisesDone();
        bindSteps();
    }

    /**
     * Pas du jour (US-5.1). Sans permission, la tuile invite à l'accorder d'un toucher ; sans
     * Play services compatible, elle le dit plutôt que d'afficher un zéro trompeur.
     */
    private void bindSteps() {
        binding.stepsTile.getRoot().setOnClickListener(null);
        binding.stepsTile.getRoot().setClickable(false);
        // Bloc masqué (US-5.4) : inutile de lire les pas, ni d'inviter à la permission.
        if (!GameSettings.isEnabled(GameSettings.Feature.HOME_STEPS)) return;
        stepCounter.readToday(new StepCounter.Callback() {
            @Override
            public void onSteps(int steps) {
                if (isFinishing()) return;
                binding.stepsTile.statValue.setText(
                        String.format(Locale.FRANCE, "%,d", steps));
                binding.stepsTile.statLabel.setText(R.string.home_steps_today);
            }

            @Override
            public void onUnavailable(@NonNull StepCounter.Availability reason) {
                if (isFinishing()) return;
                binding.stepsTile.statValue.setText(R.string.home_steps_none);
                if (reason == StepCounter.Availability.PERMISSION_MISSING) {
                    binding.stepsTile.statLabel.setText(R.string.home_steps_permission);
                    binding.stepsTile.getRoot().setOnClickListener(view ->
                            requestActivityRecognition.launch(
                                    Manifest.permission.ACTIVITY_RECOGNITION));
                } else {
                    binding.stepsTile.statLabel.setText(R.string.home_steps_unsupported);
                }
            }
        });
    }

    /** Exercices réalisés au total et sur les sept derniers jours (US-5.2). */
    private void bindExercisesDone() {
        ActivityCounter.Counts counts =
                ActivityCounter.count(SessionLog.readAll(), System.currentTimeMillis());
        binding.exercisesDoneTile.statValue.setText(String.valueOf(counts.getTotal()));
        binding.exercisesDoneTile.statLabel.setText(getResources().getQuantityString(
                R.plurals.home_exercises_done, counts.getTotal(), counts.getLastSevenDays()));
    }

    /**
     * Applique les interrupteurs de gamification à l'accueil (US-4.1).
     *
     * <p>Un élément désactivé disparaît entièrement : la carte « Exercice du jour » et les
     * statistiques ne doivent laisser ni emplacement vide ni onglet mort, sinon la condition
     * de test n'est pas réellement comparable.</p>
     */
    private void applyGameSettings() {
        boolean statistics = GameSettings.isEnabled(GameSettings.Feature.STATISTICS);
        binding.openStatisticsButton.setVisibility(statistics ? View.VISIBLE : View.GONE);
        Navigation.applyGameSettings(binding.bottomNav);

        binding.dailyExerciseCard.favoriteButton.setVisibility(
                GameSettings.isEnabled(GameSettings.Feature.FAVORITES) ? View.VISIBLE : View.GONE);

        // Blocs de l'accueil (US-5.4) : même règle, un bloc masqué ne laisse pas de trou.
        boolean steps = GameSettings.isEnabled(GameSettings.Feature.HOME_STEPS);
        boolean exercisesDone = GameSettings.isEnabled(GameSettings.Feature.HOME_EXERCISES_DONE);
        binding.stepsTile.getRoot().setVisibility(steps ? View.VISIBLE : View.GONE);
        binding.exercisesDoneTile.getRoot().setVisibility(exercisesDone ? View.VISIBLE : View.GONE);
        binding.homeActivitySpacer.setVisibility(steps && exercisesDone ? View.VISIBLE : View.GONE);
        binding.homeActivityRow.setVisibility(steps || exercisesDone ? View.VISIBLE : View.GONE);
        binding.openCatalogButton.setVisibility(
                GameSettings.isEnabled(GameSettings.Feature.HOME_CATALOG) ? View.VISIBLE : View.GONE);
    }

    /**
     * Relit l'exercice du jour dans le catalogue et rafraîchit la carte.
     *
     * <p>Appelé aussi depuis {@code onResume} : l'état favori, la réalisation du jour et le
     * catalogue lui-même ont pu changer sur un autre écran.</p>
     */
    private void bindExerciseOfTheDay() {
        // Élément de jeu désactivé : ni carte, ni message « catalogue vide ».
        if (!GameSettings.isEnabled(GameSettings.Feature.DAILY_EXERCISE)) {
            exerciseOfTheDay = null;
            binding.dailyExerciseCard.getRoot().setVisibility(View.GONE);
            binding.emptyCatalogMessage.setVisibility(View.GONE);
            return;
        }

        exerciseOfTheDay = dailyExerciseProvider.ofTheDay();

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

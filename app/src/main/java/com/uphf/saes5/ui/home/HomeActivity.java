package com.uphf.saes5.ui.home;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.uphf.saes5.R;
import com.uphf.saes5.data.activity.StepCounter;
import com.uphf.saes5.data.home.DailyExerciseProvider;
import com.uphf.saes5.data.home.DailyExerciseTracker;
import com.uphf.saes5.data.home.FavoritesRepository;
import com.uphf.saes5.data.session.SessionLog;
import com.uphf.saes5.databinding.ActivityMainBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.statistics.ActivityCounter;
import com.uphf.saes5.ui.catalog.ExerciseCatalogActivity;
import com.uphf.saes5.ui.common.ExerciseFormatter;
import com.uphf.saes5.ui.common.Navigation;
import com.uphf.saes5.ui.common.TabSwipe;
import com.uphf.saes5.ui.consent.ConsentActivity;
import com.uphf.saes5.ui.exercise.ExerciseActivity;
import com.uphf.saes5.ui.settings.SettingsActivity;
import com.uphf.saes5.ui.statistics.StatisticsActivity;

import java.util.Locale;

/**
 * Accueil. Il porte la carte « Exercice du jour » (US-4.2) ainsi que les raccourcis vers les
 * statistiques et le catalogue, ainsi que le nombre de pas du jour (US-5.1) et le nombre
 * d'exercices réalisés (US-5.2).
 */
public class HomeActivity extends AppCompatActivity {

    private static final String STATE_DETAILS_EXPANDED = "daily_details_expanded";

    private ActivityMainBinding binding;
    private StepCounter stepCounter;
    /** Demandée seulement au toucher de la tuile des pas, jamais d'office au lancement. */
    private final ActivityResultLauncher<String> requestActivityRecognition =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> bindSteps());
    private FavoritesRepository favoritesRepository;
    private DailyExerciseTracker dailyExerciseTracker;
    private BottomNavigationView bottomNav;
    private DailyExerciseProvider dailyExerciseProvider;

    @Nullable
    private Exercise exerciseOfTheDay;

    /** Carte « Exercice du jour » repliée par défaut : seul l'en-tête reste visible. */
    private boolean detailsExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // US-9.1 : rien ne démarre, caméra comprise, avant l'accord éclairé du participant.
        if (AppContainer.from(getApplicationContext()).consent.isRequired()) {
            startActivity(new Intent(this, ConsentActivity.class));
            finish();
            return;
        }
        detailsExpanded = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_DETAILS_EXPANDED, false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Pas de mode bord à bord ici : l'accueil était le seul écran à en faire usage, et la
        // marge système appliquée à sa racine remontait la barre d'onglets de la hauteur de la
        // barre de gestes, laissant une bande noire sous elle. Les quatre autres onglets
        // laissent le système réserver ses barres ; l'accueil fait désormais pareil.

        binding.openStatisticsButton.setOnClickListener(view ->
                Navigation.openTab(this, StatisticsActivity.class));
        binding.openCatalogButton.setOnClickListener(view ->
                Navigation.openTab(this, ExerciseCatalogActivity.class));
        binding.openSettingsButton.setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class)));

        bottomNav = findViewById(R.id.bottom_nav);
        Navigation.setupTabs(this, bottomNav, R.id.nav_home);

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
        if (!AppContainer.from(getApplicationContext()).gameSettings.isEnabled(GameFeature.HOME_STEPS)) return;
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
        boolean statistics = AppContainer.from(getApplicationContext()).gameSettings.isEnabled(GameFeature.STATISTICS);
        binding.openStatisticsButton.setVisibility(statistics ? View.VISIBLE : View.GONE);
        Navigation.applyGameSettings(bottomNav);

        binding.dailyExerciseCard.favoriteButton.setVisibility(
                AppContainer.from(getApplicationContext()).gameSettings.isEnabled(GameFeature.FAVORITES) ? View.VISIBLE : View.GONE);

        // Blocs de l'accueil (US-5.4) : même règle, un bloc masqué ne laisse pas de trou.
        boolean steps = AppContainer.from(getApplicationContext()).gameSettings.isEnabled(GameFeature.HOME_STEPS);
        boolean exercisesDone = AppContainer.from(getApplicationContext()).gameSettings.isEnabled(GameFeature.HOME_EXERCISES_DONE);
        binding.stepsTile.getRoot().setVisibility(steps ? View.VISIBLE : View.GONE);
        binding.exercisesDoneTile.getRoot().setVisibility(exercisesDone ? View.VISIBLE : View.GONE);
        binding.homeActivitySpacer.setVisibility(steps && exercisesDone ? View.VISIBLE : View.GONE);
        binding.homeActivityRow.setVisibility(steps || exercisesDone ? View.VISIBLE : View.GONE);
        binding.openCatalogButton.setVisibility(
                AppContainer.from(getApplicationContext()).gameSettings.isEnabled(GameFeature.HOME_CATALOG) ? View.VISIBLE : View.GONE);
    }

    /**
     * Relit l'exercice du jour dans le catalogue et rafraîchit la carte.
     *
     * <p>Appelé aussi depuis {@code onResume} : l'état favori, la réalisation du jour et le
     * catalogue lui-même ont pu changer sur un autre écran.</p>
     */
    private void bindExerciseOfTheDay() {
        // Élément de jeu désactivé : ni carte, ni message « catalogue vide ».
        if (!AppContainer.from(getApplicationContext()).gameSettings.isEnabled(GameFeature.DAILY_EXERCISE)) {
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Balayer vers la gauche ou la droite change d'onglet (voir TabSwipe).
        return TabSwipe.dispatch(this, event) || super.dispatchTouchEvent(event);
    }
}

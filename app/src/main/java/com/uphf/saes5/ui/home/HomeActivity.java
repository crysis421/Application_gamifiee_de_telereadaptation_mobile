package com.uphf.saes5.ui.home;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.uphf.saes5.R;
import com.uphf.saes5.databinding.ActivityMainBinding;
import com.uphf.saes5.databinding.ViewDailyExerciseCardBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.ui.catalog.ExerciseCatalogActivity;
import com.uphf.saes5.ui.common.ExerciseFormatter;
import com.uphf.saes5.ui.common.Navigation;
import com.uphf.saes5.ui.common.TabActivity;
import com.uphf.saes5.ui.consent.ConsentActivity;
import com.uphf.saes5.ui.exercise.ExerciseActivity;
import com.uphf.saes5.ui.settings.SettingsActivity;
import com.uphf.saes5.ui.statistics.StatisticsActivity;

import java.util.Locale;

/**
 * Accueil : carte « Exercice du jour » (US-4.2), pas du jour (US-5.1), exercices réalisés
 * (US-5.2), et raccourcis vers les statistiques et le catalogue.
 *
 * <p>Vue du MVVM : elle affiche l'état de {@link HomeViewModel} et lui transmet les actions.
 * Elle ne garde que ce qui demande Android : la demande de permission et la navigation.</p>
 */
public class HomeActivity extends TabActivity {

    private ActivityMainBinding binding;
    private HomeViewModel viewModel;

    /** Demandée seulement au toucher de la tuile des pas, jamais d'office au lancement. */
    private final ActivityResultLauncher<String> requestActivityRecognition =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> viewModel.refreshSteps());

    @Override
    protected int tabId() {
        return R.id.nav_home;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer container = AppContainer.from(this);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(HomeViewModel.class,
                () -> new HomeViewModel(container.gameSettings, container.consent,
                        container.dailyExerciseRepository, container.favoritesRepository,
                        container.sessionRepository, container.stepSource, container.clock)))
                .get(HomeViewModel.class);

        if (viewModel.isConsentRequired()) {
            startActivity(new Intent(this, ConsentActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Pas de mode bord à bord ici : la marge système appliquée à la racine remontait la
        // barre d'onglets de la hauteur de la barre de gestes, laissant une bande noire.

        binding.openStatisticsButton.setOnClickListener(view ->
                Navigation.openTab(this, StatisticsActivity.class));
        binding.openCatalogButton.setOnClickListener(view ->
                Navigation.openTab(this, ExerciseCatalogActivity.class));
        binding.openSettingsButton.setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class)));
        binding.stepsTile.getRoot().setOnClickListener(view ->
                requestActivityRecognition.launch(Manifest.permission.ACTIVITY_RECOGNITION));

        ViewDailyExerciseCardBinding card = binding.dailyExerciseCard;
        card.favoriteButton.setOnClickListener(view -> viewModel.toggleFavorite());
        card.startButton.setOnClickListener(view -> startDailyExercise());
        // Toute la carte plie / déplie, le lien n'est que le repère visuel de l'action.
        card.getRoot().setOnClickListener(view -> viewModel.toggleDetails());
        card.toggleDetailsButton.setOnClickListener(view -> viewModel.toggleDetails());

        setupTabs(findViewById(R.id.bottom_nav));
        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Réglages, catalogue et séances ont pu changer sur un autre écran.
        viewModel.refresh();
    }

    private void startDailyExercise() {
        HomeUiState state = viewModel.getState().getValue();
        if (state != null && state.getDailyExercise() != null) {
            startActivity(ExerciseActivity.newIntent(this, state.getDailyExercise().getId()));
        }
    }

    private void render(HomeUiState state) {
        showStatisticsTab(state.isStatisticsVisible());
        binding.openStatisticsButton.setVisibility(visibleIf(state.isStatisticsVisible()));
        binding.openCatalogButton.setVisibility(visibleIf(state.isCatalogButtonVisible()));
        binding.emptyCatalogMessage.setVisibility(visibleIf(state.isEmptyCatalogVisible()));
        renderDailyCard(state);
        renderActivityTiles(state);
    }

    private void renderDailyCard(HomeUiState state) {
        ViewDailyExerciseCardBinding card = binding.dailyExerciseCard;
        card.getRoot().setVisibility(visibleIf(state.isDailyCardVisible()));
        Exercise exercise = state.getDailyExercise();
        if (exercise == null) return;

        card.exerciseName.setText(exercise.getName());
        card.exerciseMeta.setText(ExerciseFormatter.metaLine(this, exercise));
        card.exerciseGoal.setText(getString(R.string.daily_exercise_goal, exercise.getTargetReps()));
        // Les exercices créés par l'utilisateur n'ont pas de consigne : on n'affiche pas un vide.
        card.exerciseDescription.setText(exercise.getDescription());
        card.exerciseDescription.setVisibility(visibleIf(exercise.hasDescription()));

        // Pas de chevron : c'est le libellé du lien qui dit l'action (« Voir le détail » /
        // « Réduire »). Le pliage est animé par animateLayoutChanges côté layout.
        card.dailyDetails.setVisibility(visibleIf(state.isDetailsExpanded()));
        card.toggleDetailsButton.setText(state.isDetailsExpanded()
                ? R.string.daily_exercise_collapse
                : R.string.daily_exercise_expand);

        card.favoriteButton.setVisibility(visibleIf(state.isFavoriteVisible()));
        card.favoriteButton.setIconResource(
                state.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        card.favoriteButton.setContentDescription(getString(state.isFavorite()
                ? R.string.summary_remove_favorite
                : R.string.summary_add_favorite));

        card.doneBadge.setVisibility(visibleIf(state.isDoneToday()));
        card.startButton.setText(state.isDoneToday()
                ? R.string.daily_exercise_restart
                : R.string.daily_exercise_start);
    }

    /** Tuiles des pas (US-5.1) et des exercices réalisés (US-5.2) ; masquées sans laisser de trou. */
    private void renderActivityTiles(HomeUiState state) {
        boolean steps = state.isStepsTileVisible();
        boolean done = state.isExercisesDoneTileVisible();
        binding.stepsTile.getRoot().setVisibility(visibleIf(steps));
        binding.exercisesDoneTile.getRoot().setVisibility(visibleIf(done));
        binding.homeActivitySpacer.setVisibility(visibleIf(steps && done));
        binding.homeActivityRow.setVisibility(visibleIf(steps || done));

        binding.exercisesDoneTile.statValue.setText(String.valueOf(state.getExercisesDoneTotal()));
        binding.exercisesDoneTile.statLabel.setText(getResources().getQuantityString(
                R.plurals.home_exercises_done, state.getExercisesDoneTotal(),
                state.getExercisesDoneLastSevenDays()));

        // Seule l'absence de permission rend la tuile cliquable : un toucher la demande.
        binding.stepsTile.getRoot().setClickable(
                state.getStepsStatus() == HomeUiState.StepsStatus.PERMISSION_MISSING);
        switch (state.getStepsStatus()) {
            case VALUE:
                binding.stepsTile.statValue.setText(
                        String.format(Locale.FRANCE, "%,d", state.getSteps()));
                binding.stepsTile.statLabel.setText(R.string.home_steps_today);
                break;
            case PERMISSION_MISSING:
                binding.stepsTile.statValue.setText(R.string.home_steps_none);
                binding.stepsTile.statLabel.setText(R.string.home_steps_permission);
                break;
            case UNSUPPORTED:
                binding.stepsTile.statValue.setText(R.string.home_steps_none);
                binding.stepsTile.statLabel.setText(R.string.home_steps_unsupported);
                break;
            default:
                break;
        }
    }

    private static int visibleIf(boolean visible) {
        return visible ? View.VISIBLE : View.GONE;
    }
}

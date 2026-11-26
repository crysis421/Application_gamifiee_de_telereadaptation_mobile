package com.uphf.saes5.ui.catalog;

import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.uphf.saes5.R;
import com.uphf.saes5.databinding.ActivityExerciseCatalogBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.ui.common.TabActivity;

/**
 * Catalogue d'exercices : liste filtrable, création, modification et suppression.
 *
 * <p>Vue du MVVM : les filtres saisis sont transmis à {@link CatalogViewModel}, qui renvoie
 * la liste à afficher.</p>
 */
public class ExerciseCatalogActivity extends TabActivity {

    private ActivityExerciseCatalogBinding binding;
    private CatalogViewModel viewModel;

    @Override
    protected int tabId() {
        return R.id.nav_catalog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExerciseCatalogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppContainer container = AppContainer.from(this);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(CatalogViewModel.class,
                () -> new CatalogViewModel(container.exerciseRepository, container.gameSettings)))
                .get(CatalogViewModel.class);

        ExerciseAdapter adapter = new ExerciseAdapter(
                exercise -> startActivity(ExerciseDetailActivity.newIntent(this, exercise.getId())),
                this::showExerciseMenu);
        binding.exercisesRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.exercisesRecycler.setAdapter(adapter);

        binding.filterMenuButton.setOnClickListener(v ->
                showFilters(binding.filtersContainer.getVisibility() != View.VISIBLE));
        binding.applyButton.setOnClickListener(v -> {
            viewModel.filter(
                    selectedFilter(binding.muscleChipGroup, R.id.chip_muscle_all),
                    selectedFilter(binding.difficultyChipGroup, R.id.chip_difficulty_all),
                    maxDuration(),
                    binding.equipmentInput.getText().toString().trim());
            showFilters(false);
        });
        binding.resetButton.setOnClickListener(v -> {
            binding.muscleChipGroup.check(R.id.chip_muscle_all);
            binding.difficultyChipGroup.check(R.id.chip_difficulty_all);
            binding.durationInput.setText("");
            binding.equipmentInput.setText("");
            viewModel.reset();
            showFilters(false);
        });
        binding.createExerciseButton.setOnClickListener(v ->
                startActivity(ExerciseEditorActivity.newIntent(this)));

        setupTabs(findViewById(R.id.bottom_nav));
        viewModel.getState().observe(this, state -> {
            showStatisticsTab(state.isStatisticsTabVisible());
            adapter.submitList(state.getExercises(), state.isStarsVisible());
        });
        viewModel.getEvents().observe(this, event -> {
            if (event.getContentIfNotHandled() == CatalogViewModel.CatalogEvent.DELETED) {
                Toast.makeText(this, R.string.exercise_deleted, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh(); // reflète les créations, modifications et suppressions
    }

    private void showFilters(boolean open) {
        binding.filtersContainer.setVisibility(open ? View.VISIBLE : View.GONE);
        binding.filterMenuButton.setContentDescription(
                getString(open ? R.string.close_filters : R.string.open_filters));
    }

    /** Durée maximale saisie, ou {@code null} : champ vide ou illisible (limité à 4 chiffres). */
    private Integer maxDuration() {
        String text = binding.durationInput.getText().toString().trim();
        try {
            return text.isEmpty() ? null : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Menu « modifier / supprimer » d'un exercice, ancré sur son bouton. */
    private void showExerciseMenu(Exercise exercise, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.inflate(R.menu.exercise_item_menu);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_exercise) {
                startActivity(ExerciseEditorActivity.editIntent(this, exercise.getId()));
                return true;
            }
            if (item.getItemId() == R.id.action_delete_exercise) {
                confirmDelete(exercise);
                return true;
            }
            return false;
        });
        menu.show();
    }

    /**
     * Demande confirmation avant de retirer un exercice.
     *
     * <p>La suppression est irréversible — le catalogue n'a pas d'historique — d'où la
     * confirmation explicite, avec le nom de l'exercice concerné.</p>
     */
    private void confirmDelete(Exercise exercise) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_exercise_title)
                .setMessage(getString(R.string.delete_exercise_message, exercise.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_exercise,
                        (dialog, which) -> viewModel.delete(exercise.getId()))
                .show();
    }

    /** Texte du chip sélectionné, ou "" si c'est le chip « Tous / Toutes » (= pas de filtre). */
    private String selectedFilter(ChipGroup group, int allChipId) {
        int checkedId = group.getCheckedChipId();
        if (checkedId == View.NO_ID || checkedId == allChipId) return "";
        Chip chip = group.findViewById(checkedId);
        return chip.getText().toString();
    }
}

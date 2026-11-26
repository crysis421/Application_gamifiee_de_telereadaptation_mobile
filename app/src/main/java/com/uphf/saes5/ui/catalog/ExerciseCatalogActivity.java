package com.uphf.saes5.ui.catalog;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.uphf.saes5.R;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.ui.common.Navigation;
import com.uphf.saes5.ui.common.TabSwipe;

public class ExerciseCatalogActivity extends AppCompatActivity {
    private CatalogViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_catalog);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(CatalogViewModel.class,
                () -> new CatalogViewModel(AppContainer.from(this).exerciseRepository)))
                .get(CatalogViewModel.class);

        RecyclerView recycler = findViewById(R.id.exercises_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        ExerciseAdapter adapter = new ExerciseAdapter(
                exercise -> {
                    Intent intent = new Intent(this, ExerciseDetailActivity.class);
                    intent.putExtra("exercise_id", exercise.getId());
                    startActivity(intent);
                },
                this::showExerciseMenu);
        recycler.setAdapter(adapter);
        viewModel.getExercises().observe(this, adapter::submitList);

        ChipGroup muscleGroup = findViewById(R.id.muscle_chip_group);
        ChipGroup difficultyGroup = findViewById(R.id.difficulty_chip_group);
        EditText duration = findViewById(R.id.duration_input);
        EditText equipment = findViewById(R.id.equipment_input);

        View filtersContainer = findViewById(R.id.filters_container);
        View filterMenuButton = findViewById(R.id.filter_menu_button);
        filterMenuButton.setOnClickListener(v -> {
            boolean willOpen = filtersContainer.getVisibility() != View.VISIBLE;
            filtersContainer.setVisibility(willOpen ? View.VISIBLE : View.GONE);
            filterMenuButton.setContentDescription(getString(willOpen
                    ? R.string.close_filters
                    : R.string.open_filters));
        });

        findViewById(R.id.apply_button).setOnClickListener(v -> {
            Integer maxDuration = null;
            String durationText = duration.getText().toString().trim();
            if (!durationText.isEmpty()) {
                try {
                    maxDuration = Integer.parseInt(durationText);
                } catch (NumberFormatException ignored) {
                    // champ limité à 4 chiffres : on ignore une valeur invalide
                }
            }
            viewModel.filter(
                    selectedFilter(muscleGroup, R.id.chip_muscle_all),
                    selectedFilter(difficultyGroup, R.id.chip_difficulty_all),
                    maxDuration,
                    equipment.getText().toString().trim());
            filtersContainer.setVisibility(View.GONE);
            filterMenuButton.setContentDescription(getString(R.string.open_filters));
        });
        findViewById(R.id.reset_button).setOnClickListener(v -> {
            muscleGroup.check(R.id.chip_muscle_all);
            difficultyGroup.check(R.id.chip_difficulty_all);
            duration.setText("");
            equipment.setText("");
            viewModel.reset();
            filtersContainer.setVisibility(View.GONE);
            filterMenuButton.setContentDescription(getString(R.string.open_filters));
        });

        findViewById(R.id.create_exercise_button).setOnClickListener(v ->
                startActivity(ExerciseEditorActivity.newIntent(this)));

        Navigation.setupTabs(this, findViewById(R.id.bottom_nav), R.id.nav_catalog);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh(); // reflète les créations, modifications et suppressions
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
                .setPositiveButton(R.string.delete_exercise, (dialog, which) -> {
                    AppContainer.from(getApplicationContext()).exerciseRepository.remove(exercise.getId());
                    viewModel.refresh();
                    Toast.makeText(this, R.string.exercise_deleted, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /** Texte du chip sélectionné, ou "" si c'est le chip « Tous / Toutes » (= pas de filtre). */
    private String selectedFilter(ChipGroup group, int allChipId) {
        int checkedId = group.getCheckedChipId();
        if (checkedId == View.NO_ID || checkedId == allChipId) return "";
        Chip chip = group.findViewById(checkedId);
        return chip.getText().toString();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Balayer vers la gauche ou la droite change d'onglet (voir TabSwipe).
        return TabSwipe.dispatch(this, event) || super.dispatchTouchEvent(event);
    }
}

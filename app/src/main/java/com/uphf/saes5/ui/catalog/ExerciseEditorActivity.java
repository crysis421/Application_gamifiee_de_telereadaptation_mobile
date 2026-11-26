package com.uphf.saes5.ui.catalog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.uphf.saes5.R;
import com.uphf.saes5.databinding.ActivityCreateExerciseBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.ui.recording.MovementRecordingActivity;

/**
 * Création d'un exercice, et modification d'un exercice existant.
 *
 * <p>Le même écran sert aux deux cas : avec {@link #EXTRA_EXERCISE_ID}, le formulaire est
 * prérempli et l'enregistrement remplace l'exercice au lieu d'en ajouter un. La validation
 * et l'enregistrement sont faits par {@link ExerciseEditorViewModel}.</p>
 */
public class ExerciseEditorActivity extends AppCompatActivity {

    private static final String EXTRA_EXERCISE_ID = "exercise_id";
    private static final String STATE_DRAFT_ID = "draft_exercise_id";

    private ActivityCreateExerciseBinding binding;
    private ExerciseEditorViewModel viewModel;

    /** Ouvre le formulaire vide, pour créer un exercice. */
    public static Intent newIntent(Context context) {
        return new Intent(context, ExerciseEditorActivity.class);
    }

    /** Ouvre le formulaire prérempli, pour modifier un exercice existant. */
    public static Intent editIntent(Context context, String exerciseId) {
        return new Intent(context, ExerciseEditorActivity.class)
                .putExtra(EXTRA_EXERCISE_ID, exerciseId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateExerciseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String editedId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
        String restoredDraftId = savedInstanceState == null
                ? null
                : savedInstanceState.getString(STATE_DRAFT_ID);
        AppContainer container = AppContainer.from(this);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(ExerciseEditorViewModel.class,
                () -> new ExerciseEditorViewModel(editedId, restoredDraftId,
                        container.exerciseRepository, container.movementRepository)))
                .get(ExerciseEditorViewModel.class);

        ExerciseEditorUiState initialState = viewModel.getState().getValue();
        // Les champs déjà saisis sont restaurés par Android après une recréation : on ne
        // préremplit qu'à la première ouverture.
        if (savedInstanceState == null && initialState != null) {
            prefill(initialState);
        }
        updateThresholdLabel(Math.round(binding.createSimilarityThreshold.getValue()));
        binding.createSimilarityThreshold.addOnChangeListener(
                (slider, value, fromUser) -> updateThresholdLabel(Math.round(value)));

        binding.btnRecordMovement.setOnClickListener(v -> startActivity(
                MovementRecordingActivity.recordIntent(this, referenceMovementId())));
        binding.btnCompareMovement.setOnClickListener(v -> startActivity(
                MovementRecordingActivity.compareIntent(this, referenceMovementId())));
        binding.createSaveButton.setOnClickListener(v -> save());
        binding.createCancelButton.setOnClickListener(v -> finish());

        viewModel.getState().observe(this, this::render);
        viewModel.getEvents().observe(this, event -> {
            ExerciseEditorViewModel.EditorEvent content = event.getContentIfNotHandled();
            if (content != null) onEvent(content);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Le modèle a pu être enregistré sur l'écran d'enregistrement.
        viewModel.refreshReference();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_DRAFT_ID, viewModel.getDraftId());
        super.onSaveInstanceState(outState);
    }

    private void prefill(ExerciseEditorUiState state) {
        binding.createSimilarityThreshold.setValue(state.getThreshold());
        Exercise edited = state.getInitial();
        if (edited == null) {
            binding.createTargetRepsInput.setText(String.valueOf(Exercise.DEFAULT_TARGET_REPS));
            return;
        }
        binding.createNameInput.setText(edited.getName());
        binding.createDurationInput.setText(String.valueOf(edited.getDurationMinutes()));
        binding.createEquipmentInput.setText(edited.getEquipment());
        binding.createDescriptionInput.setText(edited.getDescription());
        binding.createTargetRepsInput.setText(String.valueOf(edited.getTargetReps()));
        binding.createStarsBar.setRating(edited.getStars());
        checkChipWithText(binding.createMuscleGroup, edited.getMuscleGroup());
        checkChipWithText(binding.createDifficultyGroup, edited.getDifficulty());
    }

    private void render(ExerciseEditorUiState state) {
        if (state.isEditing()) {
            binding.createTitle.setText(R.string.edit_title);
            binding.createSaveButton.setText(R.string.save_changes);
        }
        binding.createReferenceStatus.setText(state.isReferenceAvailable()
                ? R.string.reference_model_available
                : R.string.reference_model_missing);
        binding.btnCompareMovement.setEnabled(state.isReferenceAvailable());
    }

    private String referenceMovementId() {
        ExerciseEditorUiState state = viewModel.getState().getValue();
        return state == null ? viewModel.getDraftId() : state.getReferenceMovementId();
    }

    private void updateThresholdLabel(int threshold) {
        binding.createSimilarityThresholdValue.setText(
                getString(R.string.similarity_threshold_value, threshold));
    }

    private void save() {
        binding.createNameLayout.setError(null);
        binding.createDurationLayout.setError(null);
        binding.createTargetRepsLayout.setError(null);
        viewModel.save(new ExerciseForm(
                text(binding.createNameInput.getText()),
                text(binding.createDurationInput.getText()),
                text(binding.createTargetRepsInput.getText()),
                text(binding.createEquipmentInput.getText()),
                text(binding.createDescriptionInput.getText()),
                selectedText(binding.createMuscleGroup),
                selectedText(binding.createDifficultyGroup),
                binding.createStarsBar.getRating(),
                Math.round(binding.createSimilarityThreshold.getValue())));
    }

    private void onEvent(ExerciseEditorViewModel.EditorEvent event) {
        switch (event) {
            case NAME_REQUIRED:
                binding.createNameLayout.setError(getString(R.string.error_name_required));
                break;
            case DURATION_INVALID:
                binding.createDurationLayout.setError(getString(R.string.error_duration_invalid));
                break;
            case TARGET_REPS_INVALID:
                binding.createTargetRepsLayout.setError(
                        getString(R.string.error_target_reps_invalid));
                break;
            case MODEL_MISSING:
                Toast.makeText(this, R.string.reference_model_missing_toast, Toast.LENGTH_LONG).show();
                break;
            case CREATED:
                Toast.makeText(this, R.string.exercise_created, Toast.LENGTH_SHORT).show();
                finish(); // retour au catalogue, qui se rafraîchit dans onResume()
                break;
            case UPDATED:
                Toast.makeText(this, R.string.exercise_updated, Toast.LENGTH_SHORT).show();
                finish();
                break;
            case NO_LONGER_AVAILABLE:
                Toast.makeText(this, R.string.exercise_no_longer_available, Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
    }

    private static String text(@Nullable CharSequence value) {
        return value == null ? "" : value.toString();
    }

    private static String selectedText(ChipGroup group) {
        int checkedId = group.getCheckedChipId();
        if (checkedId == View.NO_ID) return "";
        Chip chip = group.findViewById(checkedId);
        return chip.getText().toString();
    }

    /** Coche le chip portant ce libellé. Sans effet si aucun ne correspond. */
    private static void checkChipWithText(ChipGroup group, @Nullable String label) {
        if (label == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip && label.equalsIgnoreCase(((Chip) child).getText().toString())) {
                group.check(child.getId());
                return;
            }
        }
    }
}

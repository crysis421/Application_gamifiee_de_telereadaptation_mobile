package com.uphf.saes5.ui.catalog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;

import com.uphf.saes5.R;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.ui.recording.HolisticLauncher;

/**
 * Création d'un exercice, et modification d'un exercice existant.
 *
 * <p>Le même écran sert aux deux cas : avec {@link #EXTRA_EXERCISE_ID}, le formulaire est
 * prérempli et l'enregistrement remplace l'exercice au lieu d'en ajouter un.</p>
 */
public class ExerciseEditorActivity extends AppCompatActivity {

    private static final String EXTRA_EXERCISE_ID = "exercise_id";
    private static final String STATE_DRAFT_ID = "draft_exercise_id";

    private String draftExerciseId;
    private String referenceMovementId;
    private int similarityThreshold = Exercise.DEFAULT_SIMILARITY_THRESHOLD;
    private TextView referenceStatus;
    private MaterialButton compareMovementButton;

    /** Ouvre le formulaire vide, pour créer un exercice. */
    public static Intent newIntent(Context context) {
        return new Intent(context, ExerciseEditorActivity.class);
    }

    /** Ouvre le formulaire prérempli, pour modifier un exercice existant. */
    public static Intent editIntent(Context context, String exerciseId) {
        Intent intent = new Intent(context, ExerciseEditorActivity.class);
        intent.putExtra(EXTRA_EXERCISE_ID, exerciseId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_exercise);

        TextView screenTitle = findViewById(R.id.create_title);
        TextInputLayout nameLayout = findViewById(R.id.create_name_layout);
        TextInputLayout durationLayout = findViewById(R.id.create_duration_layout);
        TextInputLayout targetRepsLayout = findViewById(R.id.create_target_reps_layout);
        EditText name = findViewById(R.id.create_name_input);
        EditText duration = findViewById(R.id.create_duration_input);
        EditText equipment = findViewById(R.id.create_equipment_input);
        EditText description = findViewById(R.id.create_description_input);
        EditText targetReps = findViewById(R.id.create_target_reps_input);
        ChipGroup muscleGroup = findViewById(R.id.create_muscle_group);
        ChipGroup difficultyGroup = findViewById(R.id.create_difficulty_group);
        RatingBar stars = findViewById(R.id.create_stars_bar);
        Slider thresholdSlider = findViewById(R.id.create_similarity_threshold);
        TextView thresholdValue = findViewById(R.id.create_similarity_threshold_value);
        referenceStatus = findViewById(R.id.create_reference_status);
        MaterialButton recordMovementButton = findViewById(R.id.btn_record_movement);
        compareMovementButton = findViewById(R.id.btn_compare_movement);
        TextView saveButton = findViewById(R.id.create_save_button);

        // En modification, l'exercice d'origine ; null en création.
        String editedId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
        final Exercise edited = AppContainer.from(getApplicationContext()).exerciseRepository.findById(editedId);

        // Modification demandée sur un exercice qui n'est plus au catalogue : sans ce garde-fou
        // le formulaire basculerait silencieusement en création et enregistrerait un doublon.
        if (editedId != null && edited == null) {
            Toast.makeText(this, R.string.exercise_no_longer_available, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        draftExerciseId = savedInstanceState == null
                ? (edited != null ? edited.getId() : AppContainer.from(getApplicationContext()).exerciseRepository.newId())
                : savedInstanceState.getString(STATE_DRAFT_ID,
                        edited != null ? edited.getId() : AppContainer.from(getApplicationContext()).exerciseRepository.newId());
        referenceMovementId = edited != null
                ? edited.getReferenceMovementId()
                : draftExerciseId;
        similarityThreshold = edited != null
                ? edited.getSimilarityThreshold()
                : Exercise.DEFAULT_SIMILARITY_THRESHOLD;

        thresholdSlider.setValue(similarityThreshold);
        thresholdValue.setText(getString(R.string.similarity_threshold_value,
                similarityThreshold));
        thresholdSlider.addOnChangeListener((slider, value, fromUser) -> {
            similarityThreshold = Math.round(value);
            thresholdValue.setText(getString(R.string.similarity_threshold_value,
                    similarityThreshold));
        });

        recordMovementButton.setOnClickListener(v ->
                HolisticLauncher.startRecording(this, referenceMovementId));
        compareMovementButton.setOnClickListener(v ->
                HolisticLauncher.startComparison(this, referenceMovementId));

        if (edited != null) {
            screenTitle.setText(R.string.edit_title);
            saveButton.setText(R.string.save_changes);

            name.setText(edited.getName());
            duration.setText(String.valueOf(edited.getDurationMinutes()));
            equipment.setText(edited.getEquipment());
            description.setText(edited.getDescription());
            targetReps.setText(String.valueOf(edited.getTargetReps()));
            stars.setRating(edited.getStars());
            checkChipWithText(muscleGroup, edited.getMuscleGroup());
            checkChipWithText(difficultyGroup, edited.getDifficulty());
        } else {
            targetReps.setText(String.valueOf(Exercise.DEFAULT_TARGET_REPS));
        }

        saveButton.setOnClickListener(v -> {
            nameLayout.setError(null);
            durationLayout.setError(null);
            targetRepsLayout.setError(null);

            String nameValue = name.getText().toString().trim();
            if (nameValue.isEmpty()) {
                nameLayout.setError(getString(R.string.error_name_required));
                return;
            }

            int durationValue = parsePositive(duration);
            if (durationValue <= 0) {
                durationLayout.setError(getString(R.string.error_duration_invalid));
                return;
            }

            int targetRepsValue = parsePositive(targetReps);
            if (targetRepsValue <= 0) {
                targetRepsLayout.setError(getString(R.string.error_target_reps_invalid));
                return;
            }

            String equipmentValue = equipment.getText().toString().trim();
            if (equipmentValue.isEmpty()) equipmentValue = "Aucun";

            int starsValue = Math.max(1, Math.round(stars.getRating()));

            // Un exercice n'existe qu'avec son modèle : sans lui, rien à quoi comparer le
            // mouvement. La création attend donc qu'il ait été enregistré.
            if (edited == null && !AppContainer.from(getApplicationContext()).movementRepository.exists(referenceMovementId)) {
                Toast.makeText(this, R.string.reference_model_missing_toast,
                        Toast.LENGTH_LONG).show();
                return;
            }

            // En modification on garde l'identifiant : les favoris et l'exercice du jour
            // qui le référencent continuent de pointer sur le bon exercice.
            Exercise result = new Exercise(
                    draftExerciseId,
                    nameValue,
                    selectedText(muscleGroup),
                    selectedText(difficultyGroup),
                    durationValue,
                    equipmentValue,
                    starsValue,
                    description.getText().toString().trim(),
                    targetRepsValue,
                    referenceMovementId,
                    similarityThreshold);

            if (edited != null) {
                AppContainer.from(getApplicationContext()).exerciseRepository.update(result);
                Toast.makeText(this, R.string.exercise_updated, Toast.LENGTH_SHORT).show();
            } else {
                AppContainer.from(getApplicationContext()).exerciseRepository.add(result);
                Toast.makeText(this, R.string.exercise_created, Toast.LENGTH_SHORT).show();
            }
            finish(); // retour au catalogue, qui se rafraîchit dans onResume()
        });

        findViewById(R.id.create_cancel_button).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateReferenceAvailability();
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        outState.putString(STATE_DRAFT_ID, draftExerciseId);
        super.onSaveInstanceState(outState);
    }

    private void updateReferenceAvailability() {
        if (referenceStatus == null || compareMovementButton == null
                || referenceMovementId == null) return;
        boolean available = AppContainer.from(getApplicationContext()).movementRepository.exists(referenceMovementId);
        referenceStatus.setText(available
                ? R.string.reference_model_available
                : R.string.reference_model_missing);
        compareMovementButton.setEnabled(available);
    }

    /** Valeur entière saisie, ou 0 si le champ est vide ou illisible. */
    private int parsePositive(EditText field) {
        try {
            return Integer.parseInt(field.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String selectedText(ChipGroup group) {
        int checkedId = group.getCheckedChipId();
        if (checkedId == View.NO_ID) return "";
        Chip chip = group.findViewById(checkedId);
        return chip.getText().toString();
    }

    /** Coche le chip portant ce libellé. Sans effet si aucun ne correspond. */
    private void checkChipWithText(ChipGroup group, @Nullable String text) {
        if (text == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip && text.equalsIgnoreCase(((Chip) child).getText().toString())) {
                group.check(child.getId());
                return;
            }
        }
    }
}

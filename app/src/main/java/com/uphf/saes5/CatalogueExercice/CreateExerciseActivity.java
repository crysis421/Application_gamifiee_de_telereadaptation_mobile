package com.uphf.saes5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

public class CreateExerciseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_exercise);

        TextInputLayout nameLayout = findViewById(R.id.create_name_layout);
        TextInputLayout durationLayout = findViewById(R.id.create_duration_layout);
        EditText name = findViewById(R.id.create_name_input);
        EditText duration = findViewById(R.id.create_duration_input);
        EditText equipment = findViewById(R.id.create_equipment_input);
        ChipGroup muscleGroup = findViewById(R.id.create_muscle_group);
        ChipGroup difficultyGroup = findViewById(R.id.create_difficulty_group);
        RatingBar stars = findViewById(R.id.create_stars_bar);
        MaterialButton btnHolisticCamera = findViewById(R.id.btn_holistic_camera);

        btnHolisticCamera.setOnClickListener(v -> {
            Intent intent = new Intent(this, HolisticCameraActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.create_save_button).setOnClickListener(v -> {
            nameLayout.setError(null);
            durationLayout.setError(null);

            String nameValue = name.getText().toString().trim();
            if (nameValue.isEmpty()) {
                nameLayout.setError(getString(R.string.error_name_required));
                return;
            }

            int durationValue;
            try {
                durationValue = Integer.parseInt(duration.getText().toString().trim());
            } catch (NumberFormatException e) {
                durationValue = 0;
            }
            if (durationValue <= 0) {
                durationLayout.setError(getString(R.string.error_duration_invalid));
                return;
            }

            String equipmentValue = equipment.getText().toString().trim();
            if (equipmentValue.isEmpty()) equipmentValue = "Aucun";

            int starsValue = Math.max(1, Math.round(stars.getRating()));

            ExerciseRepository.add(new Exercise(
                    ExerciseRepository.newId(),
                    nameValue,
                    selectedText(muscleGroup),
                    selectedText(difficultyGroup),
                    durationValue,
                    equipmentValue,
                    starsValue));

            Toast.makeText(this, R.string.exercise_created, Toast.LENGTH_SHORT).show();
            finish(); // retour au catalogue, qui se rafraîchit dans onResume()
        });

        findViewById(R.id.create_cancel_button).setOnClickListener(v -> finish());
    }

    private String selectedText(ChipGroup group) {
        int checkedId = group.getCheckedChipId();
        if (checkedId == View.NO_ID) return "";
        Chip chip = group.findViewById(checkedId);
        return chip.getText().toString();
    }
}

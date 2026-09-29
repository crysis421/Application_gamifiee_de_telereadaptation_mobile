package com.uphf.saes5;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateExerciseActivity extends AppCompatActivity {
    private static final String[] MUSCLE_GROUPS = {"Jambes", "Pectoraux", "Tronc", "Dos"};
    private static final String[] DIFFICULTIES = {"Facile", "Moyen", "Difficile"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_exercise);

        EditText name = findViewById(R.id.create_name_input);
        Spinner muscle = findViewById(R.id.create_muscle_spinner);
        Spinner difficulty = findViewById(R.id.create_difficulty_spinner);
        EditText duration = findViewById(R.id.create_duration_input);
        EditText equipment = findViewById(R.id.create_equipment_input);
        RatingBar stars = findViewById(R.id.create_stars_bar);

        muscle.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, MUSCLE_GROUPS));
        difficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, DIFFICULTIES));

        findViewById(R.id.create_save_button).setOnClickListener(v -> {
            String nameValue = name.getText().toString().trim();
            if (nameValue.isEmpty()) {
                name.setError(getString(R.string.error_name_required));
                return;
            }

            int durationValue;
            try {
                durationValue = Integer.parseInt(duration.getText().toString().trim());
            } catch (NumberFormatException e) {
                durationValue = 0;
            }
            if (durationValue <= 0) {
                duration.setError(getString(R.string.error_duration_invalid));
                return;
            }

            String equipmentValue = equipment.getText().toString().trim();
            if (equipmentValue.isEmpty()) equipmentValue = "Aucun";

            int starsValue = Math.max(1, Math.round(stars.getRating()));

            ExerciseRepository.add(new Exercise(
                    ExerciseRepository.newId(),
                    nameValue,
                    muscle.getSelectedItem().toString(),
                    difficulty.getSelectedItem().toString(),
                    durationValue,
                    equipmentValue,
                    starsValue));

            Toast.makeText(this, R.string.exercise_created, Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.create_cancel_button).setOnClickListener(v -> finish());
    }
}

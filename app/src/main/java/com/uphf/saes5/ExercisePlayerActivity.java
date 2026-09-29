package com.uphf.saes5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ExercisePlayerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_player);
        String exerciseId = getIntent().getStringExtra("exercise_id");
        Exercise exercise = null;
        for (Exercise candidate : ExerciseRepository.getAll()) {
            if (candidate.getId().equals(exerciseId)) {
                exercise = candidate;
                break;
            }
        }
        if (exercise != null) {
            ((android.widget.TextView) findViewById(R.id.player_name)).setText(exercise.getName());
            ((android.widget.TextView) findViewById(R.id.player_metadata)).setText(
                    exercise.getMuscleGroup() + " • " + exercise.getDifficulty()
                            + " • " + exercise.getEquipment());
            Exercise selected = exercise;
            findViewById(R.id.start_button).setOnClickListener(v ->
                    Toast.makeText(this, "Démarrage de " + selected.getName(), Toast.LENGTH_SHORT).show());
        }
        BottomNavigationView navigation = findViewById(R.id.bottom_nav);
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.nav_catalog) {
                startActivity(new Intent(this, ExerciseCatalogActivity.class));
                return true;
            }
            return true;
        });
    }
}

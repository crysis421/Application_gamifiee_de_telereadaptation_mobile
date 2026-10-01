package com.uphf.saes5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.uphf.saes5.accueil.ui.ExerciseActivity;

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
            // Lance la vraie séance, celle de la carte « Exercice du jour » : ce bouton
            // n'affichait qu'un Toast, le catalogue ne menait donc nulle part.
            findViewById(R.id.start_button).setOnClickListener(v ->
                    startActivity(ExerciseActivity.newIntent(this, selected.getId())));
        }
        BottomNavigationView navigation = findViewById(R.id.bottom_nav);
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                Navigation.openTab(this, MainActivity.class);
                return true;
            }
            if (item.getItemId() == R.id.nav_catalog) {
                Navigation.openTab(this, ExerciseCatalogActivity.class);
                return true;
            }
            return true;
        });
    }
}

package com.uphf.saes5.ui.catalog;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.uphf.saes5.R;
import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.ui.common.Navigation;
import com.uphf.saes5.ui.exercise.ExerciseActivity;

public class ExerciseDetailActivity extends AppCompatActivity {
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
        Navigation.setupTabs(this, findViewById(R.id.bottom_nav), R.id.nav_catalog);
    }
}

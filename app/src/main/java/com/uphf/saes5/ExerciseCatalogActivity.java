package com.uphf.saes5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ExerciseCatalogActivity extends AppCompatActivity {
    private ExerciseViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_catalog);
        viewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);

        RecyclerView recycler = findViewById(R.id.exercises_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        ExerciseAdapter adapter = new ExerciseAdapter(exercise -> {
            Intent intent = new Intent(this, ExercisePlayerActivity.class);
            intent.putExtra("exercise_id", exercise.getId());
            startActivity(intent);
        });
        recycler.setAdapter(adapter);
        viewModel.getExercises().observe(this, adapter::submitList);

        Spinner muscle = findViewById(R.id.muscle_spinner);
        Spinner difficulty = findViewById(R.id.difficulty_spinner);
        muscle.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Tous les groupes", "Jambes", "Pectoraux", "Tronc", "Dos"}));
        difficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Toutes les difficultés", "Facile", "Moyen", "Difficile"}));
        EditText duration = findViewById(R.id.duration_input);
        EditText equipment = findViewById(R.id.equipment_input);

        ((Button) findViewById(R.id.apply_button)).setOnClickListener(v -> {
            String selectedMuscle = muscle.getSelectedItem().toString();
            String selectedDifficulty = difficulty.getSelectedItem().toString();
            Integer maxDuration = null;
            if (!duration.getText().toString().trim().isEmpty()) {
                maxDuration = Integer.parseInt(duration.getText().toString().trim());
            }
            viewModel.filter(
                    selectedMuscle.startsWith("Tous") ? "" : selectedMuscle,
                    selectedDifficulty.startsWith("Toutes") ? "" : selectedDifficulty,
                    maxDuration,
                    equipment.getText().toString().trim());
        });
        ((Button) findViewById(R.id.reset_button)).setOnClickListener(v -> {
            muscle.setSelection(0);
            difficulty.setSelection(0);
            duration.setText("");
            equipment.setText("");
            viewModel.reset();
        });

        BottomNavigationView navigation = findViewById(R.id.bottom_nav);
        navigation.setSelectedItemId(R.id.nav_catalog);
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            }
            return true;
        });
    }
}

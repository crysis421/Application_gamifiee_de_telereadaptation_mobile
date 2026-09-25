package com.uphf.saes5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExerciseCatalogActivity extends AppCompatActivity {

    private ExerciseViewModel viewModel;
    private ExerciseAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_catalog);

        viewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);

        RecyclerView rv = findViewById(R.id.rv_exercises);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExerciseAdapter(exercise -> {
            // open player activity
            Intent it = new Intent(ExerciseCatalogActivity.this, ExercisePlayerActivity.class);
            it.putExtra("exercise_id", exercise.getId());
            startActivity(it);
        });
        rv.setAdapter(adapter);

        // filter ui
        Spinner spMuscle = findViewById(R.id.sp_muscle);
        Spinner spDifficulty = findViewById(R.id.sp_difficulty);
        EditText etDuration = findViewById(R.id.et_duration);
        EditText etEquipment = findViewById(R.id.et_equipment);
        Button btnApply = findViewById(R.id.btn_apply);
        Button btnReset = findViewById(R.id.btn_reset);

        // populate spinners with simple values
        spMuscle.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"", "Jambes", "Pectoraux", "Tronc", "Dos"}));
        spDifficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"", "Facile", "Moyen", "Difficile"}));

        btnApply.setOnClickListener(v -> {
            String muscle = spMuscle.getSelectedItem() == null ? "" : spMuscle.getSelectedItem().toString();
            String diff = spDifficulty.getSelectedItem() == null ? "" : spDifficulty.getSelectedItem().toString();
            Integer dur = null;
            String durText = etDuration.getText().toString().trim();
            if (!durText.isEmpty()) {
                try { dur = Integer.parseInt(durText); } catch (NumberFormatException ex) { dur = null; }
            }
            String equip = etEquipment.getText().toString().trim();
            viewModel.filter(muscle, diff, dur, equip);
        });

        btnReset.setOnClickListener(v -> {
            spMuscle.setSelection(0);
            spDifficulty.setSelection(0);
            etDuration.setText("");
            etEquipment.setText("");
            viewModel.resetFilters();
        });

        viewModel.getExercises().observe(this, exercises -> {
            adapter.setItems(exercises);
            Toast.makeText(this, "" + (exercises==null?0:exercises.size()) + " exercices", Toast.LENGTH_SHORT).show();
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottom = findViewById(R.id.bottom_nav);
        if (bottom != null) {
            if (bottom.getMenu().size() == 0) {
                bottom.getMenu().add(0, R.id.nav_home, 0, getString(R.string.nav_home)).setIcon(R.drawable.ic_home);
                bottom.getMenu().add(0, R.id.nav_catalog, 1, getString(R.string.nav_catalog)).setIcon(R.drawable.ic_catalog);
            }
            bottom.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_catalog) {
                    // already here
                    return true;
                } else if (id == R.id.nav_home) {
                    startActivity(new android.content.Intent(this, MainActivity.class));
                    return true;
                }
                return false;
            });
        }
    }
}
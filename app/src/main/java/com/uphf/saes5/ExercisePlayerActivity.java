package com.uphf.saes5;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class ExercisePlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_player);

        TextView tvName = findViewById(R.id.player_name);
        TextView tvMeta = findViewById(R.id.player_meta);
        Button btnStart = findViewById(R.id.player_start);

        String id = getIntent().getStringExtra("exercise_id");
        Exercise ex = findById(id);
        if (ex != null) {
            tvName.setText(ex.getName());
            tvMeta.setText(ex.getMuscleGroup() + " • " + ex.getDifficulty() + " • " + ex.getEquipment());
        }

        btnStart.setOnClickListener(v -> {
            // placeholder for starting the exercise (camera/IA engine)
            Toast.makeText(this, "Démarrage de l'exercice : " + (ex==null?"?":ex.getName()), Toast.LENGTH_SHORT).show();
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottom = findViewById(R.id.bottom_nav);
        if (bottom != null) {
            if (bottom.getMenu().size() == 0) {
                bottom.getMenu().add(0, R.id.nav_home, 0, getString(R.string.nav_home)).setIcon(R.drawable.ic_home);
                bottom.getMenu().add(0, R.id.nav_catalog, 1, getString(R.string.nav_catalog)).setIcon(R.drawable.ic_catalog);
            }
            bottom.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_catalog) {
                    startActivity(new android.content.Intent(this, ExerciseCatalogActivity.class));
                    return true;
                } else if (itemId == R.id.nav_home) {
                    startActivity(new android.content.Intent(this, MainActivity.class));
                    return true;
                }
                return false;
            });
        }
    }
    private Exercise findById(String id) {
        if (id == null) return null;
        List<Exercise> list = ExerciseRepository.getAllExercises();
        for (Exercise e : list) if (id.equals(e.getId())) return e;
        return null;
    }
}
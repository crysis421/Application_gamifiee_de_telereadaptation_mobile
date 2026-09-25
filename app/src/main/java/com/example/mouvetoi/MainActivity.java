package com.example.mouvetoi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCreerExercice = findViewById(R.id.btn_creer_exercice);
        btnCreerExercice.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreerExerciceActivity.class);
            startActivity(intent);
        });
    }
}

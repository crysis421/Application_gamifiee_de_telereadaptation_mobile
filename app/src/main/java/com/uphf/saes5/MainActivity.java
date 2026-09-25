package com.uphf.saes5;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_catalog).setOnClickListener(v -> startActivity(new android.content.Intent(this, ExerciseCatalogActivity.class)));

        com.google.android.material.bottomnavigation.BottomNavigationView bottom = findViewById(R.id.bottom_nav);
        if (bottom != null) {
            if (bottom.getMenu().size() == 0) {
                bottom.getMenu().add(0, R.id.nav_home, 0, getString(R.string.nav_home)).setIcon(R.drawable.ic_home);
                bottom.getMenu().add(0, R.id.nav_catalog, 1, getString(R.string.nav_catalog)).setIcon(R.drawable.ic_catalog);
            }
            bottom.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_catalog) {
                    startActivity(new android.content.Intent(this, ExerciseCatalogActivity.class));
                    return true;
                } else if (id == R.id.nav_home) {
                    // already here
                    return true;
                }
                return false;
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
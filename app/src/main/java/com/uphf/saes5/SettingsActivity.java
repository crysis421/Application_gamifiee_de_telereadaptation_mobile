package com.uphf.saes5;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;

import com.uphf.saes5.accueil.ui.EdgeToEdgeSupport;

/**
 * Réglages des éléments de gamification (US-4.1).
 *
 * <p>La liste des interrupteurs est construite à partir de {@link GameSettings.Feature} :
 * ajouter un élément de jeu ne demande aucune modification ici.</p>
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        EdgeToEdgeSupport.applySystemBarInsets(findViewById(R.id.settings_root));

        LinearLayout container = findViewById(R.id.settings_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (GameSettings.Feature feature : GameSettings.Feature.values()) {
            View row = inflater.inflate(R.layout.item_setting, container, false);

            ((TextView) row.findViewById(R.id.setting_label)).setText(feature.getLabelRes());
            ((TextView) row.findViewById(R.id.setting_help)).setText(feature.getHelpRes());

            MaterialSwitch toggle = row.findViewById(R.id.setting_switch);
            toggle.setChecked(GameSettings.isEnabled(feature));
            toggle.setOnCheckedChangeListener((button, checked) -> {
                GameSettings.setEnabled(feature, checked);
                refreshSnapshot();
            });

            // Toute la ligne bascule l'interrupteur : la cible est plus large que le seul bouton.
            row.setOnClickListener(v -> toggle.toggle());

            container.addView(row);
        }

        refreshSnapshot();
        findViewById(R.id.settings_back_button).setOnClickListener(v -> finish());
    }

    /**
     * Affiche la configuration active telle qu'elle sera journalisée.
     *
     * <p>Le chercheur voit ainsi, sans ambiguïté, la condition de test qu'il vient de
     * composer (US-7.3).</p>
     */
    private void refreshSnapshot() {
        ((TextView) findViewById(R.id.settings_snapshot)).setText(GameSettings.snapshot());
    }
}

package com.uphf.saes5;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import com.uphf.saes5.accueil.ui.EdgeToEdgeSupport;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Réglages des éléments de gamification (US-4.1), nommage de la condition de test (US-7.3)
 * et export du journal des séances (US-7.2).
 *
 * <p>La liste des interrupteurs est construite à partir de {@link GameSettings.Feature} :
 * ajouter un élément de jeu ne demande aucune modification ici.</p>
 */
public class SettingsActivity extends AppCompatActivity {

    /** Périodes proposées à l'export, en jours. 0 signifie « tout l'historique ». */
    private static final int[] EXPORT_PERIODS_DAYS = {0, 7, 30};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        EdgeToEdgeSupport.applySystemBarInsets(findViewById(R.id.settings_root));

        buildFeatureSwitches();
        bindAnalysisSettings();
        bindConditionName();
        bindExport();

        refreshSnapshot();
        findViewById(R.id.settings_back_button).setOnClickListener(v -> finish());
    }

    private void buildFeatureSwitches() {
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
    }

    /** Modèle d'analyse et part d'images analysées (US-1.4). */
    private void bindAnalysisSettings() {
        refreshAnalysisButtons();

        findViewById(R.id.settings_analysis_model).setOnClickListener(v -> {
            AnalysisSettings.Model[] models = AnalysisSettings.Model.values();
            String[] labels = new String[models.length];
            for (int i = 0; i < models.length; i++) labels[i] = getString(models[i].getLabelRes());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.settings_analysis_model_title)
                    .setSingleChoiceItems(labels, AnalysisSettings.getModel().ordinal(),
                            (dialog, which) -> {
                                AnalysisSettings.setModel(models[which]);
                                refreshAnalysisButtons();
                                dialog.dismiss();
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        findViewById(R.id.settings_analysis_stride).setOnClickListener(v -> {
            String[] labels = new String[AnalysisSettings.MAX_FRAME_STRIDE];
            for (int stride = 1; stride <= AnalysisSettings.MAX_FRAME_STRIDE; stride++) {
                labels[stride - 1] = strideLabel(stride);
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.settings_analysis_stride_title)
                    .setSingleChoiceItems(labels, AnalysisSettings.getFrameStride() - 1,
                            (dialog, which) -> {
                                AnalysisSettings.setFrameStride(which + 1);
                                refreshAnalysisButtons();
                                dialog.dismiss();
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void refreshAnalysisButtons() {
        ((TextView) findViewById(R.id.settings_analysis_model)).setText(getString(
                R.string.settings_analysis_model_value,
                getString(AnalysisSettings.getModel().getLabelRes())));
        ((TextView) findViewById(R.id.settings_analysis_stride)).setText(getString(
                R.string.settings_analysis_stride_value,
                strideLabel(AnalysisSettings.getFrameStride())));
    }

    private String strideLabel(int stride) {
        return stride == 1
                ? getString(R.string.settings_analysis_stride_all)
                : getString(R.string.settings_analysis_stride_one_in, stride);
    }

    /** Nom donné à la condition de test, enregistré à chaque frappe (US-7.3). */
    private void bindConditionName() {
        EditText field = findViewById(R.id.settings_condition_name);
        field.setText(GameSettings.getConditionName());
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable editable) {
                GameSettings.setConditionName(editable.toString());
            }
        });
    }

    private void bindExport() {
        findViewById(R.id.settings_export_button).setOnClickListener(v -> askPeriodThenExport());
        refreshSessionCount();
    }

    private void refreshSessionCount() {
        ((TextView) findViewById(R.id.settings_session_count))
                .setText(getString(R.string.settings_sessions_recorded, SessionLog.count()));
    }

    /**
     * Demande la période puis produit l'export (US-7.2).
     *
     * <p>Exporter un journal vide n'aurait aucun sens : on le dit plutôt que de partager un
     * fichier contenant {@code []}.</p>
     */
    private void askPeriodThenExport() {
        if (SessionLog.count() == 0) {
            Toast.makeText(this, R.string.settings_export_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = {
                getString(R.string.settings_export_all),
                getString(R.string.settings_export_7_days),
                getString(R.string.settings_export_30_days),
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_export_period)
                .setItems(labels, (dialog, which) -> export(EXPORT_PERIODS_DAYS[which]))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void export(int days) {
        long from = days == 0 ? 0L : System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        File exported = SessionLog.writeExport(this, from, Long.MAX_VALUE);
        if (exported == null) {
            Toast.makeText(this, R.string.settings_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", exported);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/json");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, exported.getName());
        // Sans ce drapeau, l'application qui reçoit le fichier ne peut pas le lire.
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(share, getString(R.string.settings_export_button)));
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

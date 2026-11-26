package com.uphf.saes5.ui.settings;

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

import com.uphf.saes5.R;
import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.FrameSampling;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.TestCondition;
import com.uphf.saes5.ui.common.EdgeToEdgeSupport;
import com.uphf.saes5.ui.common.FeatureLabels;
import com.uphf.saes5.ui.consent.ConsentActivity;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Réglages des éléments de gamification (US-4.1), nommage de la condition de test (US-7.3)
 * et export du journal des séances (US-7.2).
 *
 * <p>La liste des interrupteurs est construite à partir de {@link GameFeature} :
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
        bindSavedConditions();
        bindExport();

        refreshSnapshot();
        findViewById(R.id.settings_back_button).setOnClickListener(v -> finish());
        findViewById(R.id.settings_consent_button).setOnClickListener(
                v -> startActivity(ConsentActivity.reviewIntent(this)));
    }

    private void buildFeatureSwitches() {
        LinearLayout gameContainer = findViewById(R.id.settings_container);
        LinearLayout homeContainer = findViewById(R.id.settings_home_container);
        // Reconstruit aussi après l'application d'une condition (US-7.3).
        gameContainer.removeAllViews();
        homeContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (GameFeature feature : GameFeature.values()) {
            LinearLayout container = feature.getGroup() == GameFeature.Group.HOME
                    ? homeContainer
                    : gameContainer;
            View row = inflater.inflate(R.layout.item_setting, container, false);

            ((TextView) row.findViewById(R.id.setting_label)).setText(FeatureLabels.label(feature));
            ((TextView) row.findViewById(R.id.setting_help)).setText(FeatureLabels.help(feature));

            MaterialSwitch toggle = row.findViewById(R.id.setting_switch);
            toggle.setChecked(AppContainer.from(getApplicationContext()).gameSettings.isEnabled(feature));
            toggle.setOnCheckedChangeListener((button, checked) -> {
                AppContainer.from(getApplicationContext()).gameSettings.setEnabled(feature, checked);
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
            AnalysisModel[] models = AnalysisModel.values();
            String[] labels = new String[models.length];
            for (int i = 0; i < models.length; i++) labels[i] = getString(FeatureLabels.label(models[i]));
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.settings_analysis_model_title)
                    .setSingleChoiceItems(labels, AppContainer.from(getApplicationContext()).analysisSettings.getModel().ordinal(),
                            (dialog, which) -> {
                                AppContainer.from(getApplicationContext()).analysisSettings.setModel(models[which]);
                                refreshAnalysisButtons();
                                dialog.dismiss();
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        findViewById(R.id.settings_analysis_stride).setOnClickListener(v -> {
            String[] labels = new String[FrameSampling.MAX_FRAME_STRIDE];
            for (int stride = 1; stride <= FrameSampling.MAX_FRAME_STRIDE; stride++) {
                labels[stride - 1] = strideLabel(stride);
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.settings_analysis_stride_title)
                    .setSingleChoiceItems(labels, AppContainer.from(getApplicationContext()).analysisSettings.getFrameStride() - 1,
                            (dialog, which) -> {
                                AppContainer.from(getApplicationContext()).analysisSettings.setFrameStride(which + 1);
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
                getString(FeatureLabels.label(AppContainer.from(getApplicationContext()).analysisSettings.getModel()))));
        ((TextView) findViewById(R.id.settings_analysis_stride)).setText(getString(
                R.string.settings_analysis_stride_value,
                strideLabel(AppContainer.from(getApplicationContext()).analysisSettings.getFrameStride())));
    }

    private String strideLabel(int stride) {
        return stride == 1
                ? getString(R.string.settings_analysis_stride_all)
                : getString(R.string.settings_analysis_stride_one_in, stride);
    }

    /**
     * Conditions enregistrées (US-7.3) : la configuration courante se sauvegarde sous le nom
     * saisi, et une condition enregistrée se réapplique d'un toucher avant chaque participant.
     */
    private void bindSavedConditions() {
        findViewById(R.id.settings_condition_save).setOnClickListener(v -> {
            String name = AppContainer.from(getApplicationContext()).gameSettings.getConditionName();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.settings_condition_name_required,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            AppContainer.from(getApplicationContext()).testConditions.saveCurrent(name);
            Toast.makeText(this, getString(R.string.settings_condition_saved, name),
                    Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.settings_condition_apply).setOnClickListener(v -> chooseCondition());
    }

    private void chooseCondition() {
        List<TestCondition> conditions = AppContainer.from(getApplicationContext()).testConditions.getSaved();
        if (conditions.isEmpty()) {
            Toast.makeText(this, R.string.settings_condition_none, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_condition_choose)
                .setItems(names(conditions), (dialog, which) -> {
                    TestCondition condition = conditions.get(which);
                    AppContainer.from(getApplicationContext()).testConditions.apply(condition);
                    buildFeatureSwitches();
                    ((EditText) findViewById(R.id.settings_condition_name))
                            .setText(condition.getName());
                    refreshSnapshot();
                    Toast.makeText(this, getString(R.string.settings_condition_applied,
                            condition.getName()), Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.settings_condition_delete,
                        (dialog, which) -> chooseConditionToDelete(conditions))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chooseConditionToDelete(List<TestCondition> conditions) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_condition_delete_choose)
                .setItems(names(conditions), (dialog, which) -> {
                    String name = conditions.get(which).getName();
                    new MaterialAlertDialogBuilder(this)
                            .setMessage(getString(R.string.settings_condition_delete_confirm, name))
                            .setPositiveButton(R.string.settings_condition_delete_ok,
                                    (confirm, ignored) -> AppContainer.from(getApplicationContext()).testConditions.delete(name))
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static String[] names(List<TestCondition> conditions) {
        String[] names = new String[conditions.size()];
        for (int i = 0; i < names.length; i++) names[i] = conditions.get(i).getName();
        return names;
    }

    /** Nom donné à la condition de test, enregistré à chaque frappe (US-7.3). */
    private void bindConditionName() {
        EditText field = findViewById(R.id.settings_condition_name);
        field.setText(AppContainer.from(getApplicationContext()).gameSettings.getConditionName());
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable editable) {
                AppContainer.from(getApplicationContext()).gameSettings.setConditionName(editable.toString());
            }
        });
    }

    private void bindExport() {
        findViewById(R.id.settings_export_button).setOnClickListener(v -> askPeriodThenExport());
        refreshSessionCount();
    }

    private void refreshSessionCount() {
        ((TextView) findViewById(R.id.settings_session_count))
                .setText(getString(R.string.settings_sessions_recorded, AppContainer.from(getApplicationContext()).sessionRepository.count()));
    }

    /**
     * Demande la période puis produit l'export (US-7.2).
     *
     * <p>Exporter un journal vide n'aurait aucun sens : on le dit plutôt que de partager un
     * fichier contenant {@code []}.</p>
     */
    private void askPeriodThenExport() {
        if (AppContainer.from(getApplicationContext()).sessionRepository.count() == 0) {
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
                .setItems(labels, (dialog, which) -> askFormatThenExport(EXPORT_PERIODS_DAYS[which]))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Tableur par défaut : c'est ce que le chercheur ouvre le plus souvent (US-7.2). */
    private void askFormatThenExport(int days) {
        String[] labels = {
                getString(R.string.settings_export_format_csv),
                getString(R.string.settings_export_format_json),
        };
        SessionRepository.ExportFormat[] formats = {
                SessionRepository.ExportFormat.CSV,
                SessionRepository.ExportFormat.JSON,
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_export_format)
                .setItems(labels, (dialog, which) -> export(days, formats[which]))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void export(int days, SessionRepository.ExportFormat format) {
        long from = days == 0 ? 0L : System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        File exported = AppContainer.from(getApplicationContext()).sessionRepository.writeExport(from, Long.MAX_VALUE, format);
        if (exported == null) {
            Toast.makeText(this, R.string.settings_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", exported);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(format.getMimeType());
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
        ((TextView) findViewById(R.id.settings_snapshot)).setText(AppContainer.from(getApplicationContext()).gameSettings.snapshot());
    }
}

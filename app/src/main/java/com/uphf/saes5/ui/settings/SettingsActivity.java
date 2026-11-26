package com.uphf.saes5.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import com.uphf.saes5.R;
import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.databinding.ActivitySettingsBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.model.AnalysisModel;
import com.uphf.saes5.domain.model.FrameSampling;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.model.TestCondition;
import com.uphf.saes5.ui.common.EdgeToEdgeSupport;
import com.uphf.saes5.ui.common.FeatureLabels;
import com.uphf.saes5.ui.consent.ConsentActivity;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Réglages des éléments de gamification (US-4.1), de l'analyse (US-1.4), conditions de test
 * (US-7.3) et export du journal des séances (US-7.2).
 *
 * <p>Vue du MVVM : la liste des interrupteurs est construite à partir de {@link GameFeature},
 * et chaque changement passe par {@link SettingsViewModel}. Ajouter un élément de jeu ne
 * demande aucune modification ici.</p>
 */
public class SettingsActivity extends AppCompatActivity {

    /** Périodes proposées à l'export, en jours. 0 signifie « tout l'historique ». */
    private static final int[] EXPORT_PERIODS_DAYS = {0, 7, 30};

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;
    private final Map<GameFeature, MaterialSwitch> switches = new EnumMap<>(GameFeature.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeSupport.applySystemBarInsets(binding.settingsRoot);

        AppContainer container = AppContainer.from(this);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(SettingsViewModel.class,
                () -> new SettingsViewModel(container.gameSettings, container.testConditions,
                        container.analysisSettings, container.sessionRepository, container.clock)))
                .get(SettingsViewModel.class);

        buildFeatureSwitches();
        binding.settingsAnalysisModel.setOnClickListener(v -> chooseModel());
        binding.settingsAnalysisStride.setOnClickListener(v -> chooseStride());
        binding.settingsConditionName.setText(viewModel.getState().getValue().getConditionName());
        binding.settingsConditionName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.setConditionName(editable.toString());
            }
        });
        binding.settingsConditionSave.setOnClickListener(v -> viewModel.saveCondition());
        binding.settingsConditionApply.setOnClickListener(v -> chooseCondition());
        binding.settingsExportButton.setOnClickListener(v -> viewModel.requestExport());
        binding.settingsBackButton.setOnClickListener(v -> finish());
        binding.settingsConsentButton.setOnClickListener(
                v -> startActivity(ConsentActivity.reviewIntent(this)));

        viewModel.getState().observe(this, this::render);
        viewModel.getEvents().observe(this, event -> {
            SettingsViewModel.SettingsEvent content = event.getContentIfNotHandled();
            if (content != null) onEvent(content);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    /** Un interrupteur par élément, dans la section de son groupe. */
    private void buildFeatureSwitches() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (GameFeature feature : GameFeature.values()) {
            LinearLayout container = feature.getGroup() == GameFeature.Group.HOME
                    ? binding.settingsHomeContainer
                    : binding.settingsContainer;
            View row = inflater.inflate(R.layout.item_setting, container, false);
            ((TextView) row.findViewById(R.id.setting_label)).setText(FeatureLabels.label(feature));
            ((TextView) row.findViewById(R.id.setting_help)).setText(FeatureLabels.help(feature));

            MaterialSwitch toggle = row.findViewById(R.id.setting_switch);
            // Les neuf interrupteurs partagent le même identifiant : restaurés par Android après
            // une rotation, ils prendraient tous la valeur du dernier. L'état vient du ViewModel.
            toggle.setSaveEnabled(false);
            toggle.setOnCheckedChangeListener(
                    (button, checked) -> viewModel.setFeatureEnabled(feature, checked));
            // Toute la ligne bascule l'interrupteur : la cible est plus large que le seul bouton.
            row.setOnClickListener(v -> toggle.toggle());
            switches.put(feature, toggle);
            container.addView(row);
        }
    }

    private void render(SettingsUiState state) {
        for (SettingsUiState.FeatureToggle toggle : state.getFeatures()) {
            MaterialSwitch view = switches.get(toggle.getFeature());
            if (view != null && view.isChecked() != toggle.isEnabled()) {
                view.setChecked(toggle.isEnabled());
            }
        }
        binding.settingsAnalysisModel.setText(getString(R.string.settings_analysis_model_value,
                getString(FeatureLabels.label(state.getModel()))));
        binding.settingsAnalysisStride.setText(getString(R.string.settings_analysis_stride_value,
                strideLabel(state.getFrameStride())));
        binding.settingsSessionCount.setText(
                getString(R.string.settings_sessions_recorded, state.getSessionCount()));
        // La configuration telle qu'elle sera journalisée : le chercheur voit sans ambiguïté
        // la condition de test qu'il vient de composer (US-7.3).
        binding.settingsSnapshot.setText(state.getSnapshot());
    }

    private void onEvent(SettingsViewModel.SettingsEvent event) {
        switch (event.getKind()) {
            case CONDITION_NAME_REQUIRED:
                toast(getString(R.string.settings_condition_name_required));
                break;
            case CONDITION_SAVED:
                toast(getString(R.string.settings_condition_saved, event.getName()));
                break;
            case CONDITION_APPLIED:
                binding.settingsConditionName.setText(event.getName());
                toast(getString(R.string.settings_condition_applied, event.getName()));
                break;
            case CHOOSE_EXPORT_PERIOD:
                askPeriodThenExport();
                break;
            case EXPORT_EMPTY:
                toast(getString(R.string.settings_export_empty));
                break;
            case EXPORT_FAILED:
                toast(getString(R.string.settings_export_failed));
                break;
            case EXPORT_READY:
                share(event.getFile(), event.getMimeType());
                break;
        }
    }

    // --- analyse (US-1.4) ------------------------------------------------------------------

    private void chooseModel() {
        AnalysisModel[] models = AnalysisModel.values();
        String[] labels = new String[models.length];
        for (int i = 0; i < models.length; i++) labels[i] = getString(FeatureLabels.label(models[i]));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_analysis_model_title)
                .setSingleChoiceItems(labels, viewModel.getState().getValue().getModel().ordinal(),
                        (dialog, which) -> {
                            viewModel.setModel(models[which]);
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chooseStride() {
        String[] labels = new String[FrameSampling.MAX_FRAME_STRIDE];
        for (int stride = 1; stride <= FrameSampling.MAX_FRAME_STRIDE; stride++) {
            labels[stride - 1] = strideLabel(stride);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_analysis_stride_title)
                .setSingleChoiceItems(labels, viewModel.getState().getValue().getFrameStride() - 1,
                        (dialog, which) -> {
                            viewModel.setFrameStride(which + 1);
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String strideLabel(int stride) {
        return stride == 1
                ? getString(R.string.settings_analysis_stride_all)
                : getString(R.string.settings_analysis_stride_one_in, stride);
    }

    // --- conditions de test (US-7.3) -------------------------------------------------------

    private void chooseCondition() {
        List<TestCondition> conditions = viewModel.getSavedConditions();
        if (conditions.isEmpty()) {
            toast(getString(R.string.settings_condition_none));
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_condition_choose)
                .setItems(names(conditions),
                        (dialog, which) -> viewModel.applyCondition(conditions.get(which)))
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
                                    (confirm, ignored) -> viewModel.deleteCondition(name))
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

    // --- export (US-7.2) -------------------------------------------------------------------

    private void askPeriodThenExport() {
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
                .setItems(labels, (dialog, which) -> viewModel.export(days, formats[which]))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void share(File exported, String mimeType) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", exported);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(mimeType);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, exported.getName());
        // Sans ce drapeau, l'application qui reçoit le fichier ne peut pas le lire.
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, getString(R.string.settings_export_button)));
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

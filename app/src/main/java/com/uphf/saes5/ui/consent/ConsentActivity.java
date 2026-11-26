package com.uphf.saes5.ui.consent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.uphf.saes5.R;
import com.uphf.saes5.databinding.ActivityConsentBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.ui.common.EdgeToEdgeSupport;
import com.uphf.saes5.ui.home.HomeActivity;

import java.text.DateFormat;
import java.util.Date;

/**
 * Notice d'information et consentement de recherche (US-9.1).
 *
 * <ul>
 *   <li>Premier lancement (ou notice modifiée) : il faut cocher puis accepter pour entrer dans
 *   l'application ; refuser la ferme.</li>
 *   <li>Depuis les réglages : la notice se relit, et le consentement peut être retiré, ce qui
 *   efface les séances enregistrées sur le téléphone.</li>
 * </ul>
 */
public class ConsentActivity extends AppCompatActivity {

    private static final String EXTRA_REVIEW = "extra_review";

    private ActivityConsentBinding binding;
    private ConsentViewModel viewModel;

    /** Relecture depuis les réglages, consentement déjà donné. */
    public static Intent reviewIntent(Context context) {
        return new Intent(context, ConsentActivity.class).putExtra(EXTRA_REVIEW, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityConsentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeSupport.applySystemBarInsets(binding.consentRoot);

        boolean review = getIntent().getBooleanExtra(EXTRA_REVIEW, false);
        AppContainer container = AppContainer.from(this);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(ConsentViewModel.class,
                () -> new ConsentViewModel(container.consent, container.sessionRepository, review)))
                .get(ConsentViewModel.class);
        viewModel.getEvents().observe(this, event -> {
            ConsentViewModel.ConsentEvent content = event.getContentIfNotHandled();
            if (content == ConsentViewModel.ConsentEvent.OPEN_HOME) {
                startActivity(new Intent(this, HomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            } else if (content == ConsentViewModel.ConsentEvent.CLOSE_APP) {
                finishAffinity();
            }
        });

        if (viewModel.isReviewMode()) {
            bindReview();
        } else {
            bindFirstLaunch();
        }
    }

    private void bindFirstLaunch() {
        binding.consentCheckbox.setOnCheckedChangeListener(
                (button, checked) -> binding.consentAccept.setEnabled(checked));
        binding.consentAccept.setOnClickListener(v -> viewModel.accept());
        binding.consentSecondary.setOnClickListener(v -> finishAffinity());

        // Revenir en arrière ne doit pas contourner la notice.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    private void bindReview() {
        binding.consentCheckbox.setVisibility(View.GONE);
        binding.consentAcceptedOn.setVisibility(View.VISIBLE);
        binding.consentAcceptedOn.setText(getString(R.string.consent_accepted_on,
                DateFormat.getDateInstance(DateFormat.LONG).format(new Date(viewModel.acceptedAt()))));

        binding.consentAccept.setEnabled(true);
        binding.consentAccept.setText(R.string.consent_close);
        binding.consentAccept.setOnClickListener(v -> finish());

        binding.consentSecondary.setText(R.string.consent_withdraw);
        binding.consentSecondary.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.consent_withdraw_confirm)
                .setPositiveButton(R.string.consent_withdraw_ok, (dialog, which) -> viewModel.withdraw())
                .setNegativeButton(R.string.cancel, null)
                .show());
    }
}

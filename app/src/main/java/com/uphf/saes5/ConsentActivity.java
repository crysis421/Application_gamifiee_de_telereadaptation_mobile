package com.uphf.saes5;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.uphf.saes5.accueil.ui.EdgeToEdgeSupport;

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

    /** Relecture depuis les réglages, consentement déjà donné. */
    public static Intent reviewIntent(Context context) {
        return new Intent(context, ConsentActivity.class).putExtra(EXTRA_REVIEW, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_consent);
        EdgeToEdgeSupport.applySystemBarInsets(findViewById(R.id.consent_root));

        if (getIntent().getBooleanExtra(EXTRA_REVIEW, false) && !Consent.isRequired()) {
            bindReview();
        } else {
            bindFirstLaunch();
        }
    }

    private void bindFirstLaunch() {
        CheckBox checkbox = findViewById(R.id.consent_checkbox);
        Button accept = findViewById(R.id.consent_accept);
        checkbox.setOnCheckedChangeListener((button, checked) -> accept.setEnabled(checked));
        accept.setOnClickListener(v -> {
            Consent.accept();
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
        findViewById(R.id.consent_secondary).setOnClickListener(v -> finishAffinity());

        // Revenir en arrière ne doit pas contourner la notice.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    private void bindReview() {
        findViewById(R.id.consent_checkbox).setVisibility(View.GONE);

        TextView acceptedOn = findViewById(R.id.consent_accepted_on);
        acceptedOn.setVisibility(View.VISIBLE);
        acceptedOn.setText(getString(R.string.consent_accepted_on,
                DateFormat.getDateInstance(DateFormat.LONG).format(new Date(Consent.acceptedAt()))));

        Button close = findViewById(R.id.consent_accept);
        close.setEnabled(true);
        close.setText(R.string.consent_close);
        close.setOnClickListener(v -> finish());

        Button withdraw = findViewById(R.id.consent_secondary);
        withdraw.setText(R.string.consent_withdraw);
        withdraw.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.consent_withdraw_confirm)
                .setPositiveButton(R.string.consent_withdraw_ok, (dialog, which) -> withdraw())
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    private void withdraw() {
        SessionLog.clear();
        Consent.withdraw();
        finishAffinity();
    }
}

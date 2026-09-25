package com.example.mouvetoi;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * US-2.1 — Créer un exercice
 * Formulaire de création avec validation des champs obligatoires :
 * nom, description, groupe musculaire, difficulté, durée estimée.
 * Le matériel est optionnel.
 */
public class CreerExerciceActivity extends AppCompatActivity {

    private TextInputLayout tilNom, tilDescription, tilDuree;
    private TextInputEditText etNom, etDescription, etMateriel, etDuree;
    private Spinner spinnerGroupeMusculaire, spinnerDifficulte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creer_exercice);

        tilNom = findViewById(R.id.til_nom);
        tilDescription = findViewById(R.id.til_description);
        tilDuree = findViewById(R.id.til_duree);

        etNom = findViewById(R.id.et_nom);
        etDescription = findViewById(R.id.et_description);
        etMateriel = findViewById(R.id.et_materiel);
        etDuree = findViewById(R.id.et_duree);

        spinnerGroupeMusculaire = findViewById(R.id.spinner_groupe_musculaire);
        spinnerDifficulte = findViewById(R.id.spinner_difficulte);

        Button btnEnregistrer = findViewById(R.id.btn_enregistrer);
        btnEnregistrer.setOnClickListener(v -> enregistrerExercice());
    }

    private void enregistrerExercice() {
        // Réinitialise les erreurs précédentes
        tilNom.setError(null);
        tilDescription.setError(null);
        tilDuree.setError(null);

        String nom = getText(etNom);
        String description = getText(etDescription);
        String materiel = getText(etMateriel);
        String dureeStr = getText(etDuree);

        String groupeMusculaire = spinnerGroupeMusculaire.getSelectedItem().toString();
        String difficulte = spinnerDifficulte.getSelectedItem().toString();

        boolean valide = true;

        if (TextUtils.isEmpty(nom)) {
            tilNom.setError("Le nom est obligatoire");
            valide = false;
        }

        if (TextUtils.isEmpty(description)) {
            tilDescription.setError("La description est obligatoire");
            valide = false;
        }

        if ("Sélectionner...".equals(groupeMusculaire)) {
            Toast.makeText(this, "Sélectionnez un groupe musculaire", Toast.LENGTH_SHORT).show();
            valide = false;
        }

        if ("Sélectionner...".equals(difficulte)) {
            Toast.makeText(this, "Sélectionnez une difficulté", Toast.LENGTH_SHORT).show();
            valide = false;
        }

        int duree = 0;
        if (TextUtils.isEmpty(dureeStr)) {
            tilDuree.setError("La durée estimée est obligatoire");
            valide = false;
        } else {
            try {
                duree = Integer.parseInt(dureeStr);
                if (duree <= 0) {
                    tilDuree.setError("La durée doit être supérieure à 0");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                tilDuree.setError("Durée invalide");
                valide = false;
            }
        }

        if (!valide) {
            return;
        }

        // Construction de l'exercice (à brancher plus tard sur la persistance : BDD/API)
        Exercice exercice = new Exercice(nom, description, groupeMusculaire, materiel, difficulte, duree);

        Toast.makeText(this, "Exercice « " + exercice.getNom() + " » créé", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}

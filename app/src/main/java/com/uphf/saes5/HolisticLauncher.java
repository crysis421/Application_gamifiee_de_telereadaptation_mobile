package com.uphf.saes5;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

public final class HolisticLauncher {

    private HolisticLauncher() {}

    /** Demande un nom, puis ouvre la caméra en mode enregistrement. */
    public static void startRecordFlow(Context activityContext) {
        EditText input = new EditText(activityContext);
        input.setHint("Nom de l'exercice");

        new AlertDialog.Builder(activityContext)
                .setTitle("Enregistrer un mouvement")
                .setView(input)
                .setPositiveButton("Démarrer", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(activityContext, "Nom requis", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    open(activityContext, HolisticCameraActivity.MODE_RECORD, name);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    /** Liste les mouvements enregistrés, puis ouvre la caméra en mode comparaison. */
    public static void startCompareFlow(Context activityContext) {
        List<String> names = MovementStorage.list(activityContext);
        if (names.isEmpty()) {
            Toast.makeText(activityContext, "Aucun mouvement enregistré", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(activityContext)
                .setTitle("Choisir un mouvement")
                .setItems(names.toArray(new String[0]), (d, which) ->
                        open(activityContext, HolisticCameraActivity.MODE_COMPARE, names.get(which)))
                .show();
    }

    private static void open(Context c, String mode, String name) {
        Intent intent = new Intent(c, HolisticCameraActivity.class);
        intent.putExtra(HolisticCameraActivity.EXTRA_MODE, mode);
        intent.putExtra(HolisticCameraActivity.EXTRA_NAME, name);
        c.startActivity(intent);
    }
}
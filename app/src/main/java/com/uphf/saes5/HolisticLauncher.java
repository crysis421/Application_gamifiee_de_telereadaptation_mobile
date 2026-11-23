package com.uphf.saes5;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public final class HolisticLauncher {

    private HolisticLauncher() {}

    /** Enregistre le modèle associé à un exercice précis. */
    public static void startRecording(Context activityContext, String referenceMovementId) {
        open(activityContext, HolisticCameraActivity.MODE_RECORD, referenceMovementId);
    }

    /** Teste la caméra contre le modèle explicitement associé à l'exercice. */
    public static void startComparison(Context activityContext, String referenceMovementId) {
        if (!MovementStorage.exists(activityContext, referenceMovementId)) {
            Toast.makeText(activityContext, R.string.reference_model_missing_toast,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        open(activityContext, HolisticCameraActivity.MODE_COMPARE, referenceMovementId);
    }

    private static void open(Context c, String mode, String name) {
        if (name == null || name.trim().isEmpty()) {
            Toast.makeText(c, R.string.reference_model_missing_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(c, HolisticCameraActivity.class);
        intent.putExtra(HolisticCameraActivity.EXTRA_MODE, mode);
        intent.putExtra(HolisticCameraActivity.EXTRA_NAME, name);
        c.startActivity(intent);
    }
}

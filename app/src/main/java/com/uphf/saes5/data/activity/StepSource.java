package com.uphf.saes5.data.activity;

import androidx.annotation.NonNull;

/**
 * Nombre de pas du jour (US-5.1).
 *
 * <p>Interface : l'accueil ne dépend pas des Play services, et se teste avec une source
 * factice. L'implémentation réelle est {@link StepCounter}.</p>
 */
public interface StepSource {

    /** Pourquoi les pas ne peuvent pas être lus. */
    enum Availability { AVAILABLE, PERMISSION_MISSING, UNSUPPORTED }

    /** Réponse de la lecture, rendue sur le fil principal. */
    interface Callback {
        void onSteps(int steps);

        void onUnavailable(@NonNull Availability reason);
    }

    /** Lit les pas depuis minuit. */
    void readToday(@NonNull Callback callback);
}

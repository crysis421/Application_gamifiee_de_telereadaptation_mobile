package com.uphf.saes5.ui.common;

import androidx.annotation.Nullable;

/**
 * Un événement ponctuel émis par un ViewModel : message à afficher, son à jouer, écran à
 * ouvrir.
 *
 * <p>Un {@code LiveData} rejoue sa dernière valeur à chaque nouvel observateur, par exemple
 * après une rotation de l'écran. L'état doit être rejoué ; un événement, lui, ne doit être
 * traité qu'une fois : {@link #getContentIfNotHandled()} ne le rend qu'au premier appel.</p>
 */
public final class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    /** Le contenu, ou {@code null} s'il a déjà été traité. */
    @Nullable
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    /** Le contenu, traité ou non : pour les tests. */
    public T peek() {
        return content;
    }
}

package com.uphf.saes5.accueil.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Exercices mis en favori par l'utilisateur.
 *
 * <p>Stockage volontairement simple (SharedPreferences) : il suffit pour la fonctionnalité et
 * sera migré vers la base de données en même temps que le reste. Le catalogue (US-6.1) pourra
 * lire ces identifiants pour afficher/filtrer les favoris.</p>
 */
public class FavoritesRepository {

    private static final String PREFS_NAME = "exergame_prefs";
    private static final String KEY_FAVORITE_IDS = "favorite_exercise_ids";

    private final SharedPreferences preferences;

    public FavoritesRepository(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Les identifiants des exercices favoris. */
    public Set<String> getFavoriteIds() {
        // Copie défensive : l'ensemble renvoyé par SharedPreferences ne doit jamais être modifié.
        return Collections.unmodifiableSet(
                new HashSet<>(preferences.getStringSet(KEY_FAVORITE_IDS, Collections.emptySet())));
    }

    public boolean isFavorite(String exerciseId) {
        return getFavoriteIds().contains(exerciseId);
    }

    public void setFavorite(String exerciseId, boolean favorite) {
        Set<String> updated = new HashSet<>(getFavoriteIds());
        if (favorite) {
            updated.add(exerciseId);
        } else {
            updated.remove(exerciseId);
        }
        preferences.edit().putStringSet(KEY_FAVORITE_IDS, updated).apply();
    }

    /** Bascule l'état favori et renvoie le nouvel état. */
    public boolean toggleFavorite(String exerciseId) {
        boolean newState = !isFavorite(exerciseId);
        setFavorite(exerciseId, newState);
        return newState;
    }
}

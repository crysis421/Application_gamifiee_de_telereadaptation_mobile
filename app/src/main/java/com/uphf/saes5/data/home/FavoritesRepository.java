package com.uphf.saes5.data.home;

import com.uphf.saes5.data.storage.PreferenceStore;

import java.util.HashSet;
import java.util.Set;

/**
 * Exercices mis en favori par l'utilisateur, conservés d'une session à l'autre.
 *
 * <p><strong>Fonctionnalité incomplète.</strong> Marquer un favori fonctionne, mais aucun
 * écran ne permet encore de consulter ses favoris : {@link #getFavoriteIds()} n'est appelé
 * nulle part. Il reste à les afficher et à les filtrer dans le catalogue (US-6.1).</p>
 */
public final class FavoritesRepository {

    private static final String KEY_FAVORITE_IDS = "favorite_exercise_ids";

    private final PreferenceStore store;

    public FavoritesRepository(PreferenceStore store) {
        this.store = store;
    }

    /** Les identifiants des exercices favoris (copie non modifiable). */
    public Set<String> getFavoriteIds() {
        return store.getStringSet(KEY_FAVORITE_IDS);
    }

    public boolean isFavorite(String exerciseId) {
        return getFavoriteIds().contains(exerciseId);
    }

    /** Bascule l'état favori et renvoie le nouvel état. */
    public boolean toggleFavorite(String exerciseId) {
        Set<String> updated = new HashSet<>(getFavoriteIds());
        boolean favorite = !updated.remove(exerciseId);
        if (favorite) {
            updated.add(exerciseId);
        }
        store.putStringSet(KEY_FAVORITE_IDS, updated);
        return favorite;
    }
}

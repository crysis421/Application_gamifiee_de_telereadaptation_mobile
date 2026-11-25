package com.uphf.saes5;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Barre d'onglets de l'application.
 *
 * <p>Chaque écran recâblait sa barre à la main, et les copies avaient divergé : l'un oubliait
 * de marquer l'onglet courant, un autre ne proposait pas le catalogue, un autre encore
 * laissait des {@code finish()} qui réempilaient les écrans. Tout passe désormais par
 * {@link #setupTabs}, et la barre elle-même vient du layout partagé
 * {@code view_bottom_nav.xml}.</p>
 *
 * <p>Les onglets ne doivent jamais s'empiler : {@code CLEAR_TOP} ramène au premier plan
 * l'écran déjà présent dans la pile en fermant ceux empilés par-dessus, {@code SINGLE_TOP}
 * évite de le recréer. Associés au {@code launchMode="singleTop"} déclaré au manifeste, la
 * pile reste bornée aux quatre onglets et le retour quitte l'application depuis l'accueil.</p>
 *
 * <p>Corollaire : un écran ramené de la pile n'est pas recréé, donc sa barre n'est pas
 * reconstruite. Elle doit par conséquent ne jamais marquer autre chose que son propre
 * onglet — voir le refus de sélection dans {@link #setupTabs}.</p>
 */
public final class Navigation {

    private Navigation() {
    }

    /**
     * Installe la barre d'onglets d'un écran.
     *
     * <p>Applique les réglages de gamification, marque l'onglet courant et branche la
     * navigation vers les trois autres.</p>
     *
     * @param selectedItemId l'onglet à marquer comme actif, ou {@code 0} pour n'en marquer
     *                       aucun — un écran qui n'est pas lui-même un onglet
     */
    public static void setupTabs(Activity activity,
                                 @Nullable BottomNavigationView navigation,
                                 @IdRes int selectedItemId) {
        if (navigation == null) {
            return;
        }
        applyGameSettings(navigation);
        if (selectedItemId != 0) {
            navigation.setSelectedItemId(selectedItemId);
        }
        navigation.setOnItemSelectedListener(item -> {
            Class<? extends Activity> target = screenOf(item.getItemId());
            if (target == null) {
                return false;
            }
            if (activity.getClass().equals(target)) {
                // Déjà sur cet onglet : on garde la marque, sans rien recharger.
                return true;
            }
            openTab(activity, target);
            // La sélection est refusée à dessein : l'écran que l'on quitte doit continuer de
            // marquer son propre onglet. Sinon il gardait la marque de l'onglet touché, et le
            // rouvrir — bouton retour, ou nouvel appui sur son onglet depuis un écran empilé
            // par-dessus — affichait un écran en en désignant un autre dans la barre.
            return false;
        });
    }

    /** L'écran derrière un onglet, ou {@code null} si l'élément n'est pas un onglet. */
    @Nullable
    private static Class<? extends Activity> screenOf(@IdRes int itemId) {
        if (itemId == R.id.nav_home) {
            return MainActivity.class;
        }
        if (itemId == R.id.nav_statistics) {
            return StatisticsActivity.class;
        }
        if (itemId == R.id.nav_catalog) {
            return ExerciseCatalogActivity.class;
        }
        if (itemId == R.id.nav_profile) {
            return ProfileActivity.class;
        }
        return null;
    }

    /**
     * Bascule vers l'onglet demandé.
     *
     * <p>Sans effet si l'on y est déjà : réappuyer sur son propre onglet ne doit pas
     * recharger l'écran.</p>
     */
    public static void openTab(Activity from, Class<? extends Activity> target) {
        if (from.getClass().equals(target)) {
            return;
        }
        Intent intent = new Intent(from, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        from.startActivity(intent);
    }

    /**
     * Masque les onglets dont l'élément de jeu est désactivé (US-4.1).
     *
     * <p>Appelé par {@link #setupTabs}, et à rappeler depuis {@code onResume} des écrans qui
     * peuvent voir les réglages changer pendant leur affichage : un onglet laissé visible
     * mènerait à un écran que la condition de test est censée retirer.</p>
     */
    public static void applyGameSettings(BottomNavigationView navigation) {
        MenuItem statistics = navigation.getMenu().findItem(R.id.nav_statistics);
        if (statistics != null) {
            statistics.setVisible(GameSettings.isEnabled(GameSettings.Feature.STATISTICS));
        }
    }
}

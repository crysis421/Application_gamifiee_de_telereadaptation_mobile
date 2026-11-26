package com.uphf.saes5.ui.common;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.uphf.saes5.R;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.ui.catalog.ExerciseCatalogActivity;
import com.uphf.saes5.ui.home.HomeActivity;
import com.uphf.saes5.ui.profile.ProfileActivity;
import com.uphf.saes5.ui.statistics.StatisticsActivity;

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
 *
 * <p>Les changements d'onglet glissent dans le sens de la barre : un onglet situé à droite
 * arrive par la droite, un onglet situé à gauche arrive par la gauche, et le retour rejoue
 * le mouvement à l'envers.</p>
 *
 * <p>On change aussi d'onglet en balayant l'écran vers la gauche ou la droite : voir
 * {@link TabSwipe}, qui s'appuie sur {@link #neighbourOf}.</p>
 */
public final class Navigation {

    /**
     * Position, dans la barre, de l'onglet depuis lequel un écran a été ouvert. L'écran qui
     * l'a ouvert reste juste en dessous dans la pile : c'est donc là que le retour ramène,
     * et le sens de l'animation de retour s'en déduit.
     */
    private static final String EXTRA_OPENER_POSITION = "com.uphf.saes5.OPENER_TAB_POSITION";

    /** Les onglets, dans l'ordre de la barre — de gauche à droite. */
    private static final int[] TAB_ORDER = {
            R.id.nav_home, R.id.nav_statistics, R.id.nav_catalog, R.id.nav_profile
    };

    private Navigation() {
    }

    /**
     * Installe la barre d'onglets d'un écran.
     *
     * <p>Applique les réglages de gamification, marque l'onglet courant, branche la
     * navigation vers les trois autres et oriente l'animation de retour.</p>
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
        orientBackTransition(activity, selectedItemId);
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

    /**
     * Bascule vers l'onglet demandé, en glissant dans le sens de la barre.
     *
     * <p>Sans effet si l'on y est déjà : réappuyer sur son propre onglet ne doit pas
     * recharger l'écran.</p>
     */
    public static void openTab(Activity from, Class<? extends Activity> target) {
        if (from.getClass().equals(target)) {
            return;
        }
        int fromPosition = positionOf(currentTab(from));
        int toPosition = positionOf(tabOf(target));

        Intent intent = new Intent(from, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_OPENER_POSITION, fromPosition);

        if (fromPosition < 0 || toPosition < 0 || fromPosition == toPosition) {
            // Pas de sens à donner — un écran de détail qui rejoint son propre onglet, par
            // exemple : l'animation par défaut du système convient.
            from.startActivity(intent);
            return;
        }
        boolean towardsRight = toPosition > fromPosition;
        from.startActivity(intent);
        overrideTransition(from,
                towardsRight ? R.anim.tab_enter_from_right : R.anim.tab_enter_from_left,
                towardsRight ? R.anim.tab_exit_to_left : R.anim.tab_exit_to_right);
    }

    /**
     * Impose l'animation de la transition qui vient d'être lancée.
     *
     * <p>{@code overridePendingTransition} plutôt qu'{@code ActivityOptions} : Android 14
     * (constaté sur Samsung) ignore les options de lancement quand l'onglet visé est déjà
     * dans la pile et que {@code CLEAR_TOP} le ramène — le cas de l'accueil, presque
     * toujours. Le système jouait alors son animation par défaut, par le bas. Appelé juste
     * après {@code startActivity}, cet appel prime sur toute autre animation, quel que soit
     * le type de transition. Il est déprécié depuis Android 14 au profit
     * d'{@code overrideActivityTransition}, qui ne convient pas ici : celui-ci fixe une
     * animation par écran et non par trajet, et c'est ce dernier qui donne le sens.</p>
     */
    @SuppressWarnings("deprecation")
    private static void overrideTransition(Activity from, int enterAnim, int exitAnim) {
        from.overridePendingTransition(enterAnim, exitAnim);
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
            statistics.setVisible(AppContainer.from(navigation.getContext()).gameSettings.isEnabled(GameFeature.STATISTICS));
        }
    }

    /**
     * Oriente l'animation de retour : l'écran d'en dessous revient du côté de son onglet.
     *
     * <p>L'extra lu ici est celui de la création de l'écran. Un retour sur l'écran par
     * {@code SINGLE_TOP} passe par {@code onNewIntent} sans remplacer cet intent, et c'est
     * voulu : l'écran situé en dessous dans la pile ne change pas.</p>
     *
     * <p>Android 14 et plus seulement, car {@code overrideActivityTransition} n'existe pas
     * avant et c'est aussi la seule voie que respecte le retour prédictif. En dessous, le
     * retour garde l'animation du système ; l'aller reste orienté sur toutes les versions.</p>
     */
    private static void orientBackTransition(Activity activity, @IdRes int selectedItemId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return;
        }
        int own = positionOf(selectedItemId);
        int opener = activity.getIntent().getIntExtra(EXTRA_OPENER_POSITION, -1);
        if (own < 0 || opener < 0 || own == opener) {
            return;
        }
        boolean openerOnLeft = opener < own;
        activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE,
                openerOnLeft ? R.anim.tab_enter_from_left : R.anim.tab_enter_from_right,
                openerOnLeft ? R.anim.tab_exit_to_right : R.anim.tab_exit_to_left);
    }

    /** L'écran derrière un onglet, ou {@code null} si l'élément n'est pas un onglet. */
    @Nullable
    private static Class<? extends Activity> screenOf(@IdRes int itemId) {
        if (itemId == R.id.nav_home) {
            return HomeActivity.class;
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
     * L'onglet voisin de l'écran dans la barre, en sautant ceux que les réglages masquent.
     *
     * @param step {@code +1} pour l'onglet de droite, {@code -1} pour celui de gauche
     * @return l'écran de cet onglet, ou {@code null} s'il n'y en a pas de ce côté, ou si
     *         l'écran n'est pas lui-même un onglet — un écran de détail ne se balaie pas
     */
    @Nullable
    static Class<? extends Activity> neighbourOf(Activity activity, int step) {
        BottomNavigationView navigation = activity.findViewById(R.id.bottom_nav);
        if (navigation == null) {
            return null;
        }
        int own = navigation.getSelectedItemId();
        if (!activity.getClass().equals(screenOf(own))) {
            return null;
        }
        for (int i = positionOf(own) + step; i >= 0 && i < TAB_ORDER.length; i += step) {
            MenuItem item = navigation.getMenu().findItem(TAB_ORDER[i]);
            if (item != null && item.isVisible()) {
                return screenOf(TAB_ORDER[i]);
            }
        }
        return null;
    }

    /** L'onglet d'un écran, ou {@code 0} si l'écran n'est pas un onglet. */
    @IdRes
    private static int tabOf(Class<? extends Activity> screen) {
        for (int itemId : TAB_ORDER) {
            if (screen.equals(screenOf(itemId))) {
                return itemId;
            }
        }
        return 0;
    }

    /**
     * L'onglet que marque la barre de l'écran — celui de son onglet parent pour un écran
     * de détail, ce qui situe aussi ce dernier dans la barre.
     */
    @IdRes
    private static int currentTab(Activity activity) {
        BottomNavigationView navigation = activity.findViewById(R.id.bottom_nav);
        return navigation != null ? navigation.getSelectedItemId() : tabOf(activity.getClass());
    }

    /** Position de l'onglet dans la barre, de gauche à droite, ou {@code -1}. */
    private static int positionOf(@IdRes int itemId) {
        for (int i = 0; i < TAB_ORDER.length; i++) {
            if (TAB_ORDER[i] == itemId) {
                return i;
            }
        }
        return -1;
    }
}

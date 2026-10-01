package com.uphf.saes5;

import android.app.Activity;
import android.content.Intent;

/**
 * Navigation de la barre d'onglets.
 *
 * <p>Les trois onglets ne doivent jamais s'empiler : chacun appelait
 * {@code startActivity} sans drapeau, si bien qu'un aller-retour Accueil → Catalogue →
 * Accueil créait trois écrans au lieu d'en réutiliser un. Le bouton retour remontait alors
 * tout l'historique des onglets, et plusieurs catalogues coexistaient, chacun avec ses
 * propres filtres.</p>
 *
 * <p>{@code CLEAR_TOP} ramène au premier plan l'écran déjà présent dans la pile en fermant
 * ceux empilés par-dessus ; {@code SINGLE_TOP} évite de le recréer. Associés au
 * {@code launchMode="singleTop"} déclaré au manifeste, la pile reste bornée aux trois
 * onglets et le retour quitte l'application depuis l'accueil.</p>
 */
public final class Navigation {

    private Navigation() {
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
}

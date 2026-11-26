package com.uphf.saes5.ui.common;

import android.view.MotionEvent;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Écran qui est l'un des quatre onglets de la barre : accueil, statistiques, catalogue,
 * profil.
 *
 * <p>Regroupe ce que les quatre faisaient chacun de leur côté : installer la barre, masquer
 * l'onglet Statistiques quand il est désactivé, et changer d'onglet en balayant l'écran.</p>
 */
public abstract class TabActivity extends AppCompatActivity {

    @Nullable
    private BottomNavigationView tabs;

    /** L'onglet que représente cet écran ({@code R.id.nav_…}). */
    @IdRes
    protected abstract int tabId();

    /** À appeler une fois la vue créée. */
    protected void setupTabs(@Nullable BottomNavigationView navigation) {
        tabs = navigation;
        Navigation.setupTabs(this, navigation, tabId());
    }

    /** Applique le réglage de l'onglet Statistiques, tel que l'état de l'écran le donne. */
    protected void showStatisticsTab(boolean visible) {
        Navigation.setStatisticsVisible(tabs, visible);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Balayer vers la gauche ou la droite change d'onglet (voir TabSwipe).
        return TabSwipe.dispatch(this, event) || super.dispatchTouchEvent(event);
    }
}

package com.uphf.saes5.ui.common;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.uphf.saes5.R;

/**
 * Changer d'onglet en balayant l'écran.
 *
 * <p>Balayer vers la gauche ouvre l'onglet de droite, balayer vers la droite celui de gauche :
 * le contenu suit le doigt, comme une page qu'on tourne, et l'animation d'arrivée part du même
 * côté. Chaque onglet transmet ses touchers depuis {@code dispatchTouchEvent} :</p>
 *
 * <pre>
 * return TabSwipe.dispatch(this, event) || super.dispatchTouchEvent(event);
 * </pre>
 *
 * <p>Le détecteur observe les touchers <em>avant</em> les vues, sans rien leur retirer :
 * défilement, appuis et filtres fonctionnent comme avant. Il n'intervient qu'au lever du
 * doigt, et seulement pour un geste nettement horizontal. Il s'abstient :</p>
 * <ul>
 *   <li>si le geste commence sur un élément qui défile lui-même horizontalement — les
 *       rangées de filtres du catalogue — pour ne pas lui voler son défilement ;</li>
 *   <li>s'il commence dans la zone du geste « retour » du système, au bord de l'écran ;</li>
 *   <li>à plusieurs doigts, et sur un écran qui n'est pas lui-même un onglet.</li>
 * </ul>
 *
 * <p>Quand il change d'onglet, il annule le geste auprès des vues : sans cela, la carte sous
 * le doigt recevrait le lever et s'ouvrirait en même temps.</p>
 */
public final class TabSwipe {

    /** Distance minimale d'un balayage rapide. */
    private static final float MIN_DISTANCE_DP = 64f;

    /** Vitesse à partir de laquelle un balayage court suffit. */
    private static final float MIN_VELOCITY_DP_PER_S = 500f;

    /** Un glissement lent change aussi d'onglet s'il parcourt ce tiers de l'écran. */
    private static final float SLOW_DRAG_SCREEN_FRACTION = 1f / 3f;

    /** Le geste doit être au moins deux fois plus horizontal que vertical. */
    private static final float HORIZONTAL_DOMINANCE = 2f;

    private final Activity activity;
    private final float minDistance;
    private final float minVelocity;
    private final int[] location = new int[2];

    @Nullable
    private VelocityTracker velocity;
    private float downX;
    private float downY;

    private TabSwipe(Activity activity) {
        this.activity = activity;
        float density = activity.getResources().getDisplayMetrics().density;
        this.minDistance = MIN_DISTANCE_DP * density;
        this.minVelocity = MIN_VELOCITY_DP_PER_S * density;
    }

    /**
     * Observe un toucher de l'écran.
     *
     * @return {@code true} si le geste vient de changer d'onglet : l'événement est alors
     *         consommé, et les vues ont déjà reçu l'annulation du geste
     */
    public static boolean dispatch(Activity activity, MotionEvent event) {
        View decor = activity.getWindow().getDecorView();
        Object tag = decor.getTag(R.id.tab_swipe);
        TabSwipe swipe;
        if (tag instanceof TabSwipe) {
            swipe = (TabSwipe) tag;
        } else {
            swipe = new TabSwipe(activity);
            decor.setTag(R.id.tab_swipe, swipe);
        }
        return swipe.onTouchEvent(event);
    }

    private boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stopTracking();
                downX = event.getX();
                downY = event.getY();
                View decor = activity.getWindow().getDecorView();
                if (!inSystemGestureZone(decor, downX)
                        && !startsOnHorizontalScroller(decor, downX, downY)) {
                    velocity = VelocityTracker.obtain();
                    velocity.addMovement(event);
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (velocity != null) {
                    velocity.addMovement(event);
                }
                return false;
            case MotionEvent.ACTION_UP:
                boolean switched = velocity != null && switchTabIfSwiped(event);
                stopTracking();
                return switched;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                stopTracking();
                return false;
            default:
                return false;
        }
    }

    private boolean switchTabIfSwiped(MotionEvent up) {
        velocity.addMovement(up);
        velocity.computeCurrentVelocity(1000);
        float dx = up.getX() - downX;
        float dy = up.getY() - downY;
        float vx = velocity.getXVelocity();

        if (Math.abs(dx) < HORIZONTAL_DOMINANCE * Math.abs(dy)) {
            return false;
        }
        float width = activity.getWindow().getDecorView().getWidth();
        boolean slowDrag = Math.abs(dx) >= width * SLOW_DRAG_SCREEN_FRACTION;
        boolean fling = Math.abs(dx) >= minDistance
                && Math.abs(vx) >= minVelocity
                && Math.signum(vx) == Math.signum(dx);
        if (!slowDrag && !fling) {
            return false;
        }

        // Doigt vers la gauche : l'onglet de droite arrive, et inversement.
        Class<? extends Activity> target = Navigation.neighbourOf(activity, dx < 0 ? 1 : -1);
        if (target == null) {
            return false;
        }
        MotionEvent cancel = MotionEvent.obtain(up);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        activity.getWindow().superDispatchTouchEvent(cancel);
        cancel.recycle();
        Navigation.openTab(activity, target);
        return true;
    }

    private void stopTracking() {
        if (velocity != null) {
            velocity.recycle();
            velocity = null;
        }
    }

    /** Les bords où le système réserve le geste « retour » (vide en navigation à boutons). */
    private static boolean inSystemGestureZone(View decor, float x) {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
        if (insets == null) {
            return false;
        }
        Insets gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
        return x < gestures.left || x > decor.getWidth() - gestures.right;
    }

    /** Vrai si un élément sous le doigt peut lui-même défiler horizontalement. */
    private boolean startsOnHorizontalScroller(View view, float x, float y) {
        if (view.getVisibility() != View.VISIBLE) {
            return false;
        }
        view.getLocationInWindow(location);
        if (x < location[0] || x >= location[0] + view.getWidth()
                || y < location[1] || y >= location[1] + view.getHeight()) {
            return false;
        }
        if (view.canScrollHorizontally(1) || view.canScrollHorizontally(-1)) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                if (startsOnHorizontalScroller(group.getChildAt(i), x, y)) {
                    return true;
                }
            }
        }
        return false;
    }
}

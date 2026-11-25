package com.uphf.saes5;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;
import java.util.Optional;

/**
 * Le corps est-il assez visible pour juger le mouvement ?
 *
 * <p>MediaPipe renvoie toujours les 33 points du corps, y compris ceux qu'il ne voit pas : il
 * les devine. Assis devant le téléphone, on obtient donc des genoux et des chevilles
 * inventés, que la comparaison prenait pour vrais — et des répétitions étaient comptées sans
 * que la personne fasse l'exercice.</p>
 *
 * <p>On ne compare donc que si chaque partie du corps qui sert aux angles — épaules, coudes,
 * poignets, hanches, genoux, chevilles — est réellement vue d'au moins un côté : dans
 * l'image, et avec une visibilité d'au moins {@link #MIN_VISIBILITY}. Un côté suffit : de
 * profil, le bras et la jambe du côté caché sont peu visibles, sans que le mouvement soit
 * moins lisible. Une moyenne sur tous les points, essayée d'abord, laissait passer des jambes
 * invisibles : le haut du corps, très net, compensait.</p>
 */
public final class BodyVisibility {

    /** Paires gauche/droite des points des huit angles. */
    static final int[][] BODY_PARTS = {
            {11, 12}, // épaules
            {13, 14}, // coudes
            {15, 16}, // poignets
            {23, 24}, // hanches
            {25, 26}, // genoux
            {27, 28}  // chevilles
    };
    /**
     * Visibilité minimale d'un point vu. Mesurée sur téléphone, personne assise devant
     * l'écran : chevilles devinées entre 0,17 et 0,40, haut du corps vers 1.
     */
    static final float MIN_VISIBILITY = 0.5f;
    /** Marge autour de l'image : un point posé sur le bord compte comme dedans. */
    static final float FRAME_TOLERANCE = 0.02f;

    private BodyVisibility() {
    }

    public static boolean isMostlyVisible(List<NormalizedLandmark> pose) {
        if (pose == null || pose.size() < MovementMath.LANDMARKS) return false;
        float[] x = new float[MovementMath.LANDMARKS];
        float[] y = new float[MovementMath.LANDMARKS];
        float[] visibility = new float[MovementMath.LANDMARKS];
        for (int i = 0; i < MovementMath.LANDMARKS; i++) {
            NormalizedLandmark landmark = pose.get(i);
            x[i] = landmark.x();
            y[i] = landmark.y();
            Optional<Float> v = landmark.visibility();
            visibility[i] = v.isPresent() ? v.get() : Float.NaN;
        }
        return isMostlyVisible(x, y, visibility);
    }

    /**
     * Même règle, sur des tableaux indexés par point. Coordonnées normalisées (0 à 1 dans
     * l'image) ; visibilité {@code NaN} si le modèle ne la fournit pas — seul le cadrage
     * compte alors.
     */
    static boolean isMostlyVisible(float[] x, float[] y, float[] visibility) {
        for (int[] part : BODY_PARTS) {
            if (!isSeen(part[0], x, y, visibility) && !isSeen(part[1], x, y, visibility)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSeen(int point, float[] x, float[] y, float[] visibility) {
        if (!isInFrame(x[point]) || !isInFrame(y[point])) return false;
        return !Float.isFinite(visibility[point]) || visibility[point] >= MIN_VISIBILITY;
    }

    private static boolean isInFrame(float coordinate) {
        return Float.isFinite(coordinate)
                && coordinate >= -FRAME_TOLERANCE && coordinate <= 1f + FRAME_TOLERANCE;
    }
}

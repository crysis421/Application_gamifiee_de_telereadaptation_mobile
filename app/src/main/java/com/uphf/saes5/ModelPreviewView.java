package com.uphf.saes5;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * Mannequin qui rejoue en boucle le modèle de l'exercice, pour montrer le geste à reproduire.
 *
 * <p>Ce sont les images mêmes auxquelles la caméra compare l'utilisateur : le mannequin fait
 * exactement le mouvement attendu, à son rythme. Le côté gauche du corps est dessiné en
 * couleur vive et le droit en couleur sourde : de profil, on distingue la jambe avant de
 * la jambe arrière.</p>
 *
 * <p>L'animation ne tourne que tant que la vue est affichée.</p>
 */
public class ModelPreviewView extends View {

    /** Rafraîchissement de l'animation (≈ 30 images par seconde). */
    private static final long FRAME_DELAY_MS = 33;
    /** Épaisseur des membres, en longueurs de torse. */
    private static final float LIMB_WIDTH = 0.17f;
    /**
     * Épaisseur du tronc, en longueurs de torse. De profil, épaules et hanches se superposent :
     * sans cette épaisseur, le tronc ne serait qu'un trait plus fin que les jambes.
     */
    private static final float TORSO_WIDTH = 0.36f;
    /** Rayon de la tête, en longueurs de torse. */
    private static final float HEAD_RADIUS = 0.2f;
    /** Marge autour du mannequin, en longueurs de torse : ni membre ni tête ne touche le bord. */
    private static final float MARGIN = 0.25f;

    private static final int[][] LEFT_LIMBS = {{11, 13}, {13, 15}, {23, 25}, {25, 27}, {27, 31}};
    private static final int[][] RIGHT_LIMBS = {{12, 14}, {14, 16}, {24, 26}, {26, 28}, {28, 32}};

    private final Paint nearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint farPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path torso = new Path();

    @Nullable
    private ModelAnimation animation;
    private long startedAt;

    public ModelPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int near = ContextCompat.getColor(context, R.color.colorPrimary);
        int far = ContextCompat.getColor(context, R.color.model_preview_far);
        for (Paint paint : new Paint[]{nearPaint, farPaint}) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }
        nearPaint.setColor(near);
        farPaint.setColor(far);
        bodyPaint.setColor(near);
        bodyPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        bodyPaint.setStrokeJoin(Paint.Join.ROUND);
        spinePaint.setColor(near);
        spinePaint.setStyle(Paint.Style.STROKE);
        spinePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * Affiche ce modèle et relance l'animation depuis le début.
     *
     * @return {@code false} si le modèle n'a aucune image exploitable : rien n'est affiché
     */
    public boolean setMovement(List<float[]> frames, int intervalMs) {
        try {
            animation = new ModelAnimation(frames, intervalMs);
        } catch (IllegalArgumentException e) {
            animation = null;
            return false;
        }
        startedAt = SystemClock.uptimeMillis();
        invalidate();
        return true;
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (isVisible) invalidate(); // reprend la boucle laissée en pause
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        ModelAnimation current = animation;
        if (current == null) return;

        float[] pose = current.frameAt(SystemClock.uptimeMillis() - startedAt);

        // Cadrage commun à tout le mouvement, centré dans la vue.
        float spanX = current.maxX - current.minX + 2 * MARGIN;
        float spanY = current.maxY - current.minY + 2 * MARGIN;
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        float scale = Math.min(width / spanX, height / spanY);
        float offsetX = getPaddingLeft() + (width - spanX * scale) / 2f
                + (MARGIN - current.minX) * scale;
        float offsetY = getPaddingTop() + (height - spanY * scale) / 2f
                + (MARGIN - current.minY) * scale;

        float stroke = Math.max(3f, LIMB_WIDTH * scale);
        nearPaint.setStrokeWidth(stroke);
        farPaint.setStrokeWidth(stroke);
        bodyPaint.setStrokeWidth(stroke * 0.6f);
        spinePaint.setStrokeWidth(Math.max(stroke, TORSO_WIDTH * scale));

        // Côté droit d'abord, en retrait ; puis le tronc, le côté gauche et la tête.
        drawLimbs(canvas, pose, RIGHT_LIMBS, farPaint, scale, offsetX, offsetY);

        torso.reset();
        torso.moveTo(x(pose, 11, scale, offsetX), y(pose, 11, scale, offsetY));
        torso.lineTo(x(pose, 12, scale, offsetX), y(pose, 12, scale, offsetY));
        torso.lineTo(x(pose, 24, scale, offsetX), y(pose, 24, scale, offsetY));
        torso.lineTo(x(pose, 23, scale, offsetX), y(pose, 23, scale, offsetY));
        torso.close();
        canvas.drawPath(torso, bodyPaint);
        canvas.drawLine(
                (x(pose, 11, scale, offsetX) + x(pose, 12, scale, offsetX)) / 2f,
                (y(pose, 11, scale, offsetY) + y(pose, 12, scale, offsetY)) / 2f,
                (x(pose, 23, scale, offsetX) + x(pose, 24, scale, offsetX)) / 2f,
                (y(pose, 23, scale, offsetY) + y(pose, 24, scale, offsetY)) / 2f,
                spinePaint);

        drawLimbs(canvas, pose, LEFT_LIMBS, nearPaint, scale, offsetX, offsetY);

        // Tête : entre les oreilles, reliée au milieu des épaules.
        float headX = (x(pose, 7, scale, offsetX) + x(pose, 8, scale, offsetX)) / 2f;
        float headY = (y(pose, 7, scale, offsetY) + y(pose, 8, scale, offsetY)) / 2f;
        float neckX = (x(pose, 11, scale, offsetX) + x(pose, 12, scale, offsetX)) / 2f;
        float neckY = (y(pose, 11, scale, offsetY) + y(pose, 12, scale, offsetY)) / 2f;
        canvas.drawLine(neckX, neckY, headX, headY, nearPaint);
        canvas.drawCircle(headX, headY, HEAD_RADIUS * scale, bodyPaint);

        if (isShown()) postInvalidateDelayed(FRAME_DELAY_MS);
    }

    private static void drawLimbs(Canvas canvas, float[] pose, int[][] limbs, Paint paint,
                                  float scale, float offsetX, float offsetY) {
        for (int[] limb : limbs) {
            canvas.drawLine(x(pose, limb[0], scale, offsetX), y(pose, limb[0], scale, offsetY),
                    x(pose, limb[1], scale, offsetX), y(pose, limb[1], scale, offsetY), paint);
        }
    }

    private static float x(float[] pose, int point, float scale, float offset) {
        return pose[2 * point] * scale + offset;
    }

    private static float y(float[] pose, int point, float scale, float offset) {
        return pose[2 * point + 1] * scale + offset;
    }
}

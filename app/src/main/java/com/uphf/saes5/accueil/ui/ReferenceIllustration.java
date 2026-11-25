package com.uphf.saes5.accueil.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.uphf.saes5.Movement;
import com.uphf.saes5.MovementMath;

/** Vignette de la pose enregistrée, ou illustration du catalogue d'origine. */
final class ReferenceIllustration {
    private static final int[][] CHAINS = {
            {15, 13, 11, 12, 14, 16}, {11, 23, 24, 12}, {23, 25, 27}, {24, 26, 28}
    };

    private ReferenceIllustration() {}

    static Bitmap create(String id, Movement reference) {
        Bitmap image = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
        pen.setStrokeWidth(6);
        pen.setStrokeJoin(Paint.Join.ROUND);
        pen.setStrokeCap(Paint.Cap.ROUND);
        pen.setColor(Color.WHITE);
        if (reference != null && reference.frames != null) {
            int n = reference.frames.size();
            for (int offset = 0; offset < n; offset++) {
                float[] pose = reference.frames.get((n / 2 + offset) % n);
                if (MovementMath.angleFeatures(pose) != null) {
                    drawRecorded(canvas, pen, pose);
                    return image;
                }
            }
        }
        // Points dans le plan de la vignette : tête, épaule, hanche, genou, pied, coude, main.
        float[] points;
        switch (id) {
            case "squat": points = new float[]{91,26, 87,45, 57,91, 103,91, 87,138, 112,60, 140,53}; break;
            case "pompes": points = new float[]{127,67, 109,76, 64,91, 41,100, 13,111, 90,96, 114,112}; break;
            case "fentes": points = new float[]{74,21, 74,41, 70,85, 117,85, 116,134, 83,64, 96,69}; break;
            case "planche": points = new float[]{127,70, 107,78, 65,92, 41,101, 12,115, 104,112, 136,113}; break;
            case "rowing": points = new float[]{100,26, 92,44, 62,78, 79,105, 65,140, 54,53, 81,65}; break;
            default: return null;
        }
        pen.setStyle(Paint.Style.STROKE);
        if ("fentes".equals(id)) line(canvas, pen, 70,85, 49,123, 19,130);
        line(canvas, pen, points[2],points[3], points[4],points[5], points[6],points[7], points[8],points[9]);
        line(canvas, pen, points[2],points[3], points[10],points[11], points[12],points[13]);
        canvas.drawCircle(points[0], points[1], 9, pen);
        if ("rowing".equals(id)) {
            pen.setColor(0xffffd600);
            pen.setStrokeWidth(3);
            canvas.drawLine(points[12],points[13],points[8],points[9],pen);
        }
        return image;
    }

    private static void drawRecorded(Canvas canvas, Paint pen, float[] pose) {
        float minX = pose[0], maxX = minX, minY = pose[1], maxY = minY;
        for (int i = 2; i < MovementMath.LANDMARKS * 2; i += 2) {
            minX = Math.min(minX, pose[i]);
            maxX = Math.max(maxX, pose[i]);
            minY = Math.min(minY, pose[i + 1]);
            maxY = Math.max(maxY, pose[i + 1]);
        }
        float scale = 132f / Math.max(0.001f, Math.max(maxX - minX, maxY - minY));
        canvas.translate(80, 80);
        canvas.scale(scale, scale);
        canvas.translate(-(minX + maxX) / 2, -(minY + maxY) / 2);
        pen.setStrokeWidth(5 / scale);
        canvas.drawCircle(pose[0], pose[1], 6 / scale, pen);
        for (int[] chain : CHAINS) {
            for (int i = 1; i < chain.length; i++) {
                int a = 2 * chain[i - 1], b = 2 * chain[i];
                canvas.drawLine(pose[a], pose[a + 1], pose[b], pose[b + 1], pen);
            }
        }
    }

    private static void line(Canvas canvas, Paint pen, float... points) {
        Path path = new Path();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        canvas.drawPath(path, pen);
    }
}

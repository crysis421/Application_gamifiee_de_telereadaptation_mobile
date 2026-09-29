package com.uphf.saes5;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MuscleBodyView extends View {
    public interface OnMuscleSelectedListener {
        void onMuscleSelected(String muscleGroup);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<String, MuscleStatistic> statistics = new LinkedHashMap<>();
    private final float density;
    private OnMuscleSelectedListener listener;
    private String selectedMuscle;
    private float bodyHeight;

    public MuscleBodyView(Context context) { this(context, null); }

    public MuscleBodyView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MuscleBodyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        setClickable(true);
        setFocusable(true);
        setContentDescription(getContext().getString(R.string.muscle_body_description));
    }

    public void setMuscleStatistics(List<MuscleStatistic> muscleStatistics) {
        statistics.clear();
        for (MuscleStatistic statistic : muscleStatistics) {
            statistics.put(statistic.getMuscleGroup(), statistic);
        }
        updateAccessibilityDescription();
        invalidate();
    }

    public void setSelectedMuscle(@Nullable String muscleGroup) {
        selectedMuscle = muscleGroup;
        invalidate();
    }

    public void setOnMuscleSelectedListener(OnMuscleSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(Math.round(360 * density), widthMeasureSpec),
                resolveSize(Math.round(460 * density), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        bodyHeight = getHeight() - 76 * density;
        drawViewLabel(canvas, .28f, R.string.body_front_label);
        drawViewLabel(canvas, .72f, R.string.body_back_label);
        drawFigureBase(canvas, .28f);
        drawFigureBase(canvas, .72f);
        drawFrontMuscles(canvas);
        drawBackMuscles(canvas);
        drawLegend(canvas);
    }

    private void drawViewLabel(Canvas canvas, float centerX, int labelResource) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorOnSurfaceVariant));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12,
                getResources().getDisplayMetrics()));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(getContext().getString(labelResource), x(centerX), 16 * density, paint);
        paint.setTypeface(Typeface.DEFAULT);
    }

    private void drawFigureBase(Canvas canvas, float centerX) {
        int baseColor = ContextCompat.getColor(getContext(), R.color.muscle_body_base);
        drawBaseOval(canvas, centerX - .042f, .075f, centerX + .042f, .185f, baseColor);
        drawBaseRoundRect(canvas, centerX - .025f, .17f, centerX + .025f, .225f, baseColor);

        Path torso = new Path();
        torso.moveTo(x(centerX - .025f), y(.205f));
        torso.cubicTo(x(centerX - .075f), y(.215f), x(centerX - .105f), y(.235f),
                x(centerX - .105f), y(.285f));
        torso.cubicTo(x(centerX - .095f), y(.36f), x(centerX - .072f), y(.43f),
                x(centerX - .065f), y(.525f));
        torso.lineTo(x(centerX + .065f), y(.525f));
        torso.cubicTo(x(centerX + .072f), y(.43f), x(centerX + .095f), y(.36f),
                x(centerX + .105f), y(.285f));
        torso.cubicTo(x(centerX + .105f), y(.235f), x(centerX + .075f), y(.215f),
                x(centerX + .025f), y(.205f));
        torso.close();
        drawBasePath(canvas, torso, baseColor);
        drawArmBase(canvas, centerX, true, baseColor);
        drawArmBase(canvas, centerX, false, baseColor);
        drawPelvisBase(canvas, centerX, baseColor);
        drawLegBase(canvas, centerX, true, baseColor);
        drawLegBase(canvas, centerX, false, baseColor);
    }

    private void drawArmBase(Canvas canvas, float centerX, boolean left, int color) {
        float direction = left ? -1f : 1f;
        Path arm = new Path();
        arm.moveTo(x(centerX + direction * .086f), y(.235f));
        arm.cubicTo(x(centerX + direction * .135f), y(.245f),
                x(centerX + direction * .145f), y(.31f),
                x(centerX + direction * .15f), y(.37f));
        arm.lineTo(x(centerX + direction * .165f), y(.505f));
        arm.cubicTo(x(centerX + direction * .166f), y(.545f),
                x(centerX + direction * .145f), y(.555f),
                x(centerX + direction * .135f), y(.515f));
        arm.lineTo(x(centerX + direction * .105f), y(.375f));
        arm.cubicTo(x(centerX + direction * .09f), y(.32f),
                x(centerX + direction * .075f), y(.275f),
                x(centerX + direction * .065f), y(.255f));
        arm.close();
        drawBasePath(canvas, arm, color);
    }

    private void drawPelvisBase(Canvas canvas, float centerX, int color) {
        Path pelvis = new Path();
        pelvis.moveTo(x(centerX - .065f), y(.49f));
        pelvis.lineTo(x(centerX + .065f), y(.49f));
        pelvis.lineTo(x(centerX + .078f), y(.59f));
        pelvis.lineTo(x(centerX + .014f), y(.61f));
        pelvis.lineTo(x(centerX), y(.575f));
        pelvis.lineTo(x(centerX - .014f), y(.61f));
        pelvis.lineTo(x(centerX - .078f), y(.59f));
        pelvis.close();
        drawBasePath(canvas, pelvis, color);
    }

    private void drawLegBase(Canvas canvas, float centerX, boolean left, int color) {
        float inner = left ? -.012f : .012f;
        float outer = left ? -.074f : .074f;
        Path leg = new Path();
        leg.moveTo(x(centerX + outer), y(.57f));
        leg.lineTo(x(centerX + inner), y(.57f));
        leg.cubicTo(x(centerX + inner * .8f), y(.68f),
                x(centerX + inner * 1.5f), y(.79f),
                x(centerX + inner * 1.4f), y(.9f));
        leg.lineTo(x(centerX + inner + (left ? -.035f : .035f)), y(.92f));
        leg.lineTo(x(centerX + outer + (left ? -.018f : .018f)), y(.915f));
        leg.cubicTo(x(centerX + outer * .75f), y(.79f),
                x(centerX + outer * 1.1f), y(.68f),
                x(centerX + outer), y(.57f));
        leg.close();
        drawBasePath(canvas, leg, color);
    }

    private void drawFrontMuscles(Canvas canvas) {
        drawMuscleOval(canvas, "Bras", .175f, .235f, .23f, .31f);
        drawMuscleOval(canvas, "Bras", .33f, .235f, .385f, .31f);
        drawMusclePath(canvas, "Pectoraux",
                curvedQuad(.20f, .275f, .28f, .275f, .28f, .35f, .205f, .34f));
        drawMusclePath(canvas, "Pectoraux",
                curvedQuad(.28f, .275f, .36f, .275f, .355f, .34f, .28f, .35f));
        drawMuscleOval(canvas, "Bras", .105f, .31f, .17f, .445f);
        drawMuscleOval(canvas, "Bras", .39f, .31f, .455f, .445f);
        drawMuscleRoundRect(canvas, "Tronc", .222f, .355f, .278f, .515f, 12 * density);
        drawMuscleRoundRect(canvas, "Tronc", .282f, .355f, .338f, .515f, 12 * density);
        drawAnatomyLine(canvas, .28f, .36f, .28f, .51f);
        drawAnatomyLine(canvas, .225f, .405f, .335f, .405f);
        drawAnatomyLine(canvas, .225f, .46f, .335f, .46f);
        drawFrontLegMuscles(canvas, true);
        drawFrontLegMuscles(canvas, false);
    }

    private void drawFrontLegMuscles(Canvas canvas, boolean left) {
        float leftX = left ? .202f : .292f;
        float rightX = left ? .268f : .358f;
        Path quad = new Path();
        quad.moveTo(x(leftX), y(.58f));
        quad.quadTo(x((leftX + rightX) / 2f), y(.555f), x(rightX), y(.58f));
        quad.lineTo(x(rightX - .008f), y(.735f));
        quad.quadTo(x((leftX + rightX) / 2f), y(.77f), x(leftX + .008f), y(.735f));
        quad.close();
        drawMusclePath(canvas, "Jambes", quad);

        Path shin = new Path();
        shin.moveTo(x(leftX + .012f), y(.75f));
        shin.quadTo(x((leftX + rightX) / 2f), y(.725f), x(rightX - .012f), y(.75f));
        shin.lineTo(x(rightX - .018f), y(.89f));
        shin.lineTo(x(leftX + .018f), y(.89f));
        shin.close();
        drawMusclePath(canvas, "Jambes", shin);
    }

    private void drawBackMuscles(Canvas canvas) {
        drawMuscleOval(canvas, "Bras", .615f, .235f, .67f, .31f);
        drawMuscleOval(canvas, "Bras", .77f, .235f, .825f, .31f);

        Path trapezius = new Path();
        trapezius.moveTo(x(.72f), y(.205f));
        trapezius.lineTo(x(.79f), y(.275f));
        trapezius.lineTo(x(.75f), y(.32f));
        trapezius.lineTo(x(.72f), y(.37f));
        trapezius.lineTo(x(.69f), y(.32f));
        trapezius.lineTo(x(.65f), y(.275f));
        trapezius.close();
        drawMusclePath(canvas, "Dos", trapezius);
        drawMusclePath(canvas, "Dos",
                curvedQuad(.64f, .285f, .715f, .32f, .715f, .455f, .655f, .42f));
        drawMusclePath(canvas, "Dos",
                curvedQuad(.725f, .32f, .80f, .285f, .785f, .42f, .725f, .455f));
        drawMuscleRoundRect(canvas, "Dos", .685f, .39f, .755f, .505f, 10 * density);
        drawAnatomyLine(canvas, .72f, .225f, .72f, .50f);
        drawMuscleOval(canvas, "Bras", .545f, .31f, .61f, .455f);
        drawMuscleOval(canvas, "Bras", .83f, .31f, .895f, .455f);
        drawMuscleOval(canvas, "Jambes", .642f, .51f, .72f, .62f);
        drawMuscleOval(canvas, "Jambes", .72f, .51f, .798f, .62f);
        drawBackLegMuscles(canvas, true);
        drawBackLegMuscles(canvas, false);
    }

    private void drawBackLegMuscles(Canvas canvas, boolean left) {
        float leftX = left ? .642f : .732f;
        float rightX = left ? .708f : .798f;
        Path hamstring = new Path();
        hamstring.moveTo(x(leftX + .004f), y(.615f));
        hamstring.quadTo(x((leftX + rightX) / 2f), y(.59f), x(rightX - .004f), y(.615f));
        hamstring.lineTo(x(rightX - .012f), y(.755f));
        hamstring.quadTo(x((leftX + rightX) / 2f), y(.79f), x(leftX + .012f), y(.755f));
        hamstring.close();
        drawMusclePath(canvas, "Jambes", hamstring);
        drawMuscleOval(canvas, "Jambes", leftX + .008f, .76f, rightX - .008f, .885f);
    }

    private Path curvedQuad(float x1, float y1, float x2, float y2,
                            float x3, float y3, float x4, float y4) {
        Path path = new Path();
        path.moveTo(x(x1), y(y1));
        path.quadTo(x((x1 + x2) / 2f), y(y1 - .012f), x(x2), y(y2));
        path.quadTo(x(x3 + .008f), y((y2 + y3) / 2f), x(x3), y(y3));
        path.quadTo(x((x3 + x4) / 2f), y(y3 + .012f), x(x4), y(y4));
        path.quadTo(x(x4 - .008f), y((y4 + y1) / 2f), x(x1), y(y1));
        path.close();
        return path;
    }

    private void drawBaseOval(Canvas canvas, float left, float top, float right, float bottom,
                              int color) {
        RectF oval = rect(left, top, right, bottom);
        fillBase(color);
        canvas.drawOval(oval, paint);
        outlineBase();
        canvas.drawOval(oval, paint);
    }

    private void drawBaseRoundRect(Canvas canvas, float left, float top, float right, float bottom,
                                   int color) {
        RectF region = rect(left, top, right, bottom);
        fillBase(color);
        canvas.drawRoundRect(region, 8 * density, 8 * density, paint);
        outlineBase();
        canvas.drawRoundRect(region, 8 * density, 8 * density, paint);
    }

    private void drawBasePath(Canvas canvas, Path path, int color) {
        fillBase(color);
        canvas.drawPath(path, paint);
        outlineBase();
        canvas.drawPath(path, paint);
    }

    private void fillBase(int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
    }

    private void outlineBase() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.25f * density);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.muscle_body_outline));
    }

    private void drawMuscleOval(Canvas canvas, String group,
                                float left, float top, float right, float bottom) {
        RectF oval = rect(left, top, right, bottom);
        prepareMuscleFill(group);
        canvas.drawOval(oval, paint);
        prepareMuscleOutline(group);
        canvas.drawOval(oval, paint);
    }

    private void drawMuscleRoundRect(Canvas canvas, String group,
                                     float left, float top, float right, float bottom, float radius) {
        RectF region = rect(left, top, right, bottom);
        prepareMuscleFill(group);
        canvas.drawRoundRect(region, radius, radius, paint);
        prepareMuscleOutline(group);
        canvas.drawRoundRect(region, radius, radius, paint);
    }

    private void drawMusclePath(Canvas canvas, String group, Path path) {
        prepareMuscleFill(group);
        canvas.drawPath(path, paint);
        prepareMuscleOutline(group);
        canvas.drawPath(path, paint);
    }

    private void prepareMuscleFill(String group) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colorFor(group));
    }

    private void prepareMuscleOutline(String group) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(group.equals(selectedMuscle) ? 3.25f * density : 1f * density);
        paint.setColor(ContextCompat.getColor(getContext(), group.equals(selectedMuscle)
                ? R.color.colorPrimary : R.color.muscle_body_detail));
    }

    private void drawAnatomyLine(Canvas canvas, float startX, float startY, float endX, float endY) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f * density);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.muscle_body_detail));
        canvas.drawLine(x(startX), y(startY), x(endX), y(endY), paint);
    }

    private void drawLegend(Canvas canvas) {
        float top = bodyHeight + 10 * density;
        drawLegendItem(canvas, .06f * getWidth(), top,
                R.color.muscle_progress_low, R.string.legend_to_improve);
        drawLegendItem(canvas, .54f * getWidth(), top,
                R.color.muscle_progress_medium, R.string.legend_progressing);
        drawLegendItem(canvas, .06f * getWidth(), top + 27 * density,
                R.color.muscle_progress_high, R.string.legend_mastered);
        drawLegendItem(canvas, .54f * getWidth(), top + 27 * density,
                R.color.muscle_no_data, R.string.legend_no_data);
    }

    private void drawLegendItem(Canvas canvas, float x, float y, int colorResource, int textResource) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), colorResource));
        canvas.drawCircle(x + 6 * density, y + 6 * density, 6 * density, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11,
                getResources().getDisplayMetrics()));
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorOnSurfaceVariant));
        canvas.drawText(getContext().getString(textResource), x + 18 * density,
                y + 10 * density, paint);
    }

    private float x(float normalizedX) { return normalizedX * getWidth(); }
    private float y(float normalizedY) { return normalizedY * bodyHeight; }

    private RectF rect(float left, float top, float right, float bottom) {
        return new RectF(x(left), y(top), x(right), y(bottom));
    }

    private int colorFor(String group) {
        MuscleStatistic statistic = statistics.get(group);
        if (statistic == null || !statistic.hasData()) {
            return ContextCompat.getColor(getContext(), R.color.muscle_no_data);
        }
        int score = statistic.getAverageScore();
        if (score < 40) return ContextCompat.getColor(getContext(), R.color.muscle_progress_low);
        if (score < 70) return ContextCompat.getColor(getContext(), R.color.muscle_progress_medium);
        return ContextCompat.getColor(getContext(), R.color.muscle_progress_high);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP || bodyHeight <= 0) return true;
        String group = MuscleBodyGeometry.groupAt(
                event.getX() / getWidth(), event.getY() / bodyHeight);
        if (group != null) {
            performClick();
            if (listener != null) listener.onMuscleSelected(group);
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateAccessibilityDescription() {
        StringBuilder description = new StringBuilder(
                getContext().getString(R.string.muscle_body_description));
        for (String group : MuscleGroups.ALL) {
            MuscleStatistic statistic = statistics.get(group);
            description.append(". ");
            if (statistic == null || !statistic.hasData()) {
                description.append(getContext().getString(
                        R.string.muscle_accessibility_no_data, group));
            } else {
                description.append(getContext().getString(
                        R.string.muscle_accessibility_score, group, statistic.getAverageScore()));
            }
        }
        setContentDescription(description.toString());
    }
}

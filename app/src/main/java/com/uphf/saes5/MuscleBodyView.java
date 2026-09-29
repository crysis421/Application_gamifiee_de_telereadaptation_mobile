package com.uphf.saes5;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
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

    public MuscleBodyView(Context context) {
        this(context, null);
    }

    public MuscleBodyView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MuscleBodyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
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
        int desiredWidth = Math.round(320 * density);
        int desiredHeight = Math.round(520 * density);
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        bodyHeight = getHeight() - 78 * density;
        float width = getWidth();

        drawHeadAndNeck(canvas, width);
        drawRoundedGroup(canvas, "Dos", .30f, .16f, .70f, .24f, width);
        drawRoundedGroup(canvas, "Pectoraux", .34f, .245f, .495f, .36f, width);
        drawRoundedGroup(canvas, "Pectoraux", .505f, .245f, .66f, .36f, width);
        drawRoundedGroup(canvas, "Bras", .16f, .23f, .33f, .56f, width);
        drawRoundedGroup(canvas, "Bras", .67f, .23f, .84f, .56f, width);
        drawRoundedGroup(canvas, "Tronc", .36f, .36f, .64f, .59f, width);
        drawLeg(canvas, true, width);
        drawLeg(canvas, false, width);
        drawBodyDetails(canvas, width);
        drawLegend(canvas);
    }

    private void drawHeadAndNeck(Canvas canvas, float width) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.muscle_no_data));
        canvas.drawOval(rect(.42f, .02f, .58f, .15f, width), paint);
        canvas.drawRoundRect(rect(.46f, .135f, .54f, .18f, width),
                8 * density, 8 * density, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.muscle_body_outline));
        canvas.drawOval(rect(.42f, .02f, .58f, .15f, width), paint);
    }

    private void drawRoundedGroup(Canvas canvas, String group, float left, float top,
                                  float right, float bottom, float width) {
        RectF region = rect(left, top, right, bottom, width);
        float radius = 14 * density;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colorFor(group));
        canvas.drawRoundRect(region, radius, radius, paint);
        if (group.equals(selectedMuscle)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4 * density);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            canvas.drawRoundRect(region, radius, radius, paint);
        }
    }

    private void drawLeg(Canvas canvas, boolean left, float width) {
        float innerTop = left ? .49f : .51f;
        float innerBottom = left ? .48f : .52f;
        float outerTop = left ? .35f : .65f;
        float outerBottom = left ? .39f : .61f;
        Path path = new Path();
        path.moveTo(outerTop * width, .58f * bodyHeight);
        path.lineTo(innerTop * width, .58f * bodyHeight);
        path.lineTo(innerBottom * width, .94f * bodyHeight);
        path.lineTo(outerBottom * width, .94f * bodyHeight);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colorFor("Jambes"));
        canvas.drawPath(path, paint);
        if ("Jambes".equals(selectedMuscle)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4 * density);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            canvas.drawPath(path, paint);
        }
    }

    private void drawBodyDetails(Canvas canvas, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.25f * density);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.muscle_body_detail));
        canvas.drawLine(.50f * width, .25f * bodyHeight, .50f * width, .57f * bodyHeight, paint);
        canvas.drawLine(.37f * width, .43f * bodyHeight, .63f * width, .43f * bodyHeight, paint);
        canvas.drawLine(.37f * width, .50f * bodyHeight, .63f * width, .50f * bodyHeight, paint);
        canvas.drawLine(.37f * width, .57f * bodyHeight, .63f * width, .57f * bodyHeight, paint);
    }

    private void drawLegend(Canvas canvas) {
        float top = bodyHeight + 12 * density;
        drawLegendItem(canvas, .08f * getWidth(), top,
                R.color.muscle_progress_low, R.string.legend_to_improve);
        drawLegendItem(canvas, .55f * getWidth(), top,
                R.color.muscle_progress_medium, R.string.legend_progressing);
        drawLegendItem(canvas, .08f * getWidth(), top + 28 * density,
                R.color.muscle_progress_high, R.string.legend_mastered);
        drawLegendItem(canvas, .55f * getWidth(), top + 28 * density,
                R.color.muscle_no_data, R.string.legend_no_data);
    }

    private void drawLegendItem(Canvas canvas, float x, float y, int colorResource, int textResource) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), colorResource));
        canvas.drawCircle(x + 6 * density, y + 6 * density, 6 * density, paint);
        paint.setTextSize(12 * density);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorOnSurfaceVariant));
        canvas.drawText(getContext().getString(textResource), x + 18 * density, y + 10 * density, paint);
    }

    private RectF rect(float left, float top, float right, float bottom, float width) {
        return new RectF(left * width, top * bodyHeight, right * width, bottom * bodyHeight);
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

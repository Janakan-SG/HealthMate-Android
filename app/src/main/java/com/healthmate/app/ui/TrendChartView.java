package com.healthmate.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class TrendChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Integer> values = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private int target = 8_000;

    public TrendChartView(Context context) {
        super(context);
        setMinimumHeight(Ui.dp(context, 190));
    }

    public void setChartData(List<Integer> newValues, List<String> newLabels, int goal) {
        values.clear();
        values.addAll(newValues);
        labels.clear();
        labels.addAll(newLabels);
        target = Math.max(goal, 1);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.isEmpty()) {
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float left = 6 * density;
        float right = getWidth() - 6 * density;
        float top = 12 * density;
        float labelHeight = 24 * density;
        float bottom = getHeight() - labelHeight;
        float chartHeight = bottom - top;
        int maxValue = target;
        for (Integer value : values) {
            maxValue = Math.max(maxValue, value);
        }
        maxValue = Math.max(maxValue + maxValue / 5, target);

        paint.setColor(0xFFE9EEF7);
        paint.setStrokeWidth(1 * density);
        canvas.drawLine(left, bottom, right, bottom, paint);

        float targetY = bottom - (target / (float) maxValue) * chartHeight;
        paint.setColor(0xFFD7E6E3);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        PathEffect oldEffect = paint.getPathEffect();
        paint.setPathEffect(new DashPathEffect(new float[]{5 * density, 4 * density}, 0));
        canvas.drawLine(left, targetY, right, targetY, paint);
        paint.setPathEffect(oldEffect);
        paint.setStyle(Paint.Style.FILL);

        float slot = (right - left) / values.size();
        float barWidth = Math.min(28 * density, slot * 0.58f);
        for (int i = 0; i < values.size(); i++) {
            float barHeight = (values.get(i) / (float) maxValue) * chartHeight;
            float centerX = left + slot * (i + 0.5f);
            RectF bar = new RectF(centerX - barWidth / 2, bottom - barHeight, centerX + barWidth / 2, bottom);
            paint.setColor(values.get(i) >= target ? Ui.DARK_GREEN : 0xFFD5E3FB);
            canvas.drawRoundRect(bar, 5 * density, 5 * density, paint);

            paint.setColor(0xFF697586);
            paint.setTextSize(10 * density);
            paint.setTextAlign(Paint.Align.CENTER);
            String label = i < labels.size() ? labels.get(i) : "";
            canvas.drawText(label, centerX, getHeight() - 5 * density, paint);
        }
    }
}
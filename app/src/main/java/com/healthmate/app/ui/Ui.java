package com.healthmate.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BACKGROUND = Color.rgb(238, 255, 239);
    public static final int WHITE = Color.WHITE;
    public static final int TEXT = Color.rgb(29, 41, 57);
    public static final int MUTED = Color.rgb(102, 112, 133);
    public static final int GREEN = Color.rgb(0, 126, 6);
    public static final int DARK_GREEN = Color.rgb(8, 120, 92);
    public static final int PALE_GREEN = Color.rgb(225, 247, 240);
    public static final int PALE_BLUE = Color.rgb(237, 243, 255);
    public static final int BORDER = Color.rgb(229, 233, 241);
    public static final int ORANGE = Color.rgb(245, 158, 11);
    public static final int RED = Color.rgb(220, 72, 72);

    private Ui() {
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int color, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radiusDp);
        if (strokeDp > 0) {
            shape.setStroke(strokeDp, strokeColor);
        }
        return shape;
    }

    public static TextView text(Context context, String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setFontFeatureSettings("kern");
        view.setTypeface(android.graphics.Typeface.create("sans-serif", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        return view;
    }

    public static TextView button(Context context, String value, int backgroundColor, int textColor, int radiusDp, boolean bold) {
        TextView view = text(context, value, 12, textColor, bold);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(context, 18), dp(context, 8), dp(context, 18), dp(context, 10));
        view.setBackground(rounded(backgroundColor, dp(context, radiusDp), Color.TRANSPARENT, 2));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    public static EditText input(Context context, String hint, int inputType) {
        EditText field = new EditText(context);
        field.setSingleLine(true);
        field.setTextSize(15);
        field.setTextColor(TEXT);
        field.setHintTextColor(Color.rgb(145, 155, 170));
        field.setHint(hint);
        field.setInputType(inputType);
        field.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        field.setBackground(rounded(WHITE, dp(context, 13), BORDER, dp(context, 1)));
        field.setMinHeight(dp(context, 48));
        return field;
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout horizontal(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    public static LinearLayout card(Context context) {
        LinearLayout layout = vertical(context);
        layout.setPadding(dp(context, 16), dp(context, 20), dp(context, 16), dp(context, 15));
        layout.setBackground(rounded(WHITE, dp(context, 18), BORDER, dp(context, 1)));
        layout.setElevation(dp(context, 1));
        return layout;
    }

    public static View space(Context context, int heightDp) {
        View space = new View(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, heightDp)));
        return space;
    }

    public static void add(LinearLayout parent, View child, int width, int height) {
        parent.addView(child, new LinearLayout.LayoutParams(
                width < 0 ? width : dp(parent.getContext(), width),
                height < 0 ? height : dp(parent.getContext(), height)));
    }

    public static LinearLayout.LayoutParams weighted(Context context, int heightDp, float weight) {
        return new LinearLayout.LayoutParams(0, heightDp < 0 ? heightDp : dp(context, heightDp), weight);
    }

    public static LinearLayout.LayoutParams margins(Context context, int widthDp, int heightDp, int leftDp, int topDp, int rightDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(widthDp < 0 ? widthDp : dp(context, widthDp), heightDp < 0 ? heightDp : dp(context, heightDp));
        params.setMargins(dp(context, leftDp), dp(context, topDp), dp(context, rightDp), dp(context, bottomDp));
        return params;
    }
}
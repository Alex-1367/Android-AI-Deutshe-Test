package com.german.learner.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

public class DynamicButtonAdapter {

    private Context context;

    public DynamicButtonAdapter(Context context) {
        this.context = context;
    }

    /**
     * Create a button with customizable size
     * @param text Button text
     * @param backgroundColor Background color
     * @param textColor Text color
     * @param isLarge true for large button (160px), false for small button (70px)
     * @param clickListener Click listener
     * @return TextView configured as button
     */
    public TextView createButton(String text, int backgroundColor, int textColor,
                                 boolean isLarge, View.OnClickListener clickListener) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setBackgroundColor(backgroundColor);
        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        button.setOnClickListener(clickListener);

        if (isLarge) {
            // Large button (like tags and grammar)
            button.setTextSize(16);
            button.setPadding(8, 4, 8, 4);
            button.setMinWidth(0);
            button.setMinHeight(0);
            button.setHeight(70);
            button.setWidth(160);
        } else {
            // Small button (like importance, clear, close)
            button.setTextSize(12);
            button.setPadding(4, 2, 4, 2);
            button.setMinWidth(0);
            button.setMinHeight(0);
            button.setHeight(70);
            button.setWidth(70);
        }

        return button;
    }

    /**
     * Create a large button (for tags and grammar)
     */
    public TextView createLargeButton(String text, int backgroundColor, int textColor,
                                      View.OnClickListener clickListener) {
        return createButton(text, backgroundColor, textColor, true, clickListener);
    }

    /**
     * Create a small button (for importance, clear, close)
     */
    public TextView createSmallButton(String text, int backgroundColor, int textColor,
                                      View.OnClickListener clickListener) {
        return createButton(text, backgroundColor, textColor, false, clickListener);
    }
}
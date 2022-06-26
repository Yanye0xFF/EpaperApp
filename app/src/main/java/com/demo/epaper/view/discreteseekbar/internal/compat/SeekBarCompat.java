package com.demo.epaper.view.discreteseekbar.internal.compat;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;

import com.demo.epaper.view.discreteseekbar.internal.drawable.MarkerDrawable;

/**
 * Wrapper compatibility class to call some API-Specific methods
 * And offer alternate procedures when possible
 */
public class SeekBarCompat {

    /**
     * Sets the custom Outline provider on API>=21.
     * Does nothing on API<21
     *
     */
    public static void setOutlineProvider(View view, final MarkerDrawable markerDrawable) {
        SeekBarCompatDontCrash.setOutlineProvider(view, markerDrawable);
    }

    /**
     * Our DiscreteSeekBar implementation uses a circular drawable on API < 21
     * because we don't set it as Background, but draw it ourselves
     */
    public static Drawable getRipple(ColorStateList colorStateList) {
        return SeekBarCompatDontCrash.getRipple(colorStateList);
    }

    /**
     * Sets the color of the seekbar ripple
     * @param colorStateList The ColorStateList the track ripple will be changed to
     */
    public static void setRippleColor(@NonNull Drawable drawable, ColorStateList colorStateList) {
        ((RippleDrawable) drawable).setColor(colorStateList);
    }

    /**
     * As our DiscreteSeekBar implementation uses a circular drawable on API < 21
     * we want to use the same method to set its bounds as the Ripple's hotspot bounds.
     */
    public static void setHotspotBounds(Drawable drawable, int left, int top, int right, int bottom) {
        //We don't want the full size rect, Lollipop ripple would be too big
        int size = (right - left) / 8;
        DrawableCompat.setHotspotBounds(drawable, left + size, top + size, right - size, bottom - size);
        drawable.setBounds(left, top, right, bottom);
    }

    /**
     * android.support.v4.view.ViewCompat SHOULD include this once and for all!!
     * But it doesn't...
     */
    public static void setBackground(View view, Drawable background) {
        SeekBarCompatDontCrash.setBackground(view, background);
    }

    /**
     * Sets the TextView text direction attribute when possible
     * @see TextView#setTextDirection(int)
     */
    public static void setTextDirection(TextView textView, int textDirection) {
        SeekBarCompatDontCrash.setTextDirection(textView, textDirection);
    }

    public static boolean isInScrollingContainer(ViewParent p) {
        return SeekBarCompatDontCrash.isInScrollingContainer(p);
    }

    public static boolean isHardwareAccelerated(View view) {
        return SeekBarCompatDontCrash.isHardwareAccelerated(view);
    }
}

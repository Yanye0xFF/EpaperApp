package com.demo.epaper.view.discreteseekbar.internal.compat;

public abstract class AnimatorCompat {
    @FunctionalInterface
    public interface AnimationFrameUpdateListener {
        void onAnimationFrame(float currentValue);
    }

    AnimatorCompat() {
    }

    public abstract void cancel();

    public abstract boolean isRunning();

    public abstract void setDuration(int progressAnimationDuration);

    public abstract void start();

    public static AnimatorCompat create(float start, float end, AnimationFrameUpdateListener listener) {
        return new AnimatorCompatV11(start, end, listener);
    }
}

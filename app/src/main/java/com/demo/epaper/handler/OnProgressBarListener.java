package com.demo.epaper.handler;

@FunctionalInterface
public interface OnProgressBarListener {
    void onProgressChange(int current, int max);
}

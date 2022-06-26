package com.demo.epaper.imageio;

public class ImageDither {

    public static int COLOR_FILTER_BW = 1;
    public static int COLOR_FILTER_RW = 2;
    public static int COLOR_FILTER_BWR = 3;

    static {
        System.loadLibrary("dither");
    }

    public native void setColorFilter(int filter);

    public native void ostuBinary(int[] input, int width, int height, int[] output);

    public native void gaussianBinary(int[] input, int width, int height, int[] output);

    public native void floydSteinbergDither(int[] input, int width, int height, int[] output);

    public native void bayerDither(int[] input, int width, int height, int[] output);

}

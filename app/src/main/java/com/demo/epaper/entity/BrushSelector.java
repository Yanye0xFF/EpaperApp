package com.demo.epaper.entity;

public class BrushSelector {
    private boolean check;
    private final int outColor;
    private int dotColor;
    private int type;

    public BrushSelector(int outColor, int dotColor, int type, boolean check) {
        this.outColor = outColor;
        this.dotColor = dotColor;
        this.type = type;
        this.check = check;
    }
    public void setType(int type) {
        this.type = type;
    }

    public void setCheck(boolean check) {
        this.check = check;
    }

    public void setDotColor(int dotColor) {
        this.dotColor = dotColor;
    }

    public boolean isCheck() {
        return check;
    }

    public int getOutColor() {
        return outColor;
    }

    public int getDotColor() {
        return dotColor;
    }

    public int getType() {
        return type;
    }
}

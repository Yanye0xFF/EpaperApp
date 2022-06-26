package com.demo.epaper.handler;

@FunctionalInterface
public interface DialogClickListener {
    int BUTTON_LEFT = 0;
    int BUTTON_RIGHT = 1;
    void onDialogButtonClicked(int which, int tag, String str);
}

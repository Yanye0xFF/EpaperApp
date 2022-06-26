package com.demo.epaper.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class QToast {

    private final Toast toast;
    private final TextView message;
    private final GradientDrawable gradient;

    public static final int COLOR_GREEN = 0xFF45b97c;
    public static final int COLOR_ORANGE = 0xFFf58220;
    public static final int COLOR_CYAN = 0xFF50b7c1;

    public QToast(Context context) {
        gradient = new GradientDrawable();
        gradient.setColor(0xF0808080);
        gradient.setCornerRadius(20);
        message = new TextView(context);
        message.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        message.setBackground(gradient);
        message.setTextSize(14);
        message.setMaxLines(2);
        message.setEllipsize(TextUtils.TruncateAt.END);
        message.setPadding(20,20,20,20);
        message.setTextColor(0xFFFFFFFF);
        toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 300);
        toast.setView(message);
        toast.cancel();
    }

    public void showMessage(String msg){
        gradient.setColor(0xF0808080);
        message.setText(msg);
        toast.show();
    }

    public void showMessage(String msg, int color){
        gradient.setColor(color);
        message.setText(msg);
        toast.show();
    }

    public void cancel() {
        toast.cancel();
    }
}

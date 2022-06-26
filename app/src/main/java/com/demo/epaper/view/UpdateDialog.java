package com.demo.epaper.view;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.demo.epaper.R;
import com.demo.epaper.handler.DialogClickListener;

import java.util.Locale;

public class UpdateDialog extends AppCompatDialog {
    private final ProgressBar progressBar;
    private final TextView tvProgress;
    private String prefix;
    private DialogClickListener clickListener;

    public UpdateDialog(Context context) {
        this(context, 0);
    }

    public UpdateDialog(Context context, int theme) {
        super(context, theme == 0 ? R.style.MyDialogStyle : theme);
        View rootView = View.inflate(context, R.layout.view_update_dialog, null);
        this.setContentView(rootView);
        this.setCanceledOnTouchOutside(false);

        progressBar = rootView.findViewById(R.id.progressBar);
        tvProgress = rootView.findViewById(R.id.tv_update_progress);

        TextView tvCancel = rootView.findViewById(R.id.tv_update_cancel);
        tvCancel.setOnClickListener((View view) -> {
            this.dismiss();
            if(this.clickListener != null) {
                this.clickListener.onDialogButtonClicked(DialogClickListener.BUTTON_RIGHT, 0, null);
            }
        });

        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        int actualWidth = Math.min(dm.widthPixels, dm.heightPixels);

        ConstraintLayout parent = rootView.findViewById(R.id.layout_update);
        parent.setLayoutParams(new FrameLayout.LayoutParams(
                (int)(actualWidth * 0.85f), LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    public void resetView() {
        progressBar.setProgress(0);
        tvProgress.setText("");
        this.prefix = null;
    }

    public void setProgress(int progress) {
        progressBar.setProgress(progress);
        tvProgress.setText(String.format(Locale.CHINA,"%s: %d%%", prefix, progress));
    }

    public void setProgressPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setDialogClickListener(DialogClickListener listener) {
        this.clickListener = listener;
    }
}

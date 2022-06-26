package com.demo.epaper.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.demo.epaper.R;
import com.demo.epaper.view.AVLoadingIndicator.AVLoadingIndicatorView;

public class TransferDialog extends Dialog {

    private TextView tvTitle;
    private AVLoadingIndicatorView indicatorView;
    private NumberProgressBar progressBar;

    public TransferDialog(@NonNull Context context) {
        this(context, 0);
    }

    public TransferDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId == 0 ? R.style.MyDialogStyle : themeResId);
        View view = View.inflate(context, R.layout.view_transfer_dialog, null);
        this.setContentView(view);
        this.setCanceledOnTouchOutside(false);

        initView(view);

        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        int actualWidth = Math.min(dm.widthPixels, dm.heightPixels);

        LinearLayout progressLayout = view.findViewById(R.id.layout_progress);
        progressLayout.setLayoutParams(new FrameLayout.LayoutParams((int)(actualWidth * 0.85f),
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void initView(View parentView) {
        tvTitle = parentView.findViewById(R.id.transfer_title);
        indicatorView = parentView.findViewById(R.id.transfer_indicator);
        progressBar = parentView.findViewById(R.id.transfer_progress);
    }

    @Override
    public void setTitle(@Nullable CharSequence title) {
        tvTitle.setText(title);
    }

    public void setProgress(int progress) {
        this.progressBar.setProgress(progress);
    }

    @Override
    public void show() {
        super.show();
        indicatorView.smoothToShow();
    }

    @Override
    public void dismiss() {
        indicatorView.hide();
        super.dismiss();
    }
}

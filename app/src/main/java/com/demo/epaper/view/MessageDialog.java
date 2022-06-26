package com.demo.epaper.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.demo.epaper.R;
import com.demo.epaper.handler.DialogClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MessageDialog extends Dialog implements View.OnClickListener {

    private final View view;

    private LinearLayout parentLayout;
    private TextView tvTitle,tvMessage,tvSave,tvCancel;

    private int tag;
    private DialogClickListener listener;

    public MessageDialog(@NonNull Context context) {
        this(context, 0);
    }

    private MessageDialog(Context context, int theme) {
        super(context, theme == 0 ? R.style.MyDialogStyle : theme);
        view = View.inflate(context, R.layout.view_message_dialog, null);
        this.setContentView(view);
        this.setCanceledOnTouchOutside(true);

        initView();

        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        int actualWidth = Math.min(dm.widthPixels, dm.heightPixels);

        parentLayout.setLayoutParams(new FrameLayout.LayoutParams((int)(actualWidth * 0.85f),
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void initView() {
        parentLayout = view.findViewById(R.id.parent_layout);
        tvTitle = view.findViewById(R.id.tv_dialog_title);
        tvMessage = view.findViewById(R.id.tv_dialog_message);
        tvSave = view.findViewById(R.id.tv_confirm);
        tvCancel = view.findViewById(R.id.tv_cancel);
    }

    @Override
    public void setTitle(@Nullable CharSequence title) {
        tvTitle.setText(title);
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return tag;
    }

    public void setMessage(String msg) {
        tvMessage.setText(msg);
    }

    public void setButtonText(String lText,String rText) {
        if(lText != null) {
            tvSave.setText(lText);
            tvSave.setOnClickListener(this);
        }
        if(rText != null) {
            tvCancel.setText(rText);
            tvCancel.setOnClickListener(this);
        }
    }

    public void setDialogClickListener(DialogClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View v) {
        this.dismiss();
        if(listener == null) {
            return;
        }
        if(v.getId() == R.id.tv_confirm) {
            listener.onDialogButtonClicked(DialogClickListener.BUTTON_LEFT, tag, null);

        }else if(v.getId() == R.id.tv_cancel) {
            listener.onDialogButtonClicked(DialogClickListener.BUTTON_RIGHT, tag, null);
        }
    }
}

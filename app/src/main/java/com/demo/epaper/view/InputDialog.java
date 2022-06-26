package com.demo.epaper.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.demo.epaper.R;
import com.demo.epaper.handler.DialogClickListener;

public class InputDialog extends Dialog implements View.OnClickListener{

    private TextView tvTitle, tvMessage;
    private EditText edInput;

    private int tag;
    private DialogClickListener listener;

    public InputDialog(@NonNull Context context) {
        this(context, 0);
    }

    public InputDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId == 0 ? R.style.MyDialogStyle : themeResId);

        View view = View.inflate(context, R.layout.view_input_dialog, null);

        this.setContentView(view);

        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        int actualWidth = Math.min(dm.widthPixels, dm.heightPixels);

        LinearLayout dialogLayout = view.findViewById(R.id.dialog_layout);
        dialogLayout.setLayoutParams(new FrameLayout.LayoutParams(
                (int) (actualWidth * 0.85f), LinearLayout.LayoutParams.WRAP_CONTENT));

        tvTitle = view.findViewById(R.id.tv_input_title);
        tvMessage = view.findViewById(R.id.tv_input_message);
        edInput = view.findViewById(R.id.ed_input);

        TextView tvConfirm = view.findViewById(R.id.tv_confirm);
        TextView tvCancel = view.findViewById(R.id.tv_cancel);

        tvConfirm.setOnClickListener(this);
        tvCancel.setOnClickListener(this);
    }

    @Override
    public void setTitle(@Nullable CharSequence title) {
        if(title != null) {
            this.tvTitle.setText(title.toString());
        }
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return tag;
    }

    public void setMessage(String message) {
        if(TextUtils.isEmpty(message)) {
            this.tvMessage.setVisibility(View.GONE);
        }else {
            this.tvMessage.setText(message);
        }
    }

    public void setEditTextHint(String hint) {
        this.edInput.setHint(hint);
    }

    public void setEditTextInput(String text) {
        this.edInput.setText(text);
    }

    public void setDialogClickListener(DialogClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View view) {
        this.dismiss();
        if(listener == null) {
            return;
        }
        String str = edInput.getText().toString();
        if(R.id.tv_confirm == view.getId()) {
            listener.onDialogButtonClicked(DialogClickListener.BUTTON_LEFT, tag, str);
        }else if(R.id.tv_cancel == view.getId()) {
            listener.onDialogButtonClicked(DialogClickListener.BUTTON_RIGHT, tag, str);
        }
    }
}

package com.demo.epaper.view;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.demo.epaper.R;


public class LoadingDialog {

    private final Dialog loadDialog;
    private final TextView tvMessage;

    public LoadingDialog(Context context){

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_loading_dialog, null);

        LinearLayout layout = view.findViewById(R.id.dialog_loading_view);
        tvMessage = view.findViewById(R.id.tv_loading_message);

        loadDialog = new Dialog(context, R.style.MyDialogStyle);

        loadDialog.setCancelable(true);
        loadDialog.setCanceledOnTouchOutside(false);

        loadDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
    }

    public void showMessage(String msg) {
        if(loadDialog != null){
            tvMessage.setText(msg);
            loadDialog.show();
        }
    }

    public void updateMessage(String msg) {
        tvMessage.setText(msg);
    }

    public void dismiss() {
        if (loadDialog != null && loadDialog.isShowing()) {
            loadDialog.dismiss();
        }
    }

    public boolean isShowing() {
        return loadDialog.isShowing();
    }
}
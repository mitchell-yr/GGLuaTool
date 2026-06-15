package mituran.gglua.tool.util;

import android.app.Activity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 替代已过时的 ProgressDialog，提供 Material 风格的加载对话框。
 */
public class LoadingDialog {

    private AlertDialog dialog;
    private TextView tvMessage;
    private final Activity activity;

    public LoadingDialog(Activity activity) {
        this.activity = activity;
        build();
    }

    private void build() {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(60, 50, 60, 50);

        ProgressBar progressBar = new ProgressBar(activity);
        progressBar.setIndeterminate(true);
        int pbSize = (int) (activity.getResources().getDisplayMetrics().density * 28);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(pbSize, pbSize);
        progressBar.setLayoutParams(pbParams);
        layout.addView(progressBar);

        tvMessage = new TextView(activity);
        tvMessage.setTextSize(16);
        tvMessage.setPadding(40, 0, 0, 0);
        // 使用主题文字颜色，确保日间/夜间模式都可见
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        tvMessage.setTextColor(typedValue.data);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tvMessage.setLayoutParams(tvParams);
        layout.addView(tvMessage);

        dialog = new MaterialAlertDialogBuilder(activity)
                .setView(layout)
                .setCancelable(false)
                .create();
    }

    public void setMessage(CharSequence message) {
        if (tvMessage != null) {
            tvMessage.setText(message);
        }
    }

    public void setCancelable(boolean cancelable) {
        if (dialog != null) {
            dialog.setCancelable(cancelable);
        }
    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public void setOnDismissListener(android.content.DialogInterface.OnDismissListener listener) {
        if (dialog != null) {
            dialog.setOnDismissListener(listener);
        }
    }
}

package mituran.gglua.tool;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import mituran.gglua.tool.luaTool.LuaSyntaxChecker;

public class LuaCheckResultDialog extends Dialog {

    private LinearLayout layoutStatusBar;
    private ImageView imgStatusIcon;
    private TextView txtStatusTitle;
    private LinearLayout layoutErrorDetail;
    private TextView txtErrorMessage;
    private TextView txtErrorLine;
    private TextView txtSuccessMessage;
    private Button btnJumpToError;
    private Button btnClose;

    private OnJumpToErrorListener jumpToErrorListener;

    public interface OnJumpToErrorListener {
        void onJumpToError(int line);
    }

    public LuaCheckResultDialog(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_lua_check_result, null);
        setContentView(view);

        // 设置背景透明，圆角生效
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 初始化视图
        layoutStatusBar = view.findViewById(R.id.layoutStatusBar);
        imgStatusIcon = view.findViewById(R.id.imgStatusIcon);
        txtStatusTitle = view.findViewById(R.id.txtStatusTitle);
        layoutErrorDetail = view.findViewById(R.id.layoutErrorDetail);
        txtErrorMessage = view.findViewById(R.id.txtErrorMessage);
        txtErrorLine = view.findViewById(R.id.txtErrorLine);
        txtSuccessMessage = view.findViewById(R.id.txtSuccessMessage);
        btnJumpToError = view.findViewById(R.id.btnJumpToError);
        btnClose = view.findViewById(R.id.btnClose);

        // 关闭按钮
        btnClose.setOnClickListener(v -> dismiss());
    }

    /**
     * 显示检查结果
     */
    public void showResult(LuaSyntaxChecker.CheckResult result) {
        if (result.isValid()) {
            showSuccess();
        } else {
            showError(result);
        }
        show();
    }

    /**
     * 显示成功状态
     */
    private void showSuccess() {
        // 设置顶部状态栏为绿色
        layoutStatusBar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.success_green));
        imgStatusIcon.setImageResource(R.drawable.ic_check_circle);
        txtStatusTitle.setText("语法正确");

        // 显示成功消息
        txtSuccessMessage.setVisibility(View.VISIBLE);
        layoutErrorDetail.setVisibility(View.GONE);
        btnJumpToError.setVisibility(View.GONE);
    }

    /**
     * 显示错误状态
     */
    private void showError(LuaSyntaxChecker.CheckResult result) {
        // 设置顶部状态栏为红色
        layoutStatusBar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.error_red));
        imgStatusIcon.setImageResource(R.drawable.ic_error_circle);
        txtStatusTitle.setText("语法错误");

        // 显示错误详情
        txtSuccessMessage.setVisibility(View.GONE);
        layoutErrorDetail.setVisibility(View.VISIBLE);

        txtErrorMessage.setText(result.getErrorMessage());

        if (result.getErrorLine() > 0) {
            txtErrorLine.setText("位置: 第 " + result.getErrorLine() + " 行");
            txtErrorLine.setVisibility(View.VISIBLE);
            btnJumpToError.setVisibility(View.VISIBLE);

            // 跳转按钮点击事件
            btnJumpToError.setOnClickListener(v -> {
                if (jumpToErrorListener != null) {
                    jumpToErrorListener.onJumpToError(result.getErrorLine());
                    dismiss();
                }
            });
        } else {
            txtErrorLine.setVisibility(View.GONE);
            btnJumpToError.setVisibility(View.GONE);
        }
    }

    /**
     * 设置跳转监听器
     */
    public void setOnJumpToErrorListener(OnJumpToErrorListener listener) {
        this.jumpToErrorListener = listener;
    }
}
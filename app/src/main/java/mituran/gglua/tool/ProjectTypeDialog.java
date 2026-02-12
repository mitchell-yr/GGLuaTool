package mituran.gglua.tool;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ProjectTypeDialog {

    public interface OnProjectCreatedListener {
        void onProjectCreated(String projectName, ProjectType type);

        boolean isProjectNameExists2(String projectName);
    }

    public enum ProjectType {
        STANDARD_LUA("标准lua项目"),
        STANDARD_CPP("标准cpp项目"),
        VISUAL_LUA("可视化lua项目");

        private String displayName;

        ProjectType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 显示项目名称输入对话框（第一步）
     */
    public static void showProjectNameDialog(Context context,
                                             OnProjectCreatedListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_create_project_name, null);
        dialog.setContentView(view);

        // 获取控件
        TextInputLayout tilProjectName = view.findViewById(R.id.til_project_name);
        TextInputEditText etProjectName = view.findViewById(R.id.et_project_name);
        TextView tvError = view.findViewById(R.id.tv_error);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnNext = view.findViewById(R.id.btn_next);

        // 监听输入变化，清除错误提示
        etProjectName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvError.setVisibility(View.GONE);
                tilProjectName.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // 取消按钮
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 下一步按钮
        btnNext.setOnClickListener(v -> {
            String projectName = etProjectName.getText().toString().trim();

            // 验证项目名称
            if (TextUtils.isEmpty(projectName)) {
                tilProjectName.setError("项目名称不能为空");
                tvError.setText("项目名称不能为空");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            // 检查名称是否重复
            if (listener != null && listener.isProjectNameExists2(projectName)) {
                tilProjectName.setError("该项目名称已存在");
                tvError.setText("该项目名称已存在，请使用其他名称");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            // 验证通过，关闭当前对话框并显示项目类型选择对话框
            dialog.dismiss();
            showProjectTypeDialog(context, projectName, listener);
        });

        dialog.show();

        // 设置对话框宽度
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                    -2
            );
        }

        // 自动弹出键盘
        etProjectName.requestFocus();
    }

    /**
     * 显示项目类型选择对话框（第二步）
     */
    private static void showProjectTypeDialog(Context context,
                                              String projectName,
                                              OnProjectCreatedListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_create_project_type, null);
        dialog.setContentView(view);

        // 获取控件
        ImageView ivHelp = view.findViewById(R.id.iv_help);
        RadioGroup rgProjectType = view.findViewById(R.id.rg_project_type);
        RadioButton rbStandardLua = view.findViewById(R.id.rb_standard_lua);
        RadioButton rbStandardCpp = view.findViewById(R.id.rb_standard_cpp);
        RadioButton rbVisualLua = view.findViewById(R.id.rb_visual_lua);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        // 帮助按钮点击事件
        ivHelp.setOnClickListener(v -> showHelpDialog(context));

        // 取消按钮 - 返回到项目名称输入对话框
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            showProjectNameDialog(context, listener);
        });

        // 确定按钮
        btnConfirm.setOnClickListener(v -> {
            ProjectType selectedType = null;
            int checkedId = rgProjectType.getCheckedRadioButtonId();

            if (checkedId == R.id.rb_standard_lua) {
                selectedType = ProjectType.STANDARD_LUA;
            } else if (checkedId == R.id.rb_standard_cpp) {
                selectedType = ProjectType.STANDARD_CPP;
            } else if (checkedId == R.id.rb_visual_lua) {
                selectedType = ProjectType.VISUAL_LUA;
            }

            if (listener != null && selectedType != null) {
                listener.onProjectCreated(projectName, selectedType);
            }
            dialog.dismiss();
        });

        dialog.show();

        // 设置对话框宽度
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                    -2
            );
        }
    }

    /**
     * 显示帮助信息对话框
     */
    private static void showHelpDialog(Context context) {
        Dialog helpDialog = new Dialog(context);
        helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_create_project_type_help, null);
        helpDialog.setContentView(view);

        Button btnClose = view.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> helpDialog.dismiss());

        helpDialog.show();

        // 设置对话框宽度
        if (helpDialog.getWindow() != null) {
            helpDialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                    -2
            );
        }
    }
}
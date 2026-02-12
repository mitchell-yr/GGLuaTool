package mituran.gglua.tool;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.card.MaterialCardView;

public class EncryptionOptionsDialog extends Dialog {

    private Context context;
    private OnExportListener listener;
    private SwitchMaterial switchEncryption;
    private LinearLayout optionsContainer;
    private LinearLayout optionsList;
    private TextInputEditText editFilename;
    private MaterialButton btnCancel, btnExport;

    private CheckBox[] checkBoxes;
    private boolean isPackageExport; // 是否是导出包

    public interface OnExportListener {
        void onExport(String filename, boolean encrypt, boolean[] encryptionOptions);
    }

    public EncryptionOptionsDialog(Context context, boolean isPackageExport) {
        super(context);
        this.context = context;
        this.isPackageExport = isPackageExport;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_encryption_options);

        // 设置对话框背景透明
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        initViews();
        setupListeners();
        createEncryptionOptions();
    }

    private void initViews() {
        switchEncryption = findViewById(R.id.switch_enable_encryption);
        optionsContainer = findViewById(R.id.encryption_options_container);
        optionsList = findViewById(R.id.encryption_options_list);
        editFilename = findViewById(R.id.edit_output_filename);
        btnCancel = findViewById(R.id.btn_cancel);
        btnExport = findViewById(R.id.btn_export);

        // 设置默认文件名
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());

        if (isPackageExport) {
            editFilename.setText("output_" + timestamp + ".zip");
        } else {
            editFilename.setText("compiled_" + timestamp + ".luac");
        }
    }

    private void setupListeners() {
        // 加密开关监听
        switchEncryption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleEncryptionOptions(isChecked);
        });

        // 取消按钮
        btnCancel.setOnClickListener(v -> dismiss());

        // 导出按钮
        btnExport.setOnClickListener(v -> {
            String filename = editFilename.getText().toString().trim();
            if (filename.isEmpty()) {
                editFilename.setError("请输入文件名");
                return;
            }

            boolean encrypt = switchEncryption.isChecked();
            boolean[] options = new boolean[checkBoxes.length];

            if (encrypt) {
                for (int i = 0; i < checkBoxes.length; i++) {
                    options[i] = checkBoxes[i].isChecked();
                }
            }

            if (listener != null) {
                listener.onExport(filename, encrypt, options);
            }
            dismiss();
        });
    }

    private void createEncryptionOptions() {
        LuaEncryptionModule.EncryptionOption[] options =
                LuaEncryptionModule.EncryptionOption.values();

        checkBoxes = new CheckBox[options.length];

        for (int i = 0; i < options.length; i++) {
            View optionView = createOptionView(options[i], i);
            optionsList.addView(optionView);
        }
    }

    private View createOptionView(LuaEncryptionModule.EncryptionOption option, int index) {
        // 创建卡片容器
        MaterialCardView card = new MaterialCardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);
        card.setCardElevation(2);
        card.setRadius(8);
        card.setStrokeColor(Color.parseColor("#E0E0E0"));
        card.setStrokeWidth(1);
        card.setCheckable(true);

        // 创建内部布局
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // 复选框
        CheckBox checkBox = new CheckBox(context);
        checkBox.setButtonTintList(context.getColorStateList(R.color.purple_500));
        checkBoxes[index] = checkBox;

        // 文本容器
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        textParams.setMargins(16, 0, 0, 0);
        textLayout.setLayoutParams(textParams);

        // 标题
        TextView title = new TextView(context);
        title.setText(option.getName());
        title.setTextSize(14);
        title.setTextColor(Color.parseColor("#333333"));

        // 描述
        TextView description = new TextView(context);
        description.setText(option.getDescription());
        description.setTextSize(12);
        description.setTextColor(Color.parseColor("#999999"));

        textLayout.addView(title);
        textLayout.addView(description);

        layout.addView(checkBox);
        layout.addView(textLayout);

        card.addView(layout);

        // 点击卡片切换复选框
        card.setOnClickListener(v -> {
            checkBox.setChecked(!checkBox.isChecked());
        });

//        // 根据加密类型设置默认选中
//        if (index == 0 || index == 1 || index == 2) { // 默认选中前三个
//            checkBox.setChecked(true);
//        }
        if (index == 1 ) {
            checkBox.setChecked(true);
        }

        return card;
    }

    private void toggleEncryptionOptions(boolean show) {
        if (show) {
            optionsContainer.setVisibility(View.VISIBLE);
            // 添加展开动画
            Animation slideDown = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            optionsContainer.startAnimation(slideDown);
        } else {
            // 添加收起动画
            Animation slideUp = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
            optionsContainer.startAnimation(slideUp);
            optionsContainer.setVisibility(View.GONE);
        }
    }

    public void setOnExportListener(OnExportListener listener) {
        this.listener = listener;
    }
}
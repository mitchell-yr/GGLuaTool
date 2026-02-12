package mituran.gglua.tool.apktools;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import mituran.gglua.tool.R;
import mituran.gglua.tool.apktools.apksign.KeyStoreGenerator;
import mituran.gglua.tool.apktools.apksign.SignatureConfig;

public class ModifierActivity extends AppCompatActivity {

    // ==================== UI控件 ====================
    private ImageView imgAppIcon;
    private Button btnChangeIcon;
    private TextInputEditText etAppName;
    private TextInputEditText etPackageName;
    private TextInputEditText etVersionName;

    private MaterialCheckBox cbDeleteEntry;
    private RadioGroup rgEntryType;
    private RadioButton rbHwAccel;
    private RadioButton rbSwAccel;

    private MaterialCheckBox cbScript;
    private Button btnSelectScript;
    private MaterialCheckBox cbSlim;
    private LinearLayout layoutSlimOptions;
    private CheckBox cbSlimArsc, cbSlimRes, cbSlimDex;

    private MaterialCheckBox cbAddFunctions;
    private LinearLayout layoutFunctionList;

    private MaterialCheckBox cbUiBeautify;
    private LinearLayout layoutUiBeautifyList;

    private ExtendedFloatingActionButton fabStart;

    // ==================== 数据变量 ====================
    private String apkPath = null;
    private String newIconPath = null;
    private String scriptPath = null;

    // 用于安装完成后重试安装的路径
    private String pendingInstallPath = null;

    private ProgressDialog progressDialog;

    // ==================== 文件选择器 ====================
    private ActivityResultLauncher<Intent> apkPickerLauncher;
    private ActivityResultLauncher<Intent> iconPickerLauncher;
    private ActivityResultLauncher<Intent> scriptPickerLauncher;
    private ActivityResultLauncher<Intent> installPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gg_apk_modify);

        initViews();
        initLaunchers();
        setupListeners();

        showSourceSelectionDialog();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        imgAppIcon = findViewById(R.id.img_app_icon);
        btnChangeIcon = findViewById(R.id.btn_change_icon);
        etAppName = findViewById(R.id.et_app_name);
        etPackageName = findViewById(R.id.et_package_name);
        etVersionName = findViewById(R.id.et_version_name);

        cbDeleteEntry = findViewById(R.id.cb_delete_entry);
        rgEntryType = findViewById(R.id.rg_entry_type);
        rbHwAccel = findViewById(R.id.rb_hw_accel);
        rbSwAccel = findViewById(R.id.rb_sw_accel);

        cbScript = findViewById(R.id.cb_script);
        btnSelectScript = findViewById(R.id.btn_select_script);
        cbSlim = findViewById(R.id.cb_slim);
        layoutSlimOptions = findViewById(R.id.layout_slim_options);
        cbSlimArsc = findViewById(R.id.cb_slim_arsc);
        cbSlimRes = findViewById(R.id.cb_slim_res);
        cbSlimDex = findViewById(R.id.cb_slim_dex);

        cbAddFunctions = findViewById(R.id.cb_add_functions);
        layoutFunctionList = findViewById(R.id.layout_function_list);

        cbUiBeautify = findViewById(R.id.cb_ui_beautify);
        layoutUiBeautifyList = findViewById(R.id.layout_uiBeautify_list);

        fabStart = findViewById(R.id.fab_start);
    }

    private void initLaunchers() {
        apkPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleApkSelected(uri);
                        }
                    } else {
                        showSourceSelectionDialog();
                    }
                }
        );

        iconPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleIconSelected(uri);
                        }
                    }
                }
        );

        scriptPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleScriptSelected(uri);
                        }
                    }
                }
        );

        // ★ 新增：安装未知应用权限回调
        installPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 用户从设置页面返回后，重试安装
                    if (pendingInstallPath != null) {
                        installApk(pendingInstallPath);
                    }
                }
        );
    }

    private void setupListeners() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnChangeIcon.setOnClickListener(v -> selectIcon());

        cbDeleteEntry.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rgEntryType.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && rgEntryType.getCheckedRadioButtonId() == -1) {
                rbHwAccel.setChecked(true);
            }
        });

        cbScript.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnSelectScript.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                scriptPath = null;
                btnSelectScript.setText("选择脚本文件 (.lua)");
            }
        });

        btnSelectScript.setOnClickListener(v -> selectScript());

        cbSlim.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutSlimOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        cbAddFunctions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutFunctionList.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        cbUiBeautify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutUiBeautifyList.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        fabStart.setOnClickListener(v -> startModification());
    }

    // ==================== 来源选择 ====================

    private void showSourceSelectionDialog() {
        String[] options = {"选择已安装应用", "选择本地APK文件"};

        new AlertDialog.Builder(this)
                .setTitle("选择APK来源")
                .setIcon(R.drawable.ic_launcher_foreground)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            selectInstalledApp();
                            break;
                        case 1:
                            selectLocalApk();
                            break;
                    }
                })
                .setCancelable(false)
                .setNegativeButton("取消", (dialog, which) -> finish())
                .show();
    }

    private void selectInstalledApp() {
        Toast.makeText(this, "已安装应用选择功能待实现", Toast.LENGTH_SHORT).show();
        showSourceSelectionDialog();
    }

    public void onInstalledAppSelected(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);

            apkPath = packageInfo.applicationInfo.sourceDir;
            loadApkInfoToUI(packageInfo);

            Toast.makeText(this, "已选择: " + packageName, Toast.LENGTH_SHORT).show();

        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "无法获取应用信息", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void selectLocalApk() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            apkPickerLauncher.launch(Intent.createChooser(intent, "选择APK文件"));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void handleApkSelected(Uri uri) {
        try {
            File cacheDir = new File(getCacheDir(), "apk_temp");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            String fileName = "temp_" + System.currentTimeMillis() + ".apk";
            File destFile = new File(cacheDir, fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                FileOutputStream outputStream = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();

                apkPath = destFile.getAbsolutePath();
                parseAndLoadApkInfo(apkPath);

                Toast.makeText(this, "APK已加载", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "无法读取APK文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            showSourceSelectionDialog();
        }
    }

    private void parseAndLoadApkInfo(String apkPath) {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(apkPath, 0);

            if (packageInfo != null) {
                packageInfo.applicationInfo.sourceDir = apkPath;
                packageInfo.applicationInfo.publicSourceDir = apkPath;
                loadApkInfoToUI(packageInfo);
            } else {
                Toast.makeText(this, "无法解析APK信息", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "解析APK失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadApkInfoToUI(PackageInfo packageInfo) {
        PackageManager pm = getPackageManager();
        try {
            Drawable icon = packageInfo.applicationInfo.loadIcon(pm);
            imgAppIcon.setImageDrawable(icon);

            CharSequence appName = packageInfo.applicationInfo.loadLabel(pm);
            etAppName.setText(appName);

            etPackageName.setText(packageInfo.packageName);
            etVersionName.setText(packageInfo.versionName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 图标选择 ====================

    private void selectIcon() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        iconPickerLauncher.launch(Intent.createChooser(intent, "选择新图标"));
    }

    private void handleIconSelected(Uri uri) {
        try {
            File cacheDir = new File(getCacheDir(), "icon_temp");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            String fileName = "icon_" + System.currentTimeMillis() + ".png";
            File destFile = new File(cacheDir, fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                FileOutputStream outputStream = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();

                newIconPath = destFile.getAbsolutePath();
                imgAppIcon.setImageURI(Uri.fromFile(destFile));

                Toast.makeText(this, "图标已更换", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "图标加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // ==================== 脚本选择 ====================

    private void selectScript() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        scriptPickerLauncher.launch(Intent.createChooser(intent, "选择Lua脚本文件"));
    }

    private void handleScriptSelected(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);

            if (fileName == null || !fileName.toLowerCase().endsWith(".lua")) {
                Toast.makeText(this, "请选择.lua脚本文件", Toast.LENGTH_SHORT).show();
                return;
            }

            File cacheDir = new File(getCacheDir(), "script_temp");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            File destFile = new File(cacheDir, fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                FileOutputStream outputStream = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();

                scriptPath = destFile.getAbsolutePath();
                btnSelectScript.setText("已选择: " + fileName);

                Toast.makeText(this, "脚本已加载", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "脚本加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    // ==================== 开始修改 ====================

    private void startModification() {
        if (apkPath == null || apkPath.isEmpty()) {
            Toast.makeText(this, "请先选择APK文件", Toast.LENGTH_SHORT).show();
            showSourceSelectionDialog();
            return;
        }

        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            Toast.makeText(this, "APK文件不存在", Toast.LENGTH_SHORT).show();
            showSourceSelectionDialog();
            return;
        }

        ModifyOptions options = collectModifyOptions();
        showConfirmDialog(options);
    }

    private ModifyOptions collectModifyOptions() {
        ModifyOptions options = new ModifyOptions();

        options.apkPath = apkPath;

        options.newAppName = getText(etAppName);
        options.newPackageName = getText(etPackageName);
        options.newVersionName = getText(etVersionName);
        options.newIconPath = newIconPath;

        options.deleteEntry = cbDeleteEntry.isChecked();
        if (options.deleteEntry) {
            options.keepHwAccel = rbHwAccel.isChecked();
            options.keepSwAccel = rbSwAccel.isChecked();
        }

        options.embedScript = cbScript.isChecked();
        options.scriptPath = scriptPath;

        options.slimApk = cbSlim.isChecked();
        if (options.slimApk) {
            options.slimArsc = cbSlimArsc.isChecked();
            options.slimRes = cbSlimRes.isChecked();
            options.slimDex = cbSlimDex.isChecked();
        }

        options.addFunctions = cbAddFunctions.isChecked();
        options.uiBeautify = cbUiBeautify.isChecked();

        return options;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showConfirmDialog(ModifyOptions options) {
        StringBuilder summary = new StringBuilder();
        summary.append("APK路径: ").append(options.apkPath).append("\n\n");

        if (!options.newAppName.isEmpty()) {
            summary.append("• 新应用名: ").append(options.newAppName).append("\n");
        }
        if (!options.newPackageName.isEmpty()) {
            summary.append("• 新包名: ").append(options.newPackageName).append("\n");
        }
        if (!options.newVersionName.isEmpty()) {
            summary.append("• 新版本: ").append(options.newVersionName).append("\n");
        }
        if (options.newIconPath != null) {
            summary.append("• 修改图标: 是\n");
        }
        if (options.deleteEntry) {
            summary.append("• 删除多余入口: 是\n");
        }
        if (options.embedScript) {
            summary.append("• 内置脚本: 是\n");
        }
        if (options.slimApk) {
            summary.append("• 精简APK: 是\n");
        }
        if (options.addFunctions) {
            summary.append("• 函数添加: 是\n");
        }
        if (options.uiBeautify) {
            summary.append("• UI美化: 是\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("确认修改")
                .setMessage(summary.toString())
                .setPositiveButton("开始修改", (dialog, which) -> {
                    performModification(options);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void performModification(ModifierActivity.ModifyOptions options) {
        showProgressDialog("正在修改APK...");

        SignatureConfig.initDefaultKeystore(this);

        new Thread(() -> {
            ApkModifier modifier = new ApkModifier(this);
            modifier.modify(options, new ApkModifier.ModifyCallback() {
                @Override
                public void onProgress(String message) {
                    runOnUiThread(() -> updateProgressDialog(message));
                }

                @Override
                public void onSuccess(String outputPath) {
                    runOnUiThread(() -> {
                        dismissProgressDialog();
                        showSuccessDialog(outputPath);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        dismissProgressDialog();
                        showErrorDialog(error);
                    });
                }

                @Override
                public void onSignatureRequired(ApkModifier.SignatureSelectionCallback callback) {
                    runOnUiThread(() -> {
                        updateProgressDialog("等待选择签名方式...");
                        showSignatureSelectionDialog(callback);
                    });
                }
            });
        }).start();
    }

    // ==================== 签名选择 ====================

    private void showSignatureSelectionDialog(ApkModifier.SignatureSelectionCallback callback) {
        SignatureConfig signatureConfig = new SignatureConfig(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择签名方式");
        builder.setCancelable(false);

        String[] options = {"使用默认测试签名", "使用自定义签名", "创建新签名"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    useDefaultSignature(callback);
                    break;
                case 1:
                    selectCustomSignature(signatureConfig, callback);
                    break;
                case 2:
                    createNewSignature(signatureConfig, callback);
                    break;
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> callback.onCancelled());
        builder.setOnCancelListener(dialog -> callback.onCancelled());
        builder.show();
    }

    private void useDefaultSignature(ApkModifier.SignatureSelectionCallback callback) {
        SignatureConfig signatureConfig = new SignatureConfig(this);
        SignatureConfig.Config config = signatureConfig.getDefaultTestConfig();

        if (!SignatureConfig.validateConfig(config)) {
            showToast("默认签名文件不存在，正在重新生成...");
            if (SignatureConfig.initDefaultKeystore(this)) {
                callback.onSignatureSelected(config);
            } else {
                showToast("生成默认签名失败");
                showSignatureSelectionDialog(callback);
            }
            return;
        }

        callback.onSignatureSelected(config);
    }

    private void selectCustomSignature(SignatureConfig signatureConfig,
                                       ApkModifier.SignatureSelectionCallback callback) {
        java.util.List<SignatureConfig.Config> configs = signatureConfig.getAllConfigs();

        if (configs.isEmpty()) {
            showToast("暂无自定义签名，请先创建");
            createNewSignature(signatureConfig, callback);
            return;
        }

        String[] configNames = new String[configs.size()];
        for (int i = 0; i < configs.size(); i++) {
            configNames[i] = configs.get(i).name + " (" + configs.get(i).keyAlias + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择自定义签名");
        builder.setItems(configNames, (dialog, which) -> {
            SignatureConfig.Config config = configs.get(which);

            if (!SignatureConfig.validateConfig(config)) {
                showToast("签名文件无效: " + config.name);
                selectCustomSignature(signatureConfig, callback);
                return;
            }

            callback.onSignatureSelected(config);
        });

        builder.setNegativeButton("返回", (dialog, which) ->
                showSignatureSelectionDialog(callback));

        builder.show();
    }

    private void createNewSignature(SignatureConfig signatureConfig,
                                    ApkModifier.SignatureSelectionCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("创建新签名");

        String[] options = {"选择现有密钥库", "生成新密钥库"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    selectExistingKeystore(signatureConfig, callback);
                    break;
                case 1:
                    generateNewKeystore(signatureConfig, callback);
                    break;
            }
        });

        builder.setNegativeButton("返回", (dialog, which) ->
                showSignatureSelectionDialog(callback));

        builder.show();
    }

    private void selectExistingKeystore(SignatureConfig signatureConfig,
                                        ApkModifier.SignatureSelectionCallback callback) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final android.widget.EditText nameEdit = new android.widget.EditText(this);
        nameEdit.setHint("配置名称");
        layout.addView(nameEdit);

        final android.widget.EditText pathEdit = new android.widget.EditText(this);
        pathEdit.setHint("密钥库路径（如 /sdcard/my.keystore）");
        layout.addView(pathEdit);

        final android.widget.EditText keystorePassEdit = new android.widget.EditText(this);
        keystorePassEdit.setHint("密钥库密码");
        keystorePassEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(keystorePassEdit);

        final android.widget.EditText aliasEdit = new android.widget.EditText(this);
        aliasEdit.setHint("密钥别名");
        layout.addView(aliasEdit);

        final android.widget.EditText keyPassEdit = new android.widget.EditText(this);
        keyPassEdit.setHint("密钥密码");
        keyPassEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(keyPassEdit);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入密钥库信息");
        builder.setView(layout);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String path = pathEdit.getText().toString().trim();
            String keystorePass = keystorePassEdit.getText().toString().trim();
            String alias = aliasEdit.getText().toString().trim();
            String keyPass = keyPassEdit.getText().toString().trim();

            if (name.isEmpty() || path.isEmpty() || keystorePass.isEmpty() ||
                    alias.isEmpty() || keyPass.isEmpty()) {
                showToast("请填写所有字段");
                selectExistingKeystore(signatureConfig, callback);
                return;
            }

            showProgressDialog("验证密钥库...");
            new Thread(() -> {
                boolean valid = KeyStoreGenerator.validateKeyStore(path, keystorePass, alias, keyPass);
                runOnUiThread(() -> {
                    dismissProgressDialog();

                    if (!valid) {
                        showToast("密钥库验证失败，请检查路径和密码");
                        selectExistingKeystore(signatureConfig, callback);
                        return;
                    }

                    SignatureConfig.Config config = new SignatureConfig.Config(
                            name, path, keystorePass, alias, keyPass, false
                    );

                    if (signatureConfig.saveConfig(config)) {
                        showToast("签名配置已保存");
                        callback.onSignatureSelected(config);
                    } else {
                        showToast("保存失败");
                        showSignatureSelectionDialog(callback);
                    }
                });
            }).start();
        });

        builder.setNegativeButton("取消", (dialog, which) ->
                createNewSignature(signatureConfig, callback));

        builder.show();
    }

    private void generateNewKeystore(SignatureConfig signatureConfig,
                                     ApkModifier.SignatureSelectionCallback callback) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final android.widget.EditText nameEdit = new android.widget.EditText(this);
        nameEdit.setHint("配置名称");
        layout.addView(nameEdit);

        final android.widget.EditText filenameEdit = new android.widget.EditText(this);
        filenameEdit.setHint("密钥库文件名（如 my.keystore）");
        layout.addView(filenameEdit);

        final android.widget.EditText keystorePassEdit = new android.widget.EditText(this);
        keystorePassEdit.setHint("密钥库密码（至少6位）");
        keystorePassEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(keystorePassEdit);

        final android.widget.EditText aliasEdit = new android.widget.EditText(this);
        aliasEdit.setHint("密钥别名");
        layout.addView(aliasEdit);

        final android.widget.EditText keyPassEdit = new android.widget.EditText(this);
        keyPassEdit.setHint("密钥密码（至少6位）");
        keyPassEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(keyPassEdit);

        final android.widget.EditText dnEdit = new android.widget.EditText(this);
        dnEdit.setHint("证书DN名称");
        dnEdit.setText("CN=Android,O=MyOrg,C=CN");
        layout.addView(dnEdit);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("生成新密钥库");
        builder.setView(layout);

        builder.setPositiveButton("生成", (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String filename = filenameEdit.getText().toString().trim();
            String keystorePass = keystorePassEdit.getText().toString().trim();
            String alias = aliasEdit.getText().toString().trim();
            String keyPass = keyPassEdit.getText().toString().trim();
            String dn = dnEdit.getText().toString().trim();

            if (name.isEmpty() || filename.isEmpty() || keystorePass.isEmpty() ||
                    alias.isEmpty() || keyPass.isEmpty() || dn.isEmpty()) {
                showToast("请填写所有字段");
                generateNewKeystore(signatureConfig, callback);
                return;
            }

            if (keystorePass.length() < 6 || keyPass.length() < 6) {
                showToast("密码至少需要6位");
                generateNewKeystore(signatureConfig, callback);
                return;
            }

            String keystorePath = "/sdcard/GGtool/keystore/" + filename;

            showProgressDialog("正在生成密钥库，请稍候...");
            new Thread(() -> {
                java.io.File keystoreFile = new java.io.File(keystorePath);
                java.io.File parentDir = keystoreFile.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }

                KeyStoreGenerator generator = new KeyStoreGenerator();
                boolean success = generator.generateKeyStore(keystorePath, keystorePass,
                        alias, keyPass, dn, 25);

                runOnUiThread(() -> {
                    dismissProgressDialog();

                    if (success) {
                        SignatureConfig.Config config = new SignatureConfig.Config(
                                name, keystorePath, keystorePass, alias, keyPass, false
                        );

                        if (signatureConfig.saveConfig(config)) {
                            showToast("密钥库生成成功并已保存配置");
                            callback.onSignatureSelected(config);
                        } else {
                            showToast("密钥库生成成功但保存配置失败");
                            showSignatureSelectionDialog(callback);
                        }
                    } else {
                        showToast("密钥库生成失败");
                        generateNewKeystore(signatureConfig, callback);
                    }
                });
            }).start();
        });

        builder.setNegativeButton("取消", (dialog, which) ->
                createNewSignature(signatureConfig, callback));

        builder.show();
    }

    // ==================== 进度对话框 ====================

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void updateProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    // ==================== ★ 修改部分：成功对话框 + 安装APK + 打开目录 ====================

    /**
     * 显示成功对话框（分享APK → 安装APK）
     */
    private void showSuccessDialog(String outputPath) {
        new AlertDialog.Builder(this)
                .setTitle("修改成功")
                .setMessage("修改后的APK保存在:\n" + outputPath)
                .setPositiveButton("打开目录", (dialog, which) -> {
                    openFileDirectory(outputPath);
                })
                .setNeutralButton("安装APK", (dialog, which) -> {
                    installApk(outputPath);
                })
                .setNegativeButton("完成", (dialog, which) -> finish())
                .show();
    }

    /**
     * 安装APK文件
     * 处理 Android 7.0+ FileProvider 和 Android 8.0+ 未知来源安装权限
     */
    private void installApk(String apkFilePath) {
        try {
            File apkFile = new File(apkFilePath);
            if (!apkFile.exists()) {
                Toast.makeText(this, "APK文件不存在: " + apkFilePath, Toast.LENGTH_SHORT).show();
                return;
            }

            // Android 8.0+ 检查是否允许安装未知来源应用
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    // 保存待安装路径，权限授予后自动重试
                    pendingInstallPath = apkFilePath;

                    new AlertDialog.Builder(this)
                            .setTitle("需要安装权限")
                            .setMessage("请允许本应用安装未知来源应用，授权后将自动安装。")
                            .setPositiveButton("去设置", (dialog, which) -> {
                                Intent settingsIntent = new Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:" + getPackageName()));
                                installPermissionLauncher.launch(settingsIntent);
                            })
                            .setNegativeButton("取消", (dialog, which) -> {
                                pendingInstallPath = null;
                            })
                            .show();
                    return;
                }
            }

            // 清除待安装标记
            pendingInstallPath = null;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 使用 FileProvider
                try {
                    apkUri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", apkFile);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (IllegalArgumentException e) {
                    // FileProvider 未正确配置，使用兼容方案
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    apkUri = Uri.fromFile(apkFile);
                }
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "安装失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * 打开文件所在目录
     * 多种方式尝试，确保兼容各种设备和文件管理器
     */
    private void openFileDirectory(String filePath) {
        File file = new File(filePath);
        File directory = file.getParentFile();

        if (directory == null || !directory.exists()) {
            Toast.makeText(this, "目录不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        String dirPath = directory.getAbsolutePath();

        // 方法1: Android 8.0+ 使用 DocumentsContract 打开系统文件管理器到指定目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // 将绝对路径转为 externalstorage 的相对路径
                String relativePath = dirPath
                        .replaceFirst("^/storage/emulated/0/", "")
                        .replaceFirst("^/sdcard/", "");

                Uri dirUri = DocumentsContract.buildDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:" + relativePath);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(dirUri, "vnd.android.document/directory");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 方法2: 尝试用 file:// URI 打开第三方文件管理器
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + dirPath), "resource/folder");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 方法3: 尝试直接用 ACTION_VIEW 打开目录（部分文件管理器支持）
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + dirPath + "/"), "*/*");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 方法4: 打开系统文件管理器（不指定目录，但至少能打开）
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("content://com.android.externalstorage.documents/root/primary"));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                Toast.makeText(this, "请导航到: " + dirPath, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 最终回退: 复制路径到剪贴板
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("APK目录路径", dirPath);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "无法打开目录，路径已复制到剪贴板:\n" + dirPath, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "文件保存在: " + dirPath, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "文件保存在: " + dirPath, Toast.LENGTH_LONG).show();
        }
    }

    // ==================== 错误对话框 ====================

    private void showErrorDialog(String error) {
        new AlertDialog.Builder(this)
                .setTitle("修改失败")
                .setMessage(error)
                .setPositiveButton("确定", null)
                .show();
    }

    // ==================== 修改选项数据类 ====================

    public static class ModifyOptions {
        public String apkPath;

        public String newAppName;
        public String newPackageName;
        public String newVersionName;
        public String newIconPath;

        public boolean deleteEntry;
        public boolean keepHwAccel;
        public boolean keepSwAccel;

        public boolean embedScript;
        public String scriptPath;

        public boolean slimApk;
        public boolean slimArsc;
        public boolean slimRes;
        public boolean slimDex;

        public boolean addFunctions;

        public boolean uiBeautify;

        @Override
        public String toString() {
            return "ModifyOptions{" +
                    "apkPath='" + apkPath + '\'' +
                    ", newAppName='" + newAppName + '\'' +
                    ", newPackageName='" + newPackageName + '\'' +
                    ", newVersionName='" + newVersionName + '\'' +
                    ", newIconPath='" + newIconPath + '\'' +
                    ", deleteEntry=" + deleteEntry +
                    ", embedScript=" + embedScript +
                    ", slimApk=" + slimApk +
                    ", addFunctions=" + addFunctions +
                    ", uiBeautify=" + uiBeautify +
                    '}';
        }
    }

    // ==================== 生命周期 ====================

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
    }

    private void cleanTempFiles() {
        try {
            File cacheDir = getCacheDir();
            deleteRecursive(new File(cacheDir, "apk_temp"));
            deleteRecursive(new File(cacheDir, "icon_temp"));
            deleteRecursive(new File(cacheDir, "script_temp"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
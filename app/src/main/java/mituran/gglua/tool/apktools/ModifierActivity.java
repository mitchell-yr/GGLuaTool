package mituran.gglua.tool.apktools;

import mituran.gglua.tool.util.LoadingDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.apk.axml.aXMLDecoder;
import com.apk.axml.serializableItems.XMLEntry;

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
    private Button btnAddFunction;
    private TextView tvSelectedFunctionsTitle;
    private TextView tvNoFunctionsHint;
    private LinearLayout layoutSelectedFunctions;

    private MaterialCheckBox cbUiBeautify;
    private LinearLayout layoutUiBeautifyList;

    private Button btnSavePreset;
    private Button btnLoadPreset;

    private ExtendedFloatingActionButton fabStart;

    // ==================== 数据变量 ====================
    private String apkPath = null;
    private String newIconPath = null;
    private String scriptPath = null;

    private String pendingInstallPath = null;

    private LoadingDialog loadingDialog;

    // 已分析到的启动入口信息
    private boolean hasMultipleLauncherEntries = false;
    private String hwLauncherActivityName = null;
    private String swLauncherActivityName = null;

    // ==================== 函数管理数据 ====================

    /** 函数信息 */
    public static class FunctionItem {
        public String name;
        public String description;
        public boolean isBuiltin; // true=内置, false=自定义

        public FunctionItem(String name, String description, boolean isBuiltin) {
            this.name = name;
            this.description = description;
            this.isBuiltin = isBuiltin;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FunctionItem that = (FunctionItem) o;
            return isBuiltin == that.isBuiltin && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + (isBuiltin ? 1 : 0);
        }
    }

    /** 所有可选内置函数 */
    private final List<FunctionItem> builtinFunctions = new ArrayList<>();
    /** 所有可选自定义函数 */
    private final List<FunctionItem> customFunctions = new ArrayList<>();
    /** 已选中的函数集合 */
    private final Set<FunctionItem> selectedFunctions = new LinkedHashSet<>();

    // ==================== 文件选择器 ====================
    private ActivityResultLauncher<Intent> apkPickerLauncher;
    private ActivityResultLauncher<Intent> iconPickerLauncher;
    private ActivityResultLauncher<Intent> scriptPickerLauncher;
    private ActivityResultLauncher<Intent> installPermissionLauncher;
    private ActivityResultLauncher<Intent> funcPackPickerLauncher;
    private ActivityResultLauncher<Intent> presetPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gg_apk_modify);

        initFunctionData();
        initViews();
        initLaunchers();
        setupListeners();

        showSourceSelectionDialog();
    }

    private void initFunctionData() {
        // 内置函数列表（与原来保持一致，按需增删）
        builtinFunctions.add(new FunctionItem("test",      "测试",   true));

        // [修改] 自定义函数：从本地 .ggfunc 文件夹加载，而非硬编码
        loadCustomFunctions();
    }

    /** 从本地加载所有已安装的自定义函数包并刷新 customFunctions 列表 */
    private void loadCustomFunctions() {
        customFunctions.clear();
        List<CustomFunctionPackage.PackageInfo> locals = CustomFunctionPackage.loadAllLocal();
        for (CustomFunctionPackage.PackageInfo p : locals) {
            customFunctions.add(new FunctionItem(p.name, p.description, false));
        }
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
        btnAddFunction = findViewById(R.id.btn_add_function);
        tvSelectedFunctionsTitle = findViewById(R.id.tv_selected_functions_title);
        tvNoFunctionsHint = findViewById(R.id.tv_no_functions_hint);
        layoutSelectedFunctions = findViewById(R.id.layout_selected_functions);

        cbUiBeautify = findViewById(R.id.cb_ui_beautify);
        layoutUiBeautifyList = findViewById(R.id.layout_uiBeautify_list);

        btnSavePreset = findViewById(R.id.btn_save_preset);
        btnLoadPreset = findViewById(R.id.btn_load_preset);

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

        installPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (pendingInstallPath != null) {
                        installApk(pendingInstallPath);
                    }
                }
        );
        funcPackPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) handleFuncPackImport(uri);
                    }
                }
        );

        presetPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) handlePresetImport(uri);
                    }
                }
        );
    }

    private void setupListeners() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnChangeIcon.setOnClickListener(v -> selectIcon());

        cbDeleteEntry.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 只有同时存在 HW 和 SW 两种入口时才显示选择面板
            if (isChecked && hasMultipleLauncherEntries
                    && hwLauncherActivityName != null && swLauncherActivityName != null) {
                rgEntryType.setVisibility(View.VISIBLE);
                if (rgEntryType.getCheckedRadioButtonId() == -1) {
                    rbHwAccel.setChecked(true);
                }
            } else {
                rgEntryType.setVisibility(View.GONE);
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

        // ★ 函数添加 - 勾选展开/收起
        cbAddFunctions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutFunctionList.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                // 取消勾选时清空已选函数
                selectedFunctions.clear();
                refreshSelectedFunctionsList();
            }
        });

        // ★ 添加按钮 - 弹出函数选择弹窗
        btnAddFunction.setOnClickListener(v -> showFunctionPickerDialog());

        cbUiBeautify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutUiBeautifyList.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        fabStart.setOnClickListener(v -> startModification());

        btnSavePreset.setOnClickListener(v -> showSavePresetDialog());
        btnLoadPreset.setOnClickListener(v -> showLoadPresetDialog());
    }

    // ==================== ★ 函数选择弹窗 ====================

    private void showFunctionPickerDialog() {
        android.view.View dialogView = android.view.LayoutInflater.from(this)
                .inflate(R.layout.dialog_gg_add_function_picker, null);

        android.widget.LinearLayout layoutBuiltin = dialogView.findViewById(R.id.layout_builtin_functions);
        android.widget.LinearLayout layoutCustom  = dialogView.findViewById(R.id.layout_custom_functions);
        android.widget.Button btnManage = dialogView.findViewById(R.id.btn_manage_function);
        android.widget.TextView tvNoCustom = dialogView.findViewById(R.id.tv_no_custom_hint);

        btnManage.setText("管理");

        final java.util.Set<FunctionItem> tempSelected = new java.util.LinkedHashSet<>(selectedFunctions);

        // 填充内置函数
        for (FunctionItem item : builtinFunctions) {
            android.widget.CheckBox cb = createFunctionCheckBox(item, tempSelected.contains(item));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) tempSelected.add(item); else tempSelected.remove(item);
            });
            layoutBuiltin.addView(cb);
        }

        // 填充自定义函数
        if (customFunctions.isEmpty()) {
            tvNoCustom.setVisibility(android.view.View.VISIBLE);
        } else {
            tvNoCustom.setVisibility(android.view.View.GONE);
            for (FunctionItem item : customFunctions) {
                android.widget.CheckBox cb = createFunctionCheckBox(item, tempSelected.contains(item));
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) tempSelected.add(item); else tempSelected.remove(item);
                });
                layoutCustom.addView(cb);
            }
        }

        androidx.appcompat.app.AlertDialog[] dialogRef = {null};
        //管理按钮 → 跳转到 FunctionManagerActivity
        btnManage.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            showFunctionManager();
        });


        // 构建弹窗
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("选择要添加的函数")
                .setView(dialogView)
                .setPositiveButton("确定", (d, which) -> {
                    selectedFunctions.clear();
                    selectedFunctions.addAll(tempSelected);
                    refreshSelectedFunctionsList();
                })
                .setNegativeButton("取消", null)
                .create();

        dialogRef[0] = dialog;
        dialog.show();
    }
    /** [新增] 跳转到自定义函数管理界面 */
    private void showFunctionManager() {
        android.content.Intent intent = new android.content.Intent(
                this, FunctionManagerActivity.class);
        startActivity(intent);
        // onResume 中重新加载（见下方 onResume）
    }
    /** [新增] 打开文件选择器，选择 .ggfunc 文件进行导入 */
    private void pickFuncPackFile() {
        android.content.Intent intent = new android.content.Intent(
                android.content.Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        funcPackPickerLauncher.launch(
                android.content.Intent.createChooser(intent, "选择 .ggfunc 函数包"));
    }
    /** [新增] 处理从文件选择器返回的 .ggfunc 文件 */
    private void handleFuncPackImport(android.net.Uri uri) {
        try {
            // 1. 获取文件名并校验扩展名
            String fileName = getFileNameFromUri(uri);
            if (fileName == null || !fileName.endsWith(CustomFunctionPackage.EXTENSION)) {
                Toast.makeText(this,
                        "请选择 " + CustomFunctionPackage.EXTENSION + " 格式的函数包",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. 复制到缓存
            File cacheFile = new File(getCacheDir(),
                    "import_" + System.currentTimeMillis() + CustomFunctionPackage.EXTENSION);
            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(cacheFile)) {
                if (is == null) throw new Exception("无法读取文件");
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            }

            // 3. 解析函数包
            CustomFunctionPackage.PackageInfo info = CustomFunctionPackage.importPackage(cacheFile);
            cacheFile.delete();

            // 4. 检查是否同名已存在
            boolean exists = false;
            for (FunctionItem existing : customFunctions) {
                if (existing.name.equals(info.name)) { exists = true; break; }
            }

            if (exists) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("函数包已存在")
                        .setMessage("本地已有函数 " + info.name + "，是否覆盖？")
                        .setPositiveButton("覆盖", (d, w) -> doSaveFuncPack(info))
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                doSaveFuncPack(info);
            }

        } catch (Exception e) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("导入失败")
                    .setMessage(e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    /** [新增] 保存函数包到本地并刷新列表 */
    private void doSaveFuncPack(CustomFunctionPackage.PackageInfo info) {
        try {
            CustomFunctionPackage.saveLocally(info);
            loadCustomFunctions();   // 刷新 customFunctions 列表
            Toast.makeText(this, "导入成功：" + info.name, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从 FunctionManagerActivity 返回后刷新自定义函数列表
        // 同时清除已被删除的函数（避免 selectedFunctions 中存在失效的自定义函数）
        loadCustomFunctions();
        cleanupDeletedFromSelected();
    }

    /** [新增] 清除 selectedFunctions 中本地已不存在的自定义函数 */
    private void cleanupDeletedFromSelected() {
        selectedFunctions.removeIf(item -> {
            if (item.isBuiltin) return false;
            // 检查该自定义函数是否仍在 customFunctions 中
            for (FunctionItem f : customFunctions) {
                if (f.name.equals(item.name)) return false;
            }
            return true;  // 已删除，从已选中移除
        });
        refreshSelectedFunctionsList();
    }

    /** 创建弹窗中的函数CheckBox */
    private CheckBox createFunctionCheckBox(FunctionItem item, boolean checked) {
        CheckBox cb = new CheckBox(this);

        String displayText = item.name;
        if (item.description != null && !item.description.isEmpty()) {
            displayText += "  (" + item.description + ")";
        }
        cb.setText(displayText);
        cb.setChecked(checked);
        cb.setButtonTintList(getResources().getColorStateList(R.color.purple_700, getTheme()));
        cb.setTextSize(14);
        cb.setPadding(4, 4, 4, 4);

        return cb;
    }

    // ==================== ★ 刷新已选函数列表 ====================

    private void refreshSelectedFunctionsList() {
        layoutSelectedFunctions.removeAllViews();

        if (selectedFunctions.isEmpty()) {
            tvNoFunctionsHint.setVisibility(View.VISIBLE);
            tvSelectedFunctionsTitle.setVisibility(View.GONE);
        } else {
            tvNoFunctionsHint.setVisibility(View.GONE);
            tvSelectedFunctionsTitle.setVisibility(View.VISIBLE);
            tvSelectedFunctionsTitle.setText("已选择的函数 (" + selectedFunctions.size() + ")");

            for (FunctionItem item : selectedFunctions) {
                View itemView = LayoutInflater.from(this)
                        .inflate(R.layout.item_gg_selected_function, layoutSelectedFunctions, false);

                TextView tvBadge = itemView.findViewById(R.id.tv_function_type_badge);
                TextView tvName = itemView.findViewById(R.id.tv_function_name);
                ImageButton btnRemove = itemView.findViewById(R.id.btn_remove_function);

                // 设置类型标签
                if (item.isBuiltin) {
                    tvBadge.setText("内置");
                    tvBadge.setBackgroundColor(Color.parseColor("#7B1FA2")); // 紫色
                } else {
                    tvBadge.setText("自定义");
                    tvBadge.setBackgroundColor(Color.parseColor("#00897B")); // 青色
                }

                // 圆角背景
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setCornerRadius(6f);
                badgeBg.setColor(item.isBuiltin ?
                        Color.parseColor("#7B1FA2") : Color.parseColor("#00897B"));
                tvBadge.setBackground(badgeBg);

                // 函数名
                String displayName = item.name;
                if (item.description != null && !item.description.isEmpty()) {
                    displayName += " - " + item.description;
                }
                tvName.setText(displayName);

                // 移除按钮 - 点击取消选择
                btnRemove.setOnClickListener(v -> {
                    selectedFunctions.remove(item);
                    refreshSelectedFunctionsList();
                });

                layoutSelectedFunctions.addView(itemView);
            }
        }
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
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);

        if (packages == null || packages.isEmpty()) {
            Toast.makeText(this, "未找到已安装应用", Toast.LENGTH_SHORT).show();
            showSourceSelectionDialog();
            return;
        }

        // 按应用名称字母排序
        Collections.sort(packages, (a, b) -> {
            String nameA = a.applicationInfo.loadLabel(pm).toString();
            String nameB = b.applicationInfo.loadLabel(pm).toString();
            return nameA.compareToIgnoreCase(nameB);
        });

        // 使用自定义布局：搜索框 + 列表
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_installed_app_picker, null);
        EditText etSearch = dialogView.findViewById(R.id.et_search);
        ListView lvApps = dialogView.findViewById(R.id.lv_apps);

        InstalledAppAdapter adapter = new InstalledAppAdapter(packages, pm);
        lvApps.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择已安装应用（共" + packages.size() + "个）")
                .setView(dialogView)
                .setNegativeButton("返回", (d, w) -> showSourceSelectionDialog())
                .setCancelable(false)
                .create();

        lvApps.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            PackageInfo info = adapter.getItem(position);
            onInstalledAppSelected(info.packageName);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    /** 已安装应用列表适配器（支持搜索过滤） */
    private class InstalledAppAdapter extends BaseAdapter {
        private final List<PackageInfo> allApps;
        private final List<PackageInfo> filteredApps;
        private final PackageManager pm;

        InstalledAppAdapter(List<PackageInfo> apps, PackageManager pm) {
            this.allApps = apps;
            this.filteredApps = new ArrayList<>(apps);
            this.pm = pm;
        }

        public void filter(String text) {
            filteredApps.clear();
            if (text.isEmpty()) {
                filteredApps.addAll(allApps);
            } else {
                String lowerText = text.toLowerCase();
                for (PackageInfo info : allApps) {
                    String name = info.applicationInfo.loadLabel(pm).toString().toLowerCase();
                    String pkg = info.packageName.toLowerCase();
                    if (name.contains(lowerText) || pkg.contains(lowerText)) {
                        filteredApps.add(info);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return filteredApps.size();
        }

        @Override
        public PackageInfo getItem(int position) {
            return filteredApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_installed_app, parent, false);
                holder = new ViewHolder();
                holder.ivIcon = convertView.findViewById(R.id.iv_app_icon);
                holder.tvName = convertView.findViewById(R.id.tv_app_name);
                holder.tvPackage = convertView.findViewById(R.id.tv_app_package);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            PackageInfo info = filteredApps.get(position);
            holder.ivIcon.setImageDrawable(info.applicationInfo.loadIcon(pm));
            holder.tvName.setText(info.applicationInfo.loadLabel(pm));
            holder.tvPackage.setText(info.packageName);

            return convertView;
        }

        class ViewHolder {
            ImageView ivIcon;
            TextView tvName;
            TextView tvPackage;
        }
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

        // 加载完成后分析 AndroidManifest 中的启动入口
        if (apkPath != null) {
            analyzeApkManifest(apkPath);
        }
    }

    /** 分析 APK 的 AndroidManifest.xml 中是否有多个启动入口 */
    private void analyzeApkManifest(String apkPath) {
        // 重置状态
        hasMultipleLauncherEntries = false;
        hwLauncherActivityName = null;
        swLauncherActivityName = null;

        try (ZipFile zipFile = new ZipFile(apkPath)) {
            ZipEntry manifestEntry = zipFile.getEntry("AndroidManifest.xml");
            if (manifestEntry == null) {
                updateEntryDeleteUI(false, false, false);
                return;
            }

            InputStream is = zipFile.getInputStream(manifestEntry);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            is.close();

            aXMLDecoder decoder = new aXMLDecoder(new ByteArrayInputStream(baos.toByteArray()));
            List<XMLEntry> xmlEntries = decoder.decode();

            ManifestModifier modifier = new ManifestModifier(xmlEntries);
            ManifestModifier.LauncherEntryInfo info = modifier.analyzeLauncherEntries();

            ManifestModifier.LauncherActivity hwEntry = info.getHwEntry();
            ManifestModifier.LauncherActivity swEntry = info.getSwEntry();

            boolean hasHw = hwEntry != null;
            boolean hasSw = swEntry != null;
            boolean hasMultiple = info.hasMultipleEntries;

            hasMultipleLauncherEntries = hasMultiple;
            hwLauncherActivityName = hasHw ? hwEntry.activityName : null;
            swLauncherActivityName = hasSw ? swEntry.activityName : null;

            updateEntryDeleteUI(hasMultiple, hasHw, hasSw);

        } catch (Exception e) {
            // 分析失败，禁用删除入口选项
            updateEntryDeleteUI(false, false, false);
            e.printStackTrace();
        }
    }

    /** 根据分析结果更新"删除多余启动入口"相关控件的状态 */
    private void updateEntryDeleteUI(boolean hasMultiple, boolean hasHw, boolean hasSw) {
        if (hasMultiple) {
            cbDeleteEntry.setEnabled(true);

            // 根据实际存在的入口类型设置 RadioGroup 可见的选项
            rbHwAccel.setEnabled(hasHw);
            rbSwAccel.setEnabled(hasSw);

            // 如果只有一个类型可用，预选那个
            if (hasHw && !hasSw) {
                rbHwAccel.setChecked(true);
            } else if (!hasHw && hasSw) {
                rbSwAccel.setChecked(true);
            } else {
                // 两个都有，默认保留 HW
                rbHwAccel.setChecked(true);
            }

            // 为只有一种类型的情况，隐藏 RadioGroup（无需选择）
            if (hasHw && hasSw) {
                rgEntryType.setVisibility(cbDeleteEntry.isChecked() ? View.VISIBLE : View.GONE);
            } else {
                // 只有一个入口类型，默认就是保留那唯一的一个
                rgEntryType.setVisibility(View.GONE);
            }
        } else {
            cbDeleteEntry.setEnabled(false);
            cbDeleteEntry.setChecked(false);
            rgEntryType.setVisibility(View.GONE);
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

    // ==================== 预设管理 ====================

    /** 保存预设对话框 */
    private void showSavePresetDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("输入预设名称");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("保存修改预设")
                .setMessage("将当前所有修改项保存为预设，方便下次快速加载。")
                .setView(input)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    doSavePreset(name);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void doSavePreset(String name) {
        // 检查是否已存在同名预设
        List<ModifyPreset.PresetInfo> existing = ModifyPreset.loadAll();
        boolean exists = false;
        for (ModifyPreset.PresetInfo p : existing) {
            if (p.name.equals(name)) { exists = true; break; }
        }

        if (exists) {
            new AlertDialog.Builder(this)
                    .setTitle("预设已存在")
                    .setMessage("本地已有预设\"" + name + "\"，是否覆盖？")
                    .setPositiveButton("覆盖", (d, w) -> performSave(name))
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            performSave(name);
        }
    }

    private void performSave(String name) {
        try {
            ModifyOptions options = collectModifyOptions();
            ModifyPreset.PresetInfo info = ModifyPreset.PresetInfo.fromModifyOptions(options, name);
            ModifyPreset.save(info);
            Toast.makeText(this, "预设已保存: " + name, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 加载预设对话框 - 显示已保存的预设列表 */
    private void showLoadPresetDialog() {
        List<ModifyPreset.PresetInfo> presets = ModifyPreset.loadAll();

        if (presets.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("加载预设")
                    .setMessage("暂无已保存的预设。\n\n你可以通过以下方式添加预设：\n1. 配置修改项后点击\"保存预设\"\n2. 点击\"从文件导入\"加载外部预设")
                    .setPositiveButton("从文件导入", (d, w) -> openPresetFilePicker())
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }

        // 构建预设列表字符串
        String[] presetNames = new String[presets.size()];
        for (int i = 0; i < presets.size(); i++) {
            ModifyPreset.PresetInfo p = presets.get(i);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            presetNames[i] = p.name + "  (" + sdf.format(new java.util.Date(p.timestamp)) + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("加载预设 (" + presets.size() + "个)")
                .setItems(presetNames, (dialog, which) -> {
                    ModifyPreset.PresetInfo selected = presets.get(which);
                    showPresetPreviewDialog(selected);
                })
                .setPositiveButton("从文件导入", (d, w) -> openPresetFilePicker())
                .setNegativeButton("取消", null)
                .show();
    }

    /** 预设预览确认对话框 */
    private void showPresetPreviewDialog(ModifyPreset.PresetInfo preset) {
        StringBuilder summary = new StringBuilder();
        if (!preset.newAppName.isEmpty()) summary.append("• 应用名: ").append(preset.newAppName).append("\n");
        if (!preset.newPackageName.isEmpty()) summary.append("• 包名: ").append(preset.newPackageName).append("\n");
        if (!preset.newVersionName.isEmpty()) summary.append("• 版本: ").append(preset.newVersionName).append("\n");
        if (preset.newIconPath != null) summary.append("• 修改图标: 是\n");
        if (preset.deleteEntry) summary.append("• 删除多余启动入口: 是\n");
        if (preset.embedScript) summary.append("• 内置脚本: 是\n");
        if (preset.slimApk) summary.append("• 精简APK: 是\n");
        if (preset.addFunctions) {
            int count = preset.selectedFunctionNames != null ? preset.selectedFunctionNames.size() : 0;
            summary.append("• 函数添加: ").append(count).append("个\n");
        }
        if (preset.uiBeautify) summary.append("• UI美化: 是\n");

        if (summary.length() == 0) summary.append("（空预设 - 无配置项）\n");

        new AlertDialog.Builder(this)
                .setTitle("预设: " + preset.name)
                .setMessage(summary.toString())
                .setPositiveButton("应用", (d, w) -> applyPreset(preset))
                .setNeutralButton("删除", (d, w) -> confirmDeletePreset(preset))
                .setNegativeButton("取消", null)
                .show();
    }

    /** 删除预设确认 */
    private void confirmDeletePreset(ModifyPreset.PresetInfo preset) {
        new AlertDialog.Builder(this)
                .setTitle("删除预设")
                .setMessage("确定要删除预设\"" + preset.name + "\"吗？此操作不可撤销。")
                .setPositiveButton("删除", (d, w) -> {
                    ModifyPreset.delete(preset.name);
                    Toast.makeText(this, "已删除: " + preset.name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 将预设数据应用到 UI 控件 */
    private void applyPreset(ModifyPreset.PresetInfo preset) {
        // 基础信息
        etAppName.setText(preset.newAppName);
        etPackageName.setText(preset.newPackageName);
        etVersionName.setText(preset.newVersionName);
        newIconPath = preset.newIconPath;
        if (newIconPath != null) {
            File iconFile = new File(newIconPath);
            if (iconFile.exists()) {
                imgAppIcon.setImageURI(android.net.Uri.fromFile(iconFile));
            }
        }

        // 启动入口
        cbDeleteEntry.setChecked(preset.deleteEntry);
        if (preset.deleteEntry) {
            if (preset.keepHwAccel) {
                rbHwAccel.setChecked(true);
            } else if (preset.keepSwAccel) {
                rbSwAccel.setChecked(true);
            }
        }

        // 内置脚本
        cbScript.setChecked(preset.embedScript);
        if (preset.embedScript && preset.scriptPath != null) {
            scriptPath = preset.scriptPath;
            btnSelectScript.setText(new File(preset.scriptPath).getName());
        }

        // 精简
        cbSlim.setChecked(preset.slimApk);
        if (preset.slimApk) {
            cbSlimArsc.setChecked(preset.slimArsc);
            cbSlimRes.setChecked(preset.slimRes);
            cbSlimDex.setChecked(preset.slimDex);
        }

        // 函数添加
        cbAddFunctions.setChecked(preset.addFunctions);
        if (preset.addFunctions && preset.selectedFunctionNames != null) {
            selectedFunctions.clear();
            for (String funcName : preset.selectedFunctionNames) {
                boolean isBuiltin = funcName.startsWith("[内置]");
                String pureName = funcName.replaceFirst("^\\[(内置|自定义)\\]\\s*", "");
                FunctionItem item = new FunctionItem(pureName, "", isBuiltin);
                if (isBuiltin) {
                    for (FunctionItem bf : builtinFunctions) {
                        if (bf.name.equals(pureName)) { item = bf; break; }
                    }
                } else {
                    for (FunctionItem cf : customFunctions) {
                        if (cf.name.equals(pureName)) { item = cf; break; }
                    }
                }
                selectedFunctions.add(item);
            }
            refreshSelectedFunctionsList();
        }

        // UI美化
        cbUiBeautify.setChecked(preset.uiBeautify);

        Toast.makeText(this, "预设已应用: " + preset.name, Toast.LENGTH_SHORT).show();
    }

    /** 打开文件选择器导入外部 .ggpreset 文件 */
    private void openPresetFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        presetPickerLauncher.launch(Intent.createChooser(intent, "选择 .ggpreset 预设文件"));
    }

    /** 处理从文件选择器返回的 .ggpreset 文件 */
    private void handlePresetImport(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            if (fileName == null || !fileName.endsWith(ModifyPreset.EXTENSION)) {
                Toast.makeText(this, "请选择 " + ModifyPreset.EXTENSION + " 格式的预设文件", Toast.LENGTH_SHORT).show();
                return;
            }

            File cacheFile = new File(getCacheDir(), "import_" + System.currentTimeMillis() + ModifyPreset.EXTENSION);
            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(cacheFile)) {
                if (is == null) throw new Exception("无法读取文件");
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            }

            ModifyPreset.PresetInfo preset = ModifyPreset.importPreset(cacheFile);
            cacheFile.delete();

            // 检查是否同名预设已存在
            List<ModifyPreset.PresetInfo> existing = ModifyPreset.loadAll();
            boolean exists = false;
            for (ModifyPreset.PresetInfo p : existing) {
                if (p.name.equals(preset.name)) { exists = true; break; }
            }

            if (exists) {
                new AlertDialog.Builder(this)
                        .setTitle("预设已存在")
                        .setMessage("本地已有预设\"" + preset.name + "\"，是否覆盖？")
                        .setPositiveButton("覆盖", (d, w) -> {
                            try {
                                ModifyPreset.save(preset);
                                Toast.makeText(this, "已覆盖: " + preset.name, Toast.LENGTH_SHORT).show();
                                showPresetPreviewDialog(preset);
                            } catch (Exception e) {
                                Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                ModifyPreset.save(preset);
                Toast.makeText(this, "导入成功: " + preset.name, Toast.LENGTH_SHORT).show();
                showPresetPreviewDialog(preset);
            }

        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("导入失败")
                    .setMessage(e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
        }
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

        options.functionEntries = buildFunctionEntries();

        options.apkPath = apkPath;

        options.newAppName = getText(etAppName);
        options.newPackageName = getText(etPackageName);
        options.newVersionName = getText(etVersionName);
        options.newIconPath = newIconPath;

        options.deleteEntry = cbDeleteEntry.isChecked();
        if (options.deleteEntry) {
            options.keepHwAccel = rbHwAccel.isChecked();
            options.keepSwAccel = rbSwAccel.isChecked();
            options.hwActivityName = hwLauncherActivityName;
            options.swActivityName = swLauncherActivityName;
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
        if (options.addFunctions) {
            options.selectedFunctionNames = new ArrayList<>();
            options.functionEntries = buildFunctionEntries();
            for (FunctionItem item : selectedFunctions) {
                options.selectedFunctionNames.add(
                        (item.isBuiltin ? "[内置]" : "[自定义]") + " " + item.name);
            }
        }

        options.uiBeautify = cbUiBeautify.isChecked();

        return options;
    }
    /** [新增] 把 selectedFunctions 转换为 GgFunctionAdder.FunctionEntry 列表 */
    private List<GgFunctionAdder.FunctionEntry> buildFunctionEntries() {
        List<GgFunctionAdder.FunctionEntry> entries = new ArrayList<>();
        // 加载本地包信息以便获取 smali 内容
        List<CustomFunctionPackage.PackageInfo> localPkgs = CustomFunctionPackage.loadAllLocal();

        for (FunctionItem item : selectedFunctions) {
            if (item.isBuiltin) {
                entries.add(new GgFunctionAdder.FunctionEntry(item.name));
            } else {
                // 找对应的本地包
                CustomFunctionPackage.PackageInfo pkg = null;
                for (CustomFunctionPackage.PackageInfo p : localPkgs) {
                    if (p.name.equals(item.name)) { pkg = p; break; }
                }
                if (pkg != null) {
                    entries.add(pkg.toFunctionEntry());
                }
            }
        }
        return entries;
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
            summary.append("• 删除多余启动入口: 是\n");
            if (options.keepHwAccel) {
                summary.append("    保留: 硬件加速入口 (").append(options.hwActivityName).append(")\n");
                if (options.swActivityName != null) {
                    summary.append("    删除: 软件加速入口 (").append(options.swActivityName).append(")\n");
                }
            } else if (options.keepSwAccel) {
                summary.append("    保留: 软件加速入口 (").append(options.swActivityName).append(")\n");
                if (options.hwActivityName != null) {
                    summary.append("    删除: 硬件加速入口 (").append(options.hwActivityName).append(")\n");
                }
            }
        }
        if (options.embedScript) {
            summary.append("• 内置脚本: 是\n");
        }
        if (options.slimApk) {
            summary.append("• 精简APK: 是\n");
        }
        if (options.addFunctions) {
            summary.append("• 函数添加: 是 (").append(selectedFunctions.size()).append("个)\n");
            for (FunctionItem item : selectedFunctions) {
                summary.append("    - ").append(item.isBuiltin ? "[内置]" : "[自定义]")
                        .append(" ").append(item.name).append("\n");
            }
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
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(this);
        }
        loadingDialog.setMessage(message);
        loadingDialog.show();
    }

    private void updateProgressDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.setMessage(message);
        }
    }

    private void dismissProgressDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    // ==================== 成功/错误对话框 + 安装 + 打开目录 ====================

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

    private void installApk(String apkFilePath) {
        try {
            File apkFile = new File(apkFilePath);
            if (!apkFile.exists()) {
                Toast.makeText(this, "APK文件不存在: " + apkFilePath, Toast.LENGTH_SHORT).show();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
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

            pendingInstallPath = null;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    apkUri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", apkFile);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (IllegalArgumentException e) {
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

    private void openFileDirectory(String filePath) {
        File file = new File(filePath);
        File directory = file.getParentFile();

        if (directory == null || !directory.exists()) {
            Toast.makeText(this, "目录不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        String dirPath = directory.getAbsolutePath();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
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

    private void showErrorDialog(String error) {
        new AlertDialog.Builder(this)
                .setTitle("修改失败")
                .setMessage(error)
                .setPositiveButton("确定", null)
                .show();
    }

    // ==================== 修改选项数据类 ====================

    public static class ModifyOptions {

        public List<GgFunctionAdder.FunctionEntry> functionEntries;
        public String apkPath;

        public String newAppName;
        public String newPackageName;
        public String newVersionName;
        public String newIconPath;

        public boolean deleteEntry;
        public boolean keepHwAccel;
        public boolean keepSwAccel;
        public String hwActivityName; // HW 加速的启动入口 activity 名称
        public String swActivityName; // SW 加速的启动入口 activity 名称

        public boolean embedScript;
        public String scriptPath;

        public boolean slimApk;
        public boolean slimArsc;
        public boolean slimRes;
        public boolean slimDex;

        public boolean addFunctions;
        public List<String> selectedFunctionNames; // ★ 新增：已选函数名列表

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
                    ", selectedFunctions=" + (selectedFunctionNames != null ? selectedFunctionNames.size() : 0) +
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
package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import mituran.gglua.tool.LuaEngine;
import mituran.gglua.tool.R;

public class MainActivity extends Activity {
    private CustomActionBar customActionBar;
    private RecyclerView tabRecyclerView;
    private FixedWidthRecyclerView codeBlockRecyclerView;

    private TabAdapter tabAdapter;
    private CodeBlockAdapter codeBlockAdapter;

    private List<CodeTab> tabs;
    private CodeTab currentTab;
    private CodeBlock clipboardBlock;
    private int selectedBlockPosition = -1;

    private String projectPath;
    private boolean hasUnsavedChanges = false;

    // 运行错误定位
    private int errorTabIndex = -1;
    private int errorBlockIndex = -1;

    // 自动保存相关
    private boolean autoSaveEnabled = false;
    private int autoSaveIntervalSec = 60;
    private android.os.Handler autoSaveHandler;
    private Runnable autoSaveRunnable;
    private boolean autoBackupEnabled = false;

    private static final String PREFS_NAME = "visual_lua_editor_settings";
    private static final String KEY_AUTO_SAVE_ENABLED = "auto_save_enabled";
    private static final String KEY_AUTO_SAVE_INTERVAL = "auto_save_interval_sec";
    private static final String KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visual_lua_script_editor_activity_main);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey("path")) {
            projectPath = bundle.getString("path");
        }

        loadSettings();
        initViews();
        initData();
        setupListeners();
        applyAutoSaveState();
    }

    // ==================== 设置持久化 ====================

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        autoSaveEnabled = prefs.getBoolean(KEY_AUTO_SAVE_ENABLED, false);
        autoSaveIntervalSec = prefs.getInt(KEY_AUTO_SAVE_INTERVAL, 60);
        autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false);
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_AUTO_SAVE_ENABLED, autoSaveEnabled)
                .putInt(KEY_AUTO_SAVE_INTERVAL, autoSaveIntervalSec)
                .putBoolean(KEY_AUTO_BACKUP_ENABLED, autoBackupEnabled)
                .apply();
    }

    // ==================== 自动保存 ====================

    private void applyAutoSaveState() {
        stopAutoSave();
        if (autoSaveEnabled) {
            startAutoSave();
        }
    }

    private void startAutoSave() {
        if (autoSaveHandler == null) {
            autoSaveHandler = new android.os.Handler();
        }
        autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                saveProjectSilently();
                saveProjectBackup();
                if (autoSaveHandler != null) {
                    autoSaveHandler.postDelayed(this, autoSaveIntervalSec * 1000L);
                }
            }
        };
        autoSaveHandler.postDelayed(autoSaveRunnable, autoSaveIntervalSec * 1000L);
    }

    private void stopAutoSave() {
        if (autoSaveHandler != null && autoSaveRunnable != null) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
            autoSaveRunnable = null;
        }
    }

    private void saveProjectBackup() {
        if (!autoBackupEnabled || projectPath == null) {
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            File backupDir = new File(projectPath);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            File backupFile = new File(projectPath, "code_backup_" + timestamp + ".json");
            ProjectManager.saveProject(projectPath, tabs);

            // 复制当前 code.json 为备份文件
            File currentFile = new File(projectPath, "code.json");
            if (currentFile.exists()) {
                java.io.FileInputStream fis = new java.io.FileInputStream(currentFile);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(backupFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.close();
                fis.close();

                // 限制备份数量，最多保留20个
                File[] backups = backupDir.listFiles((dir, name) ->
                        name.startsWith("code_backup_") && name.endsWith(".json"));
                if (backups != null && backups.length > 20) {
                    // 按文件名排序（时间戳排序），删除最旧的
                    java.util.Arrays.sort(backups, (a, b) -> a.getName().compareTo(b.getName()));
                    for (int i = 0; i < backups.length - 20; i++) {
                        backups[i].delete();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ==================== 初始化 ====================

    private void initViews() {
        customActionBar = findViewById(R.id.custom_action_bar);
        tabRecyclerView = findViewById(R.id.tab_recycler_view);
        codeBlockRecyclerView = findViewById(R.id.code_block_recycler_view);

        tabRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        codeBlockRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initData() {
        if (projectPath != null && ProjectManager.projectExists(projectPath)) {
            ProjectManager.ProjectLoadResult result = ProjectManager.loadProject(projectPath);
            if (result.isSuccess()) {
                tabs = result.tabs;
                currentTab = tabs.get(0);
                Toast.makeText(this, "项目加载成功", Toast.LENGTH_SHORT).show();
            } else {
                createDefaultProject();
                showParseErrorDialog(result);
            }
        } else {
            createDefaultProject();
            if (projectPath != null) {
                Toast.makeText(this, "已创建新项目", Toast.LENGTH_SHORT).show();
            }
        }

        tabAdapter = new TabAdapter(this, tabs, new TabAdapter.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                switchToTab(position);
            }

            @Override
            public void onTabLongClick(int position) {
                showTabOptionsDialog(position);
            }
        });
        tabRecyclerView.setAdapter(tabAdapter);

        codeBlockAdapter = new CodeBlockAdapter(this, currentTab.getCodeBlocks(),
                new CodeBlockAdapter.OnCodeBlockClickListener() {
                    @Override
                    public void onBlockClick(int position) {
                        selectedBlockPosition = position;
                        showBlockActionDialog(position);
                    }

                    @Override
                    public void onBlockLongClick(int position) {
                        selectedBlockPosition = position;
                        showBlockActionDialog(position);
                    }
                });
        codeBlockAdapter.setOnContentChangedListener(position -> markAsChanged());
        codeBlockRecyclerView.setAdapter(codeBlockAdapter);
    }

    private void setupListeners() {
        customActionBar.setOnRunClickListener(v -> runCode());
        customActionBar.setOnSaveClickListener(v -> saveProject());
        customActionBar.setOnDefineFunctionClickListener(v -> createNewFunction());
        customActionBar.setOnEditClickListener(v -> showEditMenu());
        customActionBar.setOnSettingsClickListener(v -> showSettingsDialog());
    }

    // ==================== 编辑菜单 ====================

    private void showEditMenu() {
        String[] items = {"保存", "导出代码", "清空代码块"};
        new AlertDialog.Builder(this)
                .setTitle("编辑")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            saveProject();
                            break;
                        case 1:
                            exportCode();
                            break;
                        case 2:
                            clearAllBlocks();
                            break;
                    }
                })
                .show();
    }

    // ==================== 设置对话框 ====================

    private void showSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        // 自动保存开关
        LinearLayout autoSaveRow = new LinearLayout(this);
        autoSaveRow.setOrientation(LinearLayout.HORIZONTAL);
        autoSaveRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView autoSaveLabel = new TextView(this);
        autoSaveLabel.setText("自动保存");
        autoSaveLabel.setTextSize(16);
        autoSaveLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        CheckBox autoSaveSwitch = new CheckBox(this);
        autoSaveSwitch.setChecked(autoSaveEnabled);
        autoSaveRow.addView(autoSaveLabel);
        autoSaveRow.addView(autoSaveSwitch);
        layout.addView(autoSaveRow);

        // 自动保存间隔
        LinearLayout intervalRow = new LinearLayout(this);
        intervalRow.setOrientation(LinearLayout.HORIZONTAL);
        intervalRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        intervalRow.setPadding(0, 12, 0, 0);

        TextView intervalLabel = new TextView(this);
        intervalLabel.setText("保存间隔(秒)");
        intervalLabel.setTextSize(14);

        EditText intervalInput = new EditText(this);
        intervalInput.setText(String.valueOf(autoSaveIntervalSec));
        intervalInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        intervalInput.setWidth(120);
        intervalInput.setPadding(12, 4, 12, 4);

        intervalRow.addView(intervalLabel);
        intervalRow.addView(intervalInput);
        layout.addView(intervalRow);

        intervalRow.setVisibility(autoSaveEnabled ? View.VISIBLE : View.GONE);
        intervalLabel.setVisibility(autoSaveEnabled ? View.VISIBLE : View.GONE);
        intervalInput.setVisibility(autoSaveEnabled ? View.VISIBLE : View.GONE);

        autoSaveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            intervalRow.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            intervalLabel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            intervalInput.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // 分隔线
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(0xFFE0E0E0);
        LinearLayout.LayoutParams dividerParams = (LinearLayout.LayoutParams) divider.getLayoutParams();
        dividerParams.setMargins(0, 16, 0, 16);
        divider.setLayoutParams(dividerParams);
        layout.addView(divider);

        // 自动备份开关
        LinearLayout backupRow = new LinearLayout(this);
        backupRow.setOrientation(LinearLayout.HORIZONTAL);
        backupRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView backupLabel = new TextView(this);
        backupLabel.setText("自动备份");
        backupLabel.setTextSize(16);
        backupLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        CheckBox backupSwitch = new CheckBox(this);
        backupSwitch.setChecked(autoBackupEnabled);
        backupRow.addView(backupLabel);
        backupRow.addView(backupSwitch);
        layout.addView(backupRow);

        // 恢复备份按钮
        LinearLayout restoreRow = new LinearLayout(this);
        restoreRow.setOrientation(LinearLayout.HORIZONTAL);
        restoreRow.setPadding(0, 12, 0, 0);

        android.widget.Button restoreBtn = new android.widget.Button(this);
        restoreBtn.setText("恢复备份");
        restoreBtn.setTextSize(14);
        restoreBtn.setOnClickListener(v -> showRestoreBackupDialog());
        restoreRow.addView(restoreBtn);
        layout.addView(restoreRow);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("设置");
        builder.setView(layout);
        builder.setPositiveButton("保存", (dialog, which) -> {
            autoSaveEnabled = autoSaveSwitch.isChecked();
            String intervalStr = intervalInput.getText().toString().trim();
            if (!intervalStr.isEmpty()) {
                try {
                    int val = Integer.parseInt(intervalStr);
                    if (val >= 5) {
                        autoSaveIntervalSec = val;
                    } else {
                        Toast.makeText(this, "间隔不能少于5秒", Toast.LENGTH_SHORT).show();
                        autoSaveIntervalSec = 5;
                    }
                } catch (NumberFormatException e) {
                    autoSaveIntervalSec = 60;
                }
            }
            autoBackupEnabled = backupSwitch.isChecked();
            saveSettings();
            applyAutoSaveState();
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ==================== 恢复备份 ====================

    private void showRestoreBackupDialog() {
        if (projectPath == null) {
            Toast.makeText(this, "无法获取项目路径", Toast.LENGTH_SHORT).show();
            return;
        }

        File dir = new File(projectPath);
        File[] backups = dir.listFiles((d, name) ->
                name.startsWith("code_backup_") && name.endsWith(".json"));

        if (backups == null || backups.length == 0) {
            Toast.makeText(this, "没有可恢复的备份", Toast.LENGTH_SHORT).show();
            return;
        }

        // 按时间排序（新的在前）
        java.util.Arrays.sort(backups, (a, b) -> b.getName().compareTo(a.getName()));

        String[] names = new String[backups.length];
        for (int i = 0; i < backups.length; i++) {
            names[i] = backups[i].getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("选择备份文件")
                .setItems(names, (dialog, which) -> {
                    restoreFromBackup(backups[which]);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void restoreFromBackup(File backupFile) {
        new AlertDialog.Builder(this)
                .setTitle("确认恢复")
                .setMessage("确定要恢复备份 \"" + backupFile.getName() + "\" 吗？\n当前未保存的更改将丢失。")
                .setPositiveButton("恢复", (dialog, which) -> {
                    try {
                        // 先保存当前状态为临时备份
                        if (hasUnsavedChanges && projectPath != null) {
                            saveProjectSilently();
                        }

                        // 复制备份文件覆盖 code.json
                        File target = new File(projectPath, "code.json");
                        java.io.FileInputStream fis = new java.io.FileInputStream(backupFile);
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(target);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                        fos.close();
                        fis.close();

                        // 重新加载项目
                        ProjectManager.ProjectLoadResult result = ProjectManager.loadProject(projectPath);
                        if (result.isSuccess()) {
                            tabs = result.tabs;
                            currentTab = tabs.get(0);
                            hasUnsavedChanges = false;
                            refreshAdapters();
                            Toast.makeText(this, "备份已恢复", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "备份文件解析失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "恢复失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 标签页操作 ====================

    private void switchToTab(int position) {
        currentTab = tabs.get(position);
        codeBlockAdapter.updateCodeBlocks(currentTab.getCodeBlocks());
        codeBlockAdapter.notifyDataSetChanged();
        markAsChanged();
        codeBlockRecyclerView.recalculateWidth();
    }

    private void showTabOptionsDialog(int position) {
        CodeTab tab = tabs.get(position);

        if (tab.getType() == CodeTab.TabType.MAIN) {
            Toast.makeText(this, "主程序不能删除", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"重命名", "删除"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("函数: " + tab.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        renameFunctionTab(position);
                    } else {
                        deleteFunctionTab(position);
                    }
                })
                .show();
    }

    private void renameFunctionTab(int position) {
        CodeTab tab = tabs.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重命名函数");

        final EditText input = new EditText(this);
        input.setText(tab.getName());
        input.setHint("输入函数名称");
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                tab.setName(newName);
                tabAdapter.notifyItemChanged(position);
                markAsChanged();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void deleteFunctionTab(int position) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除函数 \"" + tabs.get(position).getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    tabs.remove(position);
                    tabAdapter.notifyItemRemoved(position);

                    if (position == tabAdapter.getSelectedPosition()) {
                        switchToTab(0);
                        tabAdapter.setSelectedPosition(0);
                    }
                    markAsChanged();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void createNewFunction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("定义新函数");

        final EditText input = new EditText(this);
        input.setHint("输入函数名称（如: myFunction）");
        builder.setView(input);

        builder.setPositiveButton("创建", (dialog, which) -> {
            String functionName = input.getText().toString().trim();
            if (!functionName.isEmpty()) {
                for (CodeTab tab : tabs) {
                    if (tab.getType() == CodeTab.TabType.FUNCTION &&
                            tab.getName().equals(functionName)) {
                        Toast.makeText(this, "函数名已存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                CodeTab newTab = new CodeTab(UUID.randomUUID().toString(),
                        functionName, CodeTab.TabType.FUNCTION);
                newTab.addStartBlock();
                tabs.add(newTab);
                tabAdapter.notifyItemInserted(tabs.size() - 1);

                int newPosition = tabs.size() - 1;
                switchToTab(newPosition);
                tabAdapter.setSelectedPosition(newPosition);

                markAsChanged();
                Toast.makeText(this, "已创建函数: " + functionName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "函数名不能为空", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ==================== 代码块操作 ====================

    private void showBlockActionDialog(int position) {
        CodeBlock block = currentTab.getCodeBlocks().get(position);

        if (block.getType().isSpecialStartBlock()) {
            showExpandableBlockSelector(position + 1);
            return;
        }

        String[] actions = {"在后面插入代码块", "复制此代码块",
                "剪切此代码块", "粘贴代码块", "删除此代码块"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("代码块操作")
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showExpandableBlockSelector(position + 1);
                            break;
                        case 1:
                            copyBlock(position);
                            break;
                        case 2:
                            cutBlock(position);
                            break;
                        case 3:
                            pasteBlock(position + 1);
                            break;
                        case 4:
                            deleteBlock(position);
                            break;
                    }
                })
                .show();
    }

    private int findBlockEnd(int startPosition) {
        List<CodeBlock> blocks = currentTab.getCodeBlocks();
        int depth = 0;

        for (int i = startPosition; i < blocks.size(); i++) {
            CodeBlock block = blocks.get(i);

            if (block.getType().isBlockStart()) {
                depth++;
            } else if (block.getType().isBlockEnd()) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        return blocks.size() - 1;
    }

    private void showExpandableBlockSelector(int insertPosition) {
        List<CodeBlockTypeItem> categories = CodeBlockTypeItem.createAllCategories();

        CodeBlockTypeItem customFunctions = new CodeBlockTypeItem("自定义函数");
        for (CodeTab tab : tabs) {
            if (tab.getType() == CodeTab.TabType.FUNCTION) {
                customFunctions.addDynamicBlockType(
                        DynamicCodeBlockType.createFunctionCall(tab.getName())
                );
            }
        }
        if (customFunctions.getBlockTypes().size() > 0) {
            categories.add(customFunctions);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择代码块类型");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 20, 0, 20);

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ExpandableBlockTypeAdapter adapter = new ExpandableBlockTypeAdapter(
                this, categories, (dynamicBlockType) -> {
            insertCodeBlockFromDynamic(dynamicBlockType, insertPosition);
        }
        );

        recyclerView.setAdapter(adapter);
        layout.addView(recyclerView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        builder.setView(layout);
        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.7)
            );
        }
    }

    private void insertCodeBlockFromDynamic(DynamicCodeBlockType dynamicType, int position) {
        CodeBlockType type = dynamicType.toCodeBlockType();
        String value = dynamicType.getDefaultValue();

        int indentLevel = calculateIndentLevel(position);

        List<CodeBlock> blocks = currentTab.getCodeBlocks();
        CodeBlock newBlock = new CodeBlock(type, value, indentLevel);

        if (position == -1 || position >= blocks.size()) {
            blocks.add(newBlock);
            position = blocks.size() - 1;
        } else {
            blocks.add(position, newBlock);
        }

        if (dynamicType.isBlockStart() && !dynamicType.isBlockEnd()) {
            CodeBlockType closingType = getClosingBlockType(type);
            if (closingType != null) {
                CodeBlock closingBlock = new CodeBlock(closingType, closingType.getDefaultValue(), indentLevel);
                blocks.add(position + 1, closingBlock);
            }
        }

        updateIndentLevels();
        codeBlockAdapter.notifyDataSetChanged();
        markAsChanged();
        codeBlockRecyclerView.recalculateWidth();
        Toast.makeText(this, "已插入: " + dynamicType.getDisplayName(), Toast.LENGTH_SHORT).show();
    }

    private void insertCodeBlock(CodeBlockType type, int position) {
        int indentLevel = calculateIndentLevel(position);

        List<CodeBlock> blocks = currentTab.getCodeBlocks();
        CodeBlock newBlock = new CodeBlock(type, type.getDefaultValue(), indentLevel);

        if (position == -1 || position >= blocks.size()) {
            blocks.add(newBlock);
            position = blocks.size() - 1;
        } else {
            blocks.add(position, newBlock);
        }

        if (type.isBlockStart() && !type.isBlockEnd()) {
            CodeBlockType closingType = getClosingBlockType(type);
            if (closingType != null) {
                CodeBlock closingBlock = new CodeBlock(closingType, closingType.getDefaultValue(), indentLevel);
                blocks.add(position + 1, closingBlock);
            }
        }

        updateIndentLevels();
        codeBlockAdapter.notifyDataSetChanged();
        markAsChanged();
        codeBlockRecyclerView.recalculateWidth();
        Toast.makeText(this, "已插入: " + type.getDisplayName(), Toast.LENGTH_SHORT).show();
    }

    private CodeBlockType getClosingBlockType(CodeBlockType openingType) {
        switch (openingType) {
            case IF:
            case FOR:
            case WHILE:
            case FUNCTION:
                return CodeBlockType.END;
            case REPEAT:
                return CodeBlockType.UNTIL;
            default:
                return null;
        }
    }

    private int calculateIndentLevel(int position) {
        List<CodeBlock> blocks = currentTab.getCodeBlocks();
        if (position <= 0 || blocks.isEmpty()) {
            return 0;
        }

        int currentLevel = 0;
        for (int i = 0; i < position && i < blocks.size(); i++) {
            CodeBlock block = blocks.get(i);
            CodeBlockType type = block.getType();

            if (type.isSpecialStartBlock()) {
                continue;
            }

            if (type.isBlockStart()) {
                currentLevel++;
            } else if (type.isBlockEnd()) {
                currentLevel--;
            }
        }

        return Math.max(0, currentLevel);
    }

    private void updateIndentLevels() {
        List<CodeBlock> blocks = currentTab.getCodeBlocks();
        int currentLevel = 0;

        for (CodeBlock block : blocks) {
            CodeBlockType type = block.getType();

            if (type.isSpecialStartBlock()) {
                block.setIndentLevel(0);
                continue;
            }

            if (type.isBlockEnd() || type.isBlockMiddle()) {
                currentLevel = Math.max(0, currentLevel - 1);
            }

            block.setIndentLevel(currentLevel);

            if (type.isBlockStart()) {
                currentLevel++;
            } else if (type.isBlockMiddle()) {
                currentLevel++;
            }
        }
    }

    private void copyBlock(int position) {
        clipboardBlock = currentTab.getCodeBlocks().get(position).copy();
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }

    private void cutBlock(int position) {
        CodeBlock block = currentTab.getCodeBlocks().get(position);

        if (block.getType().isSpecialStartBlock()) {
            Toast.makeText(this, "起始块不能剪切", Toast.LENGTH_SHORT).show();
            return;
        }

        clipboardBlock = block.copy();
        currentTab.getCodeBlocks().remove(position);
        updateIndentLevels();
        codeBlockAdapter.notifyDataSetChanged();
        markAsChanged();
        codeBlockRecyclerView.recalculateWidth();
        Toast.makeText(this, "已剪切", Toast.LENGTH_SHORT).show();
    }

    private void pasteBlock(int position) {
        if (clipboardBlock == null) {
            Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (clipboardBlock.getType().isSpecialStartBlock()) {
            Toast.makeText(this, "不能粘贴起始块", Toast.LENGTH_SHORT).show();
            return;
        }

        CodeBlock newBlock = clipboardBlock.copy();
        List<CodeBlock> blocks = currentTab.getCodeBlocks();

        if (position == -1 || position >= blocks.size()) {
            blocks.add(newBlock);
        } else {
            blocks.add(position, newBlock);
        }

        updateIndentLevels();
        codeBlockAdapter.notifyDataSetChanged();
        markAsChanged();
        codeBlockRecyclerView.recalculateWidth();
        Toast.makeText(this, "已粘贴", Toast.LENGTH_SHORT).show();
    }

    private void deleteBlock(int position) {
        CodeBlock block = currentTab.getCodeBlocks().get(position);

        if (block.getType().isSpecialStartBlock()) {
            Toast.makeText(this, "起始块不能删除", Toast.LENGTH_SHORT).show();
            return;
        }

        currentTab.getCodeBlocks().remove(position);
        updateIndentLevels();
        codeBlockAdapter.notifyDataSetChanged();
        markAsChanged();
        codeBlockRecyclerView.recalculateWidth();
        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
    }

    private void clearAllBlocks() {
        new AlertDialog.Builder(this)
                .setTitle("确认")
                .setMessage("确定要清空当前标签页的所有代码块吗?\n（起始块将保留）")
                .setPositiveButton("确定", (dialog, which) -> {
                    List<CodeBlock> blocks = currentTab.getCodeBlocks();
                    if (blocks.size() > 1) {
                        CodeBlock startBlock = blocks.get(0);
                        blocks.clear();
                        if (startBlock.getType().isSpecialStartBlock()) {
                            blocks.add(startBlock);
                        }
                    }
                    codeBlockAdapter.notifyDataSetChanged();
                    markAsChanged();
                    codeBlockRecyclerView.recalculateWidth();
                    Toast.makeText(MainActivity.this, "已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 运行和导出 ====================

    private void runCode() {
        // 先保存项目以确保代码最新
        if (hasUnsavedChanges && projectPath != null) {
            saveProjectSilently();
        }

        // 生成 Lua 代码（含行号映射）
        final GeneratedLuaCode generatedCode = generateLuaCode();
        String luaCode = generatedCode.getCode();

        if (luaCode == null || luaCode.trim().isEmpty()) {
            Toast.makeText(this, "脚本内容为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 重置错误定位
        errorTabIndex = -1;
        errorBlockIndex = -1;

        // 每次执行时创建新的日志TextView，避免重复addView导致的崩溃
        final TextView logView = new TextView(this);
        logView.setTextSize(12);
        logView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logView.setPadding(16, 16, 16, 16);
        logView.setTextIsSelectable(true);

        // 创建新的LuaEngine绑定到新的logView
        final LuaEngine engine = new LuaEngine(this, logView);
        engine.initialize();

        // 显示执行对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("脚本运行中...");
        builder.setCancelable(false);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(logView, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        builder.setView(scrollView);
        builder.setPositiveButton("停止运行", null);
        builder.setNegativeButton("查看代码", null);

        AlertDialog dialog = builder.create();

        // 对话框关闭后自动定位到出错代码块
        dialog.setOnDismissListener(d -> {
            if (errorTabIndex >= 0 && errorBlockIndex >= 0) {
                scrollToErrorBlock();
            }
        });

        dialog.show();

        // 设置停止按钮
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            engine.close();
            errorTabIndex = -1;
            errorBlockIndex = -1;
            dialog.dismiss();
            Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
        });

        // 查看代码按钮
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            showCodeViewDialog(luaCode);
        });

        // 在新线程中执行脚本
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();

                engine.executeString(luaCode);

                long elapsed = System.currentTimeMillis() - startTime;
                runOnUiThread(() -> {
                    errorTabIndex = -1;
                    errorBlockIndex = -1;
                    dialog.setTitle("执行完成");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("关闭");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> dialog.dismiss());
                    Toast.makeText(this, "脚本执行完成 (耗时 " + (elapsed / 1000.0) + " 秒)", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    dialog.setTitle("执行异常");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("关闭并定位");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> dialog.dismiss());
                    logView.append("[错误] " + e.getMessage() + "\n");

                    // 解析错误行号并定位代码块
                    int errorLine = GeneratedLuaCode.parseErrorLine(e.getMessage());
                    if (errorLine > 0) {
                        int[] lineInfo = generatedCode.getLineInfo(errorLine);
                        if (lineInfo != null && lineInfo[1] >= 0) {
                            errorTabIndex = lineInfo[0];
                            errorBlockIndex = lineInfo[1];
                            CodeTab errorTab = tabs.get(errorTabIndex);
                            CodeBlock errorBlock = errorTab.getCodeBlocks().get(errorBlockIndex);
                            String blockName = errorBlock.getType().getDisplayName();
                            logView.append("[错误定位] 第" + errorLine + "行 → "
                                    + (errorTab.getType() == CodeTab.TabType.FUNCTION ? "函数\"" : "标签页\"")
                                    + errorTab.getName() + "\" → \"" + blockName + "\"块\n");
                        } else if (lineInfo != null && lineInfo[1] == -1) {
                            logView.append("[错误定位] 第" + errorLine + "行 → 标签页\""
                                    + tabs.get(lineInfo[0]).getName() + "\" (非块级代码，请检查函数定义或结构)\n");
                        } else {
                            logView.append("[错误定位] 第" + errorLine + "行，未能定位到对应代码块\n");
                        }
                    }

                    Toast.makeText(this, "脚本执行异常，关闭后将定位到出错代码块", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * 滚动到出错代码块并切换标签页
     */
    private void scrollToErrorBlock() {
        if (errorTabIndex < 0 || errorBlockIndex < 0) return;
        if (errorTabIndex >= tabs.size()) return;

        CodeTab targetTab = tabs.get(errorTabIndex);
        boolean savedUnsavedState = hasUnsavedChanges;

        // 切换到对应标签页
        if (currentTab != targetTab) {
            int tabPosition = errorTabIndex;
            switchToTab(tabPosition);
            tabAdapter.setSelectedPosition(tabPosition);
            tabRecyclerView.smoothScrollToPosition(tabPosition);
        }

        // 滚动到对应代码块
        codeBlockRecyclerView.postDelayed(() -> {
            codeBlockRecyclerView.smoothScrollToPosition(errorBlockIndex);
            hasUnsavedChanges = savedUnsavedState;
            errorTabIndex = -1;
            errorBlockIndex = -1;
        }, 300);
    }

    /**
     * 显示代码查看对话框
     */
    private void showCodeViewDialog(String luaCode) {
        AlertDialog.Builder codeBuilder = new AlertDialog.Builder(this);
        codeBuilder.setTitle("生成的Lua代码");

        ScrollView codeScrollView = new ScrollView(this);
        TextView codeView = new TextView(this);
        codeView.setText(luaCode);
        codeView.setPadding(40, 40, 40, 40);
        codeView.setTextIsSelectable(true);
        codeView.setTextSize(14);
        codeView.setTypeface(android.graphics.Typeface.MONOSPACE);
        codeScrollView.addView(codeView);

        codeBuilder.setView(codeScrollView);
        codeBuilder.setPositiveButton("关闭", null);
        codeBuilder.show();
    }

    private void exportCode() {
        String luaCode = generateLuaCode().getCode();
        Toast.makeText(this, "代码已复制到剪贴板", Toast.LENGTH_SHORT).show();

        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Lua Code", luaCode);
        clipboard.setPrimaryClip(clip);
    }

    private GeneratedLuaCode generateLuaCode() {
        GeneratedLuaCode result = new GeneratedLuaCode();
        StringBuilder sb = new StringBuilder();

        // 函数标签页
        for (int tabIdx = 0; tabIdx < tabs.size(); tabIdx++) {
            CodeTab tab = tabs.get(tabIdx);
            if (tab.getType() == CodeTab.TabType.FUNCTION && !tab.getCodeBlocks().isEmpty()) {
                sb.append(tab.generateFunctionHeader()).append("\n");
                result.addLine(tabIdx, -1); // 函数头行

                for (int blockIdx = 0; blockIdx < tab.getCodeBlocks().size(); blockIdx++) {
                    CodeBlock block = tab.getCodeBlocks().get(blockIdx);
                    if (block.getType().isSpecialStartBlock()) {
                        continue;
                    }

                    String code = block.generateCode();
                    if (code != null) {
                        for (int i = 0; i < block.getIndentLevel() + 1; i++) {
                            sb.append("    ");
                        }
                        sb.append(code).append("\n");
                        result.addLine(tabIdx, blockIdx); // 块级行
                    }
                }

                sb.append("end\n\n");
                result.addLine(tabIdx, -1); // end 行
                result.addLine(tabIdx, -1); // 空行
            }
        }

        // 主程序标签页
        CodeTab mainTab = tabs.get(0);
        sb.append("-- 主程序\n");
        result.addLine(0, -1); // 注释行

        for (int blockIdx = 0; blockIdx < mainTab.getCodeBlocks().size(); blockIdx++) {
            CodeBlock block = mainTab.getCodeBlocks().get(blockIdx);
            if (block.getType().isSpecialStartBlock()) {
                continue;
            }

            String code = block.generateCode();
            if (code != null) {
                for (int i = 0; i < block.getIndentLevel(); i++) {
                    sb.append("    ");
                }
                sb.append(code).append("\n");
                result.addLine(0, blockIdx); // 块级行
            }
        }

        result.setCode(sb.toString());
        return result;
    }

    // ==================== 项目管理 ====================

    private void createDefaultProject() {
        tabs = new ArrayList<>();
        CodeTab mainTab = new CodeTab(UUID.randomUUID().toString(), "主程序", CodeTab.TabType.MAIN);
        mainTab.addStartBlock();
        tabs.add(mainTab);
        currentTab = mainTab;
    }

    private void showParseErrorDialog(ProjectManager.ProjectLoadResult result) {
        String message = "项目文件解析失败，请选择处理方式：\n\n"
                + "错误原因: " + result.errorMessage;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("项目解析失败");
        builder.setMessage(message);
        builder.setCancelable(false);

        builder.setPositiveButton("覆盖", (dialog, which) -> {
            if (projectPath != null) {
                ProjectManager.saveProject(projectPath, tabs);
            }
            Toast.makeText(MainActivity.this, "已覆盖并创建新项目", Toast.LENGTH_SHORT).show();
            refreshAdapters();
        });

        builder.setNegativeButton("取消并退出", (dialog, which) -> {
            Toast.makeText(MainActivity.this, "已取消，项目未修改", Toast.LENGTH_SHORT).show();
            finish();
        });

        builder.setNeutralButton("尝试修复", (dialog, which) -> {
            showRepairDialog(result);
        });

        builder.show();
    }

    private void showRepairDialog(ProjectManager.ProjectLoadResult result) {
        StringBuilder suggestions = new StringBuilder();
        if ("PARSE_ERROR".equals(result.errorType)) {
            String detail = result.errorDetail != null ? result.errorDetail : "";
            if (detail.contains("JSON") || detail.contains("JSONException")) {
                suggestions.append("修复建议:\n");
                suggestions.append("1. 检查JSON格式是否正确，确保所有花括号 {} 和中括号 [] 匹配\n");
                suggestions.append("2. 检查字符串值中的引号是否正确转义\n");
                suggestions.append("3. 检查每个对象成员后是否有逗号分隔\n");
                suggestions.append("4. 可使用在线JSON验证工具检查文件内容\n");
            } else if (detail.contains("无法识别") || detail.contains("IllegalArgumentException")
                    || detail.contains("valueOf") || detail.contains("enum") || detail.contains("CodeBlockType")) {
                suggestions.append("修复建议:\n");
                suggestions.append("1. 文件中包含无法识别的代码块类型名称\n");
                suggestions.append("2. 可能是旧版本编辑器创建的项目，某些代码块类型已不再支持\n");
                suggestions.append("3. 可以手动编辑 code.json，删除或修正无法识别的代码块类型\n");
                suggestions.append("4. 检查 \"type\" 字段的值是否为有效的代码块类型\n");
            } else {
                suggestions.append("修复建议:\n");
                suggestions.append("1. 备份当前的 code.json 文件后再尝试修复\n");
                suggestions.append("2. 检查文件内容是否完整，可能文件已损坏\n");
                suggestions.append("3. 如果项目由旧版本编辑器创建，尝试理解版本差异后手动修正\n");
            }
        } else if ("EMPTY_DATA".equals(result.errorType)) {
            suggestions.append("修复建议:\n");
            suggestions.append("1. 项目文件中没有有效的标签页数据\n");
            suggestions.append("2. 可以手动添加 tabs 数组和代码块数据\n");
            suggestions.append("3. 或直接选择'覆盖'创建新的空白项目\n");
        } else {
            suggestions.append("修复建议:\n");
            suggestions.append("1. 备份当前的 code.json 文件\n");
            suggestions.append("2. 尝试手动修复文件中的错误\n");
            suggestions.append("3. 如果无法修复，请选择'覆盖'重新创建项目\n");
        }

        StringBuilder info = new StringBuilder();
        info.append("【错误信息】\n");
        info.append(result.errorMessage != null ? result.errorMessage : "未知错误");
        info.append("\n\n");

        if (result.errorDetail != null && !result.errorDetail.isEmpty()) {
            info.append("【错误详情】\n");
            info.append(result.errorDetail);
            info.append("\n\n");
        }

        info.append("【版本差异】\n");
        info.append(result.getVersionDiff());
        info.append("\n\n");

        info.append("【").append(suggestions).append("】\n");

        info.append("\n【解析失败的文件内容】\n");
        String formatted = result.getFormattedContent();
        if (formatted.length() > 3000) {
            formatted = formatted.substring(0, 3000) + "\n... (内容过长，已截断，完整内容请查看 code.json)";
        }
        info.append(formatted);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修复建议");

        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        textView.setText(info.toString());
        textView.setPadding(40, 40, 40, 40);
        textView.setTextIsSelectable(true);
        textView.setTextSize(13);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        textView.setLineSpacing(4, 1);
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setCancelable(false);

        builder.setPositiveButton("覆盖", (dialog, which) -> {
            if (projectPath != null) {
                ProjectManager.saveProject(projectPath, tabs);
            }
            Toast.makeText(MainActivity.this, "已覆盖并创建新项目", Toast.LENGTH_SHORT).show();
            refreshAdapters();
        });

        builder.setNegativeButton("取消并退出", (dialog, which) -> {
            Toast.makeText(MainActivity.this, "已取消，项目未修改", Toast.LENGTH_SHORT).show();
            finish();
        });

        builder.show();
    }

    private void refreshAdapters() {
        tabAdapter.notifyDataSetChanged();
        codeBlockAdapter.updateCodeBlocks(currentTab.getCodeBlocks());
        codeBlockAdapter.notifyDataSetChanged();
        codeBlockRecyclerView.recalculateWidth();
    }

    // ==================== 保存 ====================

    private void saveProject() {
        if (projectPath == null) {
            Toast.makeText(this, "无法保存：未指定保存路径", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = ProjectManager.saveProject(projectPath, tabs);
        if (success) {
            hasUnsavedChanges = false;
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProjectSilently() {
        if (projectPath != null && hasUnsavedChanges) {
            ProjectManager.saveProject(projectPath, tabs);
            hasUnsavedChanges = false;
        }
    }

    private void markAsChanged() {
        hasUnsavedChanges = true;
    }

    // ==================== 退出提醒 ====================

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("未保存的更改")
                    .setMessage("当前项目有未保存的更改，是否保存后退出？")
                    .setPositiveButton("保存并退出", (dialog, which) -> {
                        saveProject();
                        finish();
                    })
                    .setNegativeButton("不保存", (dialog, which) -> {
                        hasUnsavedChanges = false;
                        finish();
                    })
                    .setNeutralButton("取消", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    // ==================== 生命周期 ====================

    @Override
    protected void onPause() {
        super.onPause();
        saveProjectSilently();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoSaveHandler != null && autoSaveRunnable != null) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
        }
        saveProjectSilently();
    }
}

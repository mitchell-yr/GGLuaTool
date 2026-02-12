package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    // 保存路径和自动保存
    private String projectPath;
    private boolean hasUnsavedChanges = false;
    private static final long AUTO_SAVE_INTERVAL = 60000; // 60秒自动保存
    private android.os.Handler autoSaveHandler;
    private Runnable autoSaveRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visual_lua_script_editor_activity_main);

        // 获取传入的路径
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey("path")) {
            projectPath = bundle.getString("path");
        }

        initViews();
        initData();
        setupListeners();
        setupAutoSave();
    }

    private void initViews() {
        customActionBar = findViewById(R.id.custom_action_bar);
        tabRecyclerView = findViewById(R.id.tab_recycler_view);
        codeBlockRecyclerView = findViewById(R.id.code_block_recycler_view);

        // 设置标签页RecyclerView（横向）
        tabRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        // 设置代码块RecyclerView（纵向）
        codeBlockRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initData() {
        // 尝试加载保存的项目
        if (projectPath != null && ProjectManager.projectExists(projectPath)) {
            tabs = ProjectManager.loadProject(projectPath);
            if (tabs != null && !tabs.isEmpty()) {
                currentTab = tabs.get(0);
                Toast.makeText(this, "项目加载成功", Toast.LENGTH_SHORT).show();
            } else {
                // 加载失败，创建默认项目
                createDefaultProject();
                Toast.makeText(this, "项目加载失败，已创建新项目", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 没有保存的项目，创建默认项目
            createDefaultProject();
            if (projectPath != null) {
                Toast.makeText(this, "已创建新项目", Toast.LENGTH_SHORT).show();
            }
        }

        // 设置标签页适配器
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

        // 设置代码块适配器
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
                        showBlockActionDialog(position);  // 长按也显示操作菜单
                    }
                });
        codeBlockRecyclerView.setAdapter(codeBlockAdapter);
    }

    private void setupListeners() {
        // 运行按钮
        customActionBar.setOnRunClickListener(v -> runCode());

        // 保存按钮
        customActionBar.setOnSaveClickListener(v -> saveProject());

        // 添加代码块按钮
        customActionBar.setOnAddBlockClickListener(v -> showExpandableBlockSelector(-1));

        // 定义函数按钮
        customActionBar.setOnDefineFunctionClickListener(v -> createNewFunction());

        // 清空按钮
        customActionBar.setOnClearClickListener(v -> clearAllBlocks());

        // 导出按钮
        customActionBar.setOnExportClickListener(v -> exportCode());
    }

    // 切换到指定标签页
    private void switchToTab(int position) {
        currentTab = tabs.get(position);
        codeBlockAdapter.updateCodeBlocks(currentTab.getCodeBlocks());
        codeBlockAdapter.notifyDataSetChanged();
        markAsChanged();
        codeBlockRecyclerView.recalculateWidth();
    }

    // 显示标签页选项对话框
    private void showTabOptionsDialog(int position) {
        CodeTab tab = tabs.get(position);

        // 主程序不能删除
        if (tab.getType() == CodeTab.TabType.MAIN) {
            Toast.makeText(this, "主程序不能删除", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"重命名", "删除"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("函数: " + tab.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 重命名
                        renameFunctionTab(position);
                    } else {
                        // 删除
                        deleteFunctionTab(position);
                    }
                })
                .show();
    }

    // 重命名函数标签
    private void renameFunctionTab(int position) {
        CodeTab tab = tabs.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重命名函数");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(tab.getName());
        input.setHint("输入函数名称");
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                tab.setName(newName);
                tabAdapter.notifyItemChanged(position);
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 删除函数标签
    private void deleteFunctionTab(int position) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除函数 \"" + tabs.get(position).getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    tabs.remove(position);
                    tabAdapter.notifyItemRemoved(position);

                    // 如果删除的是当前标签，切换到主程序
                    if (position == tabAdapter.getSelectedPosition()) {
                        switchToTab(0);
                        tabAdapter.setSelectedPosition(0);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 创建新函数
    private void createNewFunction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("定义新函数");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("输入函数名称（如: myFunction）");
        builder.setView(input);

        builder.setPositiveButton("创建", (dialog, which) -> {
            String functionName = input.getText().toString().trim();
            if (!functionName.isEmpty()) {
                // 检查是否已存在同名函数
                for (CodeTab tab : tabs) {
                    if (tab.getType() == CodeTab.TabType.FUNCTION &&
                            tab.getName().equals(functionName)) {
                        Toast.makeText(this, "函数名已存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // 创建新函数标签
                CodeTab newTab = new CodeTab(UUID.randomUUID().toString(),
                        functionName, CodeTab.TabType.FUNCTION);
                tabs.add(newTab);
                tabAdapter.notifyItemInserted(tabs.size() - 1);

                // 切换到新创建的函数
                int newPosition = tabs.size() - 1;
                switchToTab(newPosition);
                tabAdapter.setSelectedPosition(newPosition);

                Toast.makeText(this, "已创建函数: " + functionName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "函数名不能为空", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 显示代码块操作对话框（简化版）
    private void showBlockActionDialog(int position) {
        String[] actions = {"在后面插入代码块", "复制此代码块",
                "剪切此代码块", "粘贴代码块", "删除此代码块"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("代码块操作")
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0: // 插入（同级）
                            showExpandableBlockSelector(position + 1);
                            break;
                        case 1: // 复制
                            copyBlock(position);
                            break;
                        case 2: // 剪切
                            cutBlock(position);
                            break;
                        case 3: // 粘贴
                            pasteBlock(position + 1);
                            break;
                        case 4: // 删除
                            deleteBlock(position);
                            break;
                    }
                })
                .show();
    }

    // 查找块的结束位置
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

    // 显示可展开的代码块选择器
    private void showExpandableBlockSelector(int insertPosition) {
        List<CodeBlockTypeItem> categories = CodeBlockTypeItem.createAllCategories();

        // 添加自定义函数分类
        CodeBlockTypeItem customFunctions = new CodeBlockTypeItem("⚙️ 自定义函数");
        for (CodeTab tab : tabs) {
            if (tab.getType() == CodeTab.TabType.FUNCTION) {
                // 为每个函数创建一个调用代码块类型
                customFunctions.addDynamicBlockType(
                        DynamicCodeBlockType.createFunctionCall(tab.getName())
                );
            }
        }
        if (customFunctions.getBlockTypes().size() > 0) {
            // 插入到"函数操作"分类之后
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

    // 从DynamicCodeBlockType插入代码块
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

        // 如果是需要闭合的块，自动插入闭合语句
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

    // 插入代码块（自动插入闭合块）
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

        // 如果是需要闭合的块，自动插入闭合语句
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

    // 获取闭合代码块类型
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

    // 计算缩进级别
    private int calculateIndentLevel(int position) {
        List<CodeBlock> blocks = currentTab.getCodeBlocks();
        if (position <= 0 || blocks.isEmpty()) {
            return 0;
        }

        int currentLevel = 0;
        for (int i = 0; i < position && i < blocks.size(); i++) {
            CodeBlock block = blocks.get(i);
            CodeBlockType type = block.getType();

            if (type.isBlockStart()) {
                currentLevel++;
            } else if (type.isBlockEnd()) {
                currentLevel--;
            }
        }

        return Math.max(0, currentLevel);
    }

    // 更新所有代码块的缩进级别
    private void updateIndentLevels() {
        List<CodeBlock> blocks = currentTab.getCodeBlocks();
        int currentLevel = 0;

        for (CodeBlock block : blocks) {
            CodeBlockType type = block.getType();

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

    // 复制代码块
    private void copyBlock(int position) {
        clipboardBlock = currentTab.getCodeBlocks().get(position).copy();
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }

    // 剪切代码块
    private void cutBlock(int position) {
        clipboardBlock = currentTab.getCodeBlocks().get(position).copy();
        currentTab.getCodeBlocks().remove(position);
        updateIndentLevels();
        codeBlockAdapter.notifyDataSetChanged();
        markAsChanged();
        codeBlockRecyclerView.recalculateWidth();
        Toast.makeText(this, "已剪切", Toast.LENGTH_SHORT).show();
    }

    // 粘贴代码块
    private void pasteBlock(int position) {
        if (clipboardBlock == null) {
            Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show();
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

    // 删除代码块
    private void deleteBlock(int position) {
        // 保护起始块（第一个块）不被删除
        if (position == 0) {
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

    // 清空所有代码块
    private void clearAllBlocks() {
        new AlertDialog.Builder(this)
                .setTitle("确认")
                .setMessage("确定要清空当前标签页的所有代码块吗?\n（起始块将保留）")
                .setPositiveButton("确定", (dialog, which) -> {
                    List<CodeBlock> blocks = currentTab.getCodeBlocks();
                    // 保留第一个起始块
                    if (blocks.size() > 1) {
                        blocks.subList(1, blocks.size()).clear();
                    }
                    codeBlockAdapter.notifyDataSetChanged();
                    markAsChanged();
                    codeBlockRecyclerView.recalculateWidth();
                    Toast.makeText(MainActivity.this, "已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 运行代码
    private void runCode() {
        String luaCode = generateLuaCode();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("生成的Lua代码");

        ScrollView scrollView = new ScrollView(this);
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(luaCode);
        textView.setPadding(40, 40, 40, 40);
        textView.setTextIsSelectable(true);
        textView.setTextSize(14);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton("关闭", null);
        builder.show();
    }

    // 导出代码
    private void exportCode() {
        String luaCode = generateLuaCode();
        Toast.makeText(this, "导出功能待实现\n代码已复制到剪贴板", Toast.LENGTH_SHORT).show();

        // 复制到剪贴板
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Lua Code", luaCode);
        clipboard.setPrimaryClip(clip);
    }

    // 生成Lua代码
    private String generateLuaCode() {
        StringBuilder sb = new StringBuilder();

        // 首先生成所有函数
        for (CodeTab tab : tabs) {
            if (tab.getType() == CodeTab.TabType.FUNCTION && !tab.getCodeBlocks().isEmpty()) {
                sb.append("function ").append(tab.getName()).append("()\n");

                for (CodeBlock block : tab.getCodeBlocks()) {
                    for (int i = 0; i < block.getIndentLevel() + 1; i++) {
                        sb.append("    ");
                    }
                    sb.append(block.generateCode()).append("\n");
                }

                sb.append("end\n\n");
            }
        }

        // 然后生成主程序
        CodeTab mainTab = tabs.get(0);
        sb.append("-- 主程序\n");
        for (CodeBlock block : mainTab.getCodeBlocks()) {
            for (int i = 0; i < block.getIndentLevel(); i++) {
                sb.append("    ");
            }
            sb.append(block.generateCode()).append("\n");
        }

        return sb.toString();
    }

    // ========== 保存/加载相关方法 ==========

    /**
     * 创建默认项目
     */
    private void createDefaultProject() {
        tabs = new ArrayList<>();
        CodeTab mainTab = new CodeTab(UUID.randomUUID().toString(), "主程序", CodeTab.TabType.MAIN);
        tabs.add(mainTab);
        currentTab = mainTab;
    }

    /**
     * 保存项目
     */
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

    /**
     * 静默保存（用于自动保存）
     */
    private void saveProjectSilently() {
        if (projectPath != null && hasUnsavedChanges) {
            ProjectManager.saveProject(projectPath, tabs);
            hasUnsavedChanges = false;
        }
    }

    /**
     * 标记有未保存的更改
     */
    private void markAsChanged() {
        hasUnsavedChanges = true;
    }

    /**
     * 设置自动保存
     */
    private void setupAutoSave() {
        autoSaveHandler = new android.os.Handler();
        autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                saveProjectSilently();
                autoSaveHandler.postDelayed(this, AUTO_SAVE_INTERVAL);
            }
        };
        // 启动自动保存
        autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停时保存
        saveProjectSilently();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止自动保存
        if (autoSaveHandler != null && autoSaveRunnable != null) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
        }
        // 销毁时保存
        saveProjectSilently();
    }
}
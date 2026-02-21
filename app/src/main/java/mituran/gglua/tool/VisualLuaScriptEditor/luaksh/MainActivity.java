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

    private String projectPath;
    private boolean hasUnsavedChanges = false;
    private static final long AUTO_SAVE_INTERVAL = 60000;
    private android.os.Handler autoSaveHandler;
    private Runnable autoSaveRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visual_lua_script_editor_activity_main);

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

        tabRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        codeBlockRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initData() {
        if (projectPath != null && ProjectManager.projectExists(projectPath)) {
            tabs = ProjectManager.loadProject(projectPath);
            if (tabs != null && !tabs.isEmpty()) {
                currentTab = tabs.get(0);
                Toast.makeText(this, "项目加载成功", Toast.LENGTH_SHORT).show();
            } else {
                createDefaultProject();
                Toast.makeText(this, "项目加载失败，已创建新项目", Toast.LENGTH_SHORT).show();
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
        codeBlockRecyclerView.setAdapter(codeBlockAdapter);
    }

    private void setupListeners() {
        customActionBar.setOnRunClickListener(v -> runCode());
        customActionBar.setOnSaveClickListener(v -> saveProject());
        customActionBar.setOnAddBlockClickListener(v -> showExpandableBlockSelector(-1));
        customActionBar.setOnDefineFunctionClickListener(v -> createNewFunction());
        customActionBar.setOnClearClickListener(v -> clearAllBlocks());
        customActionBar.setOnExportClickListener(v -> exportCode());
    }

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

        final android.widget.EditText input = new android.widget.EditText(this);
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

        final android.widget.EditText input = new android.widget.EditText(this);
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
                newTab.addStartBlock(); // 添加函数起始块
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

    private void showBlockActionDialog(int position) {
        CodeBlock block = currentTab.getCodeBlocks().get(position);

        // 特殊起始块只能编辑参数（对于函数起始块）
        if (block.getType().isSpecialStartBlock()) {
            if (block.getType() == CodeBlockType.FUNCTION_START) {
                Toast.makeText(this, "在输入框中编辑函数参数", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "这是程序入口标识块", Toast.LENGTH_SHORT).show();
            }
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

        CodeBlockTypeItem customFunctions = new CodeBlockTypeItem("⚙️ 自定义函数");
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

            // 跳过特殊起始块
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

            // 特殊起始块始终在最外层
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

        // 不允许剪切特殊起始块
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

        // 不允许粘贴特殊起始块
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

        // 不允许删除特殊起始块
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
                    // 保留特殊起始块
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

    private void exportCode() {
        String luaCode = generateLuaCode();
        Toast.makeText(this, "导出功能待实现\n代码已复制到剪贴板", Toast.LENGTH_SHORT).show();

        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Lua Code", luaCode);
        clipboard.setPrimaryClip(clip);
    }

    private String generateLuaCode() {
        StringBuilder sb = new StringBuilder();

        // 首先生成所有函数
        for (CodeTab tab : tabs) {
            if (tab.getType() == CodeTab.TabType.FUNCTION && !tab.getCodeBlocks().isEmpty()) {
                // 使用函数头生成方法（包含参数）
                sb.append(tab.generateFunctionHeader()).append("\n");

                for (CodeBlock block : tab.getCodeBlocks()) {
                    // 跳过特殊起始块
                    if (block.getType().isSpecialStartBlock()) {
                        continue;
                    }

                    String code = block.generateCode();
                    if (code != null) {
                        for (int i = 0; i < block.getIndentLevel() + 1; i++) {
                            sb.append("    ");
                        }
                        sb.append(code).append("\n");
                    }
                }

                sb.append("end\n\n");
            }
        }

        // 然后生成主程序
        CodeTab mainTab = tabs.get(0);
        sb.append("-- 主程序\n");
        for (CodeBlock block : mainTab.getCodeBlocks()) {
            // 跳过特殊起始块
            if (block.getType().isSpecialStartBlock()) {
                continue;
            }

            String code = block.generateCode();
            if (code != null) {
                for (int i = 0; i < block.getIndentLevel(); i++) {
                    sb.append("    ");
                }
                sb.append(code).append("\n");
            }
        }

        return sb.toString();
    }

    private void createDefaultProject() {
        tabs = new ArrayList<>();
        CodeTab mainTab = new CodeTab(UUID.randomUUID().toString(), "主程序", CodeTab.TabType.MAIN);
        mainTab.addStartBlock(); // 添加主程序起始块
        tabs.add(mainTab);
        currentTab = mainTab;
    }

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

    private void setupAutoSave() {
        autoSaveHandler = new android.os.Handler();
        autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                saveProjectSilently();
                autoSaveHandler.postDelayed(this, AUTO_SAVE_INTERVAL);
            }
        };
        autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_INTERVAL);
    }

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
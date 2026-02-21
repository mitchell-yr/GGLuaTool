package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import java.util.ArrayList;
import java.util.List;

public class CodeBlockTypeItem {
    private String categoryName;
    private List<DynamicCodeBlockType> blockTypes;
    private boolean isExpanded;

    public CodeBlockTypeItem(String categoryName) {
        this.categoryName = categoryName;
        this.blockTypes = new ArrayList<>();
        this.isExpanded = false;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public List<DynamicCodeBlockType> getBlockTypes() {
        return blockTypes;
    }

    public void addBlockType(CodeBlockType type) {
        if (type.isSpecialStartBlock()) {
            return;
        }
        blockTypes.add(DynamicCodeBlockType.fromCodeBlockType(type));
    }

    public void addDynamicBlockType(DynamicCodeBlockType type) {
        blockTypes.add(type);
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public void toggleExpanded() {
        isExpanded = !isExpanded;
    }

    public static List<CodeBlockTypeItem> createAllCategories() {
        List<CodeBlockTypeItem> categories = new ArrayList<>();

        // 注释
        CodeBlockTypeItem comments = new CodeBlockTypeItem("💬 注释");
        comments.addBlockType(CodeBlockType.COMMENT);
        categories.add(comments);

        // 系统输出（移除了INPUT）
        CodeBlockTypeItem systemIO = new CodeBlockTypeItem("📤 系统输出");
        systemIO.addBlockType(CodeBlockType.PRINT);
        categories.add(systemIO);

        // 变量操作
        CodeBlockTypeItem variables = new CodeBlockTypeItem("📊 变量操作");
        variables.addBlockType(CodeBlockType.VARIABLE_ASSIGN);
        variables.addBlockType(CodeBlockType.VARIABLE_DECLARE);
        variables.addBlockType(CodeBlockType.LOCAL_VARIABLE);
        categories.add(variables);

        // 控制流程
        CodeBlockTypeItem control = new CodeBlockTypeItem("🔀 控制流程");
        control.addBlockType(CodeBlockType.IF);
        control.addBlockType(CodeBlockType.ELSEIF);
        control.addBlockType(CodeBlockType.ELSE);
        control.addBlockType(CodeBlockType.END);
        categories.add(control);

        // 循环语句
        CodeBlockTypeItem loops = new CodeBlockTypeItem("🔄 循环语句");
        loops.addBlockType(CodeBlockType.FOR);
        loops.addBlockType(CodeBlockType.WHILE);
        loops.addBlockType(CodeBlockType.REPEAT);
        loops.addBlockType(CodeBlockType.UNTIL);
        loops.addBlockType(CodeBlockType.BREAK);
        categories.add(loops);

        // 函数操作
        CodeBlockTypeItem functions = new CodeBlockTypeItem("⚙️ 函数操作");
        functions.addBlockType(CodeBlockType.FUNCTION);
        functions.addBlockType(CodeBlockType.RETURN);
        functions.addBlockType(CodeBlockType.FUNCTION_CALL);
        categories.add(functions);

        // 表操作
        CodeBlockTypeItem tables = new CodeBlockTypeItem("📋 表操作");
        tables.addBlockType(CodeBlockType.TABLE_CREATE);
        tables.addBlockType(CodeBlockType.TABLE_INSERT);
        tables.addBlockType(CodeBlockType.TABLE_ACCESS);
        categories.add(tables);

        // ===== GameGuardian 分类 =====

        // GG 搜索
        CodeBlockTypeItem ggSearch = new CodeBlockTypeItem("🔍 GG 搜索");
        ggSearch.addBlockType(CodeBlockType.GG_SEARCH_NUMBER);
        ggSearch.addBlockType(CodeBlockType.GG_SEARCH_ADDRESS);
        ggSearch.addBlockType(CodeBlockType.GG_START_FUZZY);
        ggSearch.addBlockType(CodeBlockType.GG_SEARCH_FUZZY);
        ggSearch.addBlockType(CodeBlockType.GG_SEARCH_POINTER);
        ggSearch.addBlockType(CodeBlockType.GG_REFINE_NUMBER);
        ggSearch.addBlockType(CodeBlockType.GG_REFINE_ADDRESS);
        categories.add(ggSearch);

        // GG 搜索结果
        CodeBlockTypeItem ggResults = new CodeBlockTypeItem("📊 GG 搜索结果");
        ggResults.addBlockType(CodeBlockType.GG_GET_RESULTS);
        ggResults.addBlockType(CodeBlockType.GG_GET_RESULTS_COUNT);
        ggResults.addBlockType(CodeBlockType.GG_CLEAR_RESULTS);
        ggResults.addBlockType(CodeBlockType.GG_LOAD_RESULTS);
        ggResults.addBlockType(CodeBlockType.GG_REMOVE_RESULTS);
        ggResults.addBlockType(CodeBlockType.GG_EDIT_ALL);
        ggResults.addBlockType(CodeBlockType.GG_GET_SELECTED_RESULTS);
        categories.add(ggResults);

        // GG 内存读写
        CodeBlockTypeItem ggMemory = new CodeBlockTypeItem("💾 GG 内存读写");
        ggMemory.addBlockType(CodeBlockType.GG_GET_VALUES);
        ggMemory.addBlockType(CodeBlockType.GG_SET_VALUES);
        ggMemory.addBlockType(CodeBlockType.GG_COPY_MEMORY);
        ggMemory.addBlockType(CodeBlockType.GG_ALLOCATE_PAGE);
        ggMemory.addBlockType(CodeBlockType.GG_DUMP_MEMORY);
        ggMemory.addBlockType(CodeBlockType.GG_GET_VALUES_RANGE);
        categories.add(ggMemory);

        // GG 保存列表
        CodeBlockTypeItem ggList = new CodeBlockTypeItem("📋 GG 保存列表");
        ggList.addBlockType(CodeBlockType.GG_ADD_LIST_ITEMS);
        ggList.addBlockType(CodeBlockType.GG_GET_LIST_ITEMS);
        ggList.addBlockType(CodeBlockType.GG_REMOVE_LIST_ITEMS);
        ggList.addBlockType(CodeBlockType.GG_CLEAR_LIST);
        ggList.addBlockType(CodeBlockType.GG_SAVE_LIST);
        ggList.addBlockType(CodeBlockType.GG_LOAD_LIST);
        ggList.addBlockType(CodeBlockType.GG_GET_SELECTED_LIST_ITEMS);
        categories.add(ggList);

        // GG 进程管理
        CodeBlockTypeItem ggProcess = new CodeBlockTypeItem("⚙️ GG 进程管理");
        ggProcess.addBlockType(CodeBlockType.GG_GET_TARGET_INFO);
        ggProcess.addBlockType(CodeBlockType.GG_GET_TARGET_PACKAGE);
        ggProcess.addBlockType(CodeBlockType.GG_PROCESS_PAUSE);
        ggProcess.addBlockType(CodeBlockType.GG_PROCESS_RESUME);
        ggProcess.addBlockType(CodeBlockType.GG_PROCESS_TOGGLE);
        ggProcess.addBlockType(CodeBlockType.GG_PROCESS_KILL);
        ggProcess.addBlockType(CodeBlockType.GG_IS_PROCESS_PAUSED);
        categories.add(ggProcess);

        // GG UI/对话框
        CodeBlockTypeItem ggUI = new CodeBlockTypeItem("🖥️ GG UI/对话框");
        ggUI.addBlockType(CodeBlockType.GG_ALERT);
        ggUI.addBlockType(CodeBlockType.GG_TOAST);
        ggUI.addBlockType(CodeBlockType.GG_PROMPT);
        ggUI.addBlockType(CodeBlockType.GG_CHOICE);
        ggUI.addBlockType(CodeBlockType.GG_MULTI_CHOICE);
        ggUI.addBlockType(CodeBlockType.GG_SET_VISIBLE);
        ggUI.addBlockType(CodeBlockType.GG_IS_VISIBLE);
        ggUI.addBlockType(CodeBlockType.GG_SHOW_UI_BUTTON);
        ggUI.addBlockType(CodeBlockType.GG_HIDE_UI_BUTTON);
        ggUI.addBlockType(CodeBlockType.GG_IS_CLICKED_UI_BUTTON);
        categories.add(ggUI);

        // GG 速度/时间
        CodeBlockTypeItem ggSpeed = new CodeBlockTypeItem("⏱️ GG 速度/时间");
        ggSpeed.addBlockType(CodeBlockType.GG_SET_SPEED);
        ggSpeed.addBlockType(CodeBlockType.GG_GET_SPEED);
        ggSpeed.addBlockType(CodeBlockType.GG_TIME_JUMP);
        ggSpeed.addBlockType(CodeBlockType.GG_UNRANDOMIZER);
        categories.add(ggSpeed);

        // GG 内存区域
        CodeBlockTypeItem ggRanges = new CodeBlockTypeItem("🗺️ GG 内存区域");
        ggRanges.addBlockType(CodeBlockType.GG_SET_RANGES);
        ggRanges.addBlockType(CodeBlockType.GG_GET_RANGES);
        ggRanges.addBlockType(CodeBlockType.GG_GET_RANGES_LIST);
        categories.add(ggRanges);

        // GG 工具/其他
        CodeBlockTypeItem ggTools = new CodeBlockTypeItem("🔧 GG 工具/其他");
        ggTools.addBlockType(CodeBlockType.GG_SLEEP);
        ggTools.addBlockType(CodeBlockType.GG_REQUIRE);
        ggTools.addBlockType(CodeBlockType.GG_COPY_TEXT);
        ggTools.addBlockType(CodeBlockType.GG_MAKE_REQUEST);
        ggTools.addBlockType(CodeBlockType.GG_BYTES);
        ggTools.addBlockType(CodeBlockType.GG_DISASM);
        ggTools.addBlockType(CodeBlockType.GG_NUMBER_FROM_LOCALE);
        ggTools.addBlockType(CodeBlockType.GG_NUMBER_TO_LOCALE);
        ggTools.addBlockType(CodeBlockType.GG_IS_PACKAGE_INSTALLED);
        ggTools.addBlockType(CodeBlockType.GG_SAVE_VARIABLE);
        ggTools.addBlockType(CodeBlockType.GG_GET_FILE);
        ggTools.addBlockType(CodeBlockType.GG_GET_LINE);
        ggTools.addBlockType(CodeBlockType.GG_GET_LOCALE);
        ggTools.addBlockType(CodeBlockType.GG_GET_ACTIVE_TAB);
        ggTools.addBlockType(CodeBlockType.GG_GOTO_ADDRESS);
        ggTools.addBlockType(CodeBlockType.GG_GET_SELECTED_ELEMENTS);
        ggTools.addBlockType(CodeBlockType.GG_SKIP_RESTORE_STATE);
        categories.add(ggTools);

        return categories;
    }
}
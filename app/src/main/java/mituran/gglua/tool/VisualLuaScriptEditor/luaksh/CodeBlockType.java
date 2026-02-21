package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

public enum CodeBlockType {
    // 特殊起始块（不导出代码）
    MAIN_START("主程序起始", "", "#607D8B", false, false, false),
    FUNCTION_START("函数起始", "", "#795548", false, false, false),

    // 注释
    COMMENT("注释", "", "#9E9E9E", false, false, false),

    // 系统输入输出
    PRINT("打印输出", "", "#4CAF50", false, false, false),

    // 变量操作
    VARIABLE_ASSIGN("变量赋值", "", "#2196F3", false, false, false),
    VARIABLE_DECLARE("变量声明", "", "#2196F3", false, false, false),
    LOCAL_VARIABLE("局部变量", "", "#2196F3", false, false, false),

    // 控制流程
    IF("条件判断 if", "", "#FF9800", true, false, false),
    ELSEIF("否则如果 elseif", "", "#FF9800", false, true, false),
    ELSE("否则 else", "", "#FF9800", false, true, false),
    END("结束 end", "", "#FF9800", false, false, true),

    // 循环语句
    FOR("for 循环", "i = 1, 10", "#9C27B0", true, false, false),
    WHILE("while 循环", "true", "#9C27B0", true, false, false),
    REPEAT("repeat 循环", "", "#9C27B0", true, false, false),
    UNTIL("until", "", "#9C27B0", false, false, true),
    BREAK("跳出循环", "", "#9C27B0", false, false, false),

    // 函数定义
    FUNCTION("函数定义", "", "#F44336", true, false, false),
    RETURN("返回", "", "#F44336", false, false, false),
    FUNCTION_CALL("调用函数", "", "#F44336", false, false, false),

    // 表操作
    TABLE_CREATE("创建表", "", "#00BCD4", false, false, false),
    TABLE_INSERT("插入表", "", "#00BCD4", false, false, false),
    TABLE_ACCESS("访问表元素", "", "#00BCD4", false, false, false),

    // ===== GameGuardian 搜索相关 =====
    GG_SEARCH_NUMBER("gg.searchNumber", "", "#E91E63", false, false, false),
    GG_SEARCH_ADDRESS("gg.searchAddress", "", "#E91E63", false, false, false),
    GG_SEARCH_FUZZY("gg.searchFuzzy", "", "#E91E63", false, false, false),
    GG_START_FUZZY("gg.startFuzzy", "", "#E91E63", false, false, false),
    GG_SEARCH_POINTER("gg.searchPointer", "", "#E91E63", false, false, false),
    GG_REFINE_NUMBER("gg.refineNumber", "", "#E91E63", false, false, false),
    GG_REFINE_ADDRESS("gg.refineAddress", "", "#E91E63", false, false, false),

    // ===== GameGuardian 结果相关 =====
    GG_GET_RESULTS("gg.getResults", "", "#E64A19", false, false, false),
    GG_GET_RESULTS_COUNT("gg.getResultsCount", "", "#E64A19", false, false, false),
    GG_CLEAR_RESULTS("gg.clearResults", "", "#E64A19", false, false, false),
    GG_LOAD_RESULTS("gg.loadResults", "", "#E64A19", false, false, false),
    GG_REMOVE_RESULTS("gg.removeResults", "", "#E64A19", false, false, false),
    GG_EDIT_ALL("gg.editAll", "", "#E64A19", false, false, false),
    GG_GET_SELECTED_RESULTS("gg.getSelectedResults", "", "#E64A19", false, false, false),

    // ===== GameGuardian 内存读写 =====
    GG_GET_VALUES("gg.getValues", "", "#1565C0", false, false, false),
    GG_SET_VALUES("gg.setValues", "", "#1565C0", false, false, false),
    GG_COPY_MEMORY("gg.copyMemory", "", "#1565C0", false, false, false),
    GG_ALLOCATE_PAGE("gg.allocatePage", "", "#1565C0", false, false, false),
    GG_DUMP_MEMORY("gg.dumpMemory", "", "#1565C0", false, false, false),
    GG_GET_VALUES_RANGE("gg.getValuesRange", "", "#1565C0", false, false, false),

    // ===== GameGuardian 保存列表 =====
    GG_ADD_LIST_ITEMS("gg.addListItems", "", "#00695C", false, false, false),
    GG_GET_LIST_ITEMS("gg.getListItems", "", "#00695C", false, false, false),
    GG_REMOVE_LIST_ITEMS("gg.removeListItems", "", "#00695C", false, false, false),
    GG_CLEAR_LIST("gg.clearList", "", "#00695C", false, false, false),
    GG_SAVE_LIST("gg.saveList", "", "#00695C", false, false, false),
    GG_LOAD_LIST("gg.loadList", "", "#00695C", false, false, false),
    GG_GET_SELECTED_LIST_ITEMS("gg.getSelectedListItems", "", "#00695C", false, false, false),

    // ===== GameGuardian 进程相关 =====
    GG_GET_TARGET_INFO("gg.getTargetInfo", "", "#4A148C", false, false, false),
    GG_GET_TARGET_PACKAGE("gg.getTargetPackage", "", "#4A148C", false, false, false),
    GG_PROCESS_PAUSE("gg.processPause", "", "#4A148C", false, false, false),
    GG_PROCESS_RESUME("gg.processResume", "", "#4A148C", false, false, false),
    GG_PROCESS_TOGGLE("gg.processToggle", "", "#4A148C", false, false, false),
    GG_PROCESS_KILL("gg.processKill", "", "#4A148C", false, false, false),
    GG_IS_PROCESS_PAUSED("gg.isProcessPaused", "", "#4A148C", false, false, false),

    // ===== GameGuardian UI/对话框 =====
    GG_ALERT("gg.alert", "", "#BF360C", false, false, false),
    GG_TOAST("gg.toast", "", "#BF360C", false, false, false),
    GG_PROMPT("gg.prompt", "", "#BF360C", false, false, false),
    GG_CHOICE("gg.choice", "", "#BF360C", false, false, false),
    GG_MULTI_CHOICE("gg.multiChoice", "", "#BF360C", false, false, false),
    GG_SET_VISIBLE("gg.setVisible", "", "#BF360C", false, false, false),
    GG_IS_VISIBLE("gg.isVisible", "", "#BF360C", false, false, false),
    GG_SHOW_UI_BUTTON("gg.showUiButton", "", "#BF360C", false, false, false),
    GG_HIDE_UI_BUTTON("gg.hideUiButton", "", "#BF360C", false, false, false),
    GG_IS_CLICKED_UI_BUTTON("gg.isClickedUiButton", "", "#BF360C", false, false, false),

    // ===== GameGuardian 速度/时间 =====
    GG_SET_SPEED("gg.setSpeed", "", "#FF6F00", false, false, false),
    GG_GET_SPEED("gg.getSpeed", "", "#FF6F00", false, false, false),
    GG_TIME_JUMP("gg.timeJump", "", "#FF6F00", false, false, false),
    GG_UNRANDOMIZER("gg.unrandomizer", "", "#FF6F00", false, false, false),

    // ===== GameGuardian 内存区域 =====
    GG_SET_RANGES("gg.setRanges", "", "#33691E", false, false, false),
    GG_GET_RANGES("gg.getRanges", "", "#33691E", false, false, false),
    GG_GET_RANGES_LIST("gg.getRangesList", "", "#33691E", false, false, false),

    // ===== GameGuardian 工具/其他 =====
    GG_SLEEP("gg.sleep", "", "#37474F", false, false, false),
    GG_REQUIRE("gg.require", "", "#37474F", false, false, false),
    GG_COPY_TEXT("gg.copyText", "", "#37474F", false, false, false),
    GG_MAKE_REQUEST("gg.makeRequest", "", "#37474F", false, false, false),
    GG_BYTES("gg.bytes", "", "#37474F", false, false, false),
    GG_DISASM("gg.disasm", "", "#37474F", false, false, false),
    GG_NUMBER_FROM_LOCALE("gg.numberFromLocale", "", "#37474F", false, false, false),
    GG_NUMBER_TO_LOCALE("gg.numberToLocale", "", "#37474F", false, false, false),
    GG_IS_PACKAGE_INSTALLED("gg.isPackageInstalled", "", "#37474F", false, false, false),
    GG_SAVE_VARIABLE("gg.saveVariable", "", "#37474F", false, false, false),
    GG_GET_FILE("gg.getFile", "", "#37474F", false, false, false),
    GG_GET_LINE("gg.getLine", "", "#37474F", false, false, false),
    GG_GET_LOCALE("gg.getLocale", "", "#37474F", false, false, false),
    GG_GET_ACTIVE_TAB("gg.getActiveTab", "", "#37474F", false, false, false),
    GG_GOTO_ADDRESS("gg.gotoAddress", "", "#37474F", false, false, false),
    GG_GET_SELECTED_ELEMENTS("gg.getSelectedElements", "", "#37474F", false, false, false),
    GG_SKIP_RESTORE_STATE("gg.skipRestoreState", "", "#37474F", false, false, false);

    private String displayName;
    private String defaultValue;
    private String color;
    private boolean isBlockStart;
    private boolean isBlockMiddle;
    private boolean isBlockEnd;

    CodeBlockType(String displayName, String defaultValue, String color,
                  boolean isBlockStart, boolean isBlockMiddle, boolean isBlockEnd) {
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.color = color;
        this.isBlockStart = isBlockStart;
        this.isBlockMiddle = isBlockMiddle;
        this.isBlockEnd = isBlockEnd;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getColor() {
        return color;
    }

    public boolean isBlockStart() {
        return isBlockStart;
    }

    public boolean isBlockMiddle() {
        return isBlockMiddle;
    }

    public boolean isBlockEnd() {
        return isBlockEnd;
    }

    public boolean isSpecialStartBlock() {
        return this == MAIN_START || this == FUNCTION_START;
    }

    public static String getFunctionCallDisplayName(String functionName) {
        return "调用 " + functionName;
    }

    public static String getFunctionCallDefaultValue(String functionName) {
        return functionName + "()";
    }

    public boolean isFunctionCall() {
        return displayName.startsWith("调用 ");
    }
}
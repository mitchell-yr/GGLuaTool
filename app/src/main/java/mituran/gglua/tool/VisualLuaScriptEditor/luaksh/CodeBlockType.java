package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

public enum CodeBlockType {
    // 注释
    COMMENT("注释", "-- 注释内容", "#9E9E9E", false, false, false),

    // 系统输入输出
    PRINT("打印输出", "\"Hello\"", "#4CAF50", false, false, false),
    INPUT("输入", "varName", "#4CAF50", false, false, false),

    // 变量操作
    VARIABLE_ASSIGN("变量赋值", "x = 10", "#2196F3", false, false, false),
    VARIABLE_DECLARE("变量声明", "varName", "#2196F3", false, false, false),
    LOCAL_VARIABLE("局部变量", "varName = value", "#2196F3", false, false, false),

    // 控制流程
    IF("条件判断 if", "condition", "#FF9800", true, false, false),
    ELSEIF("否则如果 elseif", "condition", "#FF9800", false, true, false),
    ELSE("否则 else", "", "#FF9800", false, true, false),
    END("结束 end", "", "#FF9800", false, false, true),

    // 循环语句
    FOR("for 循环", "i = 1, 10", "#9C27B0", true, false, false),
    WHILE("while 循环", "condition", "#9C27B0", true, false, false),
    REPEAT("repeat 循环", "", "#9C27B0", true, false, false),
    UNTIL("until", "condition", "#9C27B0", false, false, true),
    BREAK("跳出循环", "", "#9C27B0", false, false, false),

    // 函数定义
    FUNCTION("函数定义", "myFunction(params)", "#F44336", true, false, false),
    RETURN("返回", "value", "#F44336", false, false, false),
    FUNCTION_CALL("调用函数", "myFunction(args)", "#F44336", false, false, false),

    // 表操作
    TABLE_CREATE("创建表", "myTable", "#00BCD4", false, false, false),
    TABLE_INSERT("插入表", "myTable, value", "#00BCD4", false, false, false),
    TABLE_ACCESS("访问表元素", "myTable[key]", "#00BCD4", false, false, false);

    private String displayName;
    private String defaultValue;
    private String color;
    private boolean isBlockStart;  // 是否是块开始(如if, for, while, function)
    private boolean isBlockMiddle; // 是否是块中间(如elseif, else)
    private boolean isBlockEnd;    // 是否是块结束(如end, until)

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

    // 获取函数调用的显示名称
    public static String getFunctionCallDisplayName(String functionName) {
        return "调用 " + functionName;
    }

    // 获取函数调用的默认值
    public static String getFunctionCallDefaultValue(String functionName) {
        return functionName + "()";
    }

    // 检查是否是函数调用类型（通过displayName判断）
    public boolean isFunctionCall() {
        return displayName.startsWith("调用 ");
    }
}
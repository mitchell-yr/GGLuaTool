package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

public class DynamicCodeBlockType {
    private String displayName;
    private String defaultValue;
    private String color;
    private boolean isBlockStart;
    private boolean isBlockMiddle;
    private boolean isBlockEnd;
    private String functionName; // 用于标识函数调用

    private DynamicCodeBlockType(String displayName, String defaultValue, String color,
                                 boolean isBlockStart, boolean isBlockMiddle, boolean isBlockEnd,
                                 String functionName) {
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.color = color;
        this.isBlockStart = isBlockStart;
        this.isBlockMiddle = isBlockMiddle;
        this.isBlockEnd = isBlockEnd;
        this.functionName = functionName;
    }

    // 创建函数调用类型
    public static DynamicCodeBlockType createFunctionCall(String functionName) {
        return new DynamicCodeBlockType(
            "调用 " + functionName,
            functionName + "()",
            "#F44336",
            false,
            false,
            false,
            functionName
        );
    }

    // 从CodeBlockType创建
    public static DynamicCodeBlockType fromCodeBlockType(CodeBlockType type) {
        return new DynamicCodeBlockType(
            type.getDisplayName(),
            type.getDefaultValue(),
            type.getColor(),
            type.isBlockStart(),
            type.isBlockMiddle(),
            type.isBlockEnd(),
            null
        );
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

    public String getFunctionName() {
        return functionName;
    }

    public boolean isFunctionCall() {
        return functionName != null;
    }
    
    // 转换为CodeBlockType（如果可能）
    public CodeBlockType toCodeBlockType() {
        if (isFunctionCall()) {
            return CodeBlockType.FUNCTION_CALL;
        }
        // 尝试匹配已有的枚举类型
        for (CodeBlockType type : CodeBlockType.values()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        return CodeBlockType.FUNCTION_CALL; // 默认返回
    }
}

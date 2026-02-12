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

    // 创建所有分类
    public static List<CodeBlockTypeItem> createAllCategories() {
        List<CodeBlockTypeItem> categories = new ArrayList<>();

        // 注释
        CodeBlockTypeItem comments = new CodeBlockTypeItem("💬 注释");
        comments.addBlockType(CodeBlockType.COMMENT);
        categories.add(comments);

        // 系统输入输出
        CodeBlockTypeItem systemIO = new CodeBlockTypeItem("📤 系统输入输出");
        systemIO.addBlockType(CodeBlockType.PRINT);
        systemIO.addBlockType(CodeBlockType.INPUT);
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

        return categories;
    }
}
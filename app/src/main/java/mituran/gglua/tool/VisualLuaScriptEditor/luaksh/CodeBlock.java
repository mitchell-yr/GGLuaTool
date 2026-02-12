package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import java.util.ArrayList;
import java.util.List;

public class CodeBlock {
    private CodeBlockType type;
    private String value;  // 保留用于兼容
    private int indentLevel;
    private List<CodeBlockStructure.Part> parts;  // 新的结构化内容

    public CodeBlock(CodeBlockType type, String value, int indentLevel) {
        this.type = type;
        this.value = value;
        this.indentLevel = indentLevel;
        // 初始化parts结构
        this.parts = CodeBlockStructure.parseOldValue(type, value);
    }

    public CodeBlockType getType() {
        return type;
    }

    public void setType(CodeBlockType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        // 更新parts
        this.parts = CodeBlockStructure.parseOldValue(type, value);
    }

    public List<CodeBlockStructure.Part> getParts() {
        if (parts == null || parts.isEmpty()) {
            parts = CodeBlockStructure.parseOldValue(type, value);
        }
        return parts;
    }

    public void setParts(List<CodeBlockStructure.Part> parts) {
        this.parts = parts;
    }

    public int getIndentLevel() {
        return indentLevel;
    }

    public void setIndentLevel(int indentLevel) {
        this.indentLevel = indentLevel;
    }

    // 生成Lua代码
    public String generateCode() {
        if (parts != null && !parts.isEmpty()) {
            return CodeBlockStructure.generateCode(type, parts);
        }

        // 兼容旧的生成方式
        switch (type) {
            case COMMENT:
                return value;
            case PRINT:
                return "print(" + value + ")";
            case INPUT:
                return value + " = io.read()";
            case VARIABLE_ASSIGN:
                return value;
            case VARIABLE_DECLARE:
                return value + " = nil";
            case LOCAL_VARIABLE:
                return "local " + value;
            case IF:
                return "if " + value + " then";
            case ELSEIF:
                return "elseif " + value + " then";
            case ELSE:
                return "else";
            case END:
                return "end";
            case FOR:
                return "for " + value + " do";
            case WHILE:
                return "while " + value + " do";
            case REPEAT:
                return "repeat";
            case UNTIL:
                return "until " + value;
            case BREAK:
                return "break";
            case FUNCTION:
                return "function " + value;
            case RETURN:
                return "return " + value;
            case FUNCTION_CALL:
                return value;
            case TABLE_CREATE:
                return value + " = {}";
            case TABLE_INSERT:
                return "table.insert(" + value + ")";
            case TABLE_ACCESS:
                return value;
            default:
                return value;
        }
    }

    // 复制代码块
    public CodeBlock copy() {
        CodeBlock newBlock = new CodeBlock(this.type, this.value, this.indentLevel);
        if (this.parts != null) {
            List<CodeBlockStructure.Part> copiedParts = new ArrayList<>();
            for (CodeBlockStructure.Part part : this.parts) {
                copiedParts.add(new CodeBlockStructure.Part(
                        part.type, part.text, part.value
                ));
            }
            newBlock.setParts(copiedParts);
        }
        return newBlock;
    }
}
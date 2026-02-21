package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import java.util.ArrayList;
import java.util.List;

public class CodeTab {
    private String id;
    private String name;
    private TabType type;
    private List<CodeBlock> codeBlocks;

    public enum TabType {
        MAIN,      // 主程序
        FUNCTION   // 函数
    }

    public CodeTab(String id, String name, TabType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.codeBlocks = new ArrayList<>();
    }

    // 添加起始代码块（仅在创建新Tab时调用）
    public void addStartBlock() {
        // 检查是否已经有起始块，避免重复添加
        if (!codeBlocks.isEmpty()) {
            CodeBlockType firstType = codeBlocks.get(0).getType();
            if (firstType == CodeBlockType.MAIN_START || firstType == CodeBlockType.FUNCTION_START) {
                return; // 已经有起始块，不重复添加
            }
        }

        if (type == TabType.MAIN) {
            // 主程序起始块
            CodeBlock startBlock = new CodeBlock(
                    CodeBlockType.MAIN_START,
                    "",
                    0
            );
            codeBlocks.add(0, startBlock);
        } else {
            // 函数起始块
            CodeBlock startBlock = new CodeBlock(
                    CodeBlockType.FUNCTION_START,
                    "",
                    0
            );
            codeBlocks.add(0, startBlock);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TabType getType() {
        return type;
    }

    public List<CodeBlock> getCodeBlocks() {
        return codeBlocks;
    }

    public void setCodeBlocks(List<CodeBlock> codeBlocks) {
        this.codeBlocks = codeBlocks;
    }

    /**
     * 获取函数参数（从FUNCTION_START块中获取）
     */
    public String getFunctionParams() {
        if (type != TabType.FUNCTION || codeBlocks.isEmpty()) {
            return "";
        }

        CodeBlock firstBlock = codeBlocks.get(0);
        if (firstBlock.getType() == CodeBlockType.FUNCTION_START) {
            List<CodeBlockStructure.Part> parts = firstBlock.getParts();
            return CodeBlockStructure.getInputValueWithDefault(parts, 0, "");
        }

        return "";
    }

    /**
     * 生成函数定义头（包含参数）
     */
    public String generateFunctionHeader() {
        if (type == TabType.FUNCTION) {
            String params = getFunctionParams();
            return "function " + name + "(" + params + ")";
        }
        return "";
    }
}
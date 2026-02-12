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

        // 添加起始代码块
        addStartBlock();
    }

    // 添加起始代码块
    private void addStartBlock() {
        if (type == TabType.MAIN) {
            // 主程序起始块
            CodeBlock startBlock = new CodeBlock(
                    CodeBlockType.COMMENT,
                    "-- 主程序开始",
                    0
            );
            codeBlocks.add(startBlock);
        } else {
            // 函数起始块
            CodeBlock startBlock = new CodeBlock(
                    CodeBlockType.COMMENT,
                    "-- 函数: " + name,
                    0
            );
            codeBlocks.add(startBlock);
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

    // 生成函数定义代码
    public String generateFunctionDefinition() {
        if (type == TabType.FUNCTION && !codeBlocks.isEmpty()) {
            return "function " + name + "\n";
        }
        return "";
    }

    // 生成函数结束代码
    public String generateFunctionEnd() {
        if (type == TabType.FUNCTION && !codeBlocks.isEmpty()) {
            return "end\n";
        }
        return "";
    }
}
package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import java.util.ArrayList;
import java.util.List;

/**
 * 定义代码块的显示结构
 * 每个代码块由多个部分组成：标签（Label）和输入框（Input）
 */
public class CodeBlockStructure {

    public enum PartType {
        LABEL,  // 固定文本标签
        INPUT   // 可编辑输入框
    }

    public static class Part {
        PartType type;
        String text;      // 对于LABEL是显示文本，对于INPUT是hint
        String value;     // 对于INPUT存储实际值

        public Part(PartType type, String text) {
            this.type = type;
            this.text = text;
            this.value = "";
        }

        public Part(PartType type, String text, String value) {
            this.type = type;
            this.text = text;
            this.value = value;
        }
    }

    private List<Part> parts;

    public CodeBlockStructure() {
        this.parts = new ArrayList<>();
    }

    public CodeBlockStructure addLabel(String text) {
        parts.add(new Part(PartType.LABEL, text));
        return this;
    }

    public CodeBlockStructure addInput(String hint) {
        parts.add(new Part(PartType.INPUT, hint));
        return this;
    }

    public CodeBlockStructure addInput(String hint, String defaultValue) {
        parts.add(new Part(PartType.INPUT, hint, defaultValue));
        return this;
    }

    public List<Part> getParts() {
        return parts;
    }

    /**
     * 根据CodeBlockType获取对应的结构
     */
    public static CodeBlockStructure getStructure(CodeBlockType type) {
        CodeBlockStructure structure = new CodeBlockStructure();

        switch (type) {
            case COMMENT:
                return structure.addLabel("💬").addInput("注释内容");

            case PRINT:
                return structure.addLabel("打印").addInput("内容");

            case INPUT:
                return structure.addLabel("输入到变量").addInput("变量名");

            case VARIABLE_ASSIGN:
                return structure.addLabel("变量").addInput("变量名")
                        .addLabel("赋值为").addInput("值");

            case VARIABLE_DECLARE:
                return structure.addLabel("声明变量").addInput("变量名");

            case LOCAL_VARIABLE:
                return structure.addLabel("局部变量").addInput("变量名")
                        .addLabel("=").addInput("初始值");

            case IF:
                return structure.addLabel("如果").addInput("条件")
                        .addLabel("成立");

            case ELSEIF:
                return structure.addLabel("否则如果").addInput("条件")
                        .addLabel("成立");

            case ELSE:
                return structure.addLabel("否则");

            case END:
                return structure.addLabel("结束");

            case FOR:
                return structure.addLabel("循环").addInput("变量名")
                        .addLabel("从").addInput("起始值")
                        .addLabel("到").addInput("结束值");

            case WHILE:
                return structure.addLabel("当").addInput("条件")
                        .addLabel("成立时循环");

            case REPEAT:
                return structure.addLabel("重复执行");

            case UNTIL:
                return structure.addLabel("直到").addInput("条件")
                        .addLabel("成立");

            case BREAK:
                return structure.addLabel("跳出循环");

            case FUNCTION:
                return structure.addLabel("函数").addInput("函数名(参数)");

            case RETURN:
                return structure.addLabel("返回").addInput("值");

            case FUNCTION_CALL:
                return structure.addLabel("调用").addInput("函数名(参数)");

            case TABLE_CREATE:
                return structure.addLabel("创建表").addInput("表名");

            case TABLE_INSERT:
                return structure.addLabel("向表").addInput("表名")
                        .addLabel("插入").addInput("值");

            case TABLE_ACCESS:
                return structure.addLabel("表").addInput("表名")
                        .addLabel("[").addInput("索引").addLabel("]");

            default:
                return structure.addInput("内容");
        }
    }

    /**
     * 从parts生成Lua代码
     */
    public static String generateCode(CodeBlockType type, List<Part> parts) {
        StringBuilder code = new StringBuilder();

        switch (type) {
            case COMMENT:
                code.append("-- ").append(getInputValue(parts, 0));
                break;

            case PRINT:
                code.append("print(").append(getInputValue(parts, 0)).append(")");
                break;

            case INPUT:
                code.append(getInputValue(parts, 0)).append(" = io.read()");
                break;

            case VARIABLE_ASSIGN:
                code.append(getInputValue(parts, 0)).append(" = ")
                        .append(getInputValue(parts, 1));
                break;

            case VARIABLE_DECLARE:
                code.append(getInputValue(parts, 0)).append(" = nil");
                break;

            case LOCAL_VARIABLE:
                code.append("local ").append(getInputValue(parts, 0))
                        .append(" = ").append(getInputValue(parts, 1));
                break;

            case IF:
                code.append("if ").append(getInputValue(parts, 0)).append(" then");
                break;

            case ELSEIF:
                code.append("elseif ").append(getInputValue(parts, 0)).append(" then");
                break;

            case ELSE:
                code.append("else");
                break;

            case END:
                code.append("end");
                break;

            case FOR:
                code.append("for ").append(getInputValue(parts, 0))
                        .append(" = ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(" do");
                break;

            case WHILE:
                code.append("while ").append(getInputValue(parts, 0)).append(" do");
                break;

            case REPEAT:
                code.append("repeat");
                break;

            case UNTIL:
                code.append("until ").append(getInputValue(parts, 0));
                break;

            case BREAK:
                code.append("break");
                break;

            case FUNCTION:
                code.append("function ").append(getInputValue(parts, 0));
                break;

            case RETURN:
                String returnValue = getInputValue(parts, 0);
                if (returnValue.isEmpty()) {
                    code.append("return");
                } else {
                    code.append("return ").append(returnValue);
                }
                break;

            case FUNCTION_CALL:
                code.append(getInputValue(parts, 0));
                break;

            case TABLE_CREATE:
                code.append(getInputValue(parts, 0)).append(" = {}");
                break;

            case TABLE_INSERT:
                code.append("table.insert(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1)).append(")");
                break;

            case TABLE_ACCESS:
                code.append(getInputValue(parts, 0)).append("[")
                        .append(getInputValue(parts, 1)).append("]");
                break;

            default:
                code.append(getInputValue(parts, 0));
                break;
        }

        return code.toString();
    }

    /**
     * 获取第n个输入框的值
     */
    private static String getInputValue(List<Part> parts, int inputIndex) {
        int currentInputIndex = 0;
        for (Part part : parts) {
            if (part.type == PartType.INPUT) {
                if (currentInputIndex == inputIndex) {
                    return part.value != null && !part.value.isEmpty()
                            ? part.value : part.text; // 如果为空返回hint
                }
                currentInputIndex++;
            }
        }
        return "";
    }

    /**
     * 解析旧格式的value到新的parts结构
     */
    public static List<Part> parseOldValue(CodeBlockType type, String oldValue) {
        CodeBlockStructure structure = getStructure(type);
        List<Part> parts = new ArrayList<>(structure.getParts());

        if (oldValue == null || oldValue.isEmpty()) {
            return parts;
        }

        // 简单解析：将oldValue分配给第一个输入框
        for (Part part : parts) {
            if (part.type == PartType.INPUT) {
                part.value = oldValue;
                break;
            }
        }

        return parts;
    }
}
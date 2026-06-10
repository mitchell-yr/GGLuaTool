package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 封装生成的 Lua 代码及其行号到代码块的映射关系
 */
public class GeneratedLuaCode {
    private String code;
    private final List<int[]> lineMapping; // 每行对应 [tabIndex, blockIndex]，blockIndex=-1 为非块级行

    public GeneratedLuaCode() {
        this.lineMapping = new ArrayList<>();
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 记录一行代码的来源
     * @param tabIndex   标签页索引
     * @param blockIndex 代码块索引，-1 表示非块级行（函数头、end、注释、空行等）
     */
    public void addLine(int tabIndex, int blockIndex) {
        lineMapping.add(new int[]{tabIndex, blockIndex});
    }

    /**
     * 根据行号(1-based)获取对应信息
     * @return int[]{tabIndex, blockIndex}，行号无效返回 null
     */
    public int[] getLineInfo(int lineNumber) {
        int index = lineNumber - 1;
        if (index >= 0 && index < lineMapping.size()) {
            return lineMapping.get(index);
        }
        return null;
    }

    /**
     * 从 Lua 错误信息中提取行号
     * 错误格式: [string "script.lua"]:2: unexpected symbol ...
     * @return 行号(1-based)，无法解析返回 -1
     */
    public static int parseErrorLine(String errorMessage) {
        if (errorMessage == null) return -1;
        Pattern pattern = Pattern.compile(":(\\d+):");
        Matcher matcher = pattern.matcher(errorMessage);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}

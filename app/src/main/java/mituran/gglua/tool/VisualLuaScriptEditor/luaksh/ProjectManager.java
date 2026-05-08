package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目保存和加载管理器
 */
public class ProjectManager {
    private static final String FILE_NAME = "code.json";
    public static final String CURRENT_VERSION = "2.3";

    /**
     * 项目加载结果，包含解析状态和详细错误信息
     */
    public static class ProjectLoadResult {
        public List<CodeTab> tabs;
        public String errorType;       // "SUCCESS", "NO_FILE", "EMPTY_DATA", "PARSE_ERROR"
        public String errorMessage;    // 简短错误描述
        public String errorDetail;     // 详细错误信息（用于修复建议对话框）
        public String rawContent;      // 原始JSON内容
        public String fileVersion;     // 文件中记录的版本号

        public boolean isSuccess() {
            return "SUCCESS".equals(errorType) && tabs != null && !tabs.isEmpty();
        }

        /**
         * 获取带行号的格式化内容，用于在修复对话框中展示
         */
        public String getFormattedContent() {
            if (rawContent == null || rawContent.isEmpty()) return "(文件为空)";
            StringBuilder sb = new StringBuilder();
            String[] lines = rawContent.split("\n");
            for (int i = 0; i < lines.length; i++) {
                sb.append(String.format("%4d | %s\n", i + 1, lines[i]));
            }
            return sb.toString();
        }

        /**
         * 获取版本差异描述
         */
        public String getVersionDiff() {
            String fv = (fileVersion != null && !fileVersion.isEmpty()) ? fileVersion : "未知";
            return "文件版本: " + fv + "\n当前编辑器版本: " + CURRENT_VERSION;
        }
    }

    public static boolean saveProject(String path, List<CodeTab> tabs) {
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(path, FILE_NAME);

            JSONObject root = new JSONObject();
            root.put("version", CURRENT_VERSION);
            root.put("timestamp", System.currentTimeMillis());

            JSONArray tabsArray = new JSONArray();
            for (CodeTab tab : tabs) {
                tabsArray.put(tabToJson(tab));
            }
            root.put("tabs", tabsArray);

            FileWriter writer = new FileWriter(file);
            writer.write(root.toString(2));
            writer.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ProjectLoadResult loadProject(String path) {
        ProjectLoadResult result = new ProjectLoadResult();

        try {
            File file = new File(path, FILE_NAME);

            if (!file.exists()) {
                result.errorType = "NO_FILE";
                result.errorMessage = "项目文件不存在";
                return result;
            }

            // 读取原始内容（保留换行，便于展示）
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            String rawContent = content.toString();
            result.rawContent = rawContent;

            // 解析JSON
            JSONObject root;
            try {
                root = new JSONObject(rawContent);
            } catch (JSONException e) {
                result.errorType = "PARSE_ERROR";
                result.errorMessage = "JSON格式错误，无法解析项目文件";
                result.errorDetail = e.getMessage();
                return result;
            }

            // 读取版本号
            result.fileVersion = root.optString("version", "未知");

            // 解析tabs数组
            JSONArray tabsArray;
            try {
                tabsArray = root.getJSONArray("tabs");
            } catch (JSONException e) {
                result.errorType = "PARSE_ERROR";
                result.errorMessage = "缺少tabs数组或格式错误";
                result.errorDetail = e.getMessage();
                return result;
            }

            List<CodeTab> tabs = new ArrayList<>();
            for (int i = 0; i < tabsArray.length(); i++) {
                try {
                    JSONObject tabJson = tabsArray.getJSONObject(i);
                    CodeTab tab = jsonToTab(tabJson);
                    if (tab != null) {
                        tabs.add(tab);
                    }
                } catch (JSONException e) {
                    result.errorType = "PARSE_ERROR";
                    result.errorMessage = "解析第" + (i + 1) + "个标签页时JSON出错";
                    result.errorDetail = "标签页 #" + (i + 1) + ": " + e.getMessage();
                    return result;
                } catch (IllegalArgumentException e) {
                    result.errorType = "PARSE_ERROR";
                    result.errorMessage = "第" + (i + 1) + "个标签页包含无法识别的数据";
                    result.errorDetail = "标签页 #" + (i + 1) + " 中存在无法识别的代码块类型或枚举值:\n" + e.getMessage();
                    return result;
                }
            }

            if (tabs.isEmpty()) {
                result.errorType = "EMPTY_DATA";
                result.errorMessage = "项目数据为空（没有有效的标签页）";
                return result;
            }

            result.tabs = tabs;
            result.errorType = "SUCCESS";
            return result;

        } catch (Exception e) {
            result.errorType = "PARSE_ERROR";
            result.errorMessage = "解析失败: " + e.getClass().getSimpleName();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.errorDetail = sw.toString();
            return result;
        }
    }

    private static JSONObject tabToJson(CodeTab tab) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", tab.getId());
        json.put("name", tab.getName());
        json.put("type", tab.getType().name());

        JSONArray blocksArray = new JSONArray();
        for (CodeBlock block : tab.getCodeBlocks()) {
            blocksArray.put(blockToJson(block));
        }
        json.put("blocks", blocksArray);

        return json;
    }

    private static JSONObject blockToJson(CodeBlock block) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", block.getType().name());
        json.put("indentLevel", block.getIndentLevel());

        JSONArray partsArray = new JSONArray();
        List<CodeBlockStructure.Part> parts = block.getParts();
        if (parts != null) {
            for (CodeBlockStructure.Part part : parts) {
                JSONObject partJson = new JSONObject();
                partJson.put("type", part.type.name());
                partJson.put("text", part.text);
                partJson.put("value", part.value != null ? part.value : "");
                partsArray.put(partJson);
            }
        }
        json.put("parts", partsArray);

        return json;
    }

    private static CodeTab jsonToTab(JSONObject json) throws JSONException {
        String id = json.getString("id");
        String name = json.getString("name");
        String typeStr = json.getString("type");
        CodeTab.TabType type = CodeTab.TabType.valueOf(typeStr);

        // 创建标签页（不自动添加起始块）
        CodeTab tab = new CodeTab(id, name, type);

        // 加载代码块
        JSONArray blocksArray = json.getJSONArray("blocks");
        List<CodeBlock> blocks = tab.getCodeBlocks();

        for (int i = 0; i < blocksArray.length(); i++) {
            JSONObject blockJson = blocksArray.getJSONObject(i);
            CodeBlock block = jsonToBlock(blockJson);
            if (block != null) {
                blocks.add(block);
            }
        }

        // 确保有起始块（处理旧版本数据）
        if (blocks.isEmpty() || !blocks.get(0).getType().isSpecialStartBlock()) {
            // 如果没有起始块或第一个块不是特殊起始块，添加新的起始块
            tab.addStartBlock();
        }

        return tab;
    }

    private static CodeBlock jsonToBlock(JSONObject json) throws JSONException {
        String typeStr = json.getString("type");

        // 处理旧版本数据：将旧的COMMENT起始块转换为新的特殊起始块
        // 跳过旧版本的注释起始块（它们会被新的起始块替代）
        if (typeStr.equals("COMMENT")) {
            if (json.has("parts")) {
                JSONArray partsArray = json.getJSONArray("parts");
                for (int i = 0; i < partsArray.length(); i++) {
                    JSONObject partJson = partsArray.getJSONObject(i);
                    String value = partJson.optString("value", "");
                    // 检查是否是旧的起始注释块
                    if (value.contains("主程序开始") || value.contains("函数:") || value.contains("函数：")) {
                        return null; // 跳过这个块，它会被新的起始块替代
                    }
                }
            }
        }

        CodeBlockType type = CodeBlockType.valueOf(typeStr);
        int indentLevel = json.getInt("indentLevel");

        CodeBlock block = new CodeBlock(type, "", indentLevel);

        if (json.has("parts")) {
            JSONArray partsArray = json.getJSONArray("parts");
            List<CodeBlockStructure.Part> parts = new ArrayList<>();

            for (int i = 0; i < partsArray.length(); i++) {
                JSONObject partJson = partsArray.getJSONObject(i);
                String partTypeStr = partJson.getString("type");
                String text = partJson.getString("text");
                String value = partJson.optString("value", "");

                CodeBlockStructure.PartType partType =
                        CodeBlockStructure.PartType.valueOf(partTypeStr);

                CodeBlockStructure.Part part =
                        new CodeBlockStructure.Part(partType, text, value);
                parts.add(part);
            }

            block.setParts(parts);
        }

        return block;
    }

    public static boolean projectExists(String path) {
        File file = new File(path, FILE_NAME);
        return file.exists();
    }

    public static boolean deleteProject(String path) {
        File file = new File(path, FILE_NAME);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
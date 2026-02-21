package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目保存和加载管理器
 */
public class ProjectManager {
    private static final String FILE_NAME = "code.json";

    public static boolean saveProject(String path, List<CodeTab> tabs) {
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(path, FILE_NAME);

            JSONObject root = new JSONObject();
            root.put("version", "2.3");
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

    public static List<CodeTab> loadProject(String path) {
        try {
            File file = new File(path, FILE_NAME);

            if (!file.exists()) {
                return null;
            }

            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();

            JSONObject root = new JSONObject(content.toString());
            JSONArray tabsArray = root.getJSONArray("tabs");

            List<CodeTab> tabs = new ArrayList<>();
            for (int i = 0; i < tabsArray.length(); i++) {
                JSONObject tabJson = tabsArray.getJSONObject(i);
                CodeTab tab = jsonToTab(tabJson);
                if (tab != null) {
                    tabs.add(tab);
                }
            }

            return tabs.isEmpty() ? null : tabs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
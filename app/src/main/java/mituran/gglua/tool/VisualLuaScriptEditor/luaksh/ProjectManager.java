package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目保存和加载管理器
 * 保存格式：JSON（易于读取和编辑）
 * 文件名：code.json
 */
public class ProjectManager {
    private static final String FILE_NAME = "code.json";

    /**
     * 保存项目到指定路径
     * @param path 保存路径
     * @param tabs 所有标签页
     * @return 是否保存成功
     */
    public static boolean saveProject(String path, List<CodeTab> tabs) {
        try {
            // 确保目录存在
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 创建保存文件
            File file = new File(path, FILE_NAME);

            // 构建JSON数据
            JSONObject root = new JSONObject();
            root.put("version", "2.2");
            root.put("timestamp", System.currentTimeMillis());

            JSONArray tabsArray = new JSONArray();
            for (CodeTab tab : tabs) {
                tabsArray.put(tabToJson(tab));
            }
            root.put("tabs", tabsArray);

            // 写入文件
            FileWriter writer = new FileWriter(file);
            writer.write(root.toString(2)); // 缩进2空格，便于阅读
            writer.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从指定路径加载项目
     * @param path 加载路径
     * @return 标签页列表，如果失败返回null
     */
    public static List<CodeTab> loadProject(String path) {
        try {
            File file = new File(path, FILE_NAME);

            // 如果文件不存在，返回null
            if (!file.exists()) {
                return null;
            }

            // 读取文件
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();

            // 解析JSON
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

    /**
     * 将CodeTab转换为JSON
     */
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

    /**
     * 将CodeBlock转换为JSON
     */
    private static JSONObject blockToJson(CodeBlock block) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", block.getType().name());
        json.put("indentLevel", block.getIndentLevel());

        // 保存parts结构
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

    /**
     * 从JSON转换为CodeTab
     */
    private static CodeTab jsonToTab(JSONObject json) throws JSONException {
        String id = json.getString("id");
        String name = json.getString("name");
        String typeStr = json.getString("type");
        CodeTab.TabType type = CodeTab.TabType.valueOf(typeStr);

        // 创建标签页（不自动添加起始块）
        CodeTab tab = new CodeTab(id, name, type);

        // 加载代码块
        JSONArray blocksArray = json.getJSONArray("blocks");
        for (int i = 0; i < blocksArray.length(); i++) {
            JSONObject blockJson = blocksArray.getJSONObject(i);
            CodeBlock block = jsonToBlock(blockJson);
            if (block != null) {
                tab.getCodeBlocks().add(block);
            }
        }

        return tab;
    }

    /**
     * 从JSON转换为CodeBlock
     */
    private static CodeBlock jsonToBlock(JSONObject json) throws JSONException {
        String typeStr = json.getString("type");
        CodeBlockType type = CodeBlockType.valueOf(typeStr);
        int indentLevel = json.getInt("indentLevel");

        // 创建代码块
        CodeBlock block = new CodeBlock(type, "", indentLevel);

        // 加载parts
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

    /**
     * 检查保存文件是否存在
     */
    public static boolean projectExists(String path) {
        File file = new File(path, FILE_NAME);
        return file.exists();
    }

    /**
     * 删除保存文件
     */
    public static boolean deleteProject(String path) {
        File file = new File(path, FILE_NAME);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
package mituran.gglua.tool.model;

//插件信息数据模型

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Plugin {
    private String name;
    private String version;
    private String description;
    private boolean enable;
    private List<JSONObject> match;
    private String folderPath;

    public Plugin(String name, String folderPath) {
        this.name = name;
        this.folderPath = folderPath;
        this.match = new ArrayList<>();
    }

    public static Plugin fromJson(String name, String folderPath, JSONObject json) throws Exception {
        Plugin plugin = new Plugin(name, folderPath);
        plugin.version = json.optString("version", "1.0.0");
        plugin.description = json.optString("description", "");
        plugin.enable = json.optBoolean("enable", false);

        JSONArray matchArray = json.optJSONArray("match");
        if (matchArray != null) {
            for (int i = 0; i < matchArray.length(); i++) {
                plugin.match.add(matchArray.getJSONObject(i));
            }
        }

        return plugin;
    }

    public JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("description", description);
        json.put("enable", enable);

        JSONArray matchArray = new JSONArray();
        for (JSONObject obj : match) {
            matchArray.put(obj);
        }
        json.put("match", matchArray);

        return json;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEnable() { return enable; }
    public void setEnable(boolean enable) { this.enable = enable; }

    public List<JSONObject> getMatch() { return match; }
    public void setMatch(List<JSONObject> match) { this.match = match; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }
}
package mituran.gglua.tool.apktools;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 修改预设（.ggpreset）格式定义、保存、加载、导入与导出。
 *
 * 预设文件为纯 JSON（非 ZIP），保存在 /sdcard/GGtool/presets/ 目录。
 *
 * JSON 结构示例：
 * {
 *   "name": "我的常用配置",
 *   "timestamp": 1680000000000,
 *   "format_version": 1,
 *   "newAppName": "...",
 *   "newPackageName": "...",
 *   ...
 * }
 */
public class ModifyPreset {

    private static final String TAG = "ModifyPreset";

    public static final String EXTENSION = ".ggpreset";
    public static final int FORMAT_VERSION = 1;

    private static final File PRESET_DIR = new File("/sdcard/GGtool/presets/");
    private static final File EXPORT_DIR = new File("/sdcard/GGtool/export/");

    // ═══════════════════════════════════════════════════════════════
    //  数据模型
    // ═══════════════════════════════════════════════════════════════

    public static class PresetInfo {
        public String name;
        public long timestamp;

        // 基础信息
        public String newAppName;
        public String newPackageName;
        public String newVersionName;
        public String newIconPath;

        // 启动入口
        public boolean deleteEntry;
        public boolean keepHwAccel;
        public boolean keepSwAccel;

        // 脚本
        public boolean embedScript;
        public String scriptPath;

        // 精简
        public boolean slimApk;
        public boolean slimArsc;
        public boolean slimRes;
        public boolean slimDex;

        // 函数
        public boolean addFunctions;
        public List<String> selectedFunctionNames;

        // UI 美化
        public boolean uiBeautify;

        /**
         * 用当前 UI 的 ModifyOptions 填充 PresetInfo（除 apkPath）。
         */
        public static PresetInfo fromModifyOptions(ModifierActivity.ModifyOptions options, String presetName) {
            PresetInfo info = new PresetInfo();
            info.name = presetName;
            info.timestamp = System.currentTimeMillis();

            info.newAppName = options.newAppName != null ? options.newAppName : "";
            info.newPackageName = options.newPackageName != null ? options.newPackageName : "";
            info.newVersionName = options.newVersionName != null ? options.newVersionName : "";
            info.newIconPath = options.newIconPath;

            info.deleteEntry = options.deleteEntry;
            info.keepHwAccel = options.keepHwAccel;
            info.keepSwAccel = options.keepSwAccel;

            info.embedScript = options.embedScript;
            info.scriptPath = options.scriptPath;

            info.slimApk = options.slimApk;
            info.slimArsc = options.slimArsc;
            info.slimRes = options.slimRes;
            info.slimDex = options.slimDex;

            info.addFunctions = options.addFunctions;
            info.selectedFunctionNames = options.selectedFunctionNames != null
                    ? new ArrayList<>(options.selectedFunctionNames) : new ArrayList<>();

            info.uiBeautify = options.uiBeautify;

            return info;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  序列化 / 反序列化
    // ═══════════════════════════════════════════════════════════════

    private static JSONObject toJson(PresetInfo info) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", info.name);
        obj.put("timestamp", info.timestamp);
        obj.put("format_version", FORMAT_VERSION);

        obj.put("newAppName", info.newAppName);
        obj.put("newPackageName", info.newPackageName);
        obj.put("newVersionName", info.newVersionName);
        obj.put("newIconPath", info.newIconPath != null ? info.newIconPath : JSONObject.NULL);

        obj.put("deleteEntry", info.deleteEntry);
        obj.put("keepHwAccel", info.keepHwAccel);
        obj.put("keepSwAccel", info.keepSwAccel);

        obj.put("embedScript", info.embedScript);
        obj.put("scriptPath", info.scriptPath != null ? info.scriptPath : JSONObject.NULL);

        obj.put("slimApk", info.slimApk);
        obj.put("slimArsc", info.slimArsc);
        obj.put("slimRes", info.slimRes);
        obj.put("slimDex", info.slimDex);

        obj.put("addFunctions", info.addFunctions);

        JSONArray arr = new JSONArray();
        if (info.selectedFunctionNames != null) {
            for (String name : info.selectedFunctionNames) arr.put(name);
        }
        obj.put("selectedFunctionNames", arr);

        obj.put("uiBeautify", info.uiBeautify);

        return obj;
    }

    private static PresetInfo fromJson(JSONObject obj) throws JSONException {
        PresetInfo info = new PresetInfo();

        info.name = obj.optString("name", "未命名");
        info.timestamp = obj.optLong("timestamp", 0);
        int fmtVer = obj.optInt("format_version", 1);
        if (fmtVer > FORMAT_VERSION) {
            throw new JSONException("预设版本过新 (format_version=" + fmtVer + ")，请升级工具后再试");
        }

        info.newAppName = obj.optString("newAppName", "");
        info.newPackageName = obj.optString("newPackageName", "");
        info.newVersionName = obj.optString("newVersionName", "");
        info.newIconPath = obj.isNull("newIconPath") ? null : obj.optString("newIconPath", null);

        info.deleteEntry = obj.optBoolean("deleteEntry", false);
        info.keepHwAccel = obj.optBoolean("keepHwAccel", true);
        info.keepSwAccel = obj.optBoolean("keepSwAccel", false);

        info.embedScript = obj.optBoolean("embedScript", false);
        info.scriptPath = obj.isNull("scriptPath") ? null : obj.optString("scriptPath", null);

        info.slimApk = obj.optBoolean("slimApk", false);
        info.slimArsc = obj.optBoolean("slimArsc", false);
        info.slimRes = obj.optBoolean("slimRes", false);
        info.slimDex = obj.optBoolean("slimDex", false);

        info.addFunctions = obj.optBoolean("addFunctions", false);

        JSONArray arr = obj.optJSONArray("selectedFunctionNames");
        info.selectedFunctionNames = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                info.selectedFunctionNames.add(arr.getString(i));
            }
        }

        info.uiBeautify = obj.optBoolean("uiBeautify", false);

        return info;
    }

    // ═══════════════════════════════════════════════════════════════
    //  文件操作
    // ═══════════════════════════════════════════════════════════════

    public static File getPresetDir() { return PRESET_DIR; }
    public static File getExportDir() { return EXPORT_DIR; }

    /**
     * 保存预设到本地。
     */
    public static void save(PresetInfo info) throws IOException {
        PRESET_DIR.mkdirs();
        File file = new File(PRESET_DIR, sanitizeFileName(info.name) + EXTENSION);

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            JSONObject json = toJson(info);
            writer.write(json.toString(2));
        } catch (JSONException e) {
            throw new IOException("生成预设数据失败: " + e.getMessage(), e);
        }
        Log.d(TAG, "预设已保存: " + file.getAbsolutePath());
    }

    /**
     * 加载所有本地预设（按时间倒序）。
     */
    public static List<PresetInfo> loadAll() {
        List<PresetInfo> result = new ArrayList<>();
        if (!PRESET_DIR.exists() || !PRESET_DIR.isDirectory()) return result;

        File[] files = PRESET_DIR.listFiles(f -> f.isFile() && f.getName().endsWith(EXTENSION));
        if (files == null) return result;

        for (File f : files) {
            try {
                result.add(loadFromFile(f));
            } catch (Exception e) {
                Log.w(TAG, "跳过损坏的预设: " + f.getName() + " (" + e.getMessage() + ")");
            }
        }

        Collections.sort(result, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        return result;
    }

    /**
     * 按名称加载预设。
     */
    public static PresetInfo loadByName(String name) throws IOException {
        File file = new File(PRESET_DIR, sanitizeFileName(name) + EXTENSION);
        if (!file.exists()) throw new IOException("预设不存在: " + name);
        return loadFromFile(file);
    }

    /**
     * 删除预设。
     */
    public static boolean delete(String name) {
        File file = new File(PRESET_DIR, sanitizeFileName(name) + EXTENSION);
        boolean ok = file.delete();
        Log.d(TAG, "删除预设 " + name + ": " + ok);
        return ok;
    }

    /**
     * 导出预设到导出目录。
     */
    public static File exportToExportDir(String name) throws IOException {
        File src = new File(PRESET_DIR, sanitizeFileName(name) + EXTENSION);
        if (!src.exists()) throw new IOException("本地不存在预设: " + name);

        EXPORT_DIR.mkdirs();
        File dest = new File(EXPORT_DIR, sanitizeFileName(name) + EXTENSION);

        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) fos.write(buf, 0, len);
        }

        Log.d(TAG, "预设已导出到: " + dest);
        return dest;
    }

    /**
     * 从文件导入预设。
     */
    public static PresetInfo importPreset(File file) throws IOException {
        if (!file.exists()) throw new IOException("文件不存在: " + file);
        return loadFromFile(file);
    }

    private static PresetInfo loadFromFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        try {
            return fromJson(new JSONObject(sb.toString()));
        } catch (JSONException e) {
            throw new IOException("解析预设失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清除文件名中的非法字符。
     */
    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}

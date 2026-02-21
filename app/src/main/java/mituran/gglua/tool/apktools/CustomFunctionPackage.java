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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 自定义函数包（.ggfunc）格式定义、导入与导出。
 *
 * ─── 包格式（ZIP 结构）───────────────────────────────────────────
 * funcpack.json          —— 元数据（函数名、描述、适用版本等）
 * inner_96.smali         —— Script$xxx.smali（96.0 版，可选）
 * inner_101.smali        —— Script$xxx.smali（101.1 版，可选）
 * registration_96.smali  —— sleep 下方插入的注册代码（96.0 版，可选）
 * registration_101.smali —— sleep 下方插入的注册代码（101.1 版，可选）
 * ─────────────────────────────────────────────────────────────────
 *
 * funcpack.json 示例：
 * {
 *   "name": "playMusic",
 *   "description": "播放背景音乐",
 *   "version": "universal",      // "96" | "101" | "universal"
 *   "format_version": 1
 * }
 *
 * version 字段含义：
 *   "96"        — 仅有 96.0 版 smali
 *   "101"       — 仅有 101.1 版 smali
 *   "universal" — 同时包含两个版本的 smali
 */
public class CustomFunctionPackage {

    private static final String TAG = "CustomFunctionPackage";

    /** 函数包文件扩展名 */
    public static final String EXTENSION = ".ggfunc";

    /** 当前包格式版本号（向后兼容时递增） */
    public static final int FORMAT_VERSION = 1;

    /** 包内文件名常量 */
    private static final String META_FILE          = "funcpack.json";
    private static final String INNER_96_FILE      = "inner_96.smali";
    private static final String INNER_101_FILE     = "inner_101.smali";
    private static final String REG_96_FILE        = "registration_96.smali";
    private static final String REG_101_FILE       = "registration_101.smali";

    // ═══════════════════════════════════════════════════════════════
    //  数据模型
    // ═══════════════════════════════════════════════════════════════

    /** 函数包支持的版本范围 */
    public enum SupportedVersion {
        V96("96"),
        V101("101"),
        UNIVERSAL("universal");

        public final String key;
        SupportedVersion(String key) { this.key = key; }

        public static SupportedVersion fromKey(String key) {
            for (SupportedVersion v : values()) if (v.key.equals(key)) return v;
            return UNIVERSAL;
        }

        public boolean supports96()  { return this == V96  || this == UNIVERSAL; }
        public boolean supports101() { return this == V101 || this == UNIVERSAL; }
    }

    /** 解析后的函数包信息 */
    public static class PackageInfo {
        /** 函数名（对应 Script$xxx 的 xxx） */
        public String name;
        /** 用户可见的描述文字 */
        public String description;
        /** 支持的版本 */
        public SupportedVersion supportedVersion;
        /** Script$xxx 的 smali 源码（96.0 版，可能为 null） */
        public String inner96;
        /** Script$xxx 的 smali 源码（101.1 版，可能为 null） */
        public String inner101;
        /** sleep 下方注册代码（96.0 版，可能为 null） */
        public String registration96;
        /** sleep 下方注册代码（101.1 版，可能为 null） */
        public String registration101;

        /** 校验包是否完整可用 */
        public boolean isValid() {
            return name != null && !name.isEmpty()
                    && (inner96 != null || inner101 != null);
        }

        /**
         * 判断此包能否用于指定的 GG 版本。
         * @param ggVersion GgFunctionAdder.VERSION_96 / VERSION_101
         */
        public boolean supportsGgVersion(String ggVersion) {
            if (GgFunctionAdder.VERSION_96.equals(ggVersion))  return supportedVersion.supports96();
            if (GgFunctionAdder.VERSION_101.equals(ggVersion)) return supportedVersion.supports101();
            return false;
        }

        /**
         * 转为 GgFunctionAdder.FunctionEntry，供注入使用。
         */
        public GgFunctionAdder.FunctionEntry toFunctionEntry() {
            return new GgFunctionAdder.FunctionEntry(
                    name, inner96, inner101, registration96, registration101);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  导出（打包 → .ggfunc）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 将函数包信息导出为 .ggfunc 文件。
     *
     * @param info       要导出的函数包信息
     * @param outputFile 输出的 .ggfunc 文件
     * @throws IOException 导出失败
     */
    public static void export(PackageInfo info, File outputFile) throws IOException {
        if (!info.isValid()) throw new IOException("函数包信息不完整，无法导出");

        outputFile.getParentFile().mkdirs();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            // 写 meta
            JSONObject meta = new JSONObject();
            try {
                meta.put("name", info.name);
                meta.put("description", info.description != null ? info.description : "");
                meta.put("version", info.supportedVersion.key);
                meta.put("format_version", FORMAT_VERSION);
            } catch (JSONException e) {
                throw new IOException("生成元数据失败: " + e.getMessage(), e);
            }
            writeZipEntry(zos, META_FILE, meta.toString().getBytes(StandardCharsets.UTF_8));

            // 写 smali
            if (info.inner96 != null) {
                writeZipEntry(zos, INNER_96_FILE, info.inner96.getBytes(StandardCharsets.UTF_8));
            }
            if (info.inner101 != null) {
                writeZipEntry(zos, INNER_101_FILE, info.inner101.getBytes(StandardCharsets.UTF_8));
            }
            if (info.registration96 != null) {
                writeZipEntry(zos, REG_96_FILE, info.registration96.getBytes(StandardCharsets.UTF_8));
            }
            if (info.registration101 != null) {
                writeZipEntry(zos, REG_101_FILE, info.registration101.getBytes(StandardCharsets.UTF_8));
            }
        }

        Log.d(TAG, "函数包已导出: " + outputFile.getAbsolutePath());
    }

    // ═══════════════════════════════════════════════════════════════
    //  导入（解析 .ggfunc → PackageInfo）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 从 .ggfunc 文件导入函数包信息。
     *
     * @param packageFile .ggfunc 文件
     * @return 解析后的 PackageInfo
     * @throws IOException 文件损坏或格式不支持
     */
    public static PackageInfo importPackage(File packageFile) throws IOException {
        if (!packageFile.exists()) throw new IOException("文件不存在: " + packageFile);

        PackageInfo info = new PackageInfo();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(packageFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                byte[] data = readZipEntryBytes(zis);
                String text = new String(data, StandardCharsets.UTF_8);

                switch (name) {
                    case META_FILE:
                        parseMeta(text, info);
                        break;
                    case INNER_96_FILE:
                        info.inner96 = text;
                        break;
                    case INNER_101_FILE:
                        info.inner101 = text;
                        break;
                    case REG_96_FILE:
                        info.registration96 = text;
                        break;
                    case REG_101_FILE:
                        info.registration101 = text;
                        break;
                    default:
                        Log.w(TAG, "忽略未知文件: " + name);
                }
                zis.closeEntry();
            }
        }

        if (!info.isValid()) {
            throw new IOException("函数包内容不完整（缺少必要的 smali 文件或元数据）");
        }

        Log.d(TAG, "函数包导入成功: " + info.name + " [" + info.supportedVersion.key + "]");
        return info;
    }

    /**
     * 批量导入目录中的所有 .ggfunc 文件。
     *
     * @param dir 目录
     * @return 成功解析的函数包列表
     */
    public static List<PackageInfo> importAll(File dir) {
        List<PackageInfo> result = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) return result;

        File[] files = dir.listFiles(f -> f.isFile() && f.getName().endsWith(EXTENSION));
        if (files == null) return result;

        for (File f : files) {
            try {
                result.add(importPackage(f));
            } catch (Exception e) {
                Log.w(TAG, "跳过损坏的函数包: " + f.getName() + " (" + e.getMessage() + ")");
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  本地自定义函数持久化（存储到 /sdcard/GGtool/functions/）
    // ═══════════════════════════════════════════════════════════════

    private static final File CUSTOM_FUNC_DIR = new File("/sdcard/GGtool/functions/");
    private static final File EXPORT_DIR      = new File("/sdcard/GGtool/export/");

    /** 获取本地自定义函数目录 */
    public static File getCustomFuncDir() { return CUSTOM_FUNC_DIR; }
    /** 获取导出目录 */
    public static File getExportDir()     { return EXPORT_DIR; }

    /**
     * 保存（安装）一个函数包到本地自定义函数目录。
     *
     * @param info 函数包信息
     * @throws IOException
     */
    public static void saveLocally(PackageInfo info) throws IOException {
        CUSTOM_FUNC_DIR.mkdirs();
        File dest = new File(CUSTOM_FUNC_DIR, info.name + EXTENSION);
        export(info, dest);
        Log.d(TAG, "函数包已保存到本地: " + dest);
    }

    /**
     * 删除本地自定义函数包。
     *
     * @param functionName 函数名
     * @return 是否删除成功
     */
    public static boolean deleteLocally(String functionName) {
        File file = new File(CUSTOM_FUNC_DIR, functionName + EXTENSION);
        boolean ok = file.delete();
        Log.d(TAG, "删除函数包 " + functionName + ": " + ok);
        return ok;
    }

    /**
     * 加载所有本地自定义函数包。
     */
    public static List<PackageInfo> loadAllLocal() {
        return importAll(CUSTOM_FUNC_DIR);
    }

    /**
     * 导出函数包到导出目录（/sdcard/GGtool/export/）。
     *
     * @param functionName 函数名（对应本地已保存的包）
     * @throws IOException
     */
    public static File exportToExportDir(String functionName) throws IOException {
        File src = new File(CUSTOM_FUNC_DIR, functionName + EXTENSION);
        if (!src.exists()) throw new IOException("本地不存在函数包: " + functionName);

        EXPORT_DIR.mkdirs();
        File dest = new File(EXPORT_DIR, functionName + EXTENSION);

        // 直接复制文件
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) fos.write(buf, 0, len);
        }

        Log.d(TAG, "函数包已导出到: " + dest);
        return dest;
    }

    // ═══════════════════════════════════════════════════════════════
    //  私有工具
    // ═══════════════════════════════════════════════════════════════

    private static void parseMeta(String json, PackageInfo info) throws IOException {
        try {
            JSONObject obj = new JSONObject(json);
            info.name        = obj.optString("name", "");
            info.description = obj.optString("description", "");
            String verKey    = obj.optString("version", "universal");
            info.supportedVersion = SupportedVersion.fromKey(verKey);

            int fmtVer = obj.optInt("format_version", 1);
            if (fmtVer > FORMAT_VERSION) {
                throw new IOException("函数包版本过新 (format_version=" + fmtVer
                        + ")，请升级工具后再试");
            }
        } catch (JSONException e) {
            throw new IOException("解析函数包元数据失败: " + e.getMessage(), e);
        }
    }

    private static void writeZipEntry(ZipOutputStream zos, String name, byte[] data)
            throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private static byte[] readZipEntryBytes(ZipInputStream zis) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = zis.read(buf)) > 0) baos.write(buf, 0, len);
        return baos.toByteArray();
    }
}

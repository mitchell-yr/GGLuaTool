package mituran.gglua.tool.apktools;

import android.content.Context;
import android.util.Log;

import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.rewriter.DexRewriter;
import com.android.tools.smali.dexlib2.rewriter.Rewriter;
import com.android.tools.smali.dexlib2.rewriter.RewriterModule;
import com.android.tools.smali.dexlib2.rewriter.Rewriters;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;

import zhao.arsceditor.ResDecoder.ARSCDecoder;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.ResTypeSpec;
import zhao.arsceditor.ResDecoder.data.ResResSpec;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;
import zhao.arsceditor.ResDecoder.data.ResTable;

/**
 * 内置脚本嵌入器
 *
 * 流程：
 *  1. 从 resources.arsc 读取 string/version_number，判断版本 (96.0 / 101.1)
 *  2. 从 assets 加载对应版本的模板 smali（android/ext/hy.smali）
 *  3. 将 smali 中 pcall(load("xxxx")) 的 xxxx 替换为用户脚本内容
 *  4. 用 smali 工具将修改后的 smali 汇编为临时 dex
 *  5. 从临时 dex 提取 Landroid/ext/hy; 类，用 DexRewriter 注入到每个 classes*.dex
 */
public class ScriptEmbedder {

    private static final String TAG = "ScriptEmbedder";

    /**
     * smali 文件中 const-string 行的原始文本占位符。
     * smali 源文件里 \" 是两个真实字符（反斜杠 + 双引号），
     * 因此 Java 字符串需要用 \\\\ + \\\" 来表示它们。
     * 实际匹配的字面内容：pcall(load(\"xxxx\"))
     */
    private static final String PLACEHOLDER = "pcall(load(\\\"xxxx\\\"))";

    /** DEX 中 hy 类的类型描述符 */
    private static final String HY_CLASS_TYPE = "Landroid/ext/hy;";

    /**
     * 版本常量，与 resources.arsc string/version_number 对应
     */
    public static final String VERSION_96  = "96.0";
    public static final String VERSION_101 = "101.1";

    private final Context mContext;

    public ScriptEmbedder(Context context) {
        this.mContext = context;
    }

    // ─────────────────────────────────────────────────────────────────
    // 公开入口
    // ─────────────────────────────────────────────────────────────────

    /**
     * 将脚本嵌入已解压的 APK 目录。
     *
     * @param extractedDir  APK 解压目录（含 resources.arsc 和 classes*.dex）
     * @param scriptContent 用户 Lua 脚本的完整文本
     * @param workDir       临时工作目录（用于中间文件）
     * @throws IOException 任何 IO / 逻辑错误
     */
    public void embed(File extractedDir, String scriptContent, File workDir) throws IOException {

        // ── 1. 读取版本号 ──────────────────────────────────────────────
        File arscFile = new File(extractedDir, "resources.arsc");
        if (!arscFile.exists()) {
            throw new IOException("resources.arsc 不存在，无法确定版本");
        }
        String version = readVersionNumber(arscFile);
        Log.d(TAG, "检测到版本: " + version);

        if (!VERSION_96.equals(version) && !VERSION_101.equals(version)) {
            throw new IOException("不支持的版本号: " + version
                    + "（仅支持 " + VERSION_96 + " 和 " + VERSION_101 + "）");
        }

        // ── 2. 加载对应模板 smali ──────────────────────────────────────
        String smaliAssetPath = "";
        if (VERSION_96.equals(version))  {
            smaliAssetPath = "smali/" + "96" + "/scriptEmbed96.smali";
        }
        if (VERSION_101.equals(version))  {
            smaliAssetPath = "smali/" + "101" + "/scriptEmbed101.smali";
        }
        String smaliSource = loadAssetText(smaliAssetPath);
        Log.d(TAG, "已加载模板: " + smaliAssetPath + "（" + smaliSource.length() + " chars）");


        // ── 3. 替换脚本占位符 ──────────────────────────────────────────
        // PLACEHOLDER 的 Java 运行时值为：pcall(load(\"xxxx\"))
        // 与 smali 文件中 const-string 行的原始内容完全一致（\" 是真实的两个字符）。
        // 切勿在 replace() 中使用字面量，否则 Java \"-转义与 smali 格式会不匹配。
        if (!smaliSource.contains(PLACEHOLDER)) {
            throw new IOException("模板 smali 中未找到占位符，文件应含: " + PLACEHOLDER);
        }
        // 转义用户脚本，处理其中的 \\ \" \n 等，使内容可嵌入 smali 字符串
        String escapedScript = escapeSmaliString(scriptContent);
        // 构造替换目标：保持与 smali \" 格式一致
        // PLACEHOLDER = pcall(load(\"xxxx\"))
        // replacement  = pcall(load(\"<script>\"))
        String smaliQuote = "\\\"";   // Java value: \"  (backslash + double-quote)
        String replacement = "pcall(load(" + smaliQuote + escapedScript + smaliQuote + "))";
        String patchedSmali = smaliSource.replace(PLACEHOLDER, replacement);
        Log.d(TAG, "脚本占位符替换完成，脚本长度: " + escapedScript.length() + " chars");

        // ── 4. 汇编 smali → 临时 dex ──────────────────────────────────
        File smaliFile  = new File(workDir, "hy_patched.smali");
        File patchedDex = new File(workDir, "hy_patched.dex");
        writeText(smaliFile, patchedSmali);

        assembleSmali(smaliFile, patchedDex);
        Log.d(TAG, "smali 汇编完成: " + patchedDex.getAbsolutePath());

        // ── 5. 提取 hy 类并注入每个 classes*.dex ─────────────────────
        List<File> dexFiles = DexModifier.findDexFiles(extractedDir);
        if (dexFiles.isEmpty()) {
            throw new IOException("未找到任何 DEX 文件");
        }

        // 从补丁 dex 提取 hy ClassDef
        ClassDef hyClassDef = extractHyClass(patchedDex);
        if (hyClassDef == null) {
            throw new IOException("补丁 dex 中未找到 " + HY_CLASS_TYPE);
        }

        for (File dexFile : dexFiles) {
            if (containsHyClass(dexFile)) {
                Log.d(TAG, "注入 hy 到: " + dexFile.getName());
                injectHyClass(dexFile, hyClassDef, workDir);
            } else {
                Log.d(TAG, dexFile.getName() + " 不含 hy 类，跳过");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 读取 resources.arsc 中的 version_number
    // ─────────────────────────────────────────────────────────────────

    /**
     * 解析 resources.arsc，返回 string/version_number 的默认值。
     */
    private String readVersionNumber(File arscFile) throws IOException {
        byte[] arscBytes = readFileBytes(arscFile);
        InputStream stream = new ByteArrayInputStream(arscBytes);
        ResTable resTable = new ResTable();
        ARSCDecoder decoder = new ARSCDecoder(stream, resTable, false);
        ResPackage[] packages = decoder.readTable();
        stream.close();

        for (ResPackage pkg : packages) {
            if (pkg == null) continue;
            try {
                ResTypeSpec stringType = pkg.getType("string");
                if (stringType == null) continue;

                ResResSpec spec = null;
                try {
                    spec = stringType.getResSpec("version_number");
                } catch (Exception e) {
                    // 该包不含此资源
                }
                if (spec == null) continue;

                ResResource res = spec.getDefaultResource();
                if (res == null) continue;

                if (res.getValue() instanceof ResScalarValue) {
                    String raw = ((ResScalarValue) res.getValue()).encodeAsResValue();
                    if (raw != null && !raw.isEmpty()) {
                        return raw;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "读取包 " + pkg.getName() + " 时出错: " + e.getMessage());
            }
        }

        // 回退：在字符串池中查找
        return fallbackReadVersionFromStringPool(arscFile);
    }

    /**
     * 回退方法：遍历字符串池，寻找 "96.0" 或 "101.1"。
     * 适用于 ResTable API 无法定位 version_number 的情况。
     */
    private String fallbackReadVersionFromStringPool(File arscFile) throws IOException {
        ArscEditor editor = new ArscEditor(arscFile);
        List<String> strings = editor.getAllStrings();
        for (String s : strings) {
            if (VERSION_96.equals(s))  return VERSION_96;
            if (VERSION_101.equals(s)) return VERSION_101;
        }
        throw new IOException("无法从 resources.arsc 中读取 version_number（96.0 / 101.1）");
    }

    // ─────────────────────────────────────────────────────────────────
    // smali 汇编
    // ─────────────────────────────────────────────────────────────────

    /**
     * 调用 smali 库将单个 .smali 文件汇编为 .dex。
     * 使用反射调用 org.jf.smali.Main（或 com.android.tools.smali.smali.Main）
     * 避免硬编码 import，以兼容不同版本的 smali 库打包方式。
     */
    private void assembleSmali(File smaliFile, File outputDex) throws IOException {
        // smali 3.x: com.android.tools.smali.smali.Main
        // smali 2.x: org.jf.smali.Main
        // 两者都通过反射调用，不引入编译期依赖
        try {
            Class<?> smaliMain;
            try {
                smaliMain = Class.forName("com.android.tools.smali.smali.Main");
            } catch (ClassNotFoundException e) {
                smaliMain = Class.forName("org.jf.smali.Main");
            }

            // smali a [options] <input> -o <output>
            String[] args = new String[]{
                    "a",                                    // assemble 子命令
                    smaliFile.getAbsolutePath(),
                    "-o", outputDex.getAbsolutePath()
            };

            java.lang.reflect.Method mainMethod =
                    smaliMain.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);

            if (!outputDex.exists()) {
                throw new IOException("smali 汇编失败，输出文件不存在");
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("smali 汇编异常: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // DEX 注入
    // ─────────────────────────────────────────────────────────────────

    /** 检查 dex 文件是否包含 hy 类 */
    private boolean containsHyClass(File dexFile) {
        try {
            DexFile dex = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault());
            for (ClassDef cls : dex.getClasses()) {
                if (HY_CLASS_TYPE.equals(cls.getType())) return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "读取 " + dexFile.getName() + " 失败: " + e.getMessage());
        }
        return false;
    }

    /** 从补丁 dex 中提取 hy ClassDef */
    private ClassDef extractHyClass(File patchedDex) throws IOException {
        DexFile dex = DexFileFactory.loadDexFile(patchedDex, Opcodes.getDefault());
        for (ClassDef cls : dex.getClasses()) {
            if (HY_CLASS_TYPE.equals(cls.getType())) return cls;
        }
        return null;
    }

    /**
     * 将补丁 hy 类替换到目标 dex，原地覆盖。
     * 通过 DexRewriter 重写所有类：遇到 hy 类时跳过（删除旧版），
     * 然后用 DexPool 手动追加补丁 ClassDef。
     */
    private void injectHyClass(File targetDex, final ClassDef patchedHy, File workDir)
            throws IOException {

        File tmpOut = new File(workDir, "inject_tmp_" + targetDex.getName());

        try {
            DexFile original = DexFileFactory.loadDexFile(targetDex, Opcodes.getDefault());

            // 用 DexPool 手动组合：先写原有类（排除旧 hy），再写补丁 hy
            DexPool pool = new DexPool(Opcodes.getDefault());

            for (ClassDef cls : original.getClasses()) {
                if (!HY_CLASS_TYPE.equals(cls.getType())) {
                    pool.internClass(cls);
                }
            }
            // 注入补丁版 hy
            pool.internClass(patchedHy);

            pool.writeTo(new FileDataStore(tmpOut));

            // 原地替换
            if (!tmpOut.renameTo(targetDex)) {
                // renameTo 跨分区可能失败，手动复制
                copyFile(tmpOut, targetDex);
                tmpOut.delete();
            }

            Log.d(TAG, "hy 注入成功: " + targetDex.getName());

        } catch (Exception e) {
            if (tmpOut.exists()) tmpOut.delete();
            throw new IOException("注入 hy 到 " + targetDex.getName() + " 失败: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 辅助工具
    // ─────────────────────────────────────────────────────────────────

    /** 从 assets 读取文本文件 */
    private String loadAssetText(String assetPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = mContext.getAssets().open(assetPath);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 转义字符串，使其可安全嵌入 smali 的 const-string 字面量。
     * smali 字符串转义规则与 Java/C 类似。
     */
    private String escapeSmaliString(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 32);
        for (char ch : raw.toCharArray()) {
            switch (ch) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    /** 读取文件字节 */
    private byte[] readFileBytes(File file) throws IOException {
        byte[] buf = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = 0;
            while (read < buf.length) {
                int n = fis.read(buf, read, buf.length - read);
                if (n < 0) break;
                read += n;
            }
        }
        return buf;
    }

    /** 写文本到文件（UTF-8） */
    private void writeText(File file, String text) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** 复制文件 */
    private void copyFile(File src, File dst) throws IOException {
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) fos.write(buf, 0, len);
        }
    }
}
package mituran.gglua.tool.apktools;

import android.content.Context;
import android.util.Log;

import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * GG函数注入器
 *
 * 功能：
 * 1. 在 Script 类的 smali 中，找到 sleep 注册段，在其下方插入新函数注册代码
 * 2. 新建对应的 Script$xxx.smali 并编译进 DEX
 *
 * 函数注册 smali 片段格式（插入在 sleep 段之后）：
 *   const-string v5, "funcName"
 *   new-instance v6, Landroid/ext/Script$funcName;
 *   invoke-direct {v6}, Landroid/ext/Script$funcName;-><init>()V
 *   invoke-virtual {v3, v5, v6}, Lluaj/LuaTable;->b(Ljava/lang/String;Lluaj/LuaValue;)V
 */
public class GgFunctionAdder {

    private static final String TAG = "GgFunctionAdder";

    /** 版本号常量，与 ScriptEmbedder 一致 */
    public static final String VERSION_96  = "96.0";
    public static final String VERSION_101 = "101.1";

    /**
     * Script 类在 DEX 中的类型描述符
     * 注意：GG 的 Script 类路径为 android/ext/Script
     */
    private static final String SCRIPT_CLASS_TYPE = "Landroid/ext/Script;";

    /**
     * sleep 段的 const-string 标记行，全文唯一，用于定位插入点
     * （匹配 smali 文本中真实出现的内容，前后允许有空白）
     */
    private static final String SLEEP_ANCHOR =
            "const-string v5, \"sleep\"";

    private final Context mContext;

    public GgFunctionAdder(Context context) {
        this.mContext = context;
    }

    // ═══════════════════════════════════════════════════════════════
    //  公开入口
    // ═══════════════════════════════════════════════════════════════

    /**
     * 向已解压的 APK 目录注入一批函数。
     *
     * @param extractedDir APK 解压目录（含 classes*.dex）
     * @param functions    要注入的函数信息列表
     * @param version      GG 版本（VERSION_96 / VERSION_101）
     * @param workDir      临时工作目录
     * @throws IOException 注入失败时抛出
     */
    public void addFunctions(File extractedDir,
                             List<FunctionEntry> functions,
                             String version,
                             File workDir) throws IOException {
        if (functions == null || functions.isEmpty()) {
            Log.d(TAG, "函数列表为空，跳过注入");
            return;
        }

        Log.d(TAG, "开始注入 " + functions.size() + " 个函数，版本=" + version);

        // 找到所有 DEX 文件
        List<File> dexFiles = DexModifier.findDexFiles(extractedDir);
        if (dexFiles.isEmpty()) throw new IOException("未找到任何 DEX 文件");

        // 找到含 Script 类的 DEX
        File scriptDex = findDexContaining(dexFiles, SCRIPT_CLASS_TYPE);
        if (scriptDex == null) throw new IOException("未找到包含 Script 类的 DEX 文件");
        Log.d(TAG, "Script 类位于: " + scriptDex.getName());

        // ── Step1：对每个函数，编译 Script$xxx 并注入到 Script 所在 DEX ──
        for (FunctionEntry fn : functions) {
            Log.d(TAG, "处理函数: " + fn.name);

            // 获取对应版本的 Script$xxx smali 源码
            String innerSmaliSrc = loadInnerSmali(fn, version);

            // 编译为临时 DEX
            File innerSmaliFile = new File(workDir, "Script_" + fn.name + ".smali");
            File innerDex       = new File(workDir, "Script_" + fn.name + ".dex");
            writeText(innerSmaliFile, innerSmaliSrc);
            assembleSmali(innerSmaliFile, innerDex);

            // 从临时 DEX 提取 ClassDef 并注入
            String innerType = "Landroid/ext/Script$" + fn.name + ";";
            ClassDef innerClass = extractClass(innerDex, innerType);
            if (innerClass == null) {
                throw new IOException("编译产物中未找到类: " + innerType);
            }
            injectClass(scriptDex, innerClass, workDir);
            Log.d(TAG, "Script$" + fn.name + " 注入成功");
        }

        // ── Step2：反编译 Script 类 smali，插入注册代码，重新汇编注入 ──
        String patchedScriptSmali = patchScriptSmali(scriptDex, functions, version, workDir);

        File patchedSmaliFile = new File(workDir, "Script_patched.smali");
        File patchedDex       = new File(workDir, "Script_patched.dex");
        writeText(patchedSmaliFile, patchedScriptSmali);
        assembleSmali(patchedSmaliFile, patchedDex);

        // 从补丁 DEX 提取 Script ClassDef，替换进原 DEX
        ClassDef patchedScript = extractClass(patchedDex, SCRIPT_CLASS_TYPE);
        if (patchedScript == null) throw new IOException("补丁 DEX 中未找到 Script 类");
        injectClass(scriptDex, patchedScript, workDir);

        Log.d(TAG, "所有函数注入完成");
    }

    // ═══════════════════════════════════════════════════════════════
    //  smali 文本层面：给 Script.smali 插入函数注册段
    // ═══════════════════════════════════════════════════════════════

    /**
     * 反编译 Script 类为 smali 文本，找到 sleep 段末尾并插入新函数注册代码。
     *
     * @param scriptDex DEX 文件（Script 类所在）
     * @param functions 要注册的函数列表
     * @param workDir   临时目录
     * @return 修改后的 Script 类 smali 文本
     */
    private String patchScriptSmali(File scriptDex, List<FunctionEntry> functions,
                                    String version, File workDir) throws IOException {
        // 用 baksmali 反编译
        File disasmDir = new File(workDir, "disasm");
        disasmDir.mkdirs();
        disassembleDex(scriptDex, disasmDir);

        // Script.smali 路径：android/ext/Script.smali
        File scriptSmali = new File(disasmDir, "android/ext/Script.smali");
        if (!scriptSmali.exists()) {
            throw new IOException("反编译后未找到 Script.smali，路径: " + scriptSmali);
        }

        String smaliText = readText(scriptSmali);
        String patched   = insertFunctionRegistrations(smaliText, functions, version);
        return patched;
    }

    /**
     * 在 smali 文本中找到 sleep 注册块的末尾，插入新函数的注册代码。
     *
     * 策略：
     *  1. 定位 SLEEP_ANCHOR 行（"const-string v5, "sleep""）
     *  2. 从锚点行向下逐行扫描，找到 sleep 块真正的最后一行：
     *     即包含 "invoke-virtual" 且同行包含 "sleep" 或紧跟在 sleep 相关
     *     new-instance 之后的那条 invoke-virtual。
     *     更健壮的方式：从锚点行起，连续向下跳过非空白、非 ".line" 行，
     *     直到遇到下一个 ".line" 指令或空行为止，前一行即为 sleep 块末尾。
     *  3. 在 sleep 块末尾行之后插入每个函数的 registration smali（原文）。
     *  4. 每个函数的 registration 之间以空行分隔，与原有风格一致。
     */
    String insertFunctionRegistrations(String smaliText, List<FunctionEntry> functions,
                                       String version) throws IOException {
        String[] lines = smaliText.split("\n", -1);

        // ── 1. 定位 sleep 锚点行 ──────────────────────────────────────
        int anchorIdx = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals(SLEEP_ANCHOR)) {
                anchorIdx = i;
                break;
            }
        }
        if (anchorIdx < 0) {
            throw new IOException(
                    "未在 Script.smali 中找到 sleep 注册锚点行: " + SLEEP_ANCHOR);
        }

        // ── 2. 向下扫描，找到 sleep 块的最后一行 ─────────────────────
        // sleep 块是连续的若干条指令（const-string / new-instance / invoke-direct / invoke-virtual）
        // 遇到空行、".line"、"." 开头的其他指令或下一个 const-string 表示块结束。
        // 我们把"sleep 块"定义为：从锚点行开始，直到碰到下一个语义边界（.line）为止。
        int sleepBlockEnd = anchorIdx; // 最后属于 sleep 块的行索引（含）
        for (int i = anchorIdx + 1; i < lines.length; i++) {
            String trimmed = lines[i].trim();
           /* // .line指令 → sleep 块到上一行为止//由于有些GG没有.line于是弃用
            if (trimmed.startsWith(".line")) {
                break;
            }*/
            if (trimmed.startsWith("invoke-virtual")) {
                sleepBlockEnd+=1;
                break;
            }
            sleepBlockEnd = i;
        }

        Log.d(TAG, "sleep 锚点行: " + anchorIdx + "，sleep 块末尾行: " + sleepBlockEnd);

        // ── 3. 构造插入内容（使用 registration smali 原文）────────────
        StringBuilder insertion = new StringBuilder();
        for (FunctionEntry fn : functions) {
            String regSmali = getRegistrationSmali(fn, version);
            // 在每段注册代码前加一个空行，与 GG 原有风格一致
            insertion.append("\n");
            // 确保每行末尾有换行（防止原文末尾缺换行）
            if (!regSmali.endsWith("\n")) {
                regSmali = regSmali + "\n";
            }
            insertion.append(regSmali);
        }

        // ── 4. 重组文本 ───────────────────────────────────────────────
        StringBuilder result = new StringBuilder();
        // sleep 块（含锚点行）之前的所有行 + sleep 块本身
        for (int i = 0; i <= sleepBlockEnd; i++) {
            result.append(lines[i]).append("\n");
        }
        // 插入新函数注册代码
        result.append(insertion);
        // sleep 块之后的剩余行
        for (int i = sleepBlockEnd + 1; i < lines.length; i++) {
            result.append(lines[i]);
            if (i < lines.length - 1) result.append("\n");
        }

        Log.d(TAG, "已在 sleep 块（行 " + sleepBlockEnd + "）后插入 "
                + functions.size() + " 个函数注册段");
        return result.toString();
    }

    /**
     * 获取某个函数对应的 registration smali 原文，按 GG 版本选择。
     * 自定义函数：直接用 FunctionEntry 里存的原文。
     * 内置函数：从 assets/smali/<version>/functions/reg_<name>.smali 读取。
     *   （如果不想单独存 registration 文件，可在 assets 中把 reg_ 文件内容与
     *    inner 合并；此处分文件存储以保持灵活性）
     */
    private String getRegistrationSmali(FunctionEntry fn, String version) throws IOException {
        if (!fn.isBuiltin) {
            // 按版本选择
            if (VERSION_96.equals(version) && fn.registrationSmali96 != null)
                return fn.registrationSmali96;
            if (VERSION_101.equals(version) && fn.registrationSmali101 != null)
                return fn.registrationSmali101;
            // 降级
            if (fn.registrationSmali96  != null) return fn.registrationSmali96;
            if (fn.registrationSmali101 != null) return fn.registrationSmali101;
            throw new IOException("自定义函数 " + fn.name + " 没有可用的 registration smali");
        }
        // 内置函数：从 assets 读取 registration 片段
        String versionDir = VERSION_96.equals(version) ? "96" : "101";
        String assetPath  = "smali/" + versionDir + "/functions/reg_" + fn.name + ".smali";
        return loadAssetText(assetPath);
    }

    // ═══════════════════════════════════════════════════════════════
    //  加载 Script$xxx 的 smali 源码
    // ═══════════════════════════════════════════════════════════════

    /**
     * 根据函数来源和版本加载 Script$xxx 的 smali 源码：
     * - 内置函数：从 assets/smali/<version>/functions/<funcName>.smali 加载
     * - 自定义函数：从 CustomFunctionPackage 中获取
     */
    private String loadInnerSmali(FunctionEntry fn, String version) throws IOException {
        if (fn.isBuiltin) {
            // assets 中的内置 smali
            String versionDir = VERSION_96.equals(version) ? "96" : "101";
            String assetPath  = "smali/" + versionDir + "/functions/" + fn.name + ".smali";
            return loadAssetText(assetPath);
        } else {
            // 自定义函数：fn.smali96 / fn.smali101 已在 FunctionEntry 中
            if (VERSION_96.equals(version) && fn.smali96 != null) {
                return fn.smali96;
            } else if (VERSION_101.equals(version) && fn.smali101 != null) {
                return fn.smali101;
            } else if (fn.smali96 != null) {
                // 版本不精确匹配，降级使用 96 版（调用方已经用弹窗确认过"强行添加"）
                Log.w(TAG, fn.name + " 不含 " + version + " 版 smali，降级使用 96 版");
                return fn.smali96;
            } else if (fn.smali101 != null) {
                Log.w(TAG, fn.name + " 不含 " + version + " 版 smali，降级使用 101 版");
                return fn.smali101;
            } else {
                throw new IOException("自定义函数 " + fn.name + " 没有可用的 smali 源码");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DEX 工具
    // ═══════════════════════════════════════════════════════════════

    /** 在 DEX 文件列表中找到含有指定类型的那个 */
    private File findDexContaining(List<File> dexFiles, String classType) {
        for (File dex : dexFiles) {
            try {
                DexFile df = DexFileFactory.loadDexFile(dex, Opcodes.getDefault());
                for (ClassDef cls : df.getClasses()) {
                    if (classType.equals(cls.getType())) return dex;
                }
            } catch (Exception e) {
                Log.w(TAG, "读取 " + dex.getName() + " 失败: " + e.getMessage());
            }
        }
        return null;
    }

    /** 从 DEX 中提取指定类型的 ClassDef */
    private ClassDef extractClass(File dexFile, String classType) throws IOException {
        DexFile df = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault());
        for (ClassDef cls : df.getClasses()) {
            if (classType.equals(cls.getType())) return cls;
        }
        return null;
    }

    /**
     * 将一个 ClassDef 注入（替换）到目标 DEX 文件中，原地覆盖。
     * 若目标 DEX 中已存在同类型的类，则先删除再追加补丁版本。
     */
    private void injectClass(File targetDex, ClassDef patchedClass, File workDir)
            throws IOException {
        File tmpOut = new File(workDir, "inject_tmp_" + System.nanoTime() + ".dex");
        try {
            DexFile original = DexFileFactory.loadDexFile(targetDex, Opcodes.getDefault());
            DexPool pool = new DexPool(Opcodes.getDefault());

            String patchedType = patchedClass.getType();
            for (ClassDef cls : original.getClasses()) {
                if (!patchedType.equals(cls.getType())) {
                    pool.internClass(cls);
                }
            }
            pool.internClass(patchedClass);
            pool.writeTo(new FileDataStore(tmpOut));

            if (!tmpOut.renameTo(targetDex)) {
                copyFile(tmpOut, targetDex);
                tmpOut.delete();
            }
        } catch (Exception e) {
            if (tmpOut.exists()) tmpOut.delete();
            throw new IOException("注入类 " + patchedClass.getType() + " 失败: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  smali / baksmali 汇编 / 反汇编（反射调用）
    // ═══════════════════════════════════════════════════════════════

    /** 汇编 smali → DEX（复用 ScriptEmbedder 相同的反射方案） */
    private void assembleSmali(File smaliFile, File outputDex) throws IOException {
        try {
            Class<?> smaliMain;
            try {
                smaliMain = Class.forName("com.android.tools.smali.smali.Main");
            } catch (ClassNotFoundException e) {
                smaliMain = Class.forName("org.jf.smali.Main");
            }
            String[] args = {"a", smaliFile.getAbsolutePath(), "-o", outputDex.getAbsolutePath()};
            java.lang.reflect.Method m = smaliMain.getMethod("main", String[].class);
            m.invoke(null, (Object) args);
            if (!outputDex.exists()) throw new IOException("smali 汇编失败，输出文件不存在");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("smali 汇编异常: " + e.getMessage(), e);
        }
    }

    /** 反汇编 DEX → smali 目录（使用 baksmali） */
    private void disassembleDex(File dexFile, File outputDir) throws IOException {
        try {
            Class<?> baksmaliMain;
            try {
                baksmaliMain = Class.forName("com.android.tools.smali.baksmali.Main");
            } catch (ClassNotFoundException e) {
                baksmaliMain = Class.forName("org.jf.baksmali.Main");
            }
            String[] args = {
                    "d",                            // disassemble 子命令
                    dexFile.getAbsolutePath(),
                    "-o", outputDir.getAbsolutePath()
            };
            java.lang.reflect.Method m = baksmaliMain.getMethod("main", String[].class);
            m.invoke(null, (Object) args);
        } catch (Exception e) {
            throw new IOException("baksmali 反汇编异常: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  文件/资源工具
    // ═══════════════════════════════════════════════════════════════

    private String loadAssetText(String assetPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = mContext.getAssets().open(assetPath);
             BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private String readText(File file) throws IOException {
        StringBuilder sb = new StringBuilder((int) file.length());
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private void writeText(File file, String text) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) fos.write(buf, 0, len);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  数据类
    // ═══════════════════════════════════════════════════════════════

    /**
     * 描述一个要注入的函数。
     * 内置函数只需 name + isBuiltin，smali 从 assets 读取。
     * 自定义函数需提供 smali96 和/或 smali101 的文本。
     */
    public static class FunctionEntry {
        /** 函数名，对应 Script$xxx 中的 xxx，也是 Lua 侧调用名 */
        public final String name;
        /** true = 内置函数（smali 在 assets 中）；false = 自定义函数 */
        public final boolean isBuiltin;
        /** 96.0 版本的 Script$xxx.smali 源码（自定义函数用，可为 null） */
        public final String smali96;
        /** 101.1 版本的 Script$xxx.smali 源码（自定义函数用，可为 null） */
        public final String smali101;
        /** 在 Script 类中的注册 smali 片段（sleep 下方插入的那几行，对应版本） */
        public final String registrationSmali96;
        public final String registrationSmali101;

        /** 内置函数构造 */
        public FunctionEntry(String name) {
            this.name = name;
            this.isBuiltin = true;
            this.smali96 = null;
            this.smali101 = null;
            this.registrationSmali96 = null;
            this.registrationSmali101 = null;
        }

        /** 自定义函数构造 */
        public FunctionEntry(String name, String smali96, String smali101,
                             String registrationSmali96, String registrationSmali101) {
            this.name = name;
            this.isBuiltin = false;
            this.smali96 = smali96;
            this.smali101 = smali101;
            this.registrationSmali96 = registrationSmali96;
            this.registrationSmali101 = registrationSmali101;
        }
    }
}
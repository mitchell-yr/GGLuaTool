package mituran.gglua.tool.apktools;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.apk.axml.aXMLDecoder;
import com.apk.axml.aXMLEncoder;
import com.apk.axml.serializableItems.XMLEntry;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import mituran.gglua.tool.apktools.apksign.ApkSignerWrapper;
import mituran.gglua.tool.apktools.apksign.SignatureConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import mituran.gglua.tool.apktools.ScriptEmbedder;

/**
 * APK修改器（修复版）
 * 适配原ModifierActivity，集成签名选择
 */
public class ApkModifier {

    private Context mContext;
    private File mApkFile;
    private File mOutputFile;
    private ModifierActivity.ModifyOptions mOptions;

    // 临时工作目录
    private File mWorkDir;

    // 签名配置
    private SignatureConfig.Config mSignatureConfig;
    private boolean mWaitingForSignature = false;

    // 回调接口
    public interface ModifyCallback {
        void onProgress(String message);
        void onSuccess(String outputPath);
        void onError(String error);
        void onSignatureRequired(SignatureSelectionCallback callback);
    }

    // 签名选择回调
    public interface SignatureSelectionCallback {
        void onSignatureSelected(SignatureConfig.Config config);
        void onCancelled();
    }

    public ApkModifier(Context context) {
        this.mContext = context;
    }

    /**
     * 开始修改APK
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void modify(ModifierActivity.ModifyOptions options, final ModifyCallback callback) {
        this.mOptions = options;
        this.mApkFile = new File(options.apkPath);

        if (!mApkFile.exists()) {
            callback.onError("APK文件不存在");
            return;
        }

        try {
            callback.onProgress("准备工作目录...");
            prepareWorkDir();

            callback.onProgress("解压APK文件...");
            File extractedDir = extractApk();

            callback.onProgress("修改AndroidManifest.xml...");
            modifyManifest(extractedDir);

            callback.onProgress("修改应用图标...");
            modifyIcon(extractedDir);

            if (mOptions.embedScript && mOptions.scriptPath != null) {
                callback.onProgress("嵌入脚本...");
                embedScript(extractedDir, mOptions.scriptPath);
            }

            if (mOptions.addFunctions && mOptions.functionEntries != null && !mOptions.functionEntries.isEmpty()) {
                callback.onProgress("注入 GG 函数...");
                addGgFunctions(extractedDir, mOptions.functionEntries);
            }


            callback.onProgress("重新打包APK...");
            final File unsignedApk = repackApk(extractedDir);

            // 标记完成打包
            android.util.Log.d("ApkModifier", "重新打包完成，开始请求签名选择");

            // 请求用户选择签名方式
            mWaitingForSignature = true;
            callback.onSignatureRequired(new SignatureSelectionCallback() {
                @Override
                public void onSignatureSelected(SignatureConfig.Config config) {
                    mWaitingForSignature = false;
                    mSignatureConfig = config;

                    android.util.Log.d("ApkModifier", "用户已选择签名配置: " + config.name);

                    try {
                        callback.onProgress("使用 " + config.name + " 签名APK...");
                        File signedApk = signApk(unsignedApk);

                        callback.onProgress("修改完成!");
                        callback.onSuccess(signedApk.getAbsolutePath());

                    } catch (Exception e) {
                        android.util.Log.e("ApkModifier", "签名失败", e);
                        e.printStackTrace();
                        callback.onError("签名失败: " + e.getMessage());
                    } finally {
                        cleanWorkDir();
                    }
                }

                @Override
                public void onCancelled() {
                    mWaitingForSignature = false;
                    android.util.Log.d("ApkModifier", "用户取消了签名");
                    callback.onError("用户取消了签名");
                    cleanWorkDir();
                }
            });

        } catch (Exception e) {
            android.util.Log.e("ApkModifier", "修改失败", e);
            e.printStackTrace();
            callback.onError("修改失败: " + e.getMessage());
            cleanWorkDir();
        }
    }

    /**
     * 准备工作目录
     */
    private void prepareWorkDir() {
        mWorkDir = new File(mContext.getCacheDir(), "apk_modify_" + System.currentTimeMillis());
        if (!mWorkDir.exists()) {
            mWorkDir.mkdirs();
        }
        android.util.Log.d("ApkModifier", "工作目录: " + mWorkDir.getAbsolutePath());
    }

    /**
     * 解压APK文件
     */
    private File extractApk() throws IOException {
        File extractedDir = new File(mWorkDir, "extracted");
        if (!extractedDir.exists()) {
            extractedDir.mkdirs();
        }

        android.util.Log.d("ApkModifier", "开始解压APK: " + mApkFile.getAbsolutePath());

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(mApkFile))) {
            ZipEntry entry;
            int fileCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(extractedDir, entry.getName());

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    fileCount++;
                }
                zis.closeEntry();
            }
            android.util.Log.d("ApkModifier", "解压完成，共 " + fileCount + " 个文件");
        }

        return extractedDir;
    }

    /**
     * 修改AndroidManifest.xml
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void modifyManifest(File extractedDir) throws IOException, XmlPullParserException {
        File manifestFile = new File(extractedDir, "AndroidManifest.xml");
        if (!manifestFile.exists()) {
            throw new IOException("AndroidManifest.xml不存在");
        }

        android.util.Log.d("ApkModifier", "开始修改Manifest");

        byte[] manifestBytes;
        try (FileInputStream fis = new FileInputStream(manifestFile)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            manifestBytes = baos.toByteArray();
        }

        aXMLDecoder decoder = new aXMLDecoder(new ByteArrayInputStream(manifestBytes));
        List<XMLEntry> xmlEntries = decoder.decode();

        ManifestModifier modifier = new ManifestModifier(xmlEntries);

        String oldPackageName = modifier.getCurrentPackageName();
        android.util.Log.d("ApkModifier", "原包名: " + oldPackageName);

        if (mOptions.newPackageName != null && !mOptions.newPackageName.isEmpty()) {
            modifier.setPackageName(mOptions.newPackageName);
            modifyArscPackageName(extractedDir, oldPackageName, mOptions.newPackageName);
            modifyDexPackageName(extractedDir, oldPackageName, mOptions.newPackageName);
        }

        if (mOptions.newVersionName != null && !mOptions.newVersionName.isEmpty()) {
            modifier.setVersionName(mOptions.newVersionName);
        }

        if (mOptions.newAppName != null && !mOptions.newAppName.isEmpty()) {
            modifier.setApplicationLabel(mOptions.newAppName);
        }

        List<XMLEntry> modifiedEntries = modifier.getModifiedEntries();

        aXMLEncoder encoder = new aXMLEncoder();
        byte[] modifiedBytes = encoder.encodeString(modifiedEntries, mContext);

        try (FileOutputStream fos = new FileOutputStream(manifestFile)) {
            fos.write(modifiedBytes);
        }

        android.util.Log.d("ApkModifier", "Manifest修改完成");
    }

    /**
     * 修改DEX文件中的包名
     */
    private void modifyDexPackageName(File extractedDir, String oldPackageName, String newPackageName) {
        if (oldPackageName == null || oldPackageName.equals(newPackageName)) {
            return;
        }

        try {
            List<File> dexFiles = DexModifier.findDexFiles(extractedDir);
            if (dexFiles.isEmpty()) {
                android.util.Log.w("ApkModifier", "未找到DEX文件");
                return;
            }

            android.util.Log.d("ApkModifier", "找到 " + dexFiles.size() + " 个DEX文件");
            int successCount = DexModifier.modifyMultipleDex(dexFiles, oldPackageName, newPackageName, extractedDir);
            android.util.Log.d("ApkModifier", "DEX修改完成: " + successCount + "/" + dexFiles.size());

        } catch (Exception e) {
            android.util.Log.e("ApkModifier", "DEX修改失败", e);
        }
    }

    /**
     * 修改resources.arsc中的包名
     */
    private void modifyArscPackageName(File extractedDir, String oldPackageName, String newPackageName) {
        if (oldPackageName == null || oldPackageName.equals(newPackageName)) {
            return;
        }

        try {
            File arscFile = new File(extractedDir, "resources.arsc");
            if (!arscFile.exists()) {
                android.util.Log.w("ApkModifier", "resources.arsc不存在");
                return;
            }

            android.util.Log.d("ApkModifier", "开始修改ARSC");

            ArscEditor editor = new ArscEditor(arscFile);
            byte[] modifiedArsc = editor.modifyPackageName(oldPackageName, newPackageName);

            try (FileOutputStream fos = new FileOutputStream(arscFile)) {
                fos.write(modifiedArsc);
                fos.flush();
            }

            android.util.Log.d("ApkModifier", "ARSC修改完成");

        } catch (Exception e) {
            android.util.Log.e("ApkModifier", "ARSC修改失败", e);
        }
    }

    /**
     * 将用户脚本嵌入 DEX 中的 hy 类（android.ext 路径）。
     *
     * @param extractedDir APK 解压目录
     * @param scriptPath   用户选择的 .lua 脚本文件路径
     */
    private void embedScript(File extractedDir, String scriptPath) throws IOException {
        // 读取脚本内容
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            throw new IOException("脚本文件不存在: " + scriptPath);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(scriptFile),
                        StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        String scriptContent = sb.toString().trim();

        if (scriptContent.isEmpty()) {
            throw new IOException("脚本文件内容为空");
        }

        android.util.Log.d("ApkModifier", "脚本内容长度: " + scriptContent.length() + " chars");

        // 调用 ScriptEmbedder 完成注入
        ScriptEmbedder embedder = new ScriptEmbedder(mContext);
        embedder.embed(extractedDir, scriptContent, mWorkDir);

        android.util.Log.d("ApkModifier", "脚本嵌入完成");
    }

    /**
     * [新增 - 放入 ApkModifier.java]
     * 向解压后的 APK 注入 GG 函数（在 Script 类中注册并添加内部类）。
     * 会自动检测 GG 版本（VERSION_96 / VERSION_101），并按版本选择 smali。
     * 对于自定义函数：若目标版本的 smali 不存在，弹窗询问是否强行添加。
     *
     * @param extractedDir APK 解压目录
     * @param entries      要注入的函数列表
     */
    private void addGgFunctions(File extractedDir,
                                List<GgFunctionAdder.FunctionEntry> entries) throws IOException {
        // 读取 GG 版本
        File arscFile = new File(extractedDir, "resources.arsc");
        String version = detectGgVersion(arscFile);
        android.util.Log.d("ApkModifier", "GG 版本: " + version);

        // 过滤出无法用于该版本的自定义函数，并处理弹窗
        // 注意：此方法在子线程调用，强行添加弹窗需通过 CountDownLatch 同步
        List<GgFunctionAdder.FunctionEntry> toAdd = new ArrayList<>();

        for (GgFunctionAdder.FunctionEntry entry : entries) {
            if (entry.isBuiltin) {
                toAdd.add(entry);
                continue;
            }
            boolean hasSmali =
                    (GgFunctionAdder.VERSION_96.equals(version)  && entry.smali96 != null)
                 || (GgFunctionAdder.VERSION_101.equals(version) && entry.smali101 != null);

            if (hasSmali) {
                toAdd.add(entry);
            } else {
                // 弹窗询问（需要在主线程）
                boolean[] choice = {false};
                CountDownLatch latch = new CountDownLatch(1);

                new Handler(Looper.getMainLooper()).post(() -> {
                    new AlertDialog.Builder(mContext)
                            .setTitle("版本不兼容")
                            .setMessage("函数 " + entry.name + " 没有适用于 "
                                    + version + " 版的 smali，\n"
                                    + "是否仍然强行添加（可能无法正常工作）？")
                            .setPositiveButton("强行添加", (d, w) -> {
                                choice[0] = true;
                                latch.countDown();
                            })
                            .setNegativeButton("取消添加", (d, w) -> latch.countDown())
                            .setCancelable(false)
                            .show();
                });

                try { latch.await(); } catch (InterruptedException ignored) {}
                if (choice[0]) toAdd.add(entry);
            }
        }

        if (!toAdd.isEmpty()) {
            GgFunctionAdder adder = new GgFunctionAdder(mContext);
            adder.addFunctions(extractedDir, toAdd, version, mWorkDir);
        }
    }

    // 检测 GG 版本（复用 ScriptEmbedder 的检测逻辑）
    private String detectGgVersion(File arscFile) throws IOException {
        ScriptEmbedder embedder = new ScriptEmbedder(mContext);
        // ScriptEmbedder 没有公开 readVersionNumber，直接用 ArscEditor 回退方法
        ArscEditor editor = new ArscEditor(arscFile);
        List<String> strings = editor.getAllStrings();
        for (String s : strings) {
            if (ScriptEmbedder.VERSION_96.equals(s))  return ScriptEmbedder.VERSION_96;
            if (ScriptEmbedder.VERSION_101.equals(s)) return ScriptEmbedder.VERSION_101;
        }
        throw new IOException("无法从 resources.arsc 检测 GG 版本");
    }

    /**
     * 修改应用图标
     */
    private void modifyIcon(File extractedDir) throws IOException {
        if (mOptions.newIconPath == null || mOptions.newIconPath.isEmpty()) {
            return;
        }

        File iconFile = new File(mOptions.newIconPath);
        if (!iconFile.exists()) {
            throw new IOException("图标文件不存在: " + mOptions.newIconPath);
        }

        android.util.Log.d("ApkModifier", "开始修改图标");

        Bitmap iconBitmap = BitmapFactory.decodeFile(mOptions.newIconPath);
        if (iconBitmap == null) {
            throw new IOException("无法读取图标文件");
        }

        IconSize[] iconSizes = {
                new IconSize("res/mipmap-mdpi/ic_launcher.png", 48),
                new IconSize("res/mipmap-hdpi/ic_launcher.png", 72),
                new IconSize("res/mipmap-xhdpi/ic_launcher.png", 96),
                new IconSize("res/mipmap-xxhdpi/ic_launcher.png", 144),
                new IconSize("res/mipmap-xxxhdpi/ic_launcher.png", 192),
                new IconSize("res/drawable-mdpi/ic_launcher.png", 48),
                new IconSize("res/drawable-hdpi/ic_launcher.png", 72),
                new IconSize("res/drawable-xhdpi/ic_launcher.png", 96),
                new IconSize("res/drawable-xxhdpi/ic_launcher.png", 144),
                new IconSize("res/drawable-xxxhdpi/ic_launcher.png", 192),
        };

        int modifiedCount = 0;
        for (IconSize iconSize : iconSizes) {
            File targetIconFile = new File(extractedDir, iconSize.path);
            if (targetIconFile.exists() || iconSize.path.contains("mipmap")) {
                Bitmap scaledIcon = Bitmap.createScaledBitmap(iconBitmap, iconSize.size, iconSize.size, true);
                targetIconFile.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(targetIconFile)) {
                    scaledIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                modifiedCount++;
            }
        }

        iconBitmap.recycle();
        android.util.Log.d("ApkModifier", "图标修改完成，共 " + modifiedCount + " 个文件");
    }

    /**
     * 重新打包APK（未签名）
     */
    private File repackApk(File extractedDir) throws IOException {
        File unsignedApk = new File(mWorkDir, "unsigned.apk");

        android.util.Log.d("ApkModifier", "开始重新打包APK");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(unsignedApk))) {
            addDirToZip(zos, extractedDir, "");
        }

        android.util.Log.d("ApkModifier", "APK打包完成: " + unsignedApk.getAbsolutePath());
        android.util.Log.d("ApkModifier", "未签名APK大小: " + unsignedApk.length() + " 字节");

        return unsignedApk;
    }

    /**
     * 签名APK
     */
    private File signApk(File unsignedApk) throws IOException {
        android.util.Log.d("ApkModifier", "开始签名APK: " + unsignedApk.getAbsolutePath());

        String originalName = mApkFile.getName();
        String baseName = originalName.substring(0, originalName.lastIndexOf("."));
        String outputName = baseName + "_modified.apk";

        File outputDir = new File("/sdcard/GGtool/apk/");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        mOutputFile = new File(outputDir, outputName);

        // 使用选择的签名配置
        boolean success;
        if (mSignatureConfig != null) {
            android.util.Log.d("ApkModifier", "使用自定义签名: " + mSignatureConfig.name);
            success = ApkSignerWrapper.signApk(unsignedApk, mOutputFile, mSignatureConfig);
        } else {
            android.util.Log.d("ApkModifier", "使用默认测试签名");
            success = ApkSignerWrapper.signApkWithTestKey(unsignedApk, mOutputFile);
        }

        if (!success) {
            throw new IOException("APK签名失败");
        }

        android.util.Log.d("ApkModifier", "APK签名完成: " + mOutputFile.getAbsolutePath());
        android.util.Log.d("ApkModifier", "已签名APK大小: " + mOutputFile.length() + " 字节");

        return mOutputFile;
    }

    /**
     * 递归添加目录到ZIP
     */
    private void addDirToZip(ZipOutputStream zos, File dir, String basePath) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String entryPath = basePath.isEmpty() ? file.getName() : basePath + "/" + file.getName();

            if (file.isDirectory()) {
                addDirToZip(zos, file, entryPath);
            } else {
                addFileToZip(zos, file, entryPath);
            }
        }
    }

    /**
     * 添加单个文件到ZIP
     */
    private void addFileToZip(ZipOutputStream zos, File file, String entryPath) throws IOException {
        ZipEntry entry = new ZipEntry(entryPath);

        if (shouldBeStored(entryPath)) {
            entry.setMethod(ZipEntry.STORED);

            byte[] fileBytes = readFileBytes(file);
            CRC32 crc = new CRC32();
            crc.update(fileBytes);
            entry.setCrc(crc.getValue());
            entry.setSize(fileBytes.length);
            entry.setCompressedSize(fileBytes.length);

            zos.putNextEntry(entry);
            zos.write(fileBytes);
        } else {
            entry.setMethod(ZipEntry.DEFLATED);
            zos.putNextEntry(entry);

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
        }

        zos.closeEntry();
    }

    /**
     * 判断文件是否应该不压缩存储
     */
    private boolean shouldBeStored(String entryPath) {
        return entryPath.equals("AndroidManifest.xml") ||
                entryPath.equals("resources.arsc") ||
                entryPath.endsWith(".so") ||
                entryPath.endsWith(".jpg") ||
                entryPath.endsWith(".jpeg") ||
                entryPath.endsWith(".png") ||
                entryPath.endsWith(".gif") ||
                entryPath.endsWith(".mp3") ||
                entryPath.endsWith(".mp4");
    }

    /**
     * 读取文件字节
     */
    private byte[] readFileBytes(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
        }
        return baos.toByteArray();
    }

    /**
     * 清理工作目录
     */
    private void cleanWorkDir() {
        if (mWorkDir != null && mWorkDir.exists()) {
            android.util.Log.d("ApkModifier", "清理工作目录");
            deleteRecursive(mWorkDir);
        }
    }

    /**
     * 递归删除文件和目录
     */
    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    /**
     * 图标尺寸类
     */
    private static class IconSize {
        String path;
        int size;

        IconSize(String path, int size) {
            this.path = path;
            this.size = size;
        }
    }
}
package mituran.gglua.tool;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class CppCompiler {

    private static final String TAG = "CppCompiler";
    private static final String MEMORY_TOOLS_FILENAME = "MemoryTools.h";
    private static final String C4DROID_PACKAGE = "com.n0n3m4.droidc";
    private static final String ACTION_EXPORT_BINARY = "com.n0n3m4.droidc.EXPORT_BINARY";

    private Context context;
    private Handler mainHandler;

    public CppCompiler(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 获取 MemoryTools.h 的外部存储路径
     */
    public static String getMemoryToolsPath() {
        return new File(Environment.getExternalStorageDirectory(),
                "GGtool/data/" + MEMORY_TOOLS_FILENAME).getAbsolutePath();
    }

    /**
     * 确保 MemoryTools.h 存在于外部存储，不存在则从 assets 复制
     */
    public void ensureMemoryToolsExist() {
        File destFile = new File(getMemoryToolsPath());
        if (!destFile.exists()) {
            resetMemoryTools();
        }
    }

    /**
     * 从 assets 重置 MemoryTools.h 到外部存储
     */
    public boolean resetMemoryTools() {
        try {
            File destFile = new File(getMemoryToolsPath());
            destFile.getParentFile().mkdirs();
            InputStream is = context.getAssets().open("memory_tools/" + MEMORY_TOOLS_FILENAME);
            OutputStream os = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            os.close();
            is.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "重置MemoryTools.h失败", e);
            return false;
        }
    }

    /**
     * 确保内置模板已复制到外部存储
     */
    public static void ensureBuiltinTemplates(String language, Context context) {
        try {
            File targetDir = new File(Environment.getExternalStorageDirectory(),
                    "GGtool/templates/" + language + "/");
            String assetPath = "builtin_templates/" + language + "/";

            // 检查资产目录是否存在
            String[] list = context.getAssets().list(assetPath);
            if (list == null || list.length == 0) return;

            for (String filename : list) {
                File targetFile = new File(targetDir, filename);
                if (!targetFile.exists()) {
                    targetDir.mkdirs();
                    InputStream is = context.getAssets().open(assetPath + filename);
                    OutputStream os = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                    os.flush();
                    os.close();
                    is.close();
                    Log.d(TAG, "内置模板已复制: " + filename);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "复制内置模板失败(" + language + "): " + e.getMessage());
        }
    }

    /**
     * 编译 C++ 源文件为 .so
     *
     * @param sourceFilePath 用户源文件路径 (code.cpp)
     * @param outputPath     输出 .so 路径
     * @param callback       编译结果回调
     */
    public void compile(String sourceFilePath, String outputPath, CompileCallback callback) {
        new Thread(() -> {
            try {
                // 确保 MemoryTools.h 存在
                ensureMemoryToolsExist();
                String memoryToolsPath = getMemoryToolsPath();

                // 确保输出目录存在
                File outputFile = new File(outputPath);
                if (outputFile.getParentFile() != null) {
                    outputFile.getParentFile().mkdirs();
                }

                // 删除旧的输出文件
                if (outputFile.exists()) {
                    outputFile.delete();
                }

                // 构建 source_files: MemoryTools.h 在前，用户代码在后
                String sourceFiles = memoryToolsPath + "," + sourceFilePath;

                Log.d(TAG, "sourceFiles: " + sourceFiles);
                Log.d(TAG, "outputPath: " + outputPath);

                // 发送编译广播
                Intent intent = new Intent(ACTION_EXPORT_BINARY);
                intent.setPackage(C4DROID_PACKAGE);
                intent.putExtra("source_files", sourceFiles);
                intent.putExtra("output_path", outputPath);
                intent.putExtra("compile_mode", "cpp");
                context.sendBroadcast(intent);

                Log.d(TAG, "广播已发送");

                // 轮询等待输出文件出现（最多等待 60 秒）
                boolean success = false;
                for (int i = 0; i < 60; i++) {
                    Thread.sleep(1000);
                    if (outputFile.exists() && outputFile.length() > 0) {
                        success = true;
                        break;
                    }
                }

                boolean finalSuccess = success;
                mainHandler.post(() -> {
                    if (finalSuccess) {
                        callback.onSuccess(outputPath);
                    } else {
                        callback.onFailure("编译超时（60秒），请检查代码或确认C4droid已安装");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "编译异常", e);
                mainHandler.post(() -> callback.onFailure("编译异常: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 检查 C4droid 是否已安装
     */
    public static boolean isC4droidInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(C4DROID_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public interface CompileCallback {
        void onSuccess(String outputPath);
        void onFailure(String errorMessage);
    }
}

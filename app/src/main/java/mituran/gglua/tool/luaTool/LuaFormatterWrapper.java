package mituran.gglua.tool.luaTool;

import android.os.Environment;
import android.util.Log;

import java.io.File;

public class LuaFormatterWrapper {
    private static final String TAG = "LuaFormatter";

    /**
     * 直接调用jar包中的格式化方法
     * @param luaFilePath Lua文件的完整路径
     * @param encoding 文件编码（如UTF-8），可为null使用默认编码
     * @param overwrite 是否覆盖原文件
     * @return 是否格式化成功
     */
    public static boolean formatLuaFile(String luaFilePath, String encoding, boolean overwrite) {
        try {
            File luaFile = new File(luaFilePath);
            if (!luaFile.exists()) {
                Log.e(TAG, "Lua文件不存在: " + luaFilePath);
                return false;
            }

            // 构建参数数组
            java.util.List<String> args = new java.util.ArrayList<>();

            if (overwrite) {
                args.add("-o");
            }

            if (encoding != null && !encoding.isEmpty()) {
                args.add("-e" + encoding);
            }

            args.add(luaFilePath);

            // 调用jar包的main方法
            // 注意：这里需要根据实际jar包的主类名修改
            // 常见的主类名可能是：com.github.luaformatter.Main 或 Main
            String[] argsArray = args.toArray(new String[0]);

            Log.d(TAG, "调用格式化工具，参数: " + args.toString());

            // 直接调用jar包的main方法
            Class<?> mainClass = Class.forName("neoe.formatter.lua.LuaFormatter");
            java.lang.reflect.Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) argsArray);

            Log.i(TAG, "格式化完成: " + luaFilePath);
            return true;

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "找不到主类，请检查jar包的主类名: " + e.getMessage(), e);
            return false;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "找不到main方法: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "格式化失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 格式化文件（覆盖原文件，使用默认编码）
     */
    public static boolean formatLuaFile(String luaFilePath) {
        return formatLuaFile(luaFilePath, null, true);
    }

    /**
     * 格式化文件（覆盖原文件，指定编码）
     */
    public static boolean formatLuaFile(String luaFilePath, String encoding) {
        return formatLuaFile(luaFilePath, encoding, true);
    }

    /**
     * 格式化sdcard目录下的Lua文件
     */
    public static boolean formatLuaFileInSdcard(String relativePath, String encoding) {
        File sdcard = Environment.getExternalStorageDirectory();
        String fullPath = new File(sdcard, relativePath).getAbsolutePath();
        return formatLuaFile(fullPath, encoding, true);
    }

    /**
     * 在后台线程中格式化文件（推荐）
     */
    public static void formatLuaFileAsync(String luaFilePath, String encoding,
                                          FormatCallback callback) {
        new Thread(() -> {
            boolean success = formatLuaFile(luaFilePath, encoding, true);
            if (callback != null) {
                callback.onComplete(success);
            }
        }).start();
    }

    /**
     * 格式化回调接口
     */
    public interface FormatCallback {
        void onComplete(boolean success);
    }
}

// ============================================
// 如果你知道jar包的具体API，可以直接调用：
// ============================================

/*
// 示例：假设jar包提供了Formatter类
import com.xxx.luaformatter.Formatter; // 根据实际包名调整

public class LuaFormatterDirect {

    public static boolean formatFile(String filePath, String encoding) {
        try {
            // 直接调用jar包的API
            Formatter formatter = new Formatter();
            formatter.setEncoding(encoding);
            formatter.setOverwrite(true);
            formatter.format(filePath);
            return true;
        } catch (Exception e) {
            Log.e("LuaFormatter", "格式化失败", e);
            return false;
        }
    }
}
*/

// ============================================
// 使用示例：
// ============================================

/*
// 在Activity中使用

// 方式1：同步调用（需在子线程）
new Thread(() -> {
    String luaPath = Environment.getExternalStorageDirectory() + "/test/script.lua";
    boolean success = LuaFormatter.formatLuaFile(luaPath, "UTF-8");

    runOnUiThread(() -> {
        Toast.makeText(this, success ? "格式化成功" : "格式化失败",
                      Toast.LENGTH_SHORT).show();
    });
}).start();

// 方式2：异步调用（推荐）
String luaPath = Environment.getExternalStorageDirectory() + "/test/script.lua";
LuaFormatter.formatLuaFileAsync(luaPath, "UTF-8", success -> {
    runOnUiThread(() -> {
        Toast.makeText(this, success ? "格式化成功" : "格式化失败",
                      Toast.LENGTH_SHORT).show();
    });
});

// 方式3：格式化sdcard相对路径
LuaFormatter.formatLuaFileInSdcard("myapp/script.lua", "UTF-8");
*/
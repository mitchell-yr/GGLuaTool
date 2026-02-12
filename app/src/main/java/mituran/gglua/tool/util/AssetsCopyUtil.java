package mituran.gglua.tool.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetsCopyUtil {
    private static final String TAG = "AssetsCopyUtil";
    private static final int BUFFER_SIZE = 1024 * 4;

    /**
     * 复制assets/templates/目录到外部存储
     * @param context 上下文
     * @return 是否复制成功
     */
    public static boolean copyTemplatesFromAssets(Context context) {
        try {
            // 目标目录路径
            String destPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/GGtool/templates";

            // 源目录在assets中的路径
            String assetPath = "templates";

            // 执行复制
            return copyAssetFolder(context, assetPath, destPath);

        } catch (Exception e) {
            Log.e(TAG, "复制失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递归复制assets目录
     * @param context 上下文
     * @param assetPath assets中的路径
     * @param destPath 目标路径
     * @return 是否成功
     */
    private static boolean copyAssetFolder(Context context, String assetPath, String destPath) {
        try {
            // 获取assets中的文件列表
            String[] files = context.getAssets().list(assetPath);

            if (files == null || files.length == 0) {
                // 如果是文件而不是目录，直接复制
                return copyAssetFile(context, assetPath, destPath);
            }

            // 创建目标目录
            File destDir = new File(destPath);
            if (!destDir.exists()) {
                if (!destDir.mkdirs()) {
                    Log.e(TAG, "创建目录失败: " + destPath);
                    return false;
                }
            }

            boolean result = true;
            // 遍历并复制每个文件/文件夹
            for (String file : files) {
                String srcPath = assetPath + "/" + file;
                String targetPath = destPath + "/" + file;

                // 递归处理
                result &= copyAssetFolder(context, srcPath, targetPath);
            }

            return result;

        } catch (IOException e) {
            e.printStackTrace();
            // 可能是文件而不是目录，尝试作为文件复制
            return copyAssetFile(context, assetPath, destPath);
        }
    }

    /**
     * 复制单个文件
     * @param context 上下文
     * @param assetPath assets中的文件路径
     * @param destPath 目标文件路径
     * @return 是否成功
     */
    private static boolean copyAssetFile(Context context, String assetPath, String destPath) {
        InputStream in = null;
        OutputStream out = null;

        try {
            // 打开assets中的文件
            in = context.getAssets().open(assetPath);

            // 确保目标文件的父目录存在
            File destFile = new File(destPath);
            File destParent = destFile.getParentFile();
            if (destParent != null && !destParent.exists()) {
                destParent.mkdirs();
            }

            // 创建输出流
            out = new FileOutputStream(destFile);

            // 复制文件
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            out.flush();

            Log.d(TAG, "文件复制成功: " + assetPath + " -> " + destPath);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "文件复制失败: " + assetPath + " -> " + destPath);
            e.printStackTrace();
            return false;

        } finally {
            // 关闭流
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查目标目录是否已存在文件
     * @return 目标目录是否存在且包含文件
     */
    public static boolean isTemplatesExist() {
        String destPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/GGtool/templates";
        File dir = new File(destPath);

        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list();
            return files != null && files.length > 0;
        }

        return false;
    }

    /**
     * 删除目标目录（用于重新复制）
     */
    public static void deleteTemplatesDir() {
        String destPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/GGtool/templates";
        deleteRecursive(new File(destPath));
    }

    /**
     * 递归删除文件/目录
     */
    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}
package mituran.gglua.tool.util;

import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import unluac.Configuration;
import unluac.decompile.Decompiler;
import unluac.decompile.Output;
import unluac.decompile.OutputProvider;
import unluac.parse.BHeader;

/**
 * Unluac 包装类 - 简化 Lua 字节码反编译操作
 *
 * 用法示例:
 * <pre>
 * UnluacWrapper wrapper = new UnluacWrapper();
 * wrapper.decompileFile("/path/to/file.luac", null, new UnluacWrapper.DecompileCallback() {
 *     public void onSuccess(String decompiled) {
 *         // 处理反编译结果
 *     }
 *
 *     public void onError(String error) {
 *         // 处理错误
 *     }
 * });
 * </pre>
 */
public class UnluacWrapper {

    /**
     * 反编译回调接口
     */
    public interface DecompileCallback {
        /**
         * 反编译成功
         * @param decompiled 反编译后的 Lua 源码
         */
        void onSuccess(String decompiled);

        /**
         * 反编译失败
         * @param error 错误信息
         */
        void onError(String error);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 反编译文件 (异步)
     *
     * @param inputPath 输入的 .luac 文件路径
     * @param outputPath 输出的 .lua 文件路径 (可选,传 null 则不保存文件)
     * @param callback 回调接口
     */
    public void decompileFile(String inputPath, String outputPath, DecompileCallback callback) {
        new Thread(() -> {
            try {
                String result = decompileFileSync(inputPath, outputPath);
                runOnMainThread(() -> callback.onSuccess(result));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "未知错误"));
            }
        }).start();
    }

    /**
     * 反编译字节数组 (异步)
     *
     * @param bytes Lua 字节码数据
     * @param callback 回调接口
     */
    public void decompileBytes(byte[] bytes, DecompileCallback callback) {
        new Thread(() -> {
            try {
                String result = decompileBytesSync(bytes);
                runOnMainThread(() -> callback.onSuccess(result));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "未知错误"));
            }
        }).start();
    }

    /**
     * 使用自定义配置反编译文件 (异步)
     *
     * @param inputPath 输入文件路径
     * @param config 自定义配置
     * @param callback 回调接口
     */
    public void decompileWithConfig(String inputPath, Configuration config, DecompileCallback callback) {
        new Thread(() -> {
            try {
                String result = decompileWithConfigSync(inputPath, config);
                runOnMainThread(() -> callback.onSuccess(result));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "未知错误"));
            }
        }).start();
    }

    /**
     * 反编译文件 (同步) - 注意:必须在后台线程调用
     *
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径 (可选)
     * @return 反编译后的源码
     * @throws IOException IO 异常
     */
    public String decompileFileSync(String inputPath, String outputPath) throws IOException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("输入文件不存在: " + inputPath);
        }

        if (!inputFile.canRead()) {
            throw new IOException("无法读取文件: " + inputPath);
        }

        byte[] bytes = readFile(inputFile);
        String result = decompileBytesSync(bytes);

        // 如果指定了输出路径,保存到文件
        if (outputPath != null && !outputPath.isEmpty()) {
            writeFile(new File(outputPath), result);
        }

        return result;
    }

    /**
     * 反编译字节数组 (同步) - 注意:必须在后台线程调用
     *
     * @param bytes Lua 字节码
     * @return 反编译后的源码
     * @throws IOException IO 异常
     */
    public String decompileBytesSync(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("字节数组为空");
        }

        Configuration config = new Configuration();
        return decompileBytesWithConfigSync(bytes, config);
    }

    /**
     * 使用自定义配置反编译文件 (同步)
     *
     * @param inputPath 输入文件路径
     * @param config 自定义配置
     * @return 反编译后的源码
     * @throws IOException IO 异常
     */
    public String decompileWithConfigSync(String inputPath, Configuration config) throws IOException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("输入文件不存在: " + inputPath);
        }

        byte[] bytes = readFile(inputFile);
        return decompileBytesWithConfigSync(bytes, config);
    }

    /**
     * 使用自定义配置反编译字节数组 (同步)
     *
     * @param bytes Lua 字节码
     * @param config 自定义配置
     * @return 反编译后的源码
     * @throws IOException IO 异常
     */
    public String decompileBytesWithConfigSync(byte[] bytes, Configuration config) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 解析 Lua 字节码头部
        BHeader header = new BHeader(buffer, config);

        // 执行反编译
        Decompiler decompiler = new Decompiler(header.main);
        Decompiler.State state = decompiler.decompile();

        // 输出到字符串
        StringBuilder resultBuilder = new StringBuilder();
        Output output = new Output(new StringOutputProvider(resultBuilder));
        decompiler.print(state, output);
        output.finish();

        return resultBuilder.toString();
    }

    /**
     * 读取文件到字节数组
     */
    private byte[] readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            return bos.toByteArray();
        }
    }

    /**
     * 写入字符串到文件
     */
    private void writeFile(File file, String content) throws IOException {
        // 确保父目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    /**
     * 在主线程执行
     */
    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    /**
     * 字符串输出提供者 - 实现 OutputProvider 接口
     */
    private static class StringOutputProvider implements OutputProvider {
        private final StringBuilder builder;
        private final String eol;

        public StringOutputProvider(StringBuilder builder) {
            this.builder = builder;
            this.eol = System.lineSeparator();
        }

        @Override
        public void print(String s) {
            builder.append(s);
        }

        @Override
        public void print(byte b) {
            builder.append((char) (b & 0xFF));
        }

        @Override
        public void println() {
            builder.append(eol);
        }

        @Override
        public void finish() {
            // StringBuilder 不需要关闭或刷新
            // 此方法为空实现
        }
    }
}
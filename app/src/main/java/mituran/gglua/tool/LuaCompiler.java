package mituran.gglua.tool;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.DumpState;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LuaCompiler {

    private BuildOutputLogManager logManager;
    private Globals globals;
    private Prototype compiledPrototype; // 保存编译后的原型

    public LuaCompiler(BuildOutputLogManager logManager) {
        this.logManager = logManager;
        this.globals = JsePlatform.standardGlobals();
    }

    /**
     * 编译Lua脚本
     */
    public boolean compile(String luaCode) {
        logManager.clear();
        logManager.logInfo("开始编译Lua脚本...");

        if (luaCode == null || luaCode.trim().isEmpty()) {
            logManager.logError("脚本内容为空");
            return false;
        }

        try {
            // 语法检查
            logManager.logInfo("正在进行语法检查...");
            StringReader reader = new StringReader(luaCode);

            // 编译脚本为Prototype
            InputStream is = new ByteArrayInputStream(luaCode.getBytes("UTF-8"));
            compiledPrototype = LuaC.instance.compile(is, "script");

            logManager.logSuccess("语法检查通过");
            logManager.logInfo("脚本编译成功");

            // 输出编译信息
            String[] lines = luaCode.split("\n");
            logManager.logDebug("脚本行数: " + lines.length);
            logManager.logDebug("脚本大小: " + luaCode.length() + " 字节");

            // 检查特征
            if (luaCode.contains("function")) {
                logManager.logInfo("检测到函数定义");
            }
            if (luaCode.matches(".*\\b[a-zA-Z_][a-zA-Z0-9_]*\\s*=.*")) {
                logManager.logInfo("检测到变量赋值");
            }

            logManager.logSuccess("编译完成，无错误");
            return true;

        } catch (Exception e) {
            logManager.logError("编译失败: " + e.getMessage());
            compiledPrototype = null;
            return false;
        }
    }

    /**
     * 将编译后的字节码保存到文件
     * @param outputPath 输出文件路径
     * @return 是否成功
     */
    public boolean saveBytecode(String outputPath) {
        if (compiledPrototype == null) {
            logManager.logError("没有可用的编译结果");
            return false;
        }

        try {
            logManager.logInfo("正在保存字节码文件...");

            File outputFile = new File(outputPath);

            // 确保父目录存在
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 写入字节码
            FileOutputStream fos = new FileOutputStream(outputFile);
            DumpState.dump(compiledPrototype, fos, true, 0, true);
            fos.close();

            // 计算文件大小
            long fileSize = outputFile.length();
            String sizeStr = formatFileSize(fileSize);

            logManager.logSuccess("字节码已保存到: " + outputPath);
            logManager.logInfo("输出文件大小: " + sizeStr);

            return true;

        } catch (Exception e) {
            logManager.logError("保存字节码失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将源码和字节码一起打包输出
     * @param sourcePath 源文件路径
     * @param outputDir 输出目录
     * @return 是否成功
     */
    public boolean exportCompiledPackage(String sourcePath, String outputDir) {
        try {
            logManager.logInfo("开始导出编译包...");

            // 创建输出目录
            File outDir = new File(outputDir);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            // 获取源文件名（不含扩展名）
            File sourceFile = new File(sourcePath);
            String baseName = sourceFile.getName().replace(".lua", "");

            // 生成时间戳
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new java.util.Date());

            // 复制源文件
            String sourceOutputPath = outputDir + "/" + baseName + "_source.lua";
            copyFile(sourcePath, sourceOutputPath);
            logManager.logInfo("源文件已复制到: " + sourceOutputPath);

            // 保存字节码
            String bytecodeOutputPath = outputDir + "/" + baseName + "_compiled.luac";
            if (saveBytecode(bytecodeOutputPath)) {

                // 创建信息文件
                String infoPath = outputDir + "/" + baseName + "_info.txt";
                createInfoFile(infoPath, sourcePath, timestamp);

                logManager.logSuccess("编译包导出完成！");
                logManager.logInfo("输出目录: " + outputDir);
                return true;
            }

            return false;

        } catch (Exception e) {
            logManager.logError("导出编译包失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建编译信息文件
     */
    private void createInfoFile(String infoPath, String sourcePath, String timestamp) {
        try {
            FileWriter writer = new FileWriter(infoPath);
            writer.write("=== Lua编译信息 ===\n");
            writer.write("编译时间: " + timestamp + "\n");
            writer.write("源文件: " + sourcePath + "\n");
            writer.write("编译器: LuaJ 3.0.1\n");
            writer.write("平台: Android\n");

            if (compiledPrototype != null) {
                writer.write("\n=== 字节码信息 ===\n");
                writer.write("函数数量: " + compiledPrototype.p.length + "\n");
                writer.write("常量数量: " + compiledPrototype.k.length + "\n");
                writer.write("指令数量: " + compiledPrototype.code.length + "\n");
            }

            writer.close();
            logManager.logInfo("信息文件已创建");

        } catch (IOException e) {
            logManager.logWarning("创建信息文件失败: " + e.getMessage());
        }
    }

    /**
     * 复制文件
     */
    private void copyFile(String sourcePath, String destPath) throws IOException {
        FileInputStream fis = new FileInputStream(sourcePath);
        FileOutputStream fos = new FileOutputStream(destPath);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }

        fis.close();
        fos.close();
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024));
    }

    /**
     * 编译并加密脚本
     */
    public boolean compileWithEncryption(String luaCode, boolean encrypt, boolean[] encryptionOptions) {
        logManager.clear();
        logManager.logInfo("开始编译Lua脚本...");

        if (luaCode == null || luaCode.trim().isEmpty()) {
            logManager.logError("脚本内容为空");
            return false;
        }

        try {
            String processedCode = luaCode;

            // 如果启用加密
            if (encrypt) {
                logManager.logInfo("正在应用加密保护...");
                processedCode = LuaEncryptionModule.applyEncryption(luaCode, encryptionOptions);

                // 记录应用的加密选项
                for (int i = 0; i < encryptionOptions.length; i++) {
                    if (encryptionOptions[i]) {
                        logManager.logSuccess("✓ " +
                                LuaEncryptionModule.EncryptionOption.values()[i].getName());


                        logManager.logError("\n错误代码：\n"+processedCode);//TODO：调试用
                    }
                }
            }

            // 编译处理后的代码
            logManager.logInfo("正在编译脚本...");
            InputStream is = new ByteArrayInputStream(processedCode.getBytes("UTF-8"));
            compiledPrototype = LuaC.instance.compile(is, "script");

            logManager.logSuccess("脚本编译成功");

            // 输出统计信息
            String[] lines = processedCode.split("\n");
            logManager.logDebug("处理后行数: " + lines.length);
            logManager.logDebug("处理后大小: " + processedCode.length() + " 字节");

            if (encrypt) {
                logManager.logInfo("加密保护已应用");
            }

            return true;

        } catch (Exception e) {
            logManager.logError("编译失败: " + e.getMessage());
            compiledPrototype = null;
            return false;
        }
    }

    /**
     * 导出为ZIP包
     */
    public boolean exportAsZip(String sourcePath, String outputPath, boolean encrypt, boolean[] encryptionOptions) {
        try {
            logManager.logInfo("开始创建ZIP编译包...");

            File sourceFile = new File(sourcePath);
            String baseName = sourceFile.getName().replace(".lua", "");

            // 创建ZIP输出流
            FileOutputStream fos = new FileOutputStream(outputPath);
            ZipOutputStream zos = new ZipOutputStream(fos);

            // 添加源文件
            logManager.logInfo("添加源文件...");
            addFileToZip(zos, sourcePath, baseName + "_source.lua");

            // 保存字节码到临时文件
            String tempBytecode = sourceFile.getParent() + "/temp_compiled.luac";
            if (saveBytecode(tempBytecode)) {
                logManager.logInfo("添加字节码文件...");
                addFileToZip(zos, tempBytecode, baseName + "_compiled.luac");
                new File(tempBytecode).delete(); // 删除临时文件
            }

            // 添加信息文件
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date());
            String info = createInfoContent(sourcePath, timestamp, encrypt, encryptionOptions);
            addTextToZip(zos, info, "README.txt");

            // 如果加密，添加加密信息
            if (encrypt) {
                String encryptInfo = createEncryptionInfo(encryptionOptions);
                addTextToZip(zos, encryptInfo, "encryption_info.txt");
                logManager.logInfo("已添加加密信息文件");
            }

            zos.close();
            fos.close();

            // 计算ZIP文件大小
            File zipFile = new File(outputPath);
            String sizeStr = formatFileSize(zipFile.length());

            logManager.logSuccess("ZIP包创建成功！");
            logManager.logInfo("输出文件: " + outputPath);
            logManager.logInfo("文件大小: " + sizeStr);

            return true;

        } catch (Exception e) {
            logManager.logError("创建ZIP包失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 添加文件到ZIP
     */
    private void addFileToZip(ZipOutputStream zos, String filePath, String entryName) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

    /**
     * 添加文本到ZIP
     */
    private void addTextToZip(ZipOutputStream zos, String text, String entryName) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);
        zos.write(text.getBytes("UTF-8"));
        zos.closeEntry();
    }

    /**
     * 创建信息内容
     */
    private String createInfoContent(String sourcePath, String timestamp, boolean encrypted, boolean[] options) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("        Lua Script Compilation Package\n");
        sb.append("========================================\n\n");
        sb.append("Compilation Time: ").append(timestamp).append("\n");
        sb.append("Source File: ").append(sourcePath).append("\n");
        sb.append("Compiler: LuaJ 3.0.1\n");
        sb.append("Platform: Android\n");
        sb.append("Encrypted: ").append(encrypted ? "Yes" : "No").append("\n");

        if (compiledPrototype != null) {
            sb.append("\n--- Bytecode Information ---\n");
            sb.append("Functions: ").append(compiledPrototype.p.length).append("\n");
            sb.append("Constants: ").append(compiledPrototype.k.length).append("\n");
            sb.append("Instructions: ").append(compiledPrototype.code.length).append("\n");
        }

        sb.append("\n========================================\n");
        return sb.toString();
    }

    /**
     * 创建加密信息
     */
    private String createEncryptionInfo(boolean[] options) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Encryption Settings ===\n\n");
        sb.append("Applied protections:\n");

        LuaEncryptionModule.EncryptionOption[] allOptions =
                LuaEncryptionModule.EncryptionOption.values();

        for (int i = 0; i < options.length && i < allOptions.length; i++) {
            if (options[i]) {
                sb.append("✓ ").append(allOptions[i].getName()).append("\n");
                sb.append("  ").append(allOptions[i].getDescription()).append("\n\n");
            }
        }

        sb.append("\nNote: This script has been protected against reverse engineering.\n");
        return sb.toString();
    }
}
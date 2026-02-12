package mituran.gglua.tool.apktools;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import zhao.arsceditor.ResDecoder.ARSCDecoder;
import zhao.arsceditor.ResDecoder.data.ResTable;

/**
 * Resources.arsc 编辑器
 * 用于修改APK中的resources.arsc文件，主要修改包名
 */
public class ArscEditor {

    private static final String TAG = "ArscEditor";

    private File arscFile;

    public ArscEditor(File arscFile) {
        this.arscFile = arscFile;
    }

    /**
     * 修改resources.arsc中的包名
     * 使用多种方法：
     * 1. 修改字符串池中的包名引用
     * 2. 修改Package Header中的包名（支持新包名更长的情况）
     *
     * @param oldPackageName 旧包名
     * @param newPackageName 新包名
     * @return 修改后的arsc文件字节数组
     * @throws IOException
     */
    public byte[] modifyPackageName(String oldPackageName, String newPackageName) throws IOException {
        Log.d(TAG, "modifyPackageName: old=" + oldPackageName + ", new=" + newPackageName);

        if (oldPackageName == null || newPackageName == null) {
            throw new IllegalArgumentException("Package name cannot be null");
        }

        if (oldPackageName.equals(newPackageName)) {
            Log.d(TAG, "Package names are the same, skipping modification");
            return readFileBytes(arscFile);
        }

        // 检查新包名长度（ARSC包名字段最大128字符）
        if (newPackageName.length() > 128) {
            throw new IllegalArgumentException("Package name too long (max 128 characters): " + newPackageName.length());
        }

        Log.d(TAG, "Reading arsc file: " + arscFile.getAbsolutePath());

        // 读取整个文件到内存
        byte[] arscBytes = readFileBytes(arscFile);
        Log.d(TAG, "ARSC file loaded: " + arscBytes.length + " bytes");

        try {
            // 第一步：读取并验证包名
            InputStream stream1 = new ByteArrayInputStream(arscBytes);
            ResTable resTable1 = new ResTable();
            ARSCDecoder decoder1 = new ARSCDecoder(stream1, resTable1, false);

            Log.d(TAG, "ARSCDecoder created for reading");
            decoder1.readTable();
            Log.d(TAG, "readTable() completed");

            // 获取ARSC中存储的包名
            String arscPackageName = decoder1.getPackageName();
            Log.d(TAG, "Package name in ARSC: " + arscPackageName);

            List<String> stringList = decoder1.mTableStrings.getList();
            Log.d(TAG, "Total strings in ARSC: " + stringList.size());

            stream1.close();

            // 第二步：修改字符串池
            Log.d(TAG, "=== Searching for package name in string pool ===");
            List<String> modifiedStrings = new ArrayList<>();
            int modifiedCount = 0;

            for (int i = 0; i < stringList.size(); i++) {
                String str = stringList.get(i);
                if (str != null && str.contains(oldPackageName)) {
                    String modifiedStr = str.replace(oldPackageName, newPackageName);
                    modifiedStrings.add(modifiedStr);
                    modifiedCount++;
                    Log.d(TAG, "Modified [" + i + "]: " + str + " -> " + modifiedStr);
                } else {
                    modifiedStrings.add(str);
                }
            }

            Log.d(TAG, "Modified " + modifiedCount + " strings in string pool");

            // 第三步：修改Package Header
            byte[] result = arscBytes;
            boolean headerModified = false;

            if (arscPackageName != null && arscPackageName.equals(oldPackageName)) {
                Log.d(TAG, "Modifying Package Header with new method");
                result = modifyPackageHeader(arscBytes, oldPackageName, newPackageName);
                headerModified = true;
                Log.d(TAG, "Package Header modified");
            } else {
                Log.d(TAG, "Package header does not need modification");
                Log.d(TAG, "ARSC package: " + arscPackageName + ", Expected: " + oldPackageName);
            }

            // 第四步：如果字符串池有修改，则写入
            if (modifiedCount > 0) {
                Log.d(TAG, "Writing modified string pool");

                // 使用修改后的header作为基础
                InputStream stream2 = new ByteArrayInputStream(result);
                ResTable resTable2 = new ResTable();
                ARSCDecoder decoder2 = new ARSCDecoder(stream2, resTable2, false);

                decoder2.readTable();

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                decoder2.write(outputStream, stringList, modifiedStrings);

                stream2.close();
                outputStream.close();

                result = outputStream.toByteArray();
                Log.d(TAG, "String pool write completed, output size: " + result.length);
            }

            if (modifiedCount == 0 && !headerModified) {
                Log.w(TAG, "No modifications made to ARSC");
            } else {
                Log.d(TAG, "ARSC modification completed: " + modifiedCount + " strings, header: " + headerModified);
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error modifying ARSC", e);
            Log.w(TAG, "Returning original ARSC due to error");
            return arscBytes;
        }
    }

    /**
     * 修改ARSC的Package Header中的包名
     * 使用类似writeNulEndedString的方式，支持任意长度包名（最大128字符）
     */
    private byte[] modifyPackageHeader(byte[] arscBytes, String oldPkg, String newPkg) throws IOException {
        Log.d(TAG, "modifyPackageHeader: " + oldPkg + " -> " + newPkg);

        // Package Header中包名的位置需要通过解析定位
        // 包名字段固定256字节（128个UTF-16LE字符）

        // 搜索旧包名（UTF-16LE编码）
        byte[] oldBytes = encodePackageName(oldPkg);

        Log.d(TAG, "Old package encoded length: " + oldBytes.length);
        Log.d(TAG, "New package length: " + newPkg.length() + " chars");

        // 在ARSC数据中搜索包名位置
        int pos = findPackageNamePosition(arscBytes, oldBytes);

        if (pos < 0) {
            Log.w(TAG, "Package name not found in ARSC data");
            return arscBytes;
        }

        Log.d(TAG, "Found package name at position: " + pos);

        // 创建新的字节数组
        byte[] result = arscBytes.clone();

        // 编码新包名（固定256字节）
        byte[] newBytes = encodePackageName(newPkg);

        // 替换整个包名字段（256字节）
        System.arraycopy(newBytes, 0, result, pos, 256);

        Log.d(TAG, "Package name replaced in header");
        return result;
    }

    /**
     * 将包名编码为UTF-16LE格式，固定256字节
     * 模拟writeNulEndedString的行为
     */
    private byte[] encodePackageName(String packageName) {
        byte[] result = new byte[256]; // 128 chars * 2 bytes

        char[] chars = packageName.toCharArray();
        int length = Math.min(chars.length, 128); // 最大128字符

        // 写入字符（UTF-16LE，小端序）
        for (int i = 0; i < length; i++) {
            char ch = chars[i];
            result[i * 2] = (byte) (ch & 0xFF);
            result[i * 2 + 1] = (byte) ((ch >>> 8) & 0xFF);
        }

        // 剩余部分自动为0（已由new byte[256]初始化）

        return result;
    }

    /**
     * 查找Package Header中包名字段的位置
     */
    private int findPackageNamePosition(byte[] data, byte[] pattern) {
        // Package name位于Package Header中
        // 格式：Type(2) + HeaderSize(2) + Size(4) + ID(4) + Name(256) + ...

        // 简单搜索：查找完整的256字节包名字段
        for (int i = 0; i <= data.length - 256; i++) {
            boolean found = true;

            // 检查前几个字符是否匹配
            int checkLen = Math.min(pattern.length, 40); // 只检查前20个字符
            for (int j = 0; j < checkLen; j++) {
                if (data[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }

            if (found) {
                // 验证这是256字节的包名字段
                // 检查后面是否有足够的0填充
                boolean hasNullPadding = false;
                for (int j = checkLen; j < 256; j += 2) {
                    if (data[i + j] == 0 && data[i + j + 1] == 0) {
                        hasNullPadding = true;
                        break;
                    }
                }

                if (hasNullPadding || checkLen >= pattern.length) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * 简单修改包名的方法（使用CloneArsc）
     * 注意：此方法通过添加随机字符修改包名，适用于制作共存包
     *
     * @param randomChar 要添加到包名末尾的随机字符
     * @return 修改后的arsc文件字节数组
     * @throws IOException
     */
    public byte[] cloneArsc(String randomChar) throws IOException {
        Log.d(TAG, "cloneArsc: randomChar=" + randomChar);

        // 读取arsc文件
        InputStream arscStream = new BufferedInputStream(new FileInputStream(arscFile));

        // 创建ResTable
        ResTable resTable = new ResTable();

        // 创建解码器
        ARSCDecoder decoder = new ARSCDecoder(arscStream, resTable, false);

        // 创建输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 使用CloneArsc方法修改包名
        decoder.CloneArsc(outputStream, randomChar, false);

        // 关闭流
        arscStream.close();
        outputStream.close();

        return outputStream.toByteArray();
    }

    /**
     * 获取arsc中的所有字符串
     *
     * @return 字符串列表
     * @throws IOException
     */
    public List<String> getAllStrings() throws IOException {
        Log.d(TAG, "getAllStrings");

        byte[] arscBytes = readFileBytes(arscFile);
        InputStream arscStream = new ByteArrayInputStream(arscBytes);

        ResTable resTable = new ResTable();
        ARSCDecoder decoder = new ARSCDecoder(arscStream, resTable, false);

        decoder.readTable();
        List<String> strings = decoder.mTableStrings.getList();

        arscStream.close();

        Log.d(TAG, "getAllStrings: found " + strings.size() + " strings");
        return strings;
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
     * 静态方法：直接修改arsc文件中的包名
     *
     * @param arscFile resources.arsc文件
     * @param oldPackageName 旧包名
     * @param newPackageName 新包名
     * @return 修改后的字节数组
     * @throws IOException
     */
    public static byte[] modifyArscPackageName(File arscFile, String oldPackageName, String newPackageName)
            throws IOException {
        ArscEditor editor = new ArscEditor(arscFile);
        return editor.modifyPackageName(oldPackageName, newPackageName);
    }

    /**
     * 静态方法：修改arsc文件并保存
     *
     * @param arscFile resources.arsc文件
     * @param oldPackageName 旧包名
     * @param newPackageName 新包名
     * @param outputFile 输出文件
     * @throws IOException
     */
    public static void modifyAndSave(File arscFile, String oldPackageName, String newPackageName, File outputFile)
            throws IOException {
        byte[] modifiedBytes = modifyArscPackageName(arscFile, oldPackageName, newPackageName);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(modifiedBytes);
        }
    }
}
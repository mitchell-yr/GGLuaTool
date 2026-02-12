package mituran.gglua.tool.apktools;

import android.util.Log;

import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.rewriter.DexRewriter;
import com.android.tools.smali.dexlib2.rewriter.Rewriter;
import com.android.tools.smali.dexlib2.rewriter.RewriterModule;
import com.android.tools.smali.dexlib2.rewriter.Rewriters;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * DEX文件修改器
 * 用于修改DEX文件中的包名引用
 */
public class DexModifier {

    private static final String TAG = "DexModifier";
    
    private File dexFile;
    private String oldPackageName;
    private String newPackageName;

    public DexModifier(File dexFile) {
        this.dexFile = dexFile;
    }

    /**
     * 修改DEX文件中的包名
     * 
     * @param oldPackageName 旧包名（Java格式：com.example.app）
     * @param newPackageName 新包名（Java格式：com.new.app）
     * @param outputFile 输出文件
     * @return 是否修改成功
     */
    public boolean modifyPackageName(String oldPackageName, String newPackageName, File outputFile) {
        this.oldPackageName = oldPackageName;
        this.newPackageName = newPackageName;
        
        Log.d(TAG, "Modifying DEX: " + dexFile.getName());
        Log.d(TAG, "Old package: " + oldPackageName);
        Log.d(TAG, "New package: " + newPackageName);
        
        try {
            // 1. 加载DEX文件
            DexFile dexFileObj = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault());
            Log.d(TAG, "DEX file loaded, classes: " + dexFileObj.getClasses().size());
            
            // 2. 创建类型重写器
            PackageNameRewriterModule rewriterModule = new PackageNameRewriterModule();
            DexRewriter rewriter = new DexRewriter(rewriterModule);
            
            // 3. 重写DEX文件
            DexFile rewrittenDexFile = rewriter.getDexFileRewriter().rewrite(dexFileObj);
            Log.d(TAG, "DEX file rewritten");
            
            // 4. 写入新的DEX文件
            DexPool.writeTo(new FileDataStore(outputFile), rewrittenDexFile);
            Log.d(TAG, "DEX file written to: " + outputFile.getAbsolutePath());
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to modify DEX file", e);
            return false;
        }
    }

    /**
     * 包名重写模块
     * 将所有类型引用中的旧包名替换为新包名
     */
    private class PackageNameRewriterModule extends RewriterModule {
        
        @Nonnull
        @Override
        public Rewriter<String> getTypeRewriter(@Nonnull Rewriters rewriters) {
            return new Rewriter<String>() {
                @Nonnull
                @Override
                public String rewrite(@Nonnull String value) {
                    // DEX中的类型格式：Lcom/example/app/MainActivity;
                    // 需要将Java包名转换为DEX格式
                    String oldDexPackage = javaPackageToDexPackage(oldPackageName);
                    String newDexPackage = javaPackageToDexPackage(newPackageName);
                    
                    if (value.contains(oldDexPackage)) {
                        String newValue = value.replace(oldDexPackage, newDexPackage);
                        Log.d(TAG, "Type rewrite: " + value + " -> " + newValue);
                        return newValue;
                    }
                    
                    return value;
                }
            };
        }
    }

    /**
     * 将Java包名转换为DEX包名格式
     * Java: com.example.app -> DEX: com/example/app
     */
    private String javaPackageToDexPackage(String javaPackage) {
        return javaPackage.replace('.', '/');
    }

    /**
     * 将DEX包名转换为Java包名格式
     * DEX: com/example/app -> Java: com.example.app
     */
    private String dexPackageToJavaPackage(String dexPackage) {
        return dexPackage.replace('/', '.');
    }

    /**
     * 静态方法：修改单个DEX文件的包名
     */
    public static boolean modifyDexPackageName(File dexFile, String oldPackage, String newPackage, File outputFile) {
        DexModifier modifier = new DexModifier(dexFile);
        return modifier.modifyPackageName(oldPackage, newPackage, outputFile);
    }

    /**
     * 静态方法：修改多个DEX文件的包名
     * 
     * @param dexFiles DEX文件列表（classes.dex, classes2.dex, ...）
     * @param oldPackage 旧包名
     * @param newPackage 新包名
     * @param outputDir 输出目录
     * @return 成功修改的文件数量
     */
    public static int modifyMultipleDex(List<File> dexFiles, String oldPackage, String newPackage, File outputDir) {
        Log.d(TAG, "Modifying " + dexFiles.size() + " DEX files");
        
        int successCount = 0;
        
        for (File dexFile : dexFiles) {
            try {
                File outputFile = new File(outputDir, dexFile.getName());
                
                DexModifier modifier = new DexModifier(dexFile);
                if (modifier.modifyPackageName(oldPackage, newPackage, outputFile)) {
                    successCount++;
                    Log.d(TAG, "Successfully modified: " + dexFile.getName());
                } else {
                    Log.w(TAG, "Failed to modify: " + dexFile.getName());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error modifying " + dexFile.getName(), e);
            }
        }
        
        Log.d(TAG, "Modified " + successCount + "/" + dexFiles.size() + " DEX files");
        return successCount;
    }

    /**
     * 查找目录中的所有DEX文件
     * 
     * @param directory 目录
     * @return DEX文件列表
     */
    public static List<File> findDexFiles(File directory) {
        List<File> dexFiles = new ArrayList<>();
        
        if (!directory.exists() || !directory.isDirectory()) {
            return dexFiles;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return dexFiles;
        }
        
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".dex")) {
                dexFiles.add(file);
                Log.d(TAG, "Found DEX file: " + file.getName());
            }
        }
        
        return dexFiles;
    }

    /**
     * 获取DEX文件信息
     */
    public static class DexInfo {
        public String fileName;
        public int classCount;
        public List<String> packageNames;
        
        public DexInfo(String fileName, int classCount, List<String> packageNames) {
            this.fileName = fileName;
            this.classCount = classCount;
            this.packageNames = packageNames;
        }
    }

    /**
     * 分析DEX文件，获取包名信息
     * 
     * @param dexFile DEX文件
     * @return DEX文件信息
     */
    public static DexInfo analyzeDexFile(File dexFile) {
        try {
            DexFile dexFileObj = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault());
            
            int classCount = dexFileObj.getClasses().size();
            List<String> packageNames = new ArrayList<>();
            
            // 提取所有不同的包名
            for (ClassDef classDef : dexFileObj.getClasses()) {
                String type = classDef.getType();
                // 类型格式：Lcom/example/app/MainActivity;
                if (type.startsWith("L") && type.endsWith(";")) {
                    String className = type.substring(1, type.length() - 1);
                    int lastSlash = className.lastIndexOf('/');
                    if (lastSlash > 0) {
                        String packageName = className.substring(0, lastSlash).replace('/', '.');
                        if (!packageNames.contains(packageName)) {
                            packageNames.add(packageName);
                        }
                    }
                }
            }
            
            return new DexInfo(dexFile.getName(), classCount, packageNames);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze DEX file", e);
            return new DexInfo(dexFile.getName(), 0, new ArrayList<String>());
        }
    }
}

package mituran.gglua.tool.plugin;

//  插件管理器

import android.content.Context;
import mituran.gglua.tool.model.Plugin;
import org.json.JSONObject;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class PluginManager {
    private static final String PLUGINS_DIR = "/sdcard/GGtool/plugins/";
    private static final String PLUGIN_INFO_FILE = "plugin_info.json";
    private static final String PLUGIN_DOC_FILE = "document.md";

    private Context context;

    public PluginManager(Context context) {
        this.context = context;
        ensurePluginsDirectory();
    }

    private void ensurePluginsDirectory() {
        File dir = new File(PLUGINS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // 获取所有插件
    public List<Plugin> getAllPlugins() {
        List<Plugin> plugins = new ArrayList<>();
        File pluginsDir = new File(PLUGINS_DIR);

        if (pluginsDir.exists() && pluginsDir.isDirectory()) {
            File[] folders = pluginsDir.listFiles(File::isDirectory);
            if (folders != null) {
                for (File folder : folders) {
                    try {
                        Plugin plugin = loadPlugin(folder);
                        if (plugin != null) {
                            plugins.add(plugin);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return plugins;
    }

    // 加载单个插件
    private Plugin loadPlugin(File folder) throws Exception {
        File infoFile = new File(folder, PLUGIN_INFO_FILE);
        if (!infoFile.exists()) {
            return null;
        }

        String jsonContent = readFile(infoFile);
        JSONObject json = new JSONObject(jsonContent);
        return Plugin.fromJson(folder.getName(), folder.getAbsolutePath(), json);
    }

    // 保存插件配置
    public boolean savePluginInfo(Plugin plugin) {
        try {
            File infoFile = new File(plugin.getFolderPath(), PLUGIN_INFO_FILE);
            JSONObject json = plugin.toJson();
            writeFile(infoFile, json.toString(2));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 删除插件
    public boolean deletePlugin(Plugin plugin) {
        File folder = new File(plugin.getFolderPath());
        return deleteRecursive(folder);
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    // 导出插件为zip
    public boolean exportPlugin(Plugin plugin, String exportPath) {
        try {
            File sourceFolder = new File(plugin.getFolderPath());
            File zipFile = new File(exportPath, plugin.getName() + ".zip");

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            zipFolder(sourceFolder, sourceFolder.getName(), zos);

            zos.close();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void zipFolder(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    zipFolder(file, parentFolder + "/" + file.getName(), zos);
                } else {
                    FileInputStream fis = new FileInputStream(file);
                    ZipEntry zipEntry = new ZipEntry(parentFolder + "/" + file.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }

                    fis.close();
                    zos.closeEntry();
                }
            }
        }
    }

    // 导入插件
    public boolean importPlugin(String zipPath) {
        try {
            File zipFile = new File(zipPath);
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(fis);

            ZipEntry entry;
            String pluginName = null;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                String[] parts = entryName.split("/");

                if (pluginName == null && parts.length > 0) {
                    pluginName = parts[0];
                }

                File outputFile = new File(PLUGINS_DIR + entryName);

                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    outputFile.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(outputFile);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }

                    fos.close();
                }
                zis.closeEntry();
            }

            zis.close();
            fis.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 读取文档
    public String readPluginDocument(Plugin plugin) {
        try {
            File docFile = new File(plugin.getFolderPath(), PLUGIN_DOC_FILE);
            if (docFile.exists()) {
                return readFile(docFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "文档不存在";
    }

    private String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    private void writeFile(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }
}
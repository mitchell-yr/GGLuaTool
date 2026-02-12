package mituran.gglua.tool.plugin;

// 插件管理界面

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import mituran.gglua.tool.MarkdownViewerActivity;
import mituran.gglua.tool.R;
import mituran.gglua.tool.model.Plugin;

import java.util.List;

public class PluginManagerActivity extends AppCompatActivity implements PluginAdapter.OnPluginActionListener {
    private static final int REQUEST_CODE_STORAGE = 100;
    private static final int REQUEST_CODE_IMPORT = 101;

    private RecyclerView recyclerView;
    private PluginAdapter adapter;
    private PluginManager pluginManager;
    private List<Plugin> plugins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_manager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("插件管理");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pluginManager = new PluginManager(this);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //checkPermissions();
        loadPlugins();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_STORAGE);
        } else {
            loadPlugins();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadPlugins();
            } else {
                Toast.makeText(this, "需要存储权限才能管理插件", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void loadPlugins() {
        plugins = pluginManager.getAllPlugins();
        adapter = new PluginAdapter(this, plugins, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_plugin_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_more) {
            showMoreMenu();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showMoreMenu() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.action_more));
        popup.getMenuInflater().inflate(R.menu.menu_plugin_more_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_import_plugin) {
                importPlugin();
                return true;
            } else if (id == R.id.action_plugin_doc) {
                showPluginFunctionDoc();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void importPlugin() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择插件zip文件"), REQUEST_CODE_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = uri.getPath();
                if (pluginManager.importPlugin(path)) {
                    Toast.makeText(this, "导入成功", Toast.LENGTH_SHORT).show();
                    loadPlugins();
                } else {
                    Toast.makeText(this, "导入失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showPluginFunctionDoc() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("插件功能文档");
        builder.setMessage("插件系统说明：\n\n" +
                "1. 插件存储在 /sdcard/GGtool/plugins/ 目录\n" +
                "2. 每个插件一个文件夹，文件夹名即插件名\n" +
                "3. 必需文件：plugin_info.json 和 document.md\n" +
                "4. 导入：选择zip格式的插件包\n" +
                "5. 导出：将插件打包为zip文件\n" +
                "6. 开关：控制插件启用/禁用状态");
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    @Override
    public void onEnableChanged(Plugin plugin, boolean enabled) {
        plugin.setEnable(enabled);
        if (pluginManager.savePluginInfo(plugin)) {
            Toast.makeText(this, plugin.getName() + " 已" + (enabled ? "启用" : "禁用"), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(Plugin plugin) {
        // 点击item展开/折叠描述，已在adapter中处理
    }

    @Override
    public void onActionClick(Plugin plugin, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add(0, 1, 0, "删除");
        popup.getMenu().add(0, 2, 1, "导出");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    deletePlugin(plugin);
                    return true;
                case 2:
                    exportPlugin(plugin);
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void deletePlugin(Plugin plugin) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除插件 \"" + plugin.getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (pluginManager.deletePlugin(plugin)) {
                        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                        loadPlugins();
                    } else {
                        Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void exportPlugin(Plugin plugin) {
        String exportPath = "/sdcard/GGtool/exports/";
        if (pluginManager.exportPlugin(plugin, exportPath)) {
            Toast.makeText(this, "导出成功: " + exportPath + plugin.getName() + ".zip", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDocumentClick(Plugin plugin) {
        String document = pluginManager.readPluginDocument(plugin);
        Intent intent = new Intent(this, MarkdownViewerActivity.class);
        intent.putExtra("title", plugin.getName() + " - 使用文档");
        intent.putExtra("content", document);
        startActivity(intent);
    }
}
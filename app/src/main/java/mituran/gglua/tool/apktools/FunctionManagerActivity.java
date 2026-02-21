package mituran.gglua.tool.apktools;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import mituran.gglua.tool.R;

/**
 * 自定义函数管理界面
 *
 * 功能：
 *  - 展示已安装的自定义函数包列表
 *  - 删除自定义函数包
 *  - 导入 .ggfunc 文件（从文件选择器）
 *  - 导出 .ggfunc 文件到 /sdcard/GGtool/export/
 *
 * 通过 startActivity(Intent) 启动，无需返回值。
 * ModifierActivity 在返回时通过 loadCustomFunctions() 重新加载列表。
 */
public class FunctionManagerActivity extends AppCompatActivity {

    private RecyclerView rvFunctions;
    private TextView tvEmpty;
    private ExtendedFloatingActionButton fabImport;

    private final List<CustomFunctionPackage.PackageInfo> packages = new ArrayList<>();
    private FunctionAdapter adapter;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gg_function_manager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("自定义函数管理");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvEmpty     = findViewById(R.id.tv_empty);
        rvFunctions = findViewById(R.id.rv_functions);
        fabImport   = findViewById(R.id.fab_import);

        adapter = new FunctionAdapter(packages);
        rvFunctions.setLayoutManager(new LinearLayoutManager(this));
        rvFunctions.setAdapter(adapter);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) handleImport(uri);
                    }
                }
        );

        fabImport.setOnClickListener(v -> openFilePicker());

        loadPackages();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ─────────────────────────────────────────────────────────────────
    //  加载本地函数包
    // ─────────────────────────────────────────────────────────────────

    private void loadPackages() {
        packages.clear();
        packages.addAll(CustomFunctionPackage.loadAllLocal());
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(packages.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────
    //  导入
    // ─────────────────────────────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 仅显示 .ggfunc 文件（部分系统文件管理器支持）
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream", "*/*"});
        filePickerLauncher.launch(Intent.createChooser(intent, "选择 .ggfunc 函数包"));
    }

    private void handleImport(Uri uri) {
        try {
            // 先把文件复制到缓存目录
            String fileName = getFileName(uri);
            if (fileName == null || !fileName.endsWith(CustomFunctionPackage.EXTENSION)) {
                toast("请选择 " + CustomFunctionPackage.EXTENSION + " 格式的函数包");
                return;
            }

            File cacheFile = new File(getCacheDir(), "import_" + System.currentTimeMillis()
                    + CustomFunctionPackage.EXTENSION);

            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(cacheFile)) {
                if (is == null) throw new Exception("无法读取文件");
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            }

            // 解析包
            CustomFunctionPackage.PackageInfo info = CustomFunctionPackage.importPackage(cacheFile);
            cacheFile.delete();

            // 检查是否已存在同名包
            boolean exists = false;
            for (CustomFunctionPackage.PackageInfo p : packages) {
                if (p.name.equals(info.name)) { exists = true; break; }
            }

            if (exists) {
                new AlertDialog.Builder(this)
                        .setTitle("函数包已存在")
                        .setMessage("本地已有函数 " + info.name + "，是否覆盖？")
                        .setPositiveButton("覆盖", (d, w) -> doSave(info))
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                doSave(info);
            }

        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("导入失败")
                    .setMessage(e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    private void doSave(CustomFunctionPackage.PackageInfo info) {
        try {
            CustomFunctionPackage.saveLocally(info);
            toast("导入成功：" + info.name);
            loadPackages();
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  删除
    // ─────────────────────────────────────────────────────────────────

    private void confirmDelete(CustomFunctionPackage.PackageInfo info) {
        new AlertDialog.Builder(this)
                .setTitle("删除函数包")
                .setMessage("确定要删除 " + info.name + "吗？此操作不可撤销。")
                .setPositiveButton("删除", (d, w) -> {
                    CustomFunctionPackage.deleteLocally(info.name);
                    toast("已删除：" + info.name);
                    loadPackages();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────
    //  导出
    // ─────────────────────────────────────────────────────────────────

    private void doExport(CustomFunctionPackage.PackageInfo info) {
        try {
            File exported = CustomFunctionPackage.exportToExportDir(info.name);
            new AlertDialog.Builder(this)
                    .setTitle("导出成功")
                    .setMessage("已导出到：\n" + exported.getAbsolutePath())
                    .setPositiveButton("确定", null)
                    .show();
        } catch (Exception e) {
            toast("导出失败：" + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  RecyclerView 适配器
    // ─────────────────────────────────────────────────────────────────

    private class FunctionAdapter extends RecyclerView.Adapter<FunctionAdapter.VH> {
        private final List<CustomFunctionPackage.PackageInfo> data;

        FunctionAdapter(List<CustomFunctionPackage.PackageInfo> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_custom_gg_function, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            CustomFunctionPackage.PackageInfo info = data.get(pos);

            h.tvName.setText(info.name);
            h.tvDesc.setText(info.description != null && !info.description.isEmpty()
                    ? info.description : "（无描述）");

            // 版本标签
            String verLabel;
            switch (info.supportedVersion) {
                case V96:        verLabel = "仅 96";   break;
                case V101:       verLabel = "仅 101";  break;
                default:         verLabel = "通用";    break;
            }
            h.tvVersion.setText(verLabel);

            h.btnDelete.setOnClickListener(v -> confirmDelete(info));
            h.btnExport.setOnClickListener(v -> doExport(info));
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvVersion;
            ImageButton btnDelete, btnExport;

            VH(View v) {
                super(v);
                tvName    = v.findViewById(R.id.tv_func_name);
                tvDesc    = v.findViewById(R.id.tv_func_desc);
                tvVersion = v.findViewById(R.id.tv_func_version);
                btnDelete = v.findViewById(R.id.btn_func_delete);
                btnExport = v.findViewById(R.id.btn_func_export);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  工具
    // ─────────────────────────────────────────────────────────────────

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor =
                         getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) result = uri.getLastPathSegment();
        return result;
    }
}

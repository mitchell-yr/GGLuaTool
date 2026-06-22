package mituran.gglua.tool.template;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import mituran.gglua.tool.CppCompiler;
import mituran.gglua.tool.R;
import mituran.gglua.tool.model.Template;

public class TemplateManagerActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TEMPLATES_DIR = "/GGtool/templates";

    private RecyclerView recyclerView;
    private TemplateAdapter adapter;
    private List<Template> templateList;
    private List<Template> filteredList;
    private EditText searchEditText;
    private ImageButton moreButton;

    // 过滤标签
    private TextView tabAll, tabLua, tabCpp;
    private String currentFilter = "all"; // "all", "lua", "cpp"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_manager);

        initViews();
        checkPermissions();
    }

    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        moreButton = findViewById(R.id.moreButton);
        recyclerView = findViewById(R.id.recyclerView);
        tabAll = findViewById(R.id.tab_all);
        tabLua = findViewById(R.id.tab_lua);
        tabCpp = findViewById(R.id.tab_cpp);

        templateList = new ArrayList<>();
        filteredList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TemplateAdapter(filteredList, this);
        recyclerView.setAdapter(adapter);

        // 搜索功能
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTemplates(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 过滤标签点击
        tabAll.setOnClickListener(v -> setFilter("all"));
        tabLua.setOnClickListener(v -> setFilter("lua"));
        tabCpp.setOnClickListener(v -> setFilter("cpp"));

        // 更多按钮点击事件
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreMenu(v);
            }
        });
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateTabStyles();
        filterTemplates(searchEditText.getText().toString());
    }

    private void updateTabStyles() {
        tabAll.setBackgroundResource("all".equals(currentFilter)
                ? R.drawable.tab_background_selected : R.drawable.tab_background_unselected);
        tabAll.setTextColor("all".equals(currentFilter) ? Color.WHITE : Color.parseColor("#666666"));

        tabLua.setBackgroundResource("lua".equals(currentFilter)
                ? R.drawable.tab_background_selected : R.drawable.tab_background_unselected);
        tabLua.setTextColor("lua".equals(currentFilter) ? Color.WHITE : Color.parseColor("#666666"));

        tabCpp.setBackgroundResource("cpp".equals(currentFilter)
                ? R.drawable.tab_background_selected : R.drawable.tab_background_unselected);
        tabCpp.setTextColor("cpp".equals(currentFilter) ? Color.WHITE : Color.parseColor("#666666"));
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            } else {
                loadTemplates();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                loadTemplates();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadTemplates();
            } else {
                Toast.makeText(this, "需要存储权限才能使用此功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void loadTemplates() {
        templateList.clear();
        File baseDir = new File(Environment.getExternalStorageDirectory() + TEMPLATES_DIR);

        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        // 确保内置模板已复制
        CppCompiler.ensureBuiltinTemplates("lua", this);
        CppCompiler.ensureBuiltinTemplates("cpp", this);

        // 从 lua/ 和 cpp/ 子目录加载
        loadTemplatesFromDir(new File(baseDir, "lua"), "lua");
        loadTemplatesFromDir(new File(baseDir, "cpp"), "cpp");

        // 兼容旧版本：直接从 templates/ 根目录加载（默认为 lua）
        File[] legacyFiles = baseDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (legacyFiles != null) {
            for (File file : legacyFiles) {
                try {
                    String content = readFile(file);
                    JSONObject json = new JSONObject(content);

                    Template template = new Template();
                    template.name = file.getName().replace(".json", "");
                    template.version = json.optString("version", "1.0");
                    template.code = json.optString("code", "");
                    template.language = json.optString("language", "lua");
                    template.file = file;

                    templateList.add(template);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        applyFilterAndSearch();
    }

    private void loadTemplatesFromDir(File dir, String language) {
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    String content = readFile(file);
                    JSONObject json = new JSONObject(content);

                    Template template = new Template();
                    template.name = file.getName().replace(".json", "");
                    template.version = json.optString("version", "1.0");
                    template.code = json.optString("code", "");
                    template.language = language;
                    template.file = file;

                    templateList.add(template);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void applyFilterAndSearch() {
        filteredList.clear();
        for (Template t : templateList) {
            if ("all".equals(currentFilter) || currentFilter.equals(t.language)) {
                filteredList.add(t);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterTemplates(String query) {
        filteredList.clear();
        for (Template template : templateList) {
            // 语言过滤
            if (!"all".equals(currentFilter) && !currentFilter.equals(template.language)) {
                continue;
            }
            // 搜索过滤
            if (!query.isEmpty() && !template.name.toLowerCase().contains(query.toLowerCase())) {
                continue;
            }
            filteredList.add(template);
        }
        adapter.notifyDataSetChanged();
    }

    private void showMoreMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.menu_more, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_import) {
                    importTemplate();
                    return true;
                } else if (id == R.id.action_new) {
                    createNewTemplate();
                    return true;
                }
                return false;
            }
        });

        popup.show();
    }

    @SuppressWarnings("deprecation")
    private void importTemplate() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        startActivityForResult(intent, 1001);
    }

    // 更新创建新模板方法
    @SuppressWarnings("deprecation")
    private void createNewTemplate() {
        Intent intent = new Intent(this, TemplateEditorActivity.class);
        startActivityForResult(intent, 2001);
    }

    // 添加编辑模板方法（在Adapter中调用）
    @SuppressWarnings("deprecation")
    public void editTemplate(Template template) {
        Intent intent = new Intent(this, TemplateEditorActivity.class);
        intent.putExtra("template_name", template.name);
        intent.putExtra("template_version", template.version);
        intent.putExtra("template_code", template.code);
        intent.putExtra("template_language", template.language != null ? template.language : "lua");
        startActivityForResult(intent, 2002);
    }

    // 处理返回结果
    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 2001 || requestCode == 2002) {
                // 刷新模板列表
                loadTemplates();
            } else if (requestCode == 1001) {
                // 处理导入模板
                // ... 现有的导入逻辑
            }
        }
    }

    private String readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[(int) file.length()];
        fis.read(buffer);
        fis.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadTemplates();
            }
        }
    }
}
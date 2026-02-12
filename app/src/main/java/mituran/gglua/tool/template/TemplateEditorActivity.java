package mituran.gglua.tool.template;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import mituran.gglua.tool.R;

public class TemplateEditorActivity extends AppCompatActivity {

    private static final String TEMPLATES_DIR = "/GGtool/templates/";

    private TextInputEditText nameEditText;
    private TextInputEditText versionEditText;
    private EditText codeEditText;
    private TextView lineNumberView;
    private TextView charCountText;
    private TextView lineCountText;
    private TextView errorOverlay;
    private ExtendedFloatingActionButton fabSave;
    private TextInputLayout nameLayout;
    private TextInputLayout versionLayout;
    private ScrollView lineNumberScrollView;
    private ScrollView codeScrollView;

    private String originalName;
    private boolean isEditMode = false;
    private boolean isCodeValid = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_editor);

        initViews();
        setupToolbar();
        loadTemplateData();
        setupCodeEditor();
    }

    private void initViews() {
        nameEditText = findViewById(R.id.nameEditText);
        versionEditText = findViewById(R.id.versionEditText);
        codeEditText = findViewById(R.id.codeEditText);
        lineNumberView = findViewById(R.id.lineNumberView);
        charCountText = findViewById(R.id.charCountText);
        lineCountText = findViewById(R.id.lineCountText);
        errorOverlay = findViewById(R.id.errorOverlay);
        fabSave = findViewById(R.id.fabSave);
        nameLayout = findViewById(R.id.nameLayout);
        versionLayout = findViewById(R.id.versionLayout);
        lineNumberScrollView = findViewById(R.id.lineNumberScrollView);
        codeScrollView = findViewById(R.id.codeScrollView);

        // 设置FAB点击事件
        fabSave.setOnClickListener(v -> saveTemplate());

        // 添加文本变化监听，实时验证
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateName();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadTemplateData() {
        Intent intent = getIntent();
        if (intent.hasExtra("template_name")) {
            isEditMode = true;
            originalName = intent.getStringExtra("template_name");
            String version = intent.getStringExtra("template_version");
            String code = intent.getStringExtra("template_code");

            nameEditText.setText(originalName);
            versionEditText.setText(version);
            codeEditText.setText(code);

            setTitle("编辑模板");
            fabSave.setText("保存修改");
            fabSave.setIconResource(R.drawable.ic_save);
        } else {
            setTitle("新建模板");
            fabSave.setText("创建模板");
            fabSave.setIconResource(R.drawable.ic_check);
            versionEditText.setText("1.0");
        }
    }

    private void setupCodeEditor() {
        // 更新行号和统计
        updateLineNumbers();
        updateStatistics();

        // 监听代码变化
        codeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLineNumbers();
                updateStatistics();
                validateCode(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 同步滚动 - 改进的滚动同步机制
        codeScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // 同步行号滚动
                lineNumberScrollView.scrollTo(0, scrollY);
            }
        });

        // 确保EditText可以获取焦点并正常滚动
        codeEditText.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    private void updateLineNumbers() {
        String code = codeEditText.getText().toString();
        String[] lines = code.split("\n", -1);

        StringBuilder lineNumbers = new StringBuilder();
        for (int i = 1; i <= lines.length; i++) {
            if (i > 1) lineNumbers.append("\n");
            lineNumbers.append(String.format("%3d", i));
        }

        lineNumberView.setText(lineNumbers.toString());
    }

    private void updateStatistics() {
        String code = codeEditText.getText().toString();
        int charCount = code.length();
        int lineCount = code.isEmpty() ? 0 : code.split("\n", -1).length;

        charCountText.setText(charCount + " 字符");
        lineCountText.setText(lineCount + " 行");
    }

    private void validateCode(String code) {
        // 验证代码是否以非空格和换行的字符开头
        if (code.isEmpty()) {
            isCodeValid = true;
            errorOverlay.setVisibility(View.GONE);
            fabSave.setEnabled(true);
            return;
        }

        // 检查是否以空格、换行、制表符等空白字符开头
        char firstChar = code.charAt(0);
        if (Character.isWhitespace(firstChar)) {
            isCodeValid = false;
            errorOverlay.setVisibility(View.VISIBLE);
            errorOverlay.setText("❌ 模板内容不能以空格、换行或其他空白字符开头");

            // 禁用保存按钮并改变颜色
            fabSave.setEnabled(false);
            fabSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
        } else {
            isCodeValid = true;
            errorOverlay.setVisibility(View.GONE);
            fabSave.setEnabled(true);
            fabSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF6200EE")));
        }
    }

    private boolean validateName() {
        String name = nameEditText.getText().toString().trim();

        if (name.isEmpty()) {
            nameLayout.setError("模板名称不能为空");
            return false;
        }

        // 检查文件名是否合法
        if (name.contains("/") || name.contains("\\") || name.contains(":") ||
                name.contains("*") || name.contains("?") || name.contains("\"") ||
                name.contains("<") || name.contains(">") || name.contains("|")) {
            nameLayout.setError("模板名称包含非法字符");
            return false;
        }

        // 检查是否重名（编辑模式下允许同名）
        if (!isEditMode || !name.equals(originalName)) {
            File file = new File(Environment.getExternalStorageDirectory() + TEMPLATES_DIR + name + ".json");
            if (file.exists()) {
                nameLayout.setError("模板名称已存在");
                return false;
            }
        }

        nameLayout.setError(null);
        return true;
    }

    private void saveTemplate() {
        // 验证名称
        if (!validateName()) {
            Toast.makeText(this, "请检查模板名称", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证代码内容
        String code = codeEditText.getText().toString();
        if (!isCodeValid) {
            Toast.makeText(this, "模板内容不能以空白字符开头", Toast.LENGTH_LONG).show();
            return;
        }

        // 二次确认代码内容（防止某些边缘情况）
        if (!code.isEmpty() && Character.isWhitespace(code.charAt(0))) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("警告")
                    .setMessage("模板内容以空白字符开头，这可能导致模板无法正常使用。是否移除开头的空白字符？")
                    .setPositiveButton("移除并保存", (dialog, which) -> {
                        String trimmedCode = code.replaceAll("^\\s+", "");
                        codeEditText.setText(trimmedCode);
                        performSave();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }

        performSave();
    }

    private void performSave() {
        String name = nameEditText.getText().toString().trim();
        String version = versionEditText.getText().toString().trim();
        String code = codeEditText.getText().toString();

        if (version.isEmpty()) {
            version = "1.0";
        }

        try {
            File dir = new File(Environment.getExternalStorageDirectory() + TEMPLATES_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 如果是编辑模式且名称改变，删除原文件
            if (isEditMode && !name.equals(originalName)) {
                File oldFile = new File(dir, originalName + ".json");
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            File file = new File(dir, name + ".json");

            JSONObject json = new JSONObject();
            json.put("version", version);
            json.put("code", code);
            json.put("createTime", System.currentTimeMillis());
            json.put("updateTime", System.currentTimeMillis());

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.toString(4).getBytes(StandardCharsets.UTF_8));
            fos.close();

            Toast.makeText(this, isEditMode ? "模板更新成功" : "模板创建成功", Toast.LENGTH_SHORT).show();

            // 返回结果
            setResult(RESULT_OK);
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_template_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_format) {
            formatCode();
            return true;
        } else if (id == R.id.action_clear) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认清空")
                    .setMessage("确定要清空所有代码吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        codeEditText.setText("");
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        } else if (id == R.id.action_insert_snippet) {
            showSnippetDialog();
            return true;
        } else if (id == R.id.action_trim) {
            trimCode();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void formatCode() {
        String code = codeEditText.getText().toString();
        // 移除每行末尾的空白字符
        String[] lines = code.split("\n");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) formatted.append("\n");
            formatted.append(lines[i].replaceAll("\\s+$", ""));
        }
        codeEditText.setText(formatted.toString());
        Toast.makeText(this, "代码已格式化", Toast.LENGTH_SHORT).show();
    }

    private void trimCode() {
        String code = codeEditText.getText().toString();
        // 移除开头和结尾的空白字符
        String trimmed = code.trim();
        if (!code.equals(trimmed)) {
            codeEditText.setText(trimmed);
            Toast.makeText(this, "已移除首尾空白字符", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "没有需要移除的空白字符", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSnippetDialog() {
        String[] snippets = {
                "-- 函数框架\nfunction main()\n    -- 在此处编写代码\nend",
                "-- 循环示例\nfor i = 1, 10 do\n    print(i)\nend",
                "-- 条件判断\nif condition then\n    -- 条件为真时执行\nelse\n    -- 条件为假时执行\nend",
                "-- 函数定义\nlocal function myFunction(param1, param2)\n    -- 函数体\n    return result\nend"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("插入代码片段")
                .setItems(new String[]{"基础函数", "循环结构", "条件判断", "函数定义"},
                        (dialog, which) -> {
                            int position = codeEditText.getSelectionStart();
                            codeEditText.getText().insert(position, snippets[which]);
                        })
                .show();
    }
}
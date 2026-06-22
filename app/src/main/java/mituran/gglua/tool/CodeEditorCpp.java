package mituran.gglua.tool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import org.eclipse.tm4e.core.registry.IThemeSource;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;
import io.github.rosemoe.sora.widget.CodeEditor;


public class CodeEditorCpp extends AppCompatActivity {

    private static final String PREFS_NAME = "EditorSettings";
    private static final String PREF_FONT_SIZE = "fontSize";
    private static final String PREF_LINE_NUMBERS = "lineNumbers";
    private static final String PREF_WORD_WRAP = "wordWrap";
    private static final String PREF_THEME = "theme";
    private static final String PREF_AUTO_SAVE = "autoSave";
    private static final String PREF_AUTO_BACKUP = "autoBackup";

    private static final long AUTO_SAVE_INTERVAL_MS = 30_000;

    private android.os.Handler autoSaveHandler;
    private Runnable autoSaveRunnable;

    private ImageButton btnUndo, btnRedo, btnEdit, btnManage, btnCopy, btnPaste, btnFind, btnSave, btnTutorial, btnCompile, btnLog;
    private String path, dirPath;
    private CodeEditor codeEditor;
    private LinearLayout symbolContainer;

    private DrawerLayout drawerLayout;
    private TextView logTextView;
    private ScrollView logScrollView;
    private StringBuilder logBuilder = new StringBuilder();

    private CppCompiler cppCompiler;
    private List<Template> templates;

    private String[] symbols = {
            ",", ".", "-", "(", "[", "{", "=", "<", ">",
            "+", "*", "/", "%", "!", "&", "|", "~", "^",
            ")", "]", "}", ";", ":", "#"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codeeditor_cpp);

        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);
        btnEdit = findViewById(R.id.btn_edit);
        btnManage = findViewById(R.id.btn_manage);
        btnCopy = findViewById(R.id.btn_copy);
        btnPaste = findViewById(R.id.btn_paste);
        btnFind = findViewById(R.id.btn_find);
        btnSave = findViewById(R.id.btn_save);
        btnTutorial = findViewById(R.id.btn_tutorial);
        btnCompile = findViewById(R.id.btn_compile);
        btnLog = findViewById(R.id.btn_log);

        symbolContainer = findViewById(R.id.symbol_container);
        drawerLayout = findViewById(R.id.drawer_layout);
        logTextView = findViewById(R.id.log_text_view);
        logScrollView = findViewById(R.id.log_scroll_view);

        cppCompiler = new CppCompiler(this);

        Bundle bundle = getIntent().getExtras();
        path = bundle.getString("path") + "/code.cpp";
        dirPath = bundle.getString("path");
        String codeTextTemp = readFileFromExternalStorage(path);

        codeEditor = findViewById(R.id.editor);
        codeEditor.setText(codeTextTemp);

        try {
            FileProviderRegistry.getInstance().addFileProvider(
                    new AssetsFileResolver(
                            getApplicationContext().getAssets()
                    )
            );
            var themeRegistry = ThemeRegistry.getInstance();
            var name = "solarized-light";
            var themeAssetsPath = "textmate/" + name + ".json";
            var model = new ThemeModel(
                    IThemeSource.fromInputStream(
                            FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath),
                            themeAssetsPath, null
                    ),
                    name
            );
            themeRegistry.loadTheme(model);
            ThemeRegistry.getInstance().setTheme(name);

            GrammarRegistry.getInstance().loadGrammars("textmate/language.json");

            codeEditor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));

            var languageScopeName = "source.cpp";
            var language = TextMateLanguage.create(
                    languageScopeName, true
            );
            codeEditor.setEditorLanguage(language);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        codeEditor.setPinLineNumber(true);

        loadEditorSettings();
        initSymbolBar();
        initTemplates();

        btnSave.setOnClickListener(v -> {
            saveData(codeEditor.getText().toString(), dirPath, "/code.cpp");
            Toast.makeText(CodeEditorCpp.this, "保存成功", Toast.LENGTH_SHORT).show();
        });

        btnTutorial.setOnClickListener(v -> {
            Toast.makeText(CodeEditorCpp.this, "C++函数列表即将推出", Toast.LENGTH_SHORT).show();
        });

        btnUndo.setOnClickListener(v -> codeEditor.undo());

        btnFind.setOnClickListener(v -> codeEditor.beginSearchMode());

        btnRedo.setOnClickListener(v -> codeEditor.redo());

        btnEdit.setOnClickListener(v -> showEditMenu(v));

        btnCopy.setOnClickListener(v -> codeEditor.copyText());

        btnPaste.setOnClickListener(v -> codeEditor.pasteText());

        btnManage.setOnClickListener(v -> showManageMenu(v));

        btnCompile.setOnClickListener(v -> startCompile());

        btnLog.setOnClickListener(v -> openLogDrawer());

        findViewById(R.id.btn_clear_log).setOnClickListener(v -> {
            logBuilder.setLength(0);
            logTextView.setText("");
            Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_copy_log).setOnClickListener(v -> copyLogToClipboard());

        // 初始化自动保存
        autoSaveHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean autoSave = prefs.getBoolean(PREF_AUTO_SAVE, false);
                boolean autoBackup = prefs.getBoolean(PREF_AUTO_BACKUP, false);
                if (autoSave) {
                    saveData(codeEditor.getText().toString(), dirPath, "/code.cpp");
                }
                if (autoBackup) {
                    performBackup(false);
                }
                autoSaveHandler.postDelayed(this, AUTO_SAVE_INTERVAL_MS);
            }
        };
        autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_INTERVAL_MS);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });
    }

    private void loadEditorSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        float fontSize = prefs.getFloat(PREF_FONT_SIZE, 16f);
        codeEditor.setTextSize(fontSize);

        boolean showLineNumbers = prefs.getBoolean(PREF_LINE_NUMBERS, true);
        codeEditor.setLineNumberEnabled(showLineNumbers);

        boolean wordWrap = prefs.getBoolean(PREF_WORD_WRAP, false);
        codeEditor.setWordwrap(wordWrap);

        String theme = prefs.getString(PREF_THEME, "solarized-light");
        if (!theme.equals("solarized-light")) {
            applyTheme(theme);
        }
    }

    private void saveEditorSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(PREF_FONT_SIZE, codeEditor.getTextSizePx() / getResources().getDisplayMetrics().density);
        editor.putBoolean(PREF_LINE_NUMBERS, codeEditor.isLineNumberEnabled());
        editor.putBoolean(PREF_WORD_WRAP, codeEditor.isWordwrap());
        editor.apply();
    }

    private void initSymbolBar() {
        for (String symbol : symbols) {
            Button btn = new Button(this);
            btn.setText(symbol);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            btn.setPadding(35, 4, 35, 4);
            btn.setAllCaps(false);
            btn.setMinHeight(0);
            btn.setMinWidth(0);
            btn.setMinimumHeight(0);
            btn.setMinimumWidth(0);
            btn.setGravity(Gravity.CENTER);

            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            btn.setBackgroundResource(outValue.resourceId);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            params.setMargins(2, 0, 2, 0);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> insertSymbol(symbol));
            symbolContainer.addView(btn);
        }
    }

    private void insertSymbol(String symbol) {
        codeEditor.insertText(symbol, symbol.length());
        switch (symbol) {
            case "(":
                codeEditor.insertText(")", 1);
                codeEditor.getCursor().set(
                        codeEditor.getCursor().getLeftLine(),
                        codeEditor.getCursor().getLeftColumn() - 1
                );
                break;
            case "[":
                codeEditor.insertText("]", 1);
                codeEditor.getCursor().set(
                        codeEditor.getCursor().getLeftLine(),
                        codeEditor.getCursor().getLeftColumn() - 1
                );
                break;
            case "{":
                codeEditor.insertText("}", 1);
                codeEditor.getCursor().set(
                        codeEditor.getCursor().getLeftLine(),
                        codeEditor.getCursor().getLeftColumn() - 1
                );
                break;
        }
    }

    private void showEditMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, 1, 0, "全选");
        popupMenu.getMenu().add(0, 2, 0, "跳转到行");

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    codeEditor.selectAll();
                    break;
                case 2:
                    showGotoLineDialog();
                    break;
            }
            return true;
        });
        popupMenu.show();
    }

    private void showManageMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);

        popupMenu.getMenu().add(0, 2, 0, "另存为");
        popupMenu.getMenu().add(0, 3, 0, "导入模板");
        popupMenu.getMenu().add(0, 11, 0, "手动备份");

        SubMenu editorSubMenu = popupMenu.getMenu().addSubMenu(0, 5, 0, "编辑器设置");
        editorSubMenu.add(0, 6, 0, "主题设置");
        editorSubMenu.add(0, 7, 0, "字体大小");
        editorSubMenu.add(0, 8, 0, "显示行号");
        editorSubMenu.add(0, 9, 0, "换行显示");
        editorSubMenu.add(0, 12, 0, getAutoSaveMenuTitle());
        editorSubMenu.add(0, 13, 0, getAutoBackupMenuTitle());

        popupMenu.getMenu().add(0, 10, 0, "关于");

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 2:
                    saveAs();
                    break;
                case 3:
                    importTemplate();
                    break;
                case 6:
                    showThemeSettings();
                    break;
                case 7:
                    showFontSizeDialog();
                    break;
                case 8:
                    toggleLineNumbers();
                    break;
                case 9:
                    toggleWordWrap();
                    break;
                case 10:
                    showAboutDialog();
                    break;
                case 11:
                    performBackup(true);
                    break;
                case 12:
                    toggleAutoSave();
                    break;
                case 13:
                    toggleAutoBackup();
                    break;
            }
            return true;
        });
        popupMenu.show();
    }

    private String getAutoSaveMenuTitle() {
        boolean enabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(PREF_AUTO_SAVE, false);
        return "自动保存：" + (enabled ? "开" : "关");
    }

    private String getAutoBackupMenuTitle() {
        boolean enabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(PREF_AUTO_BACKUP, false);
        return "自动备份：" + (enabled ? "开" : "关");
    }

    private void toggleAutoSave() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean current = prefs.getBoolean(PREF_AUTO_SAVE, false);
        prefs.edit().putBoolean(PREF_AUTO_SAVE, !current).apply();
        Toast.makeText(this, "自动保存已" + (!current ? "开启" : "关闭") + "（每30秒）", Toast.LENGTH_SHORT).show();
    }

    private void toggleAutoBackup() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean current = prefs.getBoolean(PREF_AUTO_BACKUP, false);
        prefs.edit().putBoolean(PREF_AUTO_BACKUP, !current).apply();
        Toast.makeText(this, "自动备份已" + (!current ? "开启" : "关闭") + "（每30秒）", Toast.LENGTH_SHORT).show();
    }

    private void showGotoLineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("行号跳转");

        final EditText input = new EditText(this);
        input.setHint("请输入跳转行号");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);

        builder.setView(input);

        builder.setPositiveButton("跳转", (dialog, which) -> {
            String lineNumber = input.getText().toString().trim();
            if (!lineNumber.isEmpty()) {
                try {
                    int line = Integer.parseInt(lineNumber);
                    if (line > 0 && line <= codeEditor.getLineCount()) {
                        codeEditor.jumpToLine(line);
                    } else {
                        Toast.makeText(CodeEditorCpp.this, "请输入有效的行号", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(CodeEditorCpp.this, "请输入数字", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    private void saveAs() {
        Toast.makeText(this, "另存为功能待实现", Toast.LENGTH_SHORT).show();
    }

    private void showThemeSettings() {
        String[] themeNames = {"Solarized Light", "Quiet light", "Monokai"};
        String[] themeAssets = {"solarized-light", "quietlight", "Monokai"};

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentTheme = prefs.getString(PREF_THEME, "solarized-light");
        int currentIndex = 0;
        for (int i = 0; i < themeAssets.length; i++) {
            if (themeAssets[i].equals(currentTheme)) { currentIndex = i; break; }
        }

        new AlertDialog.Builder(this)
                .setTitle("选择主题")
                .setSingleChoiceItems(themeNames, currentIndex, null)
                .setPositiveButton("应用", (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String themeName = themeAssets[selected];
                    applyTheme(themeName);
                    prefs.edit().putString(PREF_THEME, themeName).apply();
                    Toast.makeText(this, "主题已切换为：" + themeNames[selected], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void applyTheme(String themeName) {
        try {
            var themeRegistry = ThemeRegistry.getInstance();
            var themeAssetsPath = "textmate/" + themeName + ".json";
            var model = new ThemeModel(
                    IThemeSource.fromInputStream(
                            FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath),
                            themeAssetsPath, null
                    ),
                    themeName
            );
            themeRegistry.loadTheme(model);
            ThemeRegistry.getInstance().setTheme(themeName);
            codeEditor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
        } catch (Exception e) {
            Toast.makeText(this, "主题切换失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ThemeSwitch", "切换主题失败", e);
        }
    }

    private void showFontSizeDialog() {
        String[] sizes = {"12", "14", "16", "18", "20", "22", "24"};
        new AlertDialog.Builder(this)
                .setTitle("选择字体大小")
                .setItems(sizes, (dialog, which) -> {
                    float size = Float.parseFloat(sizes[which]);
                    codeEditor.setTextSize(size);
                    saveEditorSettings();
                    Toast.makeText(this, "字体大小已保存", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void toggleLineNumbers() {
        codeEditor.setLineNumberEnabled(!codeEditor.isLineNumberEnabled());
        saveEditorSettings();
        Toast.makeText(this,
                codeEditor.isLineNumberEnabled() ? "显示行号(已保存)" : "隐藏行号(已保存)",
                Toast.LENGTH_SHORT).show();
    }

    private void toggleWordWrap() {
        codeEditor.setWordwrap(!codeEditor.isWordwrap());
        saveEditorSettings();
        Toast.makeText(this,
                codeEditor.isWordwrap() ? "启用显示换行(已保存)" : "禁用显示换行(已保存)",
                Toast.LENGTH_SHORT).show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("关于")
                .setMessage("C++代码编辑器")
                .setPositiveButton("确定", null)
                .show();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("退出应用");
        builder.setMessage("退出后未保存的内容改动将无效，您确定要退出吗?");

        builder.setPositiveButton("保存并退出", (dialog, which) ->
                saveDataAndExit(codeEditor.getText().toString(), dirPath, "/code.cpp"));
        builder.setNegativeButton("直接退出", (dialog, which) -> finish());
        builder.setNeutralButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void saveDataAndExit(String textToWrite, String folderName, String fileName) {
        saveData(textToWrite, folderName, fileName);
        finish();
    }

    private void saveData(String textToWrite, String folderName, String fileName) {
        writeToFile(textToWrite, folderName, fileName);
        updateProjectLastModified(folderName);
    }

    private void updateProjectLastModified(String projectPath) {
        try {
            File infoFile = new File(projectPath, "project_info.json");
            if (infoFile.exists()) {
                String content = readFileFromExternalStorage(infoFile.getAbsolutePath());
                if (content != null) {
                    JSONObject json = new JSONObject(content);
                    json.put("lastModified", System.currentTimeMillis());
                    FileOutputStream fos = new FileOutputStream(infoFile);
                    fos.write(json.toString().getBytes());
                    fos.close();
                }
            }
        } catch (Exception e) {
            Log.e("CodeEditorCpp", "更新lastModified失败", e);
        }
    }

    public String readFileFromExternalStorage(String filePath) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return "";
            }
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            if ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append("\n");
                    stringBuilder.append(line);
                }
            }
            bufferedReader.close();
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return stringBuilder.toString();
    }

    private void writeToFile(String textToWrite, String folderName, String fileName) {
        try {
            File file = new File(folderName, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(textToWrite.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void performBackup(boolean showToast) {
        try {
            String timestamp = new java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            String backupFileName = "code_backup_" + timestamp + ".cpp";
            File backupFile = new File(dirPath, backupFileName);
            String content = codeEditor.getText().toString();
            FileOutputStream fos = new FileOutputStream(backupFile);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            if (showToast) {
                Toast.makeText(this, "备份成功：" + backupFileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            if (showToast) {
                Toast.makeText(this, "备份失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            Log.e("Backup", "备份失败", e);
        }
    }

    private void importTemplate() {
        showTemplateDialog();
    }

    // ==================== 模板相关 ====================

    private void initTemplates() {
        templates = new ArrayList<>();

        // 确保内置模板已复制
        CppCompiler.ensureBuiltinTemplates("cpp", this);

        File templateDir = new File(Environment.getExternalStorageDirectory(), "/GGtool/templates/cpp/");

        if (!templateDir.exists() || !templateDir.isDirectory()) {
            return;
        }

        File[] files = templateDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                try {
                    Template template = loadTemplateFromFile(file);
                    if (template != null) {
                        templates.add(template);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Template loadTemplateFromFile(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            String jsonContent = new String(buffer, "UTF-8");

            JSONObject jsonObject = new JSONObject(jsonContent);
            String version = jsonObject.getString("version");
            String code = jsonObject.getString("code");

            String templateName = file.getName().replace(".json", "");
            return new Template(templateName, version, code);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showTemplateDialog() {
        if (templates == null || templates.isEmpty()) {
            Toast.makeText(this, "没有可用模板", Toast.LENGTH_SHORT).show();
            return;
        }
        TemplateImportDialog dialog = new TemplateImportDialog(this, templates);
        dialog.setOnTemplateSelectedListener((template, position) -> insertTemplate(template, position));
        dialog.show();
    }

    private void insertTemplate(Template template, TemplateImportDialog.InsertPosition position) {
        String currentText = codeEditor.getText().toString();
        String templateContent = template.getContent();

        if (position == TemplateImportDialog.InsertPosition.CURSOR) {
            try {
                codeEditor.insertText(templateContent, 1);
                codeEditor.getCursor().set(
                        codeEditor.getCursor().getLeftLine(),
                        codeEditor.getCursor().getLeftColumn() - 1
                );
            } catch (Exception e) {
                Toast.makeText(this, "插入失败，请确保插件正常", Toast.LENGTH_SHORT).show();
                throw new RuntimeException(e);
            }
        } else {
            String newText = templateContent + "\n" + currentText;
            codeEditor.setText(newText);
        }
    }

    // ==================== 编译相关 ====================

    private void startCompile() {
        // 检查 C4droid 是否已安装
        if (!CppCompiler.isC4droidInstalled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("C4droid 未安装")
                    .setMessage("编译 C++ 代码需要安装 C4droid 应用。\n\n请先安装 C4droid 后再使用此功能。")
                    .setPositiveButton("确定", null)
                    .show();
            return;
        }

        // 保存当前代码
        String code = codeEditor.getText().toString();
        if (code.trim().isEmpty()) {
            Toast.makeText(this, "代码为空，无法编译", Toast.LENGTH_SHORT).show();
            return;
        }
        saveData(code, dirPath, "/code.cpp");

        // 输出路径
        String outputPath = dirPath + "/output.so";

        // 清空日志并打开日志抽屉
        logBuilder.setLength(0);
        logTextView.setText("");
        appendLog("========== 开始编译 ==========");
        appendLog("源文件: " + path);
        appendLog("输出路径: " + outputPath);
        appendLog("MemoryTools.h: " + CppCompiler.getMemoryToolsPath());
        appendLog("-------------------------------");
        openLogDrawer();

        // 禁用编译按钮
        btnCompile.setEnabled(false);
        Toast.makeText(this, "正在编译...", Toast.LENGTH_SHORT).show();

        cppCompiler.compile(path, outputPath, new CppCompiler.CompileCallback() {
            @Override
            public void onSuccess(String outputPath) {
                btnCompile.setEnabled(true);
                appendLog("-------------------------------");
                appendLog("[成功] 编译成功！");
                appendLog("输出文件: " + outputPath);

                // 询问是否导出
                new AlertDialog.Builder(CodeEditorCpp.this)
                        .setTitle("编译成功")
                        .setMessage("输出文件:\n" + outputPath + "\n\n是否查看文件？")
                        .setPositiveButton("查看", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(
                                        androidx.core.content.FileProvider.getUriForFile(
                                                CodeEditorCpp.this,
                                                getPackageName() + ".fileprovider",
                                                new File(outputPath)),
                                        "application/octet-stream");
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(Intent.createChooser(intent, "打开文件"));
                            } catch (Exception e) {
                                Toast.makeText(CodeEditorCpp.this,
                                        "输出文件: " + outputPath, Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("关闭", null)
                        .show();
            }

            @Override
            public void onFailure(String errorMessage) {
                btnCompile.setEnabled(true);
                appendLog("-------------------------------");
                appendLog("[失败] " + errorMessage);
            }
        });
    }

    // ==================== 日志相关 ====================

    private void appendLog(String message) {
        runOnUiThread(() -> {
            String time = new java.text.SimpleDateFormat(
                    "HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
            logBuilder.append("[").append(time).append("] ").append(message).append("\n");
            logTextView.setText(logBuilder.toString());
            logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void openLogDrawer() {
        drawerLayout.openDrawer(GravityCompat.END);
    }

    private void copyLogToClipboard() {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip =
                android.content.ClipData.newPlainText("Cpp Compile Log", logBuilder.toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoSaveHandler != null && autoSaveRunnable != null) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
        }
    }

    // ==================== 内部类 ====================

    public class Template {
        private String name;
        private String version;
        private String content;

        public Template(String name, String version, String content) {
            this.name = name;
            this.version = version;
            this.content = content;
        }

        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getContent() { return content; }

        @Override
        public String toString() {
            return name + " (v" + version + ")";
        }
    }

    public class TemplateImportDialog {

        public interface OnTemplateSelectedListener {
            void onTemplateSelected(Template template, InsertPosition position);
        }

        public enum InsertPosition {
            CURSOR("光标处插入"),
            HEAD("头部插入");

            private String displayName;

            InsertPosition(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() {
                return displayName;
            }
        }

        private Context context;
        private List<Template> templates;
        private OnTemplateSelectedListener listener;

        public TemplateImportDialog(Context context, List<Template> templates) {
            this.context = context;
            this.templates = templates;
        }

        public void setOnTemplateSelectedListener(OnTemplateSelectedListener listener) {
            this.listener = listener;
        }

        public void show() {
            String[] templateNames = new String[templates.size()];
            for (int i = 0; i < templates.size(); i++) {
                templateNames[i] = templates.get(i).toString();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("选择模板");
            builder.setItems(templateNames, (dialog, which) -> {
                Template selectedTemplate = templates.get(which);
                showPositionDialog(selectedTemplate);
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        }

        private void showPositionDialog(final Template template) {
            String[] positions = {
                    InsertPosition.CURSOR.getDisplayName(),
                    InsertPosition.HEAD.getDisplayName()
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("选择插入位置");
            builder.setItems(positions, (dialog, which) -> {
                InsertPosition position = (which == 0) ?
                        InsertPosition.CURSOR : InsertPosition.HEAD;
                if (listener != null) {
                    listener.onTemplateSelected(template, position);
                }
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        }
    }
}

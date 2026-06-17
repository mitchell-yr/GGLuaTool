package mituran.gglua.tool;

import static mituran.gglua.tool.luaTool.LuaFormatterWrapper.formatLuaFile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
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
import mituran.gglua.tool.luaTool.LuaFormatterWrapper;
import mituran.gglua.tool.luaTool.LuaSyntaxChecker;


public class CodeEditorLua extends AppCompatActivity {

    private static final String PREFS_NAME = "EditorSettings";
    private static final String PREF_FONT_SIZE = "fontSize";
    private static final String PREF_LINE_NUMBERS = "lineNumbers";
    private static final String PREF_WORD_WRAP = "wordWrap";
    private static final String PREF_THEME = "theme";
    private static final String PREF_AUTO_SAVE = "autoSave";
    private static final String PREF_AUTO_BACKUP = "autoBackup";
    private static final String PREF_RUNNER = "runner";
    private static final String PREF_NATIVE_GG_HINT_DISMISSED = "native_gg_hint_dismissed";

    private static final long AUTO_SAVE_INTERVAL_MS = 30_000; // 30秒自动保存
    private android.os.Handler autoSaveHandler;
    private Runnable autoSaveRunnable;

    private ImageButton btnUndo, btnRedo, btnEdit, btnManage,btnCopy,btnPaste,btnFind,btnSave,btnTutorial;
    private String path,dirPath;
    private CodeEditor codeEditor;
    private LinearLayout symbolContainer;

    private LuaSyntaxChecker luaChecker;
    private LuaFunctionSearchPopup searchPopup;
    private List<Template> templates;

    private DrawerLayout drawerLayout;
    private TextView logTextView;
    private ScrollView logScrollView;
    private ImageButton btnLog;
    private BuildOutputLogManager logManager;
    private LuaCompiler luaCompiler;
    private LuaEngine luaEngine;

    private String[] symbols = {
            ",", ".","-","(",  "[", "{", "=", "<", ">",
            "+",  "*", "/", "%", "^", "..", ":",")", "]", "}"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codeeditor_lua);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);
        btnEdit = findViewById(R.id.btn_edit);
        btnManage = findViewById(R.id.btn_manage);
        btnCopy = findViewById(R.id.btn_copy);
        btnPaste = findViewById(R.id.btn_paste);
        btnFind = findViewById(R.id.btn_find);
        btnSave = findViewById(R.id.btn_save);
        btnTutorial = findViewById(R.id.btn_tutorial);

        symbolContainer = findViewById(R.id.symbol_container);

        Bundle bundle = getIntent().getExtras();
        path = bundle.getString("path")+"/code.lua";
        dirPath = bundle.getString("path");
        String codeTextTemp=readFileFromExternalStorage(path);

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
                            FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath), themeAssetsPath, null
                    ),
                    name
            );
            themeRegistry.loadTheme(model);
            ThemeRegistry.getInstance().setTheme(name);

            GrammarRegistry.getInstance().loadGrammars("textmate/language.json");

            codeEditor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));

            var languageScopeName = "source.lua";
            var language = TextMateLanguage.create(
                    languageScopeName, true
            );
            codeEditor.setEditorLanguage(language);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        codeEditor.setPinLineNumber(true);

        // 加载并应用保存的设置
        loadEditorSettings();

        initSymbolBar();
        initTemplates();
        luaChecker = new LuaSyntaxChecker();
        searchPopup = new LuaFunctionSearchPopup(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        logTextView = findViewById(R.id.log_text_view);
        logScrollView = findViewById(R.id.log_scroll_view);
        btnLog = findViewById(R.id.btn_log);
        initLogManager();
        initLuaCompiler();
        initLuaEngine();

        btnLog.setOnClickListener(v -> openLogDrawer());

        findViewById(R.id.btn_clear_log).setOnClickListener(v -> {
            logManager.clear();
            Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_copy_log).setOnClickListener(v -> copyLogToClipboard());

        // 编译脚本按钮
        findViewById(R.id.btn_compile_script).setOnClickListener(v -> buildOutput());

        findViewById(R.id.btn_run_script).setOnClickListener(v -> runScript());

        btnTutorial.setOnClickListener(v -> {
            searchPopup.show(codeEditor);
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData(codeEditor.getText().toString(),dirPath,"/code.lua");
                Toast.makeText(CodeEditorLua.this,"保存成功",Toast.LENGTH_SHORT).show();
            }
        });

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                codeEditor.undo();
            }
        });

        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                codeEditor.beginSearchMode();
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                codeEditor.redo();
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditMenu(v);
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                codeEditor.copyText();
            }
        });

        btnPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                codeEditor.pasteText();
            }
        });

        btnManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showManageMenu(v);
            }
        });

        // 初始化自动保存
        autoSaveHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean autoSave = prefs.getBoolean(PREF_AUTO_SAVE, false);
                boolean autoBackup = prefs.getBoolean(PREF_AUTO_BACKUP, false);
                if (autoSave) {
                    saveData(codeEditor.getText().toString(), dirPath, "/code.lua");
                }
                if (autoBackup) {
                    performBackup(false);
                }
                autoSaveHandler.postDelayed(this, AUTO_SAVE_INTERVAL_MS);
            }
        };
        autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_INTERVAL_MS);
    }

    // 加载编辑器设置
    private void loadEditorSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 加载字体大小 (默认16)
        float fontSize = prefs.getFloat(PREF_FONT_SIZE, 16f);
        codeEditor.setTextSize(fontSize);

        // 加载行号显示 (默认true)
        boolean showLineNumbers = prefs.getBoolean(PREF_LINE_NUMBERS, true);
        codeEditor.setLineNumberEnabled(showLineNumbers);

        // 加载自动换行 (默认false)
        boolean wordWrap = prefs.getBoolean(PREF_WORD_WRAP, false);
        codeEditor.setWordwrap(wordWrap);

        // 主题设置：仅当保存的主题与默认不同时才重新应用
        String theme = prefs.getString(PREF_THEME, "solarized-light");
        if (!theme.equals("solarized-light")) {
            applyTheme(theme);
        }
    }

    // 保存编辑器设置
    private void saveEditorSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putFloat(PREF_FONT_SIZE, codeEditor.getTextSizePx() / getResources().getDisplayMetrics().scaledDensity);
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

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    insertSymbol(symbol);
                }
            });

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
        popupMenu.getMenu().add(0, 7, 0, "跳转到行");
        popupMenu.getMenu().add(0, 8, 0, "格式化代码");
        popupMenu.getMenu().add(0, 9, 0, "语法检查");

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 1:
                        codeEditor.selectAll();
                        break;
                    case 7:
                        showGotoLineDialog();
                        break;
                    case 8:
                        formatCode();
                        break;
                    case 9:
                        checkLuaSyntax();
                        break;
                }
                return true;
            }
        });

        popupMenu.show();
    }

    private void showManageMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);

        popupMenu.getMenu().add(0, 2, 0, "另存为");
        popupMenu.getMenu().add(0, 3, 0, "导入模板");
        popupMenu.getMenu().add(0, 11, 0, "手动备份");
        popupMenu.getMenu().add(0, 14, 0, "运行设置");

        SubMenu editorSubMenu = popupMenu.getMenu().addSubMenu(0, 5, 0, "编辑器设置");
        editorSubMenu.add(0, 6, 0, "主题设置");
        editorSubMenu.add(0, 7, 0, "字体大小");
        editorSubMenu.add(0, 8, 0, "显示行号");
        editorSubMenu.add(0, 9, 0, "换行显示");
        editorSubMenu.add(0, 12, 0, getAutoSaveMenuTitle());
        editorSubMenu.add(0, 13, 0, getAutoBackupMenuTitle());

        popupMenu.getMenu().add(0, 10, 0, "关于");

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
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
                    case 14:
                        showRunnerSettings();
                        break;
                }
                return true;
            }
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

    private void showRunnerSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentRunner = prefs.getString(PREF_RUNNER, "builtin");

        String[] runnerNames = {"内置lua虚拟机", "原生GG接口"};
        String[] runnerValues = {"builtin", "native_gg"};
        int currentIndex = currentRunner.equals("native_gg") ? 1 : 0;

        new AlertDialog.Builder(this)
                .setTitle("运行器")
                .setSingleChoiceItems(runnerNames, currentIndex, null)
                .setPositiveButton("确定", (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selected >= 0) {
                        String selectedRunner = runnerValues[selected];
                        if ("native_gg".equals(selectedRunner)) {
                            boolean hintDismissed = prefs.getBoolean(PREF_NATIVE_GG_HINT_DISMISSED, false);
                            if (hintDismissed) {
                                prefs.edit().putString(PREF_RUNNER, selectedRunner).apply();
                                Toast.makeText(this, "已切换为原生GG接口", Toast.LENGTH_SHORT).show();
                            } else {
                                showNativeGgHintDialog(selectedRunner);
                            }
                        } else {
                            prefs.edit().putString(PREF_RUNNER, selectedRunner).apply();
                            Toast.makeText(this, "已切换为内置lua虚拟机", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showNativeGgHintDialog(String targetRunner) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        TextView tvHint = new TextView(this);
        tvHint.setText("该功能需要您已安装带本地http接口特定版本GG修改器，请确保您已安装并打开GG");
        tvHint.setTextColor(Color.BLACK);
        tvHint.setTextSize(15);
        layout.addView(tvHint);

        CheckBox cbDontShow = new CheckBox(this);
        cbDontShow.setText("不再显示");
        cbDontShow.setTextColor(Color.GRAY);
        cbDontShow.setTextSize(14);
        LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cbParams.topMargin = 16;
        cbDontShow.setLayoutParams(cbParams);
        layout.addView(cbDontShow);

        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setView(layout)
                .setPositiveButton("前往安装", (dialog, which) -> {
                    if (cbDontShow.isChecked()) {
                        prefs.edit().putBoolean(PREF_NATIVE_GG_HINT_DISMISSED, true).apply();
                    }
                    prefs.edit().putString(PREF_RUNNER, targetRunner).apply();
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/mitchell-yr/GameGuardian-Api/releases/"));
                    startActivity(intent);
                })
                .setNeutralButton("已安装", (dialog, which) -> {
                    if (cbDontShow.isChecked()) {
                        prefs.edit().putBoolean(PREF_NATIVE_GG_HINT_DISMISSED, true).apply();
                    }
                    prefs.edit().putString(PREF_RUNNER, targetRunner).apply();
                    Toast.makeText(this, "已切换为原生GG接口", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    // 取消时不保存，保持原来的运行器选择
                })
                .show();
    }

    /**
     * 执行备份：在 dirPath 下生成 code_backup_时间戳.lua
     * @param showToast 是否弹出结果提示（手动备份时为true，自动备份时为false）
     */
    private void performBackup(boolean showToast) {
        try {
            String timestamp = new java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            String backupFileName = "code_backup_" + timestamp + ".lua";
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

    private void importTemplate(){
        showTemplateDialog();
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

        builder.setPositiveButton("跳转", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String lineNumber = input.getText().toString().trim();
                if (!lineNumber.isEmpty()) {
                    try {
                        int line = Integer.parseInt(lineNumber);
                        if (line > 0 && line <= codeEditor.getLineCount()) {
                            codeEditor.jumpToLine(line);
                        } else {
                            Toast.makeText(CodeEditorLua.this, "请输入有效的行号", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(CodeEditorLua.this, "请输入数字", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void formatCode() {
        saveData(codeEditor.getText().toString(), dirPath, "/code.lua");

        LuaFormatterWrapper.formatLuaFileAsync(path, "UTF-8", success -> {
            runOnUiThread(() -> {
                if (success) {
                    String tempCode = readFileFromExternalStorage(path);
                    codeEditor.setText(tempCode);
                    Toast.makeText(this, "格式化成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "格式化失败，请检查日志", Toast.LENGTH_SHORT).show();
                    Log.e("FormatCode", "格式化失败，路径: " + path);
                }
            });
        });
    }

    private void saveAs() {
        Toast.makeText(this, "另存为功能待实现", Toast.LENGTH_SHORT).show();
    }

    private void buildOutput() {
        String[] options = {"仅编译检查", "导出字节码(.luac)", "导出完整编译包"};

        new AlertDialog.Builder(this)
                .setTitle("选择编译输出方式")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            compileOnly();
                            break;
                        case 1:
                            exportBytecode();
                            break;
                        case 2:
                            exportFullPackage();
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void compileOnly() {
        String code = codeEditor.getText().toString();
        saveData(code, dirPath, "/code.lua");

        boolean success = luaCompiler.compile(code);
        openLogDrawer();

        if (success) {
            Toast.makeText(this, "编译成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "编译失败，请查看日志", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportBytecode() {
        String code = codeEditor.getText().toString();
        saveData(code, dirPath, "/code.lua");

        EncryptionOptionsDialog dialog = new EncryptionOptionsDialog(this, false);
        dialog.setOnExportListener((filename, encrypt, encryptionOptions) -> {
            new Thread(() -> {
                boolean compileSuccess;

                if (encrypt) {
                    compileSuccess = luaCompiler.compileWithEncryption(code, true, encryptionOptions);
                } else {
                    compileSuccess = luaCompiler.compile(code);
                }

                if (compileSuccess) {
                    String outputPath = dirPath + "/" + filename;
                    boolean success = luaCompiler.saveBytecode(outputPath);

                    runOnUiThread(() -> {
                        openLogDrawer();
                        if (success) {
                            showExportSuccessDialog(outputPath);
                        } else {
                            Toast.makeText(this, "导出失败，请查看日志", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        openLogDrawer();
                        Toast.makeText(this, "编译失败，请查看日志", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
        dialog.show();
    }

    private void exportFullPackage() {
        String code = codeEditor.getText().toString();
        saveData(code, dirPath, "/code.lua");

        EncryptionOptionsDialog dialog = new EncryptionOptionsDialog(this, true);
        dialog.setOnExportListener((filename, encrypt, encryptionOptions) -> {
            if (!filename.endsWith(".zip")) {
                filename += ".zip";
            }

            String outputPath = dirPath + "/" + filename;

            new Thread(() -> {
                boolean compileSuccess;

                if (encrypt) {
                    compileSuccess = luaCompiler.compileWithEncryption(code, true, encryptionOptions);
                } else {
                    compileSuccess = luaCompiler.compile(code);
                }

                if (compileSuccess) {
                    boolean success = luaCompiler.exportAsZip(path, outputPath, encrypt, encryptionOptions);

                    runOnUiThread(() -> {
                        openLogDrawer();
                        if (success) {
                            showExportSuccessDialog(outputPath);
                        } else {
                            Toast.makeText(this, "导出失败，请查看日志", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        openLogDrawer();
                        Toast.makeText(this, "编译失败，请查看日志", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
        dialog.show();
    }

    private void showExportSuccessDialog(String outputPath) {
        new AlertDialog.Builder(this)
                .setTitle("导出成功")
                .setMessage("文件已导出到:\n" + outputPath + "\n\n是否打开文件管理器查看？")
                .setPositiveButton("打开", (dialog, which) -> {
                    openFileManager(outputPath);
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void openFileManager(String path) {
        try {
            File file = new File(path);
            File parentDir = file.isDirectory() ? file : file.getParentFile();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("file://" + parentDir.getAbsolutePath());
            intent.setDataAndType(uri, "resource/folder");

            if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "请手动前往: " + parentDir.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件管理器", Toast.LENGTH_SHORT).show();
        }
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
                    saveEditorSettings(); // 保存设置
                    Toast.makeText(this, "字体大小已保存", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void toggleLineNumbers() {
        codeEditor.setLineNumberEnabled(!codeEditor.isLineNumberEnabled());
        saveEditorSettings(); // 保存设置
        Toast.makeText(this,
                codeEditor.isLineNumberEnabled() ? "显示行号(已保存)" : "隐藏行号(已保存)",
                Toast.LENGTH_SHORT).show();
    }

    private void toggleWordWrap() {
        codeEditor.setWordwrap(!codeEditor.isWordwrap());
        saveEditorSettings(); // 保存设置
        Toast.makeText(this,
                codeEditor.isWordwrap() ? "启用显示换行(已保存)" : "禁用显示换行(已保存)",
                Toast.LENGTH_SHORT).show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("关于")
                .setMessage("Lua代码编辑器")
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("退出应用");
        builder.setMessage("退出后未保存的内容改动将无效，您确定要退出吗?");

        builder.setPositiveButton("保存并退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveDataAndExit(codeEditor.getText().toString(),dirPath,"/code.lua");
            }
        });
        builder.setNegativeButton("直接退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNeutralButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void checkLuaSyntax() {
        String luaCode = codeEditor.getText().toString();

        showLoadingDialog("正在检查语法...");

        new Thread(() -> {
            LuaSyntaxChecker.CheckResult result = luaChecker.checkSyntax(luaCode);

            runOnUiThread(() -> {
                dismissLoadingDialog();
                showResultDialog(result);
            });
        }).start();
    }

    private void showResultDialog(LuaSyntaxChecker.CheckResult result) {
        LuaCheckResultDialog dialog = new LuaCheckResultDialog(this);

        dialog.setOnJumpToErrorListener(line -> {
            jumpToErrorLine(line);
        });

        dialog.showResult(result);
    }

    private void jumpToErrorLine(int lineNumber) {
        String text = codeEditor.getText().toString();
        String[] lines = text.split("\n", -1);

        if (lineNumber > 0 && lineNumber <= lines.length) {
            int start = 0;
            for (int i = 0; i < lineNumber - 1; i++) {
                start += lines[i].length() + 1;
            }
            int end = start + lines[lineNumber - 1].length();

            codeEditor.post(() -> {
                int lineHeight = codeEditor.getHeight();
                int scrollY = (lineNumber - 1) ;
                codeEditor.scrollTo(0, scrollY);
            });
        }
    }

    private android.app.ProgressDialog loadingDialog;

    private void showLoadingDialog(String msg) {
        loadingDialog = new android.app.ProgressDialog(this);
        loadingDialog.setMessage(msg);
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void initTemplates() {
        templates = new ArrayList<>();

        File templateDir = new File(Environment.getExternalStorageDirectory(),"/GGtool/templates/");

        if (!templateDir.exists() || !templateDir.isDirectory()) {
            Toast.makeText(this, "模板目录不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] files = templateDir.listFiles();
        if (files == null || files.length == 0) {
            Toast.makeText(this, "模板目录为空", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "读取模板失败: " + file.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (templates.isEmpty()) {
            Toast.makeText(this, "未找到有效的模板文件", Toast.LENGTH_SHORT).show();
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
        TemplateImportDialog dialog = new TemplateImportDialog(this, templates);
        dialog.setOnTemplateSelectedListener(new TemplateImportDialog.OnTemplateSelectedListener() {
            @Override
            public void onTemplateSelected(Template template, TemplateImportDialog.InsertPosition position) {
                insertTemplate(template, position);
            }
        });
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
                Toast.makeText(this,"插入失败，请确保插件正常",Toast.LENGTH_SHORT).show();
                throw new RuntimeException(e);
            }
        } else {
            String newText = templateContent + "\n" + currentText;
            codeEditor.setText(newText);
        }
    }

    private void saveDataAndExit(String textToWrite,String folderName,String fileName) {
        saveData(textToWrite, folderName, fileName);
        finish();
    }

    private void saveData(String textToWrite,String folderName,String fileName) {
        writeToFile( textToWrite, folderName, fileName);
        updateProjectLastModified(folderName);
    }

    /**
     * 更新 project_info.json 中的 lastModified 字段
     */
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
            Log.e("CodeEditorLua", "更新lastModified失败", e);
        }
    }

    public String readFileFromExternalStorage(String filePath) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        try {
            File file = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            if((line = bufferedReader.readLine()) != null){
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

    private void writeToFile(String textToWrite,String folderName,String fileName) {
        try {
            File file = new File(folderName, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(textToWrite.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class Template {
        private String name;
        private String version;
        private String content;

        public Template(String name, String version, String content) {
            this.name = name;
            this.version = version;
            this.content = content;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getContent() {
            return content;
        }

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
            builder.setItems(templateNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Template selectedTemplate = templates.get(which);
                    showPositionDialog(selectedTemplate);
                }
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
            builder.setItems(positions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    InsertPosition position = (which == 0) ?
                            InsertPosition.CURSOR : InsertPosition.HEAD;

                    if (listener != null) {
                        listener.onTemplateSelected(template, position);
                    }
                }
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void initLogManager() {
        logManager = new BuildOutputLogManager();
        logManager.setCallback(log -> {
            runOnUiThread(() -> {
                logTextView.setText(log);
                logScrollView.post(() ->
                        logScrollView.fullScroll(ScrollView.FOCUS_DOWN)
                );
            });
        });
    }

    private void initLuaCompiler() {
        luaCompiler = new LuaCompiler(logManager);
    }

    private void initLuaEngine() {
        // LuaEngine 会直接输出到 logTextView
        // 由于我们的 logManager 也使用同一个 logTextView，
        // 所以输出会自动同步
        luaEngine = new LuaEngine(this, logTextView);
        luaEngine.initialize();
    }

    private void openLogDrawer() {
        drawerLayout.openDrawer(GravityCompat.END);
    }

    private void copyLogToClipboard() {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip =
                android.content.ClipData.newPlainText("Lua Compile Log", logManager.getLogText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    public void addLog(String message) {
        if (logManager != null) {
            logManager.logInfo(message);
        }
    }

    public void addLog(String message, BuildOutputLogManager.LogLevel level) {
        if (logManager != null) {
            logManager.log(message, level);
        }
    }

    /**
     * 运行当前编辑器中的 Lua 脚本
     */
    private void runScript() {
        // 获取当前编辑器中的代码
        String luaCode = codeEditor.getText().toString();

        if (luaCode == null || luaCode.trim().isEmpty()) {
            Toast.makeText(this, "脚本内容为空", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String runner = prefs.getString(PREF_RUNNER, "builtin");

        if ("native_gg".equals(runner)) {
            executeViaNativeGg(luaCode);
        } else {
            executeViaBuiltinLua(luaCode);
        }
    }

    /**
     * 使用内置 Lua 虚拟机执行脚本
     */
    private void executeViaBuiltinLua(String luaCode) {
        // 打开日志抽屉
        openLogDrawer();

        // 清空之前的日志
        if (luaEngine != null) {
            luaEngine.clearLog();
        }

        // 记录开始执行的日志
        runOnUiThread(() -> {
            logTextView.append("[信息] ========== 开始执行脚本（内置Lua虚拟机） ==========\n");
            logTextView.append("[信息] 时间: " + getCurrentTime() + "\n");
            logTextView.append("[信息] ------------------------------\n");
            scrollLogToBottom();
        });

        // 在新线程中执行脚本
        new Thread(() -> {
            try {
                luaEngine.executeString(luaCode);

                runOnUiThread(() -> {
                    logTextView.append("\n[成功] ========== 执行完成 ==========\n");
                    scrollLogToBottom();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    logTextView.append("\n[错误] 执行错误: " + e.getMessage() + "\n");
                    logTextView.append("[错误] ========== 执行失败 ==========\n");
                    scrollLogToBottom();
                });
            }
        }).start();
    }

    /**
     * 通过原生GG HTTP接口执行脚本
     */
    private void executeViaNativeGg(String luaCode) {
        openLogDrawer();

        runOnUiThread(() -> {
            logTextView.setText("");
            logTextView.append("[信息] ========== 开始执行脚本（原生GG接口） ==========\n");
            logTextView.append("[信息] 时间: " + getCurrentTime() + "\n");
            logTextView.append("[信息] 目标: http://127.0.0.1:8080/api/gg/runScript\n");
            logTextView.append("[信息] 脚本长度: " + luaCode.length() + " 字符\n");
            logTextView.append("[信息] ------------------------------\n");
            scrollLogToBottom();
        });

        new Thread(() -> {
            Socket socket = null;
            try {
                // 用原始Socket发HTTP/1.0请求，避免HttpURLConnection在无Content-Length响应时阻塞
                socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress("127.0.0.1", 8080), 3000);
                socket.setSoTimeout(15000);

                byte[] body = luaCode.getBytes("UTF-8");

                // GG服务器把Content-Length当字符数用（不是字节数），所以用luaCode.length()
                StringBuilder request = new StringBuilder();
                request.append("POST /api/gg/runScript HTTP/1.0\r\n");
                request.append("Host: 127.0.0.1:8080\r\n");
                request.append("Content-Type: text/plain; charset=utf-8\r\n");
                request.append("Content-Length: ").append(luaCode.length()).append("\r\n");
                request.append("Connection: close\r\n");
                request.append("\r\n");

                OutputStream os = socket.getOutputStream();
                os.write(request.toString().getBytes("UTF-8"));
                os.write(body);
                os.flush();
                socket.shutdownOutput();

                // 读响应：逐行读到空行（headers结束），然后读body直到EOF
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), "UTF-8"));

                String statusLine = reader.readLine();
                if (statusLine == null) {
                    throw new IOException("服务器未返回状态行");
                }

                boolean isOk = statusLine.contains("200");
                runOnUiThread(() -> {
                    logTextView.append("[调试] 状态: " + statusLine + "\n");
                    scrollLogToBottom();
                });

                // 跳过响应头
                String headerLine;
                while ((headerLine = reader.readLine()) != null && headerLine.length() > 0) {
                    // skip headers
                }

                // 读响应body（读到EOF，因为HTTP/1.0 + Connection: close）
                StringBuilder responseBody = new StringBuilder();
                String bodyLine;
                while ((bodyLine = reader.readLine()) != null) {
                    responseBody.append(bodyLine);
                }
                reader.close();

                String responseStr = responseBody.toString();

                if (isOk) {
                    try {
                        JSONObject json = new JSONObject(responseStr);
                        boolean success = json.optBoolean("success", false);
                        String result = json.optString("result", "");

                        runOnUiThread(() -> {
                            if (success) {
                                logTextView.append("\n[成功] GG返回: " + result + "\n");
                            } else {
                                logTextView.append("\n[错误] GG返回失败: " + result + "\n");
                            }
                            logTextView.append("[信息] ========== 执行完成 ==========\n");
                            scrollLogToBottom();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            logTextView.append("\n[警告] JSON解析异常: " + e.getMessage() + "\n");
                            logTextView.append("[信息] 原始响应: " + responseStr + "\n");
                            logTextView.append("[信息] ========== 执行完成 ==========\n");
                            scrollLogToBottom();
                        });
                    }
                } else {
                    String finalResp = responseStr;
                    runOnUiThread(() -> {
                        logTextView.append("\n[错误] HTTP状态异常: " + statusLine + "\n");
                        if (!finalResp.isEmpty()) {
                            logTextView.append("[错误] 响应: " + finalResp + "\n");
                        }
                        logTextView.append("[错误] ========== 执行失败 ==========\n");
                        scrollLogToBottom();
                    });
                }

            } catch (java.net.ConnectException e) {
                runOnUiThread(() -> {
                    logTextView.append("\n[错误] 连接被拒绝: 127.0.0.1:8080\n");
                    logTextView.append("[错误] 请确保GG修改器已打开且HTTP服务已启动\n");
                    logTextView.append("[错误] ========== 执行失败 ==========\n");
                    scrollLogToBottom();
                });
            } catch (java.net.SocketTimeoutException e) {
                runOnUiThread(() -> {
                    logTextView.append("\n[错误] 读取超时 (15s)，GG服务器可能正在执行脚本但未响应\n");
                    logTextView.append("[错误] 请检查GG修改器状态\n");
                    logTextView.append("[错误] ========== 执行失败 ==========\n");
                    scrollLogToBottom();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    logTextView.append("\n[错误] 网络错误: " + e.getMessage() + "\n");
                    logTextView.append("[错误] ========== 执行失败 ==========\n");
                    scrollLogToBottom();
                });
            } finally {
                if (socket != null) {
                    try { socket.close(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    /**
     * 滚动日志到底部
     */
    private void scrollLogToBottom() {
        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoSaveHandler != null && autoSaveRunnable != null) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
        }
        if (luaEngine != null) {
            luaEngine.close();
        }
    }
}
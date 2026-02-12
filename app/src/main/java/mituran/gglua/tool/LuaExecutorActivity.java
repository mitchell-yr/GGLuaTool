package mituran.gglua.tool;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LuaExecutorActivity extends AppCompatActivity {

    private TextView tvLogOutput;
    private TextView tvScriptName;
    private ScrollView scrollLog;
    private MaterialButton btnSelectScript;
    private MaterialButton btnRunScript;
    private MaterialButton btnClearLog;

    private LuaEngine luaEngine;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Uri selectedScriptUri;
    private String selectedScriptName;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lua_executor);

        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        initLuaEngine();
        initFilePicker();
        setupListeners();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvLogOutput = findViewById(R.id.tv_log_output);
        tvScriptName = findViewById(R.id.tv_script_name);
        scrollLog = findViewById(R.id.scroll_log);
        btnSelectScript = findViewById(R.id.btn_select_script);
        btnRunScript = findViewById(R.id.btn_run_script);
        btnClearLog = findViewById(R.id.btn_clear_log);

        // 初始日志
        appendLog("Lua执行器已就绪", LogType.INFO);
    }

    private void initLuaEngine() {
        luaEngine = new LuaEngine(this, tvLogOutput);
        luaEngine.initialize();
    }

    private void initFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            handleSelectedFile(uri);
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        btnSelectScript.setOnClickListener(v -> openFilePicker());

        btnRunScript.setOnClickListener(v -> {
            if (selectedScriptUri != null) {
                executeScript();
            } else {
                showToast("请先选择脚本文件");
            }
        });

        btnClearLog.setOnClickListener(v -> clearLog());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // 添加MIME类型过滤
        String[] mimeTypes = {"text/x-lua", "application/x-lua", "text/plain", "*/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "选择Lua脚本"));
        } catch (Exception e) {
            showToast("没有找到文件管理器");
        }
    }

    private void handleSelectedFile(Uri uri) {
        try {
            String fileName = getFileName(uri);

            if (fileName != null && (fileName.toLowerCase().endsWith(".lua") ||
                    fileName.toLowerCase().endsWith(".luac"))) {

                selectedScriptUri = uri;
                selectedScriptName = fileName;

                runOnUiThread(() -> {
                    tvScriptName.setText(fileName);
                    btnRunScript.setEnabled(true);
                    appendLog("已选择脚本: " + fileName, LogType.SUCCESS);
                });

            } else {
                showToast("请选择 .lua 或 .luac 文件");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("选择文件失败: " + e.getMessage());
        }
    }

    private void executeScript() {
        if (selectedScriptUri == null) return;

        btnRunScript.setEnabled(false);
        btnSelectScript.setEnabled(false);

        appendLog("\n========== 开始执行 ==========", LogType.INFO);
        appendLog("脚本: " + selectedScriptName, LogType.INFO);
        appendLog("时间: " + getCurrentTime(), LogType.INFO);
        appendLog("------------------------------\n", LogType.INFO);

        new Thread(() -> {
            try {
                if (selectedScriptName.toLowerCase().endsWith(".luac")) {
                    byte[] bytecode = readBytesFromUri(selectedScriptUri);
                    luaEngine.executeBytecode(bytecode);
                } else {
                    String luaScript = readTextFromUri(selectedScriptUri);
                    luaEngine.executeString(luaScript);
                }

                runOnUiThread(() -> {
                    appendLog("\n========== 执行完成 ==========\n", LogType.SUCCESS);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    appendLog("\n执行错误: " + e.getMessage(), LogType.ERROR);
                    appendLog("========== 执行失败 ==========\n", LogType.ERROR);
                });
            } finally {
                runOnUiThread(() -> {
                    btnRunScript.setEnabled(true);
                    btnSelectScript.setEnabled(true);
                    scrollToBottom();
                });
            }
        }).start();
    }

    private void clearLog() {
        tvLogOutput.setText("");
        appendLog("日志已清空", LogType.INFO);
        if (luaEngine != null) {
            luaEngine.clearLog();
        }
    }

    private void appendLog(String message, LogType type) {
        runOnUiThread(() -> {
            String prefix = "";
            switch (type) {
                case ERROR:
                    prefix = "[错误] ";
                    break;
                case SUCCESS:
                    prefix = "[成功] ";
                    break;
                case INFO:
                    prefix = "[信息] ";
                    break;
                default:
                    break;
            }

            tvLogOutput.append(prefix + message + "\n");
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (luaEngine != null) {
            luaEngine.close();
        }
    }

    private enum LogType {
        INFO, ERROR, SUCCESS, NORMAL
    }
}
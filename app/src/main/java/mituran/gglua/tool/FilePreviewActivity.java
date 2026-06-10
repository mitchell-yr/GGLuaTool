package mituran.gglua.tool;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.eclipse.tm4e.core.registry.IThemeSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;
import io.github.rosemoe.sora.widget.CodeEditor;

public class FilePreviewActivity extends AppCompatActivity {

    private CodeEditor codeEditor;
    private String fileName;
    private boolean isLasmFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_preview);

        String filePath = getIntent().getStringExtra("file_path");
        if (filePath == null || filePath.isEmpty()) {
            Toast.makeText(this, "文件路径为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fileName = file.getName();
        isLasmFile = fileName.toLowerCase().endsWith(".lasm");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(fileName);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        codeEditor = findViewById(R.id.editor);

        // 初始化 sora-editor TextMate（模仿 CodeEditorLua）
        try {
            FileProviderRegistry.getInstance().addFileProvider(
                    new AssetsFileResolver(getApplicationContext().getAssets()));

            // 读取用户共享的主题设置
            SharedPreferences prefs = getSharedPreferences("EditorSettings", MODE_PRIVATE);
            String themeName = prefs.getString("theme", "solarized-light");

            var themeRegistry = ThemeRegistry.getInstance();
            var themeAssetsPath = "textmate/" + themeName + ".json";
            var model = new ThemeModel(
                    IThemeSource.fromInputStream(
                            FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath),
                            themeAssetsPath, null),
                    themeName);
            themeRegistry.loadTheme(model);
            ThemeRegistry.getInstance().setTheme(themeName);

            codeEditor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));

            // 加载语法高亮
            GrammarRegistry.getInstance().loadGrammars("textmate/language.json");

            // log文件 和 lasm文件 都使用 Lua 语法高亮
            String nameLower = fileName.toLowerCase();
            if (nameLower.endsWith(".lasm") || nameLower.endsWith(".log.txt")) {
                var language = TextMateLanguage.create("source.lua", true);
                codeEditor.setEditorLanguage(language);
            }
        } catch (Exception e) {
            // TextMate 初始化失败则使用纯文本，不影响基本功能
        }

        // 读取文件内容
        String content = readFileContent(filePath);
        if (content == null) {
            finish();
            return;
        }

        // 反log防御：折叠重复内容
        List<String> antiLogPatterns = getIntent().getStringArrayListExtra("anti_log_patterns");
        if (antiLogPatterns != null && !antiLogPatterns.isEmpty()) {
            content = collapseRepeatedLines(content, antiLogPatterns);
        }

        codeEditor.setText(content);

        // 只读模式
        codeEditor.setEditable(false);
        codeEditor.setPinLineNumber(true);

        // 加载编辑器设置（字体大小、行号、换行等）
        loadEditorSettings();
    }

    /**
     * 折叠重复行：支持单行连续重复（A,A,A）和两行交替重复（A,B,A,B）
     * 仅在重复单元连续出现 >= 3 次时折叠
     */
    private String collapseRepeatedLines(String rawContent, List<String> patterns) {
        if (rawContent == null || patterns == null || patterns.isEmpty()) {
            return rawContent;
        }

        Set<String> patternSet = new HashSet<>(patterns);
        String[] lines = rawContent.split("\n", -1);
        StringBuilder result = new StringBuilder();

        int i = 0;
        while (i < lines.length) {
            CollapseResult cr = tryDetectRepeat(lines, i, patternSet);
            if (cr != null) {
                result.append(buildSummary(lines, i, cr));
                i += cr.unitSize * cr.repeatCount;
            } else {
                result.append(lines[i]).append("\n");
                i++;
            }
        }

        return result.toString();
    }

    /**
     * 尝试从 start 位置检测重复单元（大小 1 或 2）
     * @return 检测结果，null 表示未检测到可折叠重复
     */
    private CollapseResult tryDetectRepeat(String[] lines, int start, Set<String> patternSet) {
        // 尝试单元大小 1（单行重复）和 2（两行交替重复）
        for (int unitSize = 1; unitSize <= 2; unitSize++) {
            if (start + unitSize > lines.length) continue;

            // 检查第一组单元的所有行是否都在模式集合中
            boolean allInPattern = true;
            for (int k = 0; k < unitSize; k++) {
                String t = lines[start + k].trim();
                if (t.isEmpty() || !patternSet.contains(t)) {
                    allInPattern = false;
                    break;
                }
            }
            if (!allInPattern) continue;

            // 向后统计连续重复次数
            int count = 1;
            int pos = start + unitSize;
            while (pos + unitSize <= lines.length) {
                boolean match = true;
                for (int k = 0; k < unitSize; k++) {
                    if (!lines[start + k].equals(lines[pos + k])) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    count++;
                    pos += unitSize;
                } else {
                    break;
                }
            }

            if (count >= 3) {
                return new CollapseResult(unitSize, count);
            }
        }
        return null;
    }

    /**
     * 构建折叠摘要行
     */
    private String buildSummary(String[] lines, int start, CollapseResult cr) {
        StringBuilder sb = new StringBuilder("-- [已折叠 ");
        if (cr.unitSize == 1) {
            String preview = truncate(lines[start].trim(), 40);
            sb.append(cr.repeatCount).append(" 行: ").append(preview);
        } else {
            String p1 = truncate(lines[start].trim(), 30);
            String p2 = truncate(lines[start + 1].trim(), 30);
            sb.append(cr.repeatCount).append(" 对 (").append(p1)
                    .append(" | ").append(p2).append(")");
        }
        sb.append("] --\n");
        return sb.toString();
    }

    /**
     * 截断过长文本
     */
    private String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }

    private String readFileContent(String path) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(path), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            Toast.makeText(this, "读取文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void loadEditorSettings() {
        SharedPreferences prefs = getSharedPreferences("EditorSettings", MODE_PRIVATE);
        float fontSize = prefs.getFloat("fontSize", 16f);
        codeEditor.setTextSize(fontSize);
        boolean showLineNumbers = prefs.getBoolean("lineNumbers", true);
        codeEditor.setLineNumberEnabled(showLineNumbers);
        boolean wordWrap = prefs.getBoolean("wordWrap", false);
        codeEditor.setWordwrap(wordWrap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_preview_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem lineNumbersItem = menu.findItem(R.id.action_line_numbers);
        if (lineNumbersItem != null) {
            lineNumbersItem.setChecked(codeEditor.isLineNumberEnabled());
        }
        MenuItem wordWrapItem = menu.findItem(R.id.action_word_wrap);
        if (wordWrapItem != null) {
            wordWrapItem.setChecked(codeEditor.isWordwrap());
        }
        MenuItem extractItem = menu.findItem(R.id.action_extract_calls);
        if (extractItem != null) {
            extractItem.setVisible(isLasmFile);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_search) {
            codeEditor.beginSearchMode();
            return true;
        } else if (id == R.id.action_line_numbers) {
            boolean newState = !codeEditor.isLineNumberEnabled();
            codeEditor.setLineNumberEnabled(newState);
            item.setChecked(newState);
            return true;
        } else if (id == R.id.action_word_wrap) {
            boolean newState = !codeEditor.isWordwrap();
            codeEditor.setWordwrap(newState);
            item.setChecked(newState);
            return true;
        } else if (id == R.id.action_extract_calls) {
            String content = codeEditor.getText().toString();
            String result = extractKeyCalls(content);
            showExtractResultDialog(result);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 从 lasm 反编译内容中提取关键 GG 调用信息，自动检测并解密加密字符串
     */
    private String extractKeyCalls(String content) {
        if (content == null || content.isEmpty()) {
            return "文件内容为空";
        }

        // ===== Phase 0: 预处理 string.char 加密（strjm 工具） =====
        content = preprocessStringChar(content);

        String[] lines = content.split("\n", -1);

        // ===== Phase 1: 从主代码段提取函数名映射和脚本名 =====
        java.util.Map<String, String> funcNameMap = new java.util.LinkedHashMap<>();
        String scriptName = null;

        java.util.regex.Pattern closurePattern = java.util.regex.Pattern.compile("CLOSURE\\s+v0\\s+(F\\d+)");
        java.util.regex.Pattern settabupPattern = java.util.regex.Pattern.compile("SETTABUP\\s+u0\\s+\"([^\"]+)\"\\s+v0");
        java.util.regex.Pattern csPattern = java.util.regex.Pattern.compile("SETTABUP\\s+u0\\s+\"cs\"\\s+\"([^\"]+)\"");

        int mainBodyEnd = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith(".func ")) {
                mainBodyEnd = i;
                break;
            }

            if (scriptName == null) {
                java.util.regex.Matcher csMatcher = csPattern.matcher(line);
                if (csMatcher.find()) {
                    scriptName = csMatcher.group(1);
                }
            }

            java.util.regex.Matcher closureMatcher = closurePattern.matcher(line);
            if (closureMatcher.find()) {
                String funcIdx = closureMatcher.group(1);
                for (int j = 1; j <= 4 && i + j < lines.length; j++) {
                    java.util.regex.Matcher settabupMatcher = settabupPattern.matcher(lines[i + j].trim());
                    if (settabupMatcher.find()) {
                        funcNameMap.put(funcIdx, settabupMatcher.group(1));
                        break;
                    }
                    if (closurePattern.matcher(lines[i + j].trim()).find()) break;
                }
            }
        }

        // ===== Phase 1.5: 从 F0 函数提取解密信息 =====
        boolean hasF0 = false;
        String decryptAlphabet = null;
        Integer xorKey = null;
        for (int i = mainBodyEnd; i < lines.length; i++) {
            if (lines[i].trim().startsWith(".func F0")) {
                hasF0 = true;
                decryptAlphabet = extractAlphabetFromF0(lines, i + 1);
                if (decryptAlphabet == null) {
                    xorKey = extractXorKeyFromF0(lines, i + 1);
                }
                break;
            }
        }

        // ===== Phase 2: 逐函数解析 =====
        java.util.Map<String, java.util.List<ExtractedCall>> funcCalls = new java.util.LinkedHashMap<>();
        java.util.List<String> choiceLabels = new java.util.ArrayList<>();
        java.util.Map<Integer, String> choiceToFunc = new java.util.LinkedHashMap<>();

        java.util.regex.Pattern funcPattern = java.util.regex.Pattern.compile("\\.func\\s+(F\\d+)");
        java.util.regex.Pattern methodPattern = java.util.regex.Pattern.compile("GETTABLE\\s+v0\\s+v0\\s+\"(searchNumber|editAll|getResults|clearResults|toast)\"");
        java.util.regex.Pattern loadkPattern = java.util.regex.Pattern.compile("LOADK\\s+v\\d+\\s+(\"[^\"]*\"|-?\\d+(?:\\.\\d+)?)");
        java.util.regex.Pattern eqPattern = java.util.regex.Pattern.compile("EQ\\s+0\\s+v0\\s+(\\d+)");
        java.util.regex.Pattern gettabupFuncPattern = java.util.regex.Pattern.compile("GETTABUP\\s+v0\\s+u0\\s+\"([^\"]+)\"");

        String currentFunc = null;
        boolean inChoiceTable = false;
        int pendingChoice = -1;
        String pendingMethod = null;

        for (int i = mainBodyEnd; i < lines.length; i++) {
            String line = lines[i].trim();

            java.util.regex.Matcher funcMatcher = funcPattern.matcher(line);
            if (funcMatcher.find()) {
                currentFunc = funcMatcher.group(1);
                funcCalls.putIfAbsent(currentFunc, new java.util.ArrayList<>());
                continue;
            }

            if (line.startsWith(".end") || line.startsWith("; ]=========]")) {
                currentFunc = null;
                inChoiceTable = false;
                pendingChoice = -1;
                pendingMethod = null;
                continue;
            }

            if (currentFunc == null) continue;

            // ---- F2/F3 (Main) special parsing: choice labels + EQ mapping ----
            // F3 is Main when there's a decryption function (F0)
            if ("F2".equals(currentFunc) || "F3".equals(currentFunc)) {
                if (line.contains("NEWTABLE")) {
                    inChoiceTable = true;
                    continue;
                }
                if (inChoiceTable) {
                    java.util.regex.Matcher lkMatcher = loadkPattern.matcher(line);
                    if (lkMatcher.find()) {
                        String val = stripQuotes(lkMatcher.group(1));
                        if (!val.isEmpty() && !val.equals("nil")) {
                            choiceLabels.add(val);
                        }
                    }
                }
                if (inChoiceTable && line.contains("SETLIST")) {
                    inChoiceTable = false;
                    continue;
                }

                java.util.regex.Matcher eqMatcher = eqPattern.matcher(line);
                if (eqMatcher.find()) {
                    pendingChoice = Integer.parseInt(eqMatcher.group(1));
                    continue;
                }
                if (pendingChoice >= 0) {
                    java.util.regex.Matcher gtabupMatcher = gettabupFuncPattern.matcher(line);
                    if (gtabupMatcher.find()) {
                        choiceToFunc.put(pendingChoice, gtabupMatcher.group(1));
                        pendingChoice = -1;
                    }
                }
            }

            // ---- Extract GG method calls ----
            java.util.regex.Matcher methodMatcher = methodPattern.matcher(line);
            if (methodMatcher.find()) {
                pendingMethod = methodMatcher.group(1);
                continue;
            }

            if (pendingMethod != null) {
                java.util.regex.Matcher lkMatcher = loadkPattern.matcher(line);
                if (lkMatcher.find()) {
                    String value = stripQuotes(lkMatcher.group(1));
                    java.util.List<ExtractedCall> calls = funcCalls.get(currentFunc);
                    if (calls != null) {
                        calls.add(new ExtractedCall(pendingMethod, value));
                    }
                    pendingMethod = null;
                }
            }
        }

        // ===== Phase 2.5: 收集加密字符串，尝试多策略解密 =====
        java.util.List<String> encryptedSamples = new java.util.ArrayList<>();
        for (java.util.List<ExtractedCall> calls : funcCalls.values()) {
            for (ExtractedCall call : calls) {
                if (looksEncrypted(call.value)) {
                    encryptedSamples.add(call.value);
                }
            }
        }
        for (String label : choiceLabels) {
            if (looksEncrypted(label)) encryptedSamples.add(label);
        }
        if (scriptName != null && looksEncrypted(scriptName)) encryptedSamples.add(scriptName);

        // 决定解密策略并建立解密映射
        java.util.Map<String, String> decryptMap = new java.util.LinkedHashMap<>();
        String decryptStrategy = null; // "base64", "xor", null = failed/not available

        if (decryptAlphabet != null) {
            // 策略1: 自定义 base64
            int successCount = 0;
            for (String val : encryptedSamples) {
                String d = tryDecodeBase64(val, decryptAlphabet);
                if (d != null) {
                    decryptMap.put(val, d);
                    successCount++;
                }
            }
            if (successCount > 0) {
                decryptStrategy = "base64";
            }
        }

        if (decryptStrategy == null && hasF0 && !encryptedSamples.isEmpty()) {
            // 策略2: 单字节 XOR（从样本中探测 key）
            xorKey = tryFindXorKey(encryptedSamples);
            if (xorKey != null) {
                for (String val : encryptedSamples) {
                    String d = xorDecrypt(val, (byte) (int) xorKey);
                    if (d != null && !d.equals(val)) {
                        decryptMap.put(val, d);
                    }
                }
                if (!decryptMap.isEmpty()) {
                    decryptStrategy = "xor";
                }
            }
        }

        // ===== Phase 3: 格式化输出（含解密） =====
        StringBuilder sb = new StringBuilder();

        sb.append("══════════════════════════════════\n");
        sb.append("  关键调用信息提取\n");
        sb.append("══════════════════════════════════\n\n");

        // 辅助函数：取值解密结果
        java.util.function.Function<String, String> getDecoded = val ->
                decryptMap.getOrDefault(val, null);

        if (scriptName != null && !scriptName.isEmpty()) {
            String decodedName = getDecoded.apply(scriptName);
            sb.append("\u25B8 脚本名称: ");
            if (decodedName != null) {
                sb.append("\"").append(decodedName).append("\"");
            } else {
                sb.append(scriptName);
            }
            sb.append("\n\n");
        }

        // Menu choices (with decryption)
        if (!choiceLabels.isEmpty() || !choiceToFunc.isEmpty()) {
            sb.append("\u25B8 主菜单选项:\n");
            if (!choiceLabels.isEmpty()) {
                for (int idx = 0; idx < choiceLabels.size(); idx++) {
                    int choiceNum = idx + 1;
                    String rawLabel = choiceLabels.get(idx);
                    String plainLabel = getDecoded.apply(rawLabel);
                    if (plainLabel == null) plainLabel = rawLabel;
                    String funcName = choiceToFunc.get(choiceNum);
                    sb.append("  ").append(choiceNum).append(". ").append(plainLabel);
                    if (!plainLabel.equals(rawLabel)) {
                        sb.append("  [密文: ").append(rawLabel).append("]");
                    }
                    if (funcName != null) {
                        sb.append(" \u2192 ").append(funcName).append("()");
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        // Function calls
        boolean hasContent = false;
        java.util.List<String> decodedLabelsForLookup = new java.util.ArrayList<>();
        for (String label : choiceLabels) {
            String d = getDecoded.apply(label);
            decodedLabelsForLookup.add(d != null ? d : label);
        }

        for (java.util.Map.Entry<String, java.util.List<ExtractedCall>> entry : funcCalls.entrySet()) {
            String funcIdx = entry.getKey();
            java.util.List<ExtractedCall> calls = entry.getValue();
            if (calls.isEmpty()) continue;

            hasContent = true;
            String funcName = funcNameMap.getOrDefault(funcIdx, funcIdx);

            String desc = "";
            for (java.util.Map.Entry<Integer, String> ctfe : choiceToFunc.entrySet()) {
                if (ctfe.getValue().equals(funcName) && ctfe.getKey() <= choiceLabels.size()) {
                    int idx = ctfe.getKey() - 1;
                    String label = (!decodedLabelsForLookup.isEmpty() && idx < decodedLabelsForLookup.size())
                            ? decodedLabelsForLookup.get(idx) : choiceLabels.get(idx);
                    desc = " \u2014 " + label;
                    break;
                }
            }

            sb.append("\u25B8 ").append(funcName).append("()").append(desc).append(":\n");

            for (ExtractedCall call : calls) {
                String rawValue = call.value;
                String decoded = getDecoded.apply(rawValue);

                sb.append("  gg.").append(call.method).append("(");
                if (call.method.equals("searchNumber") && rawValue.contains(";")) {
                    sb.append("\"").append(rawValue).append("\", ...)");
                } else {
                    sb.append(rawValue);
                }
                sb.append(")");

                if (decoded != null) {
                    sb.append("  \u2192  ");
                    if (decoded.contains(";") && call.method.equals("searchNumber")) {
                        sb.append("\"").append(decoded).append("\"");
                    } else {
                        sb.append(decoded);
                    }
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!hasContent) {
            sb.append("未检测到关键调用信息。\n");
            sb.append("该文件可能不包含 gg.searchNumber / gg.editAll / gg.getResults 等调用。\n");
        }

        // 解密诊断信息
        if (decryptStrategy != null) {
            sb.append("---\n");
            if ("base64".equals(decryptStrategy)) {
                sb.append("\u2139 已自动解密 (自定义 base64)\n");
            } else if ("xor".equals(decryptStrategy)) {
                sb.append("\u2139 已自动解密 (XOR, key=0x")
                        .append(String.format("%02X", xorKey)).append(")\n");
            }
            sb.append("  \u2192 后为解密结果\n\n");
        } else if (hasF0 && !encryptedSamples.isEmpty()) {
            sb.append("---\n");
            sb.append("\u26A0 检测到加密函数 F0，但自动解密未成功。\n");
            sb.append("  可能原因: 加密方式非标准 base64 / 非单字节 XOR / 自定义算法\n");
            sb.append("  所有参数值均为密文，需对照源码手动解密。\n");
            if (decryptAlphabet != null) {
                sb.append("  已提取 alphabet: ").append(decryptAlphabet).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Phase 0: 预处理 strjm 工具加密
     * 扫描 string.char(字节1, 字节2, ...) 调用并还原为原始字符串，
     * 替换为等效的 LOADK 指令，使后续提取管道透明
     */
    private String preprocessStringChar(String content) {
        String[] lines = content.split("\n", -1);
        java.util.regex.Pattern strGetTabup = java.util.regex.Pattern.compile(
                "GETTABUP\\s+v(\\d+)\\s+u\\d+\\s+\"string\"");
        java.util.regex.Pattern loadkInt = java.util.regex.Pattern.compile(
                "LOADK\\s+v\\d+\\s+(\\d+)");
        java.util.regex.Pattern callResultPat = java.util.regex.Pattern.compile(
                "CALL\\s+v(\\d+)\\.\\.v\\d+\\s+v\\1\\.\\.v\\1");

        java.util.List<String> output = new java.util.ArrayList<>();
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            java.util.regex.Matcher m1 = strGetTabup.matcher(line);

            if (m1.find() && i + 2 < lines.length) {
                String baseReg = m1.group(1);
                // 检查下一行是否为 GETTABLE vN vN "char"
                String nextLine = lines[i + 1].trim();
                if (nextLine.matches("GETTABLE\\s+v" + baseReg + "\\s+v" + baseReg + "\\s+\"char\"")) {

                    // 收集后续 LOADK 整数（字节值 0-255）
                    java.util.List<Integer> bytes = new java.util.ArrayList<>();
                    int j = i + 2;
                    while (j < lines.length) {
                        java.util.regex.Matcher lkm = loadkInt.matcher(lines[j].trim());
                        if (lkm.find()) {
                            int val = Integer.parseInt(lkm.group(1));
                            if (val >= 0 && val <= 255) {
                                bytes.add(val);
                                j++;
                                continue;
                            }
                        }
                        break;
                    }

                    // 检查是否以 CALL vN..vM vN..vN 结尾（string.char 调用）
                    if (!bytes.isEmpty() && j < lines.length) {
                        java.util.regex.Matcher cm = callResultPat.matcher(lines[j].trim());
                        if (cm.find() && cm.group(1).equals(baseReg)) {
                            // 还原字符串
                            StringBuilder decoded = new StringBuilder();
                            for (int b : bytes) decoded.append((char) b);
                            String d = decoded.toString();

                            // 只对可打印结果进行替换（防止误处理）
                            if (isPrintable(d) && d.length() >= 1) {
                                // 替换为 LOADK
                                output.add("LOADK v" + baseReg + " \"" + escapeLasmString(d) + "\"");
                                i = j + 1;
                                continue;
                            }
                        }
                    }
                }
            }

            output.add(lines[i]);
            i++;
        }

        StringBuilder result = new StringBuilder();
        for (String l : output) result.append(l).append("\n");
        return result.toString();
    }

    /**
     * 转义 lasm LOADK 字符串中的特殊字符
     */
    private String escapeLasmString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 从 F0 函数中提取字符串解密 alphabet
     * 收集 CONCAT 之前的所有 LOADK 字符串，拼接为完整 alphabet
     */
    private String extractAlphabetFromF0(String[] lines, int start) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        java.util.regex.Pattern loadkPattern = java.util.regex.Pattern.compile("LOADK\\s+v\\d+\\s+\"([^\"]+)\"");

        int nestedDepth = 0;
        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.startsWith(".func ")) nestedDepth++;
            if (line.startsWith(".end")) {
                if (nestedDepth > 0) nestedDepth--;
                else break; // end of F0
            }

            if (nestedDepth > 0) continue; // skip nested sub-functions

            java.util.regex.Matcher m = loadkPattern.matcher(line);
            if (m.find()) {
                parts.add(m.group(1));
            }

            if (line.contains("CONCAT")) break;
        }

        if (parts.size() < 2) return null;

        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(p);
        return sb.toString();
    }

    /**
     * 自定义 base64 解码（基于提取的 alphabet）
     * 实现与 Lua _d() 函数相同的解码逻辑
     */
    private String decodeCustomBase64(String encoded, String alphabet) {
        if (encoded == null || encoded.isEmpty() || alphabet == null) return null;

        // Step 1: 过滤，仅保留 alphabet 内字符和 '='
        StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (alphabet.indexOf(c) >= 0 || c == '=') {
                filtered.append(c);
            }
        }
        String clean = filtered.toString();
        if (clean.isEmpty()) return null;

        // Step 2: 每个字符转为 6-bit 二进制串（跳过 '='）
        StringBuilder binary = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == '=') continue;
            int pos = alphabet.indexOf(c);
            if (pos < 0) return null;
            for (int bit = 5; bit >= 0; bit--) {
                binary.append((pos & (1 << bit)) != 0 ? '1' : '0');
            }
        }

        // Step 3: 每 8-bit 转为一个字节，用 UTF-8 构建字符串（正确还原中文等多字节字符）
        String bits = binary.toString();
        int byteCount = bits.length() / 8;
        byte[] bytes = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            int byteVal = 0;
            for (int j = 0; j < 8; j++) {
                if (bits.charAt(i * 8 + j) == '1') {
                    byteVal |= (1 << (7 - j));
                }
            }
            bytes[i] = (byte) byteVal;
        }
        try {
            return new String(bytes, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(bytes);
        }
    }

    /**
     * 尝试 base64 解密字符串
     */
    private String tryDecodeBase64(String value, String alphabet) {
        if (value == null || alphabet == null) return null;
        boolean hasLetter = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (alphabet.indexOf(c) >= 0) { hasLetter = true; break; }
        }
        if (!hasLetter) return null;

        String decoded = decodeCustomBase64(value, alphabet);
        if (decoded == null || decoded.isEmpty() || decoded.equals(value)) return null;
        if (!isPrintable(decoded)) return null;
        return decoded;
    }

    /**
     * 判断一个值是否"看起来像加密字符串"（含非数字字母，长度>=2）
     */
    private boolean looksEncrypted(String value) {
        if (value == null || value.length() < 2) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isDigit(c)) return true;
        }
        return false;
    }

    /**
     * 从加密字符串样本中探测单字节 XOR key
     * 对第一个 >=4 字符的样本尝试全部 256 个 key，找到产生可打印有意义结果的 key
     */
    private Integer tryFindXorKey(java.util.List<String> samples) {
        if (samples.isEmpty()) return null;

        // 取第一个长度>=4的样本
        String sample = null;
        for (String s : samples) {
            if (s.length() >= 4) { sample = s; break; }
        }
        if (sample == null) return null;

        Integer foundKey = null;
        for (int key = 0; key < 256; key++) {
            String decrypted = xorDecrypt(sample, (byte) key);
            if (decrypted != null && isPrintable(decrypted) && looksMeaningful(decrypted)) {
                if (foundKey != null) return null; // 多个 key 都有效 → 不唯一，放弃
                foundKey = key;
            }
        }
        return foundKey;
    }

    /**
     * 单字节 XOR 解密
     */
    private String xorDecrypt(String input, byte key) {
        if (input == null || input.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append((char) (input.charAt(i) ^ key));
        }
        return sb.toString();
    }

    /**
     * 判断解密结果是否"有意义"（含常见分隔符或中文等）
     */
    private boolean looksMeaningful(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // 包含空格、点、分号、中文、常见标点 → 有意义
            if (c == ' ' || c == '.' || c == ';' || c == ',' || c == ':'
                    || c == '_' || c == '-' || c == '/'
                    || (c >= 0x4E00 && c <= 0x9FFF)
                    || (c >= 0x3000 && c <= 0x303F)) {
                return true;
            }
        }
        // 纯字母数字也可能是明文，但如果很长 (>8) 且全部可打印也算有意义
        return s.length() > 8;
    }

    /**
     * 从 F0 函数中提取 XOR key（检测 bit32.bxor/string.byte 模式）
     * 返回 XOR key 作为 Integer，未检测到则返回 null
     */
    private Integer extractXorKeyFromF0(String[] lines, int start) {
        java.util.regex.Pattern bxorLoadkPattern = java.util.regex.Pattern.compile("LOADK\\s+v\\d+\\s+(\\d+)");
        java.util.regex.Pattern bxorPattern = java.util.regex.Pattern.compile("(BXOR|GETTABLE\\s+v\\d+\\s+v\\d+\\s+\"bxor\")");

        int nestedDepth = 0;
        boolean hasBitOp = false;
        String lastLoadkNum = null;

        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.startsWith(".func ")) nestedDepth++;
            if (line.startsWith(".end")) {
                if (nestedDepth > 0) nestedDepth--;
                else break;
            }
            if (nestedDepth > 0) continue;

            // 检测 bit32/bit 库调用
            if (line.contains("bit32") || line.contains("\"bit\"")) hasBitOp = true;

            // 检测 BXOR 或 bxor 调用
            java.util.regex.Matcher bxorMatcher = bxorPattern.matcher(line);
            if (bxorMatcher.find()) hasBitOp = true;

            // 收集 LOADK 数字（可能的 XOR key）
            java.util.regex.Matcher lkMatcher = bxorLoadkPattern.matcher(line);
            if (lkMatcher.find()) {
                lastLoadkNum = lkMatcher.group(1);
            }

            if (hasBitOp && lastLoadkNum != null) {
                try {
                    int key = Integer.parseInt(lastLoadkNum);
                    if (key >= 0 && key <= 255) return key;
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    /**
     * 判断字符串是否全为可打印字符（不含控制字符）
     */
    private boolean isPrintable(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') return false;
            if (c == 0x7F) return false;
        }
        return true;
    }

    /**
     * 去除 LOADK 值的外层双引号
     */
    private String stripQuotes(String s) {
        if (s == null || s.isEmpty()) return s;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * 显示提取结果对话框，支持复制
     */
    private void showExtractResultDialog(String result) {
        android.text.SpannableString spannable = new android.text.SpannableString(result);
        new android.app.AlertDialog.Builder(this)
                .setTitle("关键调用信息")
                .setMessage(spannable)
                .setPositiveButton("复制全部", (dialog, which) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("key_calls", result);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    /**
     * 提取到的单个 GG 调用
     */
    private static class ExtractedCall {
        final String method;
        final String value;

        ExtractedCall(String method, String value) {
            this.method = method;
            this.value = value;
        }
    }

    /**
     * 折叠检测结果
     */
    private static class CollapseResult {
        final int unitSize;   // 重复单元大小（1 或 2 行）
        final int repeatCount; // 连续重复次数

        CollapseResult(int unitSize, int repeatCount) {
            this.unitSize = unitSize;
            this.repeatCount = repeatCount;
        }
    }
}

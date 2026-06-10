package mituran.gglua.tool;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LogDecompileActivity extends AppCompatActivity {

    private static final String NOTES_PATH = "/sdcard/Notes";
    private static final int ANTI_LOG_SIZE_THRESHOLD = 2 * 1024; // 2KB 以上即检测
    private static final float ANTI_LOG_DUPLICATE_RATIO = 0.5f;    // 50% 重复率
    private static final int ANTI_LOG_SAMPLE_LINES = 500;

    private LinearLayout llLogList, llLasmList;
    private TextView tvLogCount, tvLasmCount;
    private TextView tvLogEmpty, tvLasmEmpty;
    private List<String> currentAntiLogPatterns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_decompile);

        initToolbar();
        initViews();
        loadFiles();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        llLogList = findViewById(R.id.ll_log_list);
        llLasmList = findViewById(R.id.ll_lasm_list);
        tvLogCount = findViewById(R.id.tv_log_count);
        tvLasmCount = findViewById(R.id.tv_lasm_count);
        tvLogEmpty = findViewById(R.id.tv_log_empty);
        tvLasmEmpty = findViewById(R.id.tv_lasm_empty);

        findViewById(R.id.btn_tutorial).setOnClickListener(v -> showTutorialDialog());
        findViewById(R.id.btn_refresh).setOnClickListener(v -> loadFiles());
    }

    private void loadFiles() {
        llLogList.removeAllViews();
        llLasmList.removeAllViews();

        File notesDir = new File(NOTES_PATH);
        if (!notesDir.exists() || !notesDir.isDirectory()) {
            tvLogEmpty.setVisibility(View.VISIBLE);
            tvLasmEmpty.setVisibility(View.VISIBLE);
            tvLogCount.setText("0 个文件");
            tvLasmCount.setText("0 个文件");
            return;
        }

        File[] allFiles = notesDir.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            tvLogEmpty.setVisibility(View.VISIBLE);
            tvLasmEmpty.setVisibility(View.VISIBLE);
            tvLogCount.setText("0 个文件");
            tvLasmCount.setText("0 个文件");
            return;
        }

        List<File> logFiles = new ArrayList<>();
        List<File> lasmFiles = new ArrayList<>();

        for (File file : allFiles) {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".log.txt")) {
                    logFiles.add(file);
                } else if (name.endsWith(".lasm")) {
                    lasmFiles.add(file);
                }
            }
        }

        // 显示log文件
        tvLogCount.setText(logFiles.size() + " 个文件");
        if (logFiles.isEmpty()) {
            tvLogEmpty.setVisibility(View.VISIBLE);
        } else {
            tvLogEmpty.setVisibility(View.GONE);
            for (File file : logFiles) {
                llLogList.addView(createFileItemView(file, true));
            }
        }

        // 显示lasm文件
        tvLasmCount.setText(lasmFiles.size() + " 个文件");
        if (lasmFiles.isEmpty()) {
            tvLasmEmpty.setVisibility(View.VISIBLE);
        } else {
            tvLasmEmpty.setVisibility(View.GONE);
            for (File file : lasmFiles) {
                llLasmList.addView(createFileItemView(file, false));
            }
        }
    }

    private View createFileItemView(File file, boolean isLog) {
        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_log_file, llLogList, false);

        ImageView ivIcon = itemView.findViewById(R.id.iv_file_icon);
        TextView tvName = itemView.findViewById(R.id.tv_file_name);
        TextView tvSize = itemView.findViewById(R.id.tv_file_size);
        TextView tvTime = itemView.findViewById(R.id.tv_file_time);

        tvName.setText(file.getName());
        tvSize.setText(formatFileSize(file.length()));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        tvTime.setText(sdf.format(new Date(file.lastModified())));

        // 图标区分
        if (isLog) {
            ivIcon.setImageResource(R.drawable.ic_console);
            ivIcon.setColorFilter(getColor(R.color.purple_500));
        } else {
            ivIcon.setImageResource(R.drawable.ic_code);
            ivIcon.setColorFilter(getColor(R.color.teal_700));
        }

        itemView.setOnClickListener(v -> onFileClick(file, isLog));

        // 分隔线（非最后一项）
        return itemView;
    }

    private void onFileClick(File file, boolean isLog) {
        currentAntiLogPatterns = null;
        // 检查反log防御
        if (isLog) {
            try {
                currentAntiLogPatterns = detectAntiLogDefense(file);
                if (currentAntiLogPatterns != null && !currentAntiLogPatterns.isEmpty()) {
                    showAntiLogDialog(file);
                    return;
                }
            } catch (Exception e) {
                // 读取失败则直接打开
            }
        }

        openFile(file);
    }

    /**
     * 检测反log防御：文件较大 + 大量重复内容
     * @return 高频重复模式列表，null表示未检测到反log防御
     */
    private List<String> detectAntiLogDefense(File file) throws Exception {
        long fileSize = file.length();
        if (fileSize < ANTI_LOG_SIZE_THRESHOLD) {
            return null;
        }

        // 读取前N行，统计每行出现频次
        Map<String, Integer> lineCount = new LinkedHashMap<>();
        int totalLines = 0;
        int lineLimit = ANTI_LOG_SAMPLE_LINES;

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try {
            String line;
            while ((line = reader.readLine()) != null && totalLines < lineLimit) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lineCount.merge(trimmed, 1, Integer::sum);
                }
                totalLines++;
            }
        } finally {
            reader.close();
        }

        if (totalLines == 0) return null;

        int uniqueCount = lineCount.size();
        float duplicateRatio = 1.0f - ((float) uniqueCount / (float) totalLines);

        if (duplicateRatio > ANTI_LOG_DUPLICATE_RATIO) {
            // 提取出现次数 >= 3 的高频模式
            List<String> patterns = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : lineCount.entrySet()) {
                if (entry.getValue() >= 3) {
                    patterns.add(entry.getKey());
                }
            }
            return patterns;
        }
        return null;
    }

    private void showAntiLogDialog(File file) {
        new AlertDialog.Builder(this)
                .setTitle("检测到反log防御")
                .setMessage("文件 \"" + file.getName() + "\" 中检测到大量重复日志内容。\n\n"
                        + "该脚本加装了反log防御，日志系统已被崩掉，建议关闭log输出后重试。\n\n"
                        + "选择\"仍然打开\"将在预览中自动折叠重复内容。")
                .setPositiveButton("仍然打开", (dialog, which) -> openFile(file))
                .setNegativeButton("取消", null)
                .show();
    }

    private void openFile(File file) {
        Intent intent = new Intent(this, FilePreviewActivity.class);
        intent.putExtra("file_path", file.getAbsolutePath());
        if (currentAntiLogPatterns != null && !currentAntiLogPatterns.isEmpty()) {
            intent.putStringArrayListExtra("anti_log_patterns",
                    new ArrayList<>(currentAntiLogPatterns));
        }
        startActivity(intent);
    }

    private void showTutorialDialog() {
        new AlertDialog.Builder(this)
                .setTitle("使用教程")
                .setMessage("1. 打开 GameGuardian 修改器\n\n"
                        + "2. 在 GG 中执行目标脚本，GG 会自动将脚本的 log 输出和反编译结果保存到 /sdcard/Notes/ 目录\n\n"
                        + "3. log 文件格式为 xxx.log.txt，反编译文件格式为 xxx.lasm\n\n"
                        + "4. 返回本页面，点击「刷新」查看文件列表，点击文件即可打开查看\n\n"
                        + "提示：GG 的 log 输出功能需要在 GG 设置中开启。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.2f MB", size / (1024.0 * 1024.0));
        }
    }
}

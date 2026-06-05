package mituran.gglua.tool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_APP = "AppSettings";
    private static final String PREFS_EDITOR = "EditorSettings";
    private static final String KEY_CARDS_VISIBLE = "home_cards_visible";
    private static final String KEY_CARDS_ORDER = "home_cards_order";
    private static final String KEY_PROJECT_SORT = "project_sort_mode";
    private static final String KEY_SERVER_URL = "community_server_url";
    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";

    // 编辑器设置键值（与 CodeEditorLua 共享）
    private static final String PREF_FONT_SIZE = "fontSize";
    private static final String PREF_LINE_NUMBERS = "lineNumbers";
    private static final String PREF_WORD_WRAP = "wordWrap";
    private static final String PREF_THEME = "theme";
    private static final String PREF_AUTO_SAVE = "autoSave";
    private static final String PREF_AUTO_BACKUP = "autoBackup";

    private RecyclerView rvHomeCards;
    private CardSettingAdapter cardAdapter;
    private List<HomeCardItem> cardItems;

    private RadioButton rbSortName, rbSortDate;
    private TextView tvServerAddress;
    private TextView tvFontSizeValue;
    private TextView tvThemeValue;
    private SwitchMaterial switchLineNumbers, switchWordWrap, switchAutoSave, switchAutoBackup;

    private SharedPreferences appPrefs;
    private SharedPreferences editorPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        appPrefs = getSharedPreferences(PREFS_APP, MODE_PRIVATE);
        editorPrefs = getSharedPreferences(PREFS_EDITOR, MODE_PRIVATE);

        initToolbar();
        initHomeCards();
        initProjectSort();
        initServerAddress();
        initEditorSettings();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ==================== 首页卡片设置 ====================

    private void initHomeCards() {
        rvHomeCards = findViewById(R.id.rv_home_cards);
        rvHomeCards.setLayoutManager(new LinearLayoutManager(this));

        cardItems = loadCardItems();
        cardAdapter = new CardSettingAdapter(cardItems);
        rvHomeCards.setAdapter(cardAdapter);

        // ItemTouchHelper 实现拖拽排序
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from < to) {
                    for (int i = from; i < to; i++) {
                        Collections.swap(cardItems, i, i + 1);
                    }
                } else {
                    for (int i = from; i > to; i--) {
                        Collections.swap(cardItems, i, i - 1);
                    }
                }
                cardAdapter.notifyItemMoved(from, to);
                saveCardSettings();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        };

        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rvHomeCards);
    }

    private List<HomeCardItem> loadCardItems() {
        List<HomeCardItem> items = new ArrayList<>();

        // 定义所有卡片
        HomeCardItem card2 = new HomeCardItem("card2", "脚本加解密", R.drawable.ic_security);
        HomeCardItem card3 = new HomeCardItem("card3", "定制化GG修改器", R.drawable.ic_build);
        HomeCardItem card4 = new HomeCardItem("card4", "虚拟框架", R.drawable.ic_launch);

        // 从 SharedPreferences 读取可见性和顺序
        String visibleJson = appPrefs.getString(KEY_CARDS_VISIBLE, null);
        String orderJson = appPrefs.getString(KEY_CARDS_ORDER, null);

        List<String> visibleIds = new ArrayList<>();
        List<String> orderIds = new ArrayList<>();

        if (visibleJson != null) {
            try {
                JSONArray arr = new JSONArray(visibleJson);
                for (int i = 0; i < arr.length(); i++) {
                    visibleIds.add(arr.getString(i));
                }
            } catch (JSONException e) {
            // JSON format error, use defaults
        }
        }

        if (orderJson != null) {
            try {
                JSONArray arr = new JSONArray(orderJson);
                for (int i = 0; i < arr.length(); i++) {
                    orderIds.add(arr.getString(i));
                }
            } catch (JSONException e) {
            // JSON format error, use defaults
        }
        }

        // 默认全部可见
        if (visibleIds.isEmpty()) {
            visibleIds.add("card2");
            visibleIds.add("card3");
            visibleIds.add("card4");
        }
        // 默认顺序
        if (orderIds.isEmpty()) {
            orderIds.add("card2");
            orderIds.add("card3");
            orderIds.add("card4");
        }

        // 按顺序构建列表
        for (String id : orderIds) {
            if ("card2".equals(id)) {
                card2.visible = visibleIds.contains("card2");
                items.add(card2);
            } else if ("card3".equals(id)) {
                card3.visible = visibleIds.contains("card3");
                items.add(card3);
            } else if ("card4".equals(id)) {
                card4.visible = visibleIds.contains("card4");
                items.add(card4);
            }
        }

        return items;
    }

    private void saveCardSettings() {
        JSONArray orderArr = new JSONArray();
        JSONArray visibleArr = new JSONArray();
        for (HomeCardItem item : cardItems) {
            orderArr.put(item.id);
            if (item.visible) {
                visibleArr.put(item.id);
            }
        }
        appPrefs.edit()
                .putString(KEY_CARDS_ORDER, orderArr.toString())
                .putString(KEY_CARDS_VISIBLE, visibleArr.toString())
                .apply();
    }

    // ==================== 项目列表排序 ====================

    private void initProjectSort() {
        rbSortName = findViewById(R.id.rb_sort_name);
        rbSortDate = findViewById(R.id.rb_sort_date);

        String sortMode = appPrefs.getString(KEY_PROJECT_SORT, "name");
        if ("last_modified".equals(sortMode)) {
            rbSortDate.setChecked(true);
            rbSortName.setChecked(false);
        } else {
            rbSortName.setChecked(true);
            rbSortDate.setChecked(false);
        }

        View.OnClickListener sortListener = v -> {
            int id = v.getId();
            if (id == R.id.ll_sort_name) {
                rbSortName.setChecked(true);
                rbSortDate.setChecked(false);
                appPrefs.edit().putString(KEY_PROJECT_SORT, "name").apply();
            } else if (id == R.id.ll_sort_date) {
                rbSortDate.setChecked(true);
                rbSortName.setChecked(false);
                appPrefs.edit().putString(KEY_PROJECT_SORT, "last_modified").apply();
            }
        };

        findViewById(R.id.ll_sort_name).setOnClickListener(sortListener);
        findViewById(R.id.ll_sort_date).setOnClickListener(sortListener);
    }

    // ==================== 社区服务器地址 ====================

    private void initServerAddress() {
        tvServerAddress = findViewById(R.id.tv_server_address);

        String currentUrl = appPrefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
        tvServerAddress.setText(currentUrl);

        findViewById(R.id.ll_server_address).setOnClickListener(v -> showServerAddressDialog());
    }

    private void showServerAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_server_address, null);

        TextInputEditText etAddress = dialogView.findViewById(R.id.et_server_address);
        MaterialButton btnReset = dialogView.findViewById(R.id.btn_reset_server);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_server);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm_server);

        String currentUrl = appPrefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
        etAddress.setText(currentUrl);
        etAddress.setSelection(currentUrl.length());

        AlertDialog dialog = builder.setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnReset.setOnClickListener(v2 -> {
            etAddress.setText(DEFAULT_SERVER_URL);
        });

        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        btnConfirm.setOnClickListener(v2 -> {
            String newUrl = etAddress.getText().toString().trim();
            if (newUrl.isEmpty()) {
                Toast.makeText(this, "服务器地址不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            // 移除末尾斜杠
            if (newUrl.endsWith("/")) {
                newUrl = newUrl.substring(0, newUrl.length() - 1);
            }
            appPrefs.edit().putString(KEY_SERVER_URL, newUrl).apply();
            tvServerAddress.setText(newUrl);
            dialog.dismiss();
        });

        dialog.show();

        // 设置弹窗宽度
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // ==================== 编辑器设置 ====================

    private void initEditorSettings() {
        switchLineNumbers = findViewById(R.id.switch_line_numbers);
        switchWordWrap = findViewById(R.id.switch_word_wrap);
        switchAutoSave = findViewById(R.id.switch_auto_save);
        switchAutoBackup = findViewById(R.id.switch_auto_backup);
        tvFontSizeValue = findViewById(R.id.tv_font_size_value);
        tvThemeValue = findViewById(R.id.tv_theme_value);

        // 加载当前值
        float fontSize = editorPrefs.getFloat(PREF_FONT_SIZE, 16f);
        tvFontSizeValue.setText(String.valueOf((int) fontSize));

        boolean lineNumbers = editorPrefs.getBoolean(PREF_LINE_NUMBERS, true);
        switchLineNumbers.setChecked(lineNumbers);

        boolean wordWrap = editorPrefs.getBoolean(PREF_WORD_WRAP, false);
        switchWordWrap.setChecked(wordWrap);

        boolean autoSave = editorPrefs.getBoolean(PREF_AUTO_SAVE, false);
        switchAutoSave.setChecked(autoSave);

        boolean autoBackup = editorPrefs.getBoolean(PREF_AUTO_BACKUP, false);
        switchAutoBackup.setChecked(autoBackup);

        String theme = editorPrefs.getString(PREF_THEME, "solarized-light");
        tvThemeValue.setText(getThemeDisplayName(theme));

        // 字体大小点击
        findViewById(R.id.ll_font_size).setOnClickListener(v -> showFontSizeDialog());

        // 主题点击
        findViewById(R.id.ll_theme).setOnClickListener(v -> showThemeDialog());

        // Switch 监听
        CompoundButton.OnCheckedChangeListener switchListener = (buttonView, isChecked) -> {
            int id = buttonView.getId();
            if (id == R.id.switch_line_numbers) {
                editorPrefs.edit().putBoolean(PREF_LINE_NUMBERS, isChecked).apply();
            } else if (id == R.id.switch_word_wrap) {
                editorPrefs.edit().putBoolean(PREF_WORD_WRAP, isChecked).apply();
            } else if (id == R.id.switch_auto_save) {
                editorPrefs.edit().putBoolean(PREF_AUTO_SAVE, isChecked).apply();
            } else if (id == R.id.switch_auto_backup) {
                editorPrefs.edit().putBoolean(PREF_AUTO_BACKUP, isChecked).apply();
            }
        };

        switchLineNumbers.setOnCheckedChangeListener(switchListener);
        switchWordWrap.setOnCheckedChangeListener(switchListener);
        switchAutoSave.setOnCheckedChangeListener(switchListener);
        switchAutoBackup.setOnCheckedChangeListener(switchListener);
    }

    private void showFontSizeDialog() {
        String[] sizes = {"12", "14", "16", "18", "20", "22", "24"};
        float currentSize = editorPrefs.getFloat(PREF_FONT_SIZE, 16f);
        int currentIndex = -1;
        for (int i = 0; i < sizes.length; i++) {
            if (Integer.parseInt(sizes[i]) == (int) currentSize) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("选择字体大小")
                .setSingleChoiceItems(sizes, currentIndex, null)
                .setPositiveButton("确定", (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selected >= 0) {
                        float size = Float.parseFloat(sizes[selected]);
                        editorPrefs.edit().putFloat(PREF_FONT_SIZE, size).apply();
                        tvFontSizeValue.setText(sizes[selected]);
                        Toast.makeText(this, "字体大小已设置为 " + sizes[selected], Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showThemeDialog() {
        String[] themeNames = {"Solarized Light", "Quiet Light", "Monokai"};
        String[] themeKeys = {"solarized-light", "quietlight", "Monokai"};

        String currentTheme = editorPrefs.getString(PREF_THEME, "solarized-light");
        int currentIndex = 0;
        for (int i = 0; i < themeKeys.length; i++) {
            if (themeKeys[i].equals(currentTheme)) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("选择代码主题")
                .setSingleChoiceItems(themeNames, currentIndex, null)
                .setPositiveButton("应用", (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selected >= 0) {
                        String themeKey = themeKeys[selected];
                        editorPrefs.edit().putString(PREF_THEME, themeKey).apply();
                        tvThemeValue.setText(themeNames[selected]);
                        Toast.makeText(this, "主题已切换为 " + themeNames[selected], Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String getThemeDisplayName(String themeKey) {
        switch (themeKey) {
            case "solarized-light": return "Solarized Light";
            case "quietlight": return "Quiet Light";
            case "Monokai": return "Monokai";
            default: return themeKey;
        }
    }

    // ==================== RecyclerView Adapter ====================

    private class CardSettingAdapter extends RecyclerView.Adapter<CardSettingAdapter.ViewHolder> {

        private List<HomeCardItem> items;

        CardSettingAdapter(List<HomeCardItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_card_setting, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HomeCardItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.ivIcon.setImageResource(item.iconRes);
            holder.switchVisible.setOnCheckedChangeListener(null);
            holder.switchVisible.setChecked(item.visible);

            // 不可见的卡片拖拽把手变灰
            if (item.visible) {
                holder.ivDragHandle.setAlpha(1.0f);
                holder.tvName.setAlpha(1.0f);
                holder.ivIcon.setAlpha(1.0f);
            } else {
                holder.ivDragHandle.setAlpha(0.3f);
                holder.tvName.setAlpha(0.5f);
                holder.ivIcon.setAlpha(0.5f);
            }

            holder.switchVisible.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.visible = isChecked;
                notifyItemChanged(holder.getBindingAdapterPosition());
                saveCardSettings();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivDragHandle, ivIcon;
            TextView tvName;
            SwitchMaterial switchVisible;

            ViewHolder(View itemView) {
                super(itemView);
                ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
                ivIcon = itemView.findViewById(R.id.iv_card_icon);
                tvName = itemView.findViewById(R.id.tv_card_name);
                switchVisible = itemView.findViewById(R.id.switch_card_visible);
            }
        }
    }

    // ==================== 数据模型 ====================

    private static class HomeCardItem {
        String id;
        String name;
        int iconRes;
        boolean visible;

        HomeCardItem(String id, String name, int iconRes) {
            this.id = id;
            this.name = name;
            this.iconRes = iconRes;
            this.visible = true;
        }
    }
}

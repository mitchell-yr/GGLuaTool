package mituran.gglua.tool.tutorial;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;
import io.noties.markwon.image.ImageItem;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.image.SchemeHandler;
import mituran.gglua.tool.DraggableScrollBar;
import mituran.gglua.tool.R;

public class GGFunctionAddingTutorialActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView contentTextView;
    private RecyclerView chapterRecyclerView;
    private TutorialChapterAdapter chapterAdapter;
    private Markwon markwon;
    private List<TutorialChapter> chapters;
    private int currentChapterIndex = 0;
    private ScrollView scrollView;
    private DraggableScrollBar draggableScrollBar;

    // 搜索相关
    private MaterialCardView searchBar;
    private EditText searchInput;
    private ImageView searchPrev, searchNext, searchClose;
    private TextView searchCounter;
    private List<Integer> searchPositions = new ArrayList<>();
    private int currentSearchIndex = -1;
    private boolean isSearchingAllChapters = false;
    private Button searchCurrentChapter, searchAllChapters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        // ★ 初始化带图片支持的 Markwon
        initMarkwon();

        // 初始化视图
        initViews();

        // 初始化教程章节
        initChapters();

        // 设置适配器
        setupRecyclerView();

        // 显示第一章
        displayChapter(0);

        // 设置搜索功能
        setupSearch();
    }

    // =====================================================
    // ★★★ 核心修改：初始化带本地图片支持的 Markwon ★★★
    // =====================================================
    private void initMarkwon() {
        final Context context = this;

        markwon = Markwon.builder(context)
                .usePlugin(ImagesPlugin.create(plugin -> {

                    // ────────────────────────────────────────────
                    // Scheme 1: file://
                    // 用于加载设备文件系统中的图片
                    // Markdown写法: ![说明](file:///sdcard/Pictures/demo.png)
                    // ────────────────────────────────────────────
                    plugin.addSchemeHandler(new SchemeHandler() {
                        @NonNull
                        @Override
                        public ImageItem handle(@NonNull String raw, @NonNull Uri uri) {
                            try {
                                String path = uri.getPath();
                                if (path == null) {
                                    throw new IllegalStateException("Invalid file path: " + raw);
                                }
                                InputStream is = new FileInputStream(new File(path));
                                return ImageItem.withDecodingNeeded(null, is);
                            } catch (Exception e) {
                                throw new IllegalStateException("Cannot open file: " + raw, e);
                            }
                        }

                        @NonNull
                        @Override
                        public Collection<String> supportedSchemes() {
                            return Collections.singleton("file");
                        }
                    });

                    // ────────────────────────────────────────────
                    // Scheme 2: asset://
                    // 用于加载 assets 文件夹中的图片
                    // Markdown写法: ![说明](asset://images/tutorial/step1.png)
                    // 对应文件:     assets/images/tutorial/step1.png
                    // ────────────────────────────────────────────
                    plugin.addSchemeHandler(new SchemeHandler() {
                        @NonNull
                        @Override
                        public ImageItem handle(@NonNull String raw, @NonNull Uri uri) {
                            try {
                                // 从 raw 字符串中提取 asset 路径
                                // "asset://images/demo.png" -> "images/demo.png"
                                String path = raw;
                                if (path.startsWith("asset://")) {
                                    path = path.substring("asset://".length());
                                }
                                // 移除可能的前导斜杠
                                while (path.startsWith("/")) {
                                    path = path.substring(1);
                                }
                                InputStream is = context.getAssets().open(path);
                                return ImageItem.withDecodingNeeded(null, is);
                            } catch (Exception e) {
                                throw new IllegalStateException("Cannot open asset: " + raw, e);
                            }
                        }

                        @NonNull
                        @Override
                        public Collection<String> supportedSchemes() {
                            return Collections.singleton("asset");
                        }
                    });

                    // ────────────────────────────────────────────
                    // Scheme 3: res://
                    // 用于加载 drawable / mipmap 资源中的图片
                    // Markdown写法: ![说明](res://drawable/ic_launcher)
                    //              ![说明](res://mipmap/ic_launcher)
                    // ────────────────────────────────────────────
                    plugin.addSchemeHandler(new SchemeHandler() {
                        @NonNull
                        @Override
                        public ImageItem handle(@NonNull String raw, @NonNull Uri uri) {
                            try {
                                // 解析格式: res://类型/资源名
                                // uri.getAuthority() = 类型 (drawable / mipmap)
                                // uri.getLastPathSegment() = 资源名
                                String resType = uri.getAuthority();   // "drawable" 或 "mipmap"
                                String resName = uri.getLastPathSegment(); // 资源名称

                                if (resType == null || resName == null) {
                                    throw new IllegalStateException(
                                            "Invalid res URI format. Use: res://drawable/name");
                                }

                                Resources res = context.getResources();
                                int resId = res.getIdentifier(
                                        resName, resType, context.getPackageName());

                                if (resId == 0) {
                                    throw new IllegalStateException(
                                            "Resource not found: " + resType + "/" + resName);
                                }

                                // 使用 Drawable 直接返回
                                Drawable drawable = ContextCompat.getDrawable(context, resId);
                                if (drawable != null) {
                                    drawable.setBounds(
                                            0, 0,
                                            drawable.getIntrinsicWidth(),
                                            drawable.getIntrinsicHeight()
                                    );
                                    return ImageItem.withResult(drawable);
                                }

                                // 如果 Drawable 方式失败，尝试 InputStream 方式
                                InputStream is = res.openRawResource(resId);
                                return ImageItem.withDecodingNeeded(null, is);

                            } catch (Exception e) {
                                throw new IllegalStateException(
                                        "Cannot load resource: " + raw, e);
                            }
                        }

                        @NonNull
                        @Override
                        public Collection<String> supportedSchemes() {
                            return Collections.singleton("res");
                        }
                    });

                }))
                .build();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        getSupportActionBar().setTitle("GG修改器教程");

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        contentTextView = findViewById(R.id.content_text);
        chapterRecyclerView = findViewById(R.id.chapter_recycler);
        scrollView = findViewById(R.id.scroll_view);
        draggableScrollBar = findViewById(R.id.draggable_scrollbar);

        searchBar = findViewById(R.id.search_bar);
        searchInput = findViewById(R.id.search_input);
        searchPrev = findViewById(R.id.search_prev);
        searchNext = findViewById(R.id.search_next);
        searchClose = findViewById(R.id.search_close);
        searchCounter = findViewById(R.id.search_counter);
        searchCurrentChapter = findViewById(R.id.search_current_chapter);
        searchAllChapters = findViewById(R.id.search_all_chapters);

        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(navigationView));

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View view = scrollView.getChildAt(scrollView.getChildCount() - 1);
            int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
            if (diff <= 10 && diff >= 0) {
                if (currentChapterIndex < chapters.size() - 1) {
                    showNextChapterHint();
                }
            }
        });

        scrollView.post(() -> draggableScrollBar.attachToScrollView(scrollView));
    }

    private void initChapters() {
        chapters = new ArrayList<>();

        chapters.add(new TutorialChapter(
                "第一章：添加已有函数",
                "# 函数添加原理\n"+
                        "# \n"+
                        "# 函数添加\n"+
                        "本软件提供了一件添加功能，可以直接使用，如果你想自己添加，可以通过以下步骤\n"+
                        "1. 下载并学习如何使用mt管理器（安卓端）或apktool（windows端）或jeb2（windows端）等任意一款apk反编译、修改工具  \n"+
                        "2. 打开修改器apk的classes.dex，并打开android.ext.Script，找到  \n"+
                        "3. 在android.ext内新建一个类，名称为Script$xxx其中xxx为函数的名称 ，将 \n"+
                        "\n"
        ));

        chapters.add(new TutorialChapter(
                "第一章：制作自定义函数",
                "#smali与java\n"+
                        "#\n"+
                        "#\n"+
                        "#\n"
        ));
    }


    private void setupRecyclerView() {
        chapterRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chapterAdapter = new TutorialChapterAdapter(chapters, position -> {
            displayChapter(position);
            drawerLayout.closeDrawers();
        });
        chapterRecyclerView.setAdapter(chapterAdapter);

        searchCurrentChapter.setOnClickListener(v -> {
            isSearchingAllChapters = false;
            showSearchBar();
        });

        searchAllChapters.setOnClickListener(v -> {
            isSearchingAllChapters = true;
            showSearchBar();
        });
    }

    private void displayChapter(int index) {
        if (index >= 0 && index < chapters.size()) {
            currentChapterIndex = index;
            TutorialChapter chapter = chapters.get(index);
            markwon.setMarkdown(contentTextView, chapter.getContent());
            chapterAdapter.setSelectedPosition(index);
            getSupportActionBar().setTitle(chapter.getTitle());

            scrollView.post(() -> scrollView.smoothScrollTo(0, 0));

            contentTextView.post(() -> {
                draggableScrollBar.invalidate();
                draggableScrollBar.requestLayout();
            });

            if (searchBar.getVisibility() == View.VISIBLE) {
                performSearch(searchInput.getText().toString());
            }
        }
    }

    private void showNextChapterHint() {
        com.google.android.material.snackbar.Snackbar snackbar =
                com.google.android.material.snackbar.Snackbar.make(
                        findViewById(android.R.id.content),
                        "已到达本章末尾",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                );
        snackbar.setAction("下一章", v -> displayChapter(currentChapterIndex + 1));
        snackbar.setActionTextColor(getColor(R.color.purple_500));
        snackbar.show();
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchPrev.setOnClickListener(v -> navigateSearch(-1));
        searchNext.setOnClickListener(v -> navigateSearch(1));
        searchClose.setOnClickListener(v -> hideSearchBar());
    }

    private void showSearchBar() {
        searchBar.setVisibility(View.VISIBLE);
        searchInput.requestFocus();
        searchInput.postDelayed(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchInput, 0);
            }
        }, 100);
    }

    private void hideSearchBar() {
        searchBar.setVisibility(View.GONE);
        searchInput.setText("");
        searchPositions.clear();
        currentSearchIndex = -1;

        TutorialChapter chapter = chapters.get(currentChapterIndex);
        markwon.setMarkdown(contentTextView, chapter.getContent());

        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private void performSearch(String query) {
        searchPositions.clear();
        currentSearchIndex = -1;

        if (query.isEmpty()) {
            TutorialChapter chapter = chapters.get(currentChapterIndex);
            markwon.setMarkdown(contentTextView, chapter.getContent());
            searchPrev.setVisibility(View.GONE);
            searchNext.setVisibility(View.GONE);
            searchCounter.setVisibility(View.GONE);
            return;
        }

        String content;
        if (isSearchingAllChapters) {
            StringBuilder allContent = new StringBuilder();
            for (TutorialChapter chapter : chapters) {
                allContent.append(chapter.getContent()).append("\n\n");
            }
            content = allContent.toString();
        } else {
            content = chapters.get(currentChapterIndex).getContent();
        }

        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            searchPositions.add(matcher.start());
        }

        if (!searchPositions.isEmpty()) {
            currentSearchIndex = 0;
            highlightSearchResults(query);
            updateSearchCounter();
            searchPrev.setVisibility(View.VISIBLE);
            searchNext.setVisibility(View.VISIBLE);
            searchCounter.setVisibility(View.VISIBLE);
        } else {
            TutorialChapter chapter = chapters.get(currentChapterIndex);
            markwon.setMarkdown(contentTextView, chapter.getContent());
            searchPrev.setVisibility(View.GONE);
            searchNext.setVisibility(View.GONE);
            searchCounter.setVisibility(View.GONE);
            Toast.makeText(this, "未找到匹配内容", Toast.LENGTH_SHORT).show();
        }
    }

    private void highlightSearchResults(String query) {
        String content = contentTextView.getText().toString();
        SpannableString spannableString = new SpannableString(content);

        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        int index = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            int color = (index == currentSearchIndex) ?
                    Color.parseColor("#FF6200EE") : Color.parseColor("#FFBB86FC");
            spannableString.setSpan(
                    new BackgroundColorSpan(color), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            index++;
        }

        contentTextView.setText(spannableString);

        if (currentSearchIndex >= 0 && currentSearchIndex < searchPositions.size()) {
            scrollToPosition(searchPositions.get(currentSearchIndex));
        }
    }

    private void navigateSearch(int direction) {
        if (searchPositions.isEmpty()) return;
        currentSearchIndex += direction;
        if (currentSearchIndex < 0) {
            currentSearchIndex = searchPositions.size() - 1;
        } else if (currentSearchIndex >= searchPositions.size()) {
            currentSearchIndex = 0;
        }
        highlightSearchResults(searchInput.getText().toString());
        updateSearchCounter();
    }

    private void updateSearchCounter() {
        if (!searchPositions.isEmpty()) {
            searchCounter.setText(String.format("%d/%d",
                    currentSearchIndex + 1, searchPositions.size()));
        }
    }

    private void scrollToPosition(int position) {
        float charHeight = contentTextView.getLineHeight();
        int line = contentTextView.getLayout().getLineForOffset(position);
        int scrollY = (int) (line * charHeight);
        scrollView.smoothScrollTo(0, Math.max(0, scrollY - scrollView.getHeight() / 2));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tutorial, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            isSearchingAllChapters = false;
            showSearchBar();
            return true;
        } else if (id == R.id.action_exit) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else if (searchBar.getVisibility() == View.VISIBLE) {
            hideSearchBar();
        } else {
            super.onBackPressed();
        }
    }
}
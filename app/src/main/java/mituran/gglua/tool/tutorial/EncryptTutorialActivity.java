package mituran.gglua.tool.tutorial;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import io.noties.markwon.Markwon;
import mituran.gglua.tool.DraggableScrollBar;
import mituran.gglua.tool.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EncryptTutorialActivity extends AppCompatActivity {

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

        // 初始化Markwon
        markwon = Markwon.create(this);

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

        // 搜索相关视图
        searchBar = findViewById(R.id.search_bar);
        searchInput = findViewById(R.id.search_input);
        searchPrev = findViewById(R.id.search_prev);
        searchNext = findViewById(R.id.search_next);
        searchClose = findViewById(R.id.search_close);
        searchCounter = findViewById(R.id.search_counter);
        searchCurrentChapter = findViewById(R.id.search_current_chapter);
        searchAllChapters = findViewById(R.id.search_all_chapters);

        // 工具栏点击打开侧边栏
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(navigationView));

        // 监听滚动，检测是否到达底部
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View view = scrollView.getChildAt(scrollView.getChildCount() - 1);
            int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));

            // 如果滚动到底部（差值小于10像素）
            if (diff <= 10 && diff >= 0) {
                // 如果不是最后一章，显示提示或自动跳转
                if (currentChapterIndex < chapters.size() - 1) {
                    showNextChapterHint();
                }
            }
        });

        // 延迟绑定滚动条，确保视图已经测量完成
        scrollView.post(() -> draggableScrollBar.attachToScrollView(scrollView));
    }

    private void initChapters() {
        chapters = new ArrayList<>();

        chapters.add(new TutorialChapter(
                "第一章：加密的目的",
                "# 为什么要加密Lua脚本？\n\n" +
                        "## 加密的必要性\n\n" +
                        "在开发GameGuardian脚本时，加密脚本可以：\n\n" +
                        "- **保护知识产权**：防止脚本被他人直接复制和盗用\n" +
                        "- **防止逆向分析**：增加破解难度，保护核心算法\n" +
                        "- **商业用途**：为付费脚本提供基础保护\n" +
                        "- **防止篡改**：避免脚本被恶意修改后传播\n\n" +
                        "## 加密的局限性\n\n" +
                        "需要注意的是：\n\n" +
                        "- 没有绝对安全的加密方式\n" +
                        "- 加密会增加脚本执行开销，即浪费更多时间\n" +
                        "- 过度加密可能影响用户体验，如加载时间过长\n" +
                        "- 加密不能替代良好的代码设计\n\n"+
                        "\n\n\n\n\n\n\n\n"
        ));

        chapters.add(new TutorialChapter(
                "第二章：Lua字节码",
                "# Lua字节码基础\n\n" +
                        "## 什么是Lua字节码？\n\n" +
                        "Lua字节码是Lua源代码编译后的二进制格式，类似于Java的.class文件。\n\n" +
                        "## 字节码的特点\n\n" +
                        "- **体积更小**：相比源代码更紧凑\n" +
                        "- **加载更快**：跳过语法分析阶段\n" +
                        "- **轻度混淆**：不直接可读，但可以被反编译\n\n" +
                        "## 编译为字节码\n\n" +
                        "```lua\n" +
                        "-- lua实现使用loadstring编译代码\n" +
                        "local code = [[print('Hello World')]]\n" +
                        "local compiled = string.dump(loadstring(code))\n\n" +
                        "-- 执行字节码\n" +
                        "local func = load(compiled)\n" +
                        "func()\n" +
                        "```\n\n" +
                        "如以下lua代码:\n\n" +
                        "```lua\n" +
                        "a=\"请输入密码\"\n" +
                        "b=\"1284\"\n" +
                        "function right()\n" +
                        " d=\"成功\"\n" +
                        " print(d)\n" +
                        " gg.setRanges(gg.REGION_JAVA_HEAP)\n" +
                        "  gg.searchNumber(\"360;0;0;1;-1;1;2;2;0\", gg.TYPE_FLOAT, false, gg.SIGN_EQUAL, 0, -1)\n" +
                        "  gg.searchNumber(\"1\", gg.TYPE_FLOAT, false, gg.SIGN_EQUAL, 0, -1)\n" +
                        "  gg.getResults(14)\n" +
                        "  gg.editAll(\"1000\", gg.TYPE_FLOAT)\n" +
                        "  gg.searchNumber(\"1000\", gg.TYPE_FLOAT, false, gg.SIGN_EQUAL, 0, -1)\n" +
                        "  gg.getResults(14)\n" +
                        "  gg.editAll(\"1\", gg.TYPE_FLOAT)\n" +
                        "  gg.toast(\"开启成功\")\n" +
                        "end\n" +
                        "c=gg.prompt({a},{\"\"},{\"Text\"})\n" +
                        "if c[1]==b then\n" +
                        " right()\n" +
                        "else\n" +
                        " os.exit()\n" +
                        "end\n" +
                        "```\n\n" +
                        "编译后得到如下字节码（已转义）：\n\n" +
                        "```lua\n" +
                        "\027LuaR\001\004\004\004\b\025\194\141\n\026\n\001\005\029\b@@\194\136\b\194\141@\194\136%\b\194\136\006\194\141A\\a\194\141AK\194\136\194\141@d@\194\136\194\141\194\136\194\141\002\194\136@\194\136\194\141\194\136\001A\002\194\136@\194\136\029\194\136\002\b\194\136\194\141\006@A\\a\194\141BF\194\136@\024@\023\194\136\194\141\006A\029@\194\136\023\194\136\194\141\006\194\141B\\aC\029@\194\136\031\194\141\n\004\002a\004\020请输入密码\004\002b\004\0051284\004\006right\004\002c\004\003gg\004\\aprompt\004\001\004\005Text\003\194\191?\004\003os\004\005exit\001\003\017\\aC\b@@\194\136\006\194\141@F@\029@\001\006\194\141@\\aAF\194\136@G@\194\136\029@\001\006\194\141@\\a\194\141AA\194\136\001\194\136\194\141@\194\136B\001\194\136\006\194\141@\\aAB\002A\194\136\002\194\136\194\141\002\029@\194\136\003\006\194\141@\\a\194\141AA\003\194\136\194\141@\194\136B\001\194\136\006\194\141@\\aAB\002A\194\136\002\194\136\194\141\002\029@\194\136\003\006\194\141@\\a@CA\194\136\003\029@\001\006\194\141@\\a\194\141CA\004\194\136\194\141@\194\136B\001\029@\194\136\001\006\194\141@\\a\194\141AA\004\194\136\194\141@\194\136B\001\194\136\006\194\141@\\aAB\002A\194\136\002\194\136\194\141\002\029@\194\136\003\006\194\141@\\a@CA\194\136\003\029@\001\006\194\141@\\a\194\141CA\003\194\136\194\141@\194\136B\001\029@\194\136\001\006\194\141@\\a@DA\194\136\004\029@\001\031\194\136\147\004\002d\004\\a成功\004\006print\004\003gg\004\017setRanges\004\033REGION_JAVA_HEAP\004\023searchNumber\004!360;0;0;1;-1;1;2;2;0\004\rtYPE_FLOAT\004\rsIGN_EQUAL\003\003\194\191\004\0021\004\rgetResults\003,@\004\016editAll\004\0051000\004\006toast\004\017开启成功\001\001\001" +
                        "```\n\n" +
                        "## 注意事项\n\n" +
                        "字节码**不是**真正的加密，只能提供基础的混淆效果，干扰对代码的直接分析，但是可以容易的反编译。\n\n"
        ));


        chapters.add(new TutorialChapter(
                "第三章：字符串加密",
                "# 常用的加密技术\n\n"

        ));

        chapters.add(new TutorialChapter(
                "第四章：添加防御",
                "# 多层加密技术\n\n"

        ));

        chapters.add(new TutorialChapter(
                "第五章：lua实现脚本加密",
                "# 完整的加密脚本示例\n\n"

        ));

        chapters.add(new TutorialChapter(
                "第六章：制作加密工具",
                "# 增强安全性的技巧\n\n"

        ));

        chapters.add(new TutorialChapter(
                "第七章：总结",
                "# 加密脚本的最佳实践\n\n"
        ));
    }

    private void setupRecyclerView() {
        chapterRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chapterAdapter = new TutorialChapterAdapter(chapters, position -> {
            displayChapter(position);
            drawerLayout.closeDrawers();
        });
        chapterRecyclerView.setAdapter(chapterAdapter);

        // 设置搜索按钮
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

            // 滚动到顶部
            scrollView.post(() -> scrollView.smoothScrollTo(0, 0));

            // 更新滚动条（内容渲染后）
            contentTextView.post(() -> {
                draggableScrollBar.invalidate();
                draggableScrollBar.requestLayout();
            });

            // 如果正在搜索，重新执行搜索
            if (searchBar.getVisibility() == View.VISIBLE) {
                performSearch(searchInput.getText().toString());
            }
        }
    }

    private void showNextChapterHint() {
        // 使用 Snackbar 显示提示
        com.google.android.material.snackbar.Snackbar snackbar =
                com.google.android.material.snackbar.Snackbar.make(
                        findViewById(android.R.id.content),
                        "已到达本章末尾",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                );

        snackbar.setAction("下一章", v -> {
            displayChapter(currentChapterIndex + 1);
        });

        snackbar.setActionTextColor(getColor(R.color.purple_500));
        snackbar.show();
    }

    private void setupSearch() {
        // 搜索输入监听
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

        // 上一个结果
        searchPrev.setOnClickListener(v -> navigateSearch(-1));

        // 下一个结果
        searchNext.setOnClickListener(v -> navigateSearch(1));

        // 关闭搜索
        searchClose.setOnClickListener(v -> hideSearchBar());
    }

    private void showSearchBar() {
        searchBar.setVisibility(View.VISIBLE);
        searchInput.requestFocus();
        // 显示软键盘
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

        // 清除高亮 - 重新渲染markdown
        TutorialChapter chapter = chapters.get(currentChapterIndex);
        markwon.setMarkdown(contentTextView, chapter.getContent());

        // 隐藏软键盘
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
            // 清除高亮 - 重新渲染markdown
            TutorialChapter chapter = chapters.get(currentChapterIndex);
            markwon.setMarkdown(contentTextView, chapter.getContent());
            searchPrev.setVisibility(View.GONE);
            searchNext.setVisibility(View.GONE);
            searchCounter.setVisibility(View.GONE);
            return;
        }

        String content;
        if (isSearchingAllChapters) {
            // 全文搜索：搜索所有章节
            StringBuilder allContent = new StringBuilder();
            for (TutorialChapter chapter : chapters) {
                allContent.append(chapter.getContent()).append("\n\n");
            }
            content = allContent.toString();
        } else {
            // 当前章节搜索
            content = chapters.get(currentChapterIndex).getContent();
        }

        // 查找所有匹配位置
        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            searchPositions.add(matcher.start());
        }

        // 显示搜索结果
        if (!searchPositions.isEmpty()) {
            currentSearchIndex = 0;
            highlightSearchResults(query);
            updateSearchCounter();
            searchPrev.setVisibility(View.VISIBLE);
            searchNext.setVisibility(View.VISIBLE);
            searchCounter.setVisibility(View.VISIBLE);
        } else {
            // 重新渲染markdown
            TutorialChapter chapter = chapters.get(currentChapterIndex);
            markwon.setMarkdown(contentTextView, chapter.getContent());
            searchPrev.setVisibility(View.GONE);
            searchNext.setVisibility(View.GONE);
            searchCounter.setVisibility(View.GONE);
            Toast.makeText(this, "未找到匹配内容", Toast.LENGTH_SHORT).show();
        }
    }

    private void highlightSearchResults(String query) {
        TutorialChapter chapter = chapters.get(currentChapterIndex);
        String content = contentTextView.getText().toString();
        SpannableString spannableString = new SpannableString(content);

        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        int index = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // 当前高亮项使用不同颜色
            int color = (index == currentSearchIndex) ?
                    Color.parseColor("#FF6200EE") : Color.parseColor("#FFBB86FC");

            spannableString.setSpan(
                    new BackgroundColorSpan(color),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            index++;
        }

        contentTextView.setText(spannableString);

        // 滚动到当前高亮位置
        if (currentSearchIndex >= 0 && currentSearchIndex < searchPositions.size()) {
            scrollToPosition(searchPositions.get(currentSearchIndex));
        }
    }

    private void navigateSearch(int direction) {
        if (searchPositions.isEmpty()) return;

        currentSearchIndex += direction;

        // 循环导航
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
            String counterText = String.format("%d/%d",
                    currentSearchIndex + 1, searchPositions.size());
            searchCounter.setText(counterText);
        }
    }

    private void scrollToPosition(int position) {
        // 计算字符位置对应的像素位置（近似）
        float charHeight = contentTextView.getLineHeight();
        int line = contentTextView.getLayout().getLineForOffset(position);
        int scrollY = (int) (line * charHeight);

        // 滚动到该位置（居中显示）
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
            // 显示搜索栏
            isSearchingAllChapters = false;
            showSearchBar();
            return true;
        } else if (id == R.id.action_exit) {
            // 退出按钮
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
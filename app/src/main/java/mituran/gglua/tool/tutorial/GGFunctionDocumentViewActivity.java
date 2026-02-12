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

public class GGFunctionDocumentViewActivity extends AppCompatActivity {

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

        chapters.add(new TutorialChapter("GG修改器96版官方文档", """
# GG修改器96版官方文档

## 前言

**翻译者：石乐志**

翻译了十二个小时，主要是排版有点废时间，再加上英文水平太差。如有错漏，可以自行改正，如果实在受不了，有种，你顺着网线来打我呀。

---

## 概论

欢迎来到 GameGuardian 脚本文档。你可以在 GameGuardian 中编写使用多种多样的破解脚本，本文档描述和介绍了能在 GameGuardian 脚本文件中出现和使用的函数和类。

你也可以在我们的论坛参与讨论：http://gameguardian.net/forum/topic/17447-lua-scripting/

### 我们应该怎么样开始学习？

我们通过"边学边做"的方式开始编写脚本，往往效果很好，因为这样我们学得既快速，又富有乐趣。可以通过我们的站点下载脚本，开始修改和学习以作为你们学习和成长的起点：http://gameguardian.net/forum/files/category/6-lua-scripts/

反复试验运行的脚本的效果，将使您更容易理解底层的关键思想和概念，然后根据自己的需要回头阅读文档的相关内容。

你们也可能想要阅读 Lua 文档，并在 GameGuardian 论坛开始提问。

### 列出所有 API 方法

你们可以在 GG 修改器列表中使用下面的代码，列出所有的方法：

```lua
print(gg)
```

---

## 更多资源信息

### 视频教程
用 GameGuardian 破解的相关视频教程例子，可以见官方文档的 video 导航条目中：https://gameguardian.net/forum/gallery/category/2-video-tutorials/

### Lua 学习资源
GG 修改器脚本是基于 Lua 编程语言，下面是关于 Lua 编程语言学习的补充：

- **Lua 官方网站**：http://www.lua.org/about.html
- **Lua 官方文档**：http://www.lua.org/docs.html（该文档提供了所有 Lua 编程语言的所有信息，是不可缺少的用于提升我们 GG 脚本编写水平的资源）
- **脚本例子**：http://gameguardian.net/forum/files/category/6-lua-scripts/

---

## API 类：都属于 gg 类成员

### 函数接口

#### alert

**函数原型：**
```lua
int alert(
    string text,
    string positive = 'ok',
    string negative = nil,
    string neutral = nil
)
```

**函数功能：** 显示一个包含几个按钮的对话框。

**函数参数：**
- `text`：对话框文本提示信息
- `positive`：positive 按钮的文本信息，显示在对话框的最右边，选择该按钮返回值 1
- `negative`：negative 按钮的文本信息，显示在对话框的右边第二个位置，并靠近 positive 按钮，选择该按钮返回值 2
- `neutral`：neutral 按钮的文本信息，位置在最左边，离上面两个按钮距离较远，选择该按钮返回值 3

**返回值：** 对话框取消返回值 0，选择 positive 按钮返回值 1，选择 negative 按钮返回值 2，选择 neutral 按钮返回值 3。

**使用例子：**
```lua
gg.alert("script ended") -- 只显示一个 ok 按钮
gg.alert("script ended", "Yes") -- 最右边只显示一个 yes 按钮
gg.alert('A or B?', 'A', 'B') -- 从左至右显示按钮 B，显示按钮 A
gg.alert('A or B or C?', 'A', 'B', 'C') -- 显示按钮从左至右，C, B, A
gg.alert('A or C?', 'A', nil, 'C') -- 最左边和最右边分别显示按钮 C 和按钮 A。不适用按钮可以赋值为 nil
```

---

#### bytes

**函数原型：**
```lua
string bytes(
    string text,
    string encoding = 'UTF-8'
)
```

**函数功能：** 获得指定编码文本的文本字节。

**参数：**
- `text`：文本内容
- `encoding`：可选的值，'ISO-8859-1', 'US-ASCII', 'UTF-16', 'UTF-16BE', 'UTF-16LE', 'UTF-8'

**返回值：** 返回第一个参数的字符串，以指定编码文本的字节流表。索引 0 开始，每个索引存储一个字节。如果是 16 位编码的格式，那么表的第一个字节是字符码值，第二个字节是 16 位的高八位。

**例子：**
```lua
print('UTF-8', gg.bytes('example'))
print('UTF-8', gg.bytes('example', 'UTF-8'))
print('UTF-16', gg.bytes('example', 'UTF-16LE'))
```

---

#### choice

**函数原型：**
```lua
mixed choice(
    table items,
    string selected = nil,
    string message = nil
)
```

**函数功能：** 从列表中显示一个选择对话框。该列表由 table 类型参数的 item 变量组成。

**函数参数：**
- `items`：列表类型（{'A', 'B', 'C', "D"}）
- `selected`：如果没有指定或指定值为 nil，那么这列表不会默认选择指定列表项
- `message`：选择列表框的标题

**返回值：** 如果列表框取消选择返回 nil，否则返回选择项的索引值。

**例子：**
```lua
print('1: ', gg.choice({'A', 'B', 'C', 'D'}))
print('2: ', gg.choice({'A', 'B', 'C', 'D'}, 2))
print('3: ', gg.choice({'A', 'B', 'C', 'D'}, 3, 'Select letter:'))
print('4: ', gg.choice({'A', 'B', 'C', 'D'}, nil, 'Select letter:'))
```

---

#### clearResults

**函数原型：**
```lua
clearResults()
```

**函数功能：** 清空搜索到的值的列表。

**无参数**

**无返回值**

---

#### copyMemory

**函数原型：**
```lua
mixed copyMemory(
    long from,
    long to,
    int bytes
)
```

**函数功能：** 复制内存。

**函数参数：**
- `from`：需要复制内存的起始地址
- `to`：需要复制内存的结束地址
- `bytes`：需要复制内存单元的数量，数字 8 表示复制 8 个字节

**返回值：** 成功返回真，失败返回字符串 "error"。

**例子代码：**
```lua
print('copyMemory:', gg.copyMemory(0x9000, 0x9010, 3))
-- copies 3 bytes 0x9000-0x9002 to 0x9010-0x9012
```

---

#### copyText

**函数原型：**
```lua
copyText(
    string text,
    bool fixLocale = true
)
```

**函数功能：** 复制文本到剪贴板。

**函数参数：**
- `text`：需要复制的文本
- `fixLocale`：是否禁用固定区域界限分隔符的标志。true 为禁用，false 为不禁用

**无返回值**

**使用例子：**
```lua
gg.copyText('1,234,567.890') -- Will copy '1 234 567,890'
gg.copyText('1,234,567.890', true) -- Will copy '1 234 567,890'
gg.copyText('1,234,567.890', false) -- Will copy '1,234,567.890'
```

---

#### dumpMemory

**函数原型：**
```lua
mixed dumpMemory(
    long from,
    long to,
    string dir
)
```

**函数功能：** 复制指定内存单元到文件。

**函数参数：**
- `from`：起始地址
- `to`：结束地址
- `dir`：想要保存输出文件的目录

**函数返回值：** 成功返回 true，失败返回字符串 "error"。

**函数例子：**
```lua
print('dumpMemory:', gg.dumpMemory(0x9000, 0x9010, '/sdcard/dump'))
-- dump at least one memory page into the dir '/sdcard/dump'
```

---

#### editAll

**函数原型：**
```lua
mixed editAll(
    string value,
    int type
)
```

**函数功能：** 编辑搜索的结果值。编辑全部结果值。调用该方法之前，你必须已经通过 getResult 导入了结果值，只有值的指定类型才将会用于结果值。

**函数参数：**
- `value`：将要编辑数据值的字符串形式
- `type`：符号常量，指明数据类型。来源于 TYPE_* 指针。后面会接受类型符号常量

**函数返回值：** 成功返回改变的结果值的项目数，失败返回字符串 error。

**例子代码：**
```lua
gg.searchNumber('10', gg.TYPE_DWORD)
gg.getResults(5)
gg.editAll('15', gg.TYPE_DWORD)

-- with float:
gg.searchNumber('10.1', gg.TYPE_FLOAT)
gg.getResults(5)
gg.editAll('15.2', gg.TYPE_FLOAT)

-- with XOR mode
gg.searchNumber('10X4', gg.TYPE_DWORD)
gg.getResults(5)
gg.editAll('15X4', gg.TYPE_DWORD)
```

---

#### getFile

**函数原型：**
```lua
string getFile()
```

**函数功能：** 获取当前正在运行的脚本的名字。

**函数参数：** 无

**函数返回值：** 成功返回当前脚本的文件名。例如：'/sdcard/Notes/gg.example.lua'

---

#### getLine

**函数原型：**
```lua
int getLine()
```

**函数功能：** 获取将要执行的脚本的当前行的行号。

**函数参数：** 无

**函数返回值：** 返回将要被执行脚本的当前行号。

**函数例子：**
```lua
print(gg.getLine()) -- 24
```

---

#### getLocale

**函数原型：**
```lua
string getLocale()
```

**函数功能：** 获取在 GameGuardian 中的当前选择的字符串本地化情况。

**函数返回值：** 返回当前在 GameGuardian 中当前选择的字符串本地化。返回值 en_US, zh_CN, ru, pt_BR, ar, uk

---

#### getRanges

**函数原型：**
```lua
int getRanges()
```

**函数功能：** 内存区域作为一个 REGION_* 指针标志位遮罩返回。

**函数参数：** 无

**函数返回值：** REGION_* 指针标志遮罩位。

---

#### getRangesList

**函数原型：**
```lua
getRangesList(string filter = '')
```

**函数功能：** 获取选择进程的内存区域的列表。

**函数参数：**
- `filter`：过滤字符串。如果指定该选项，仅仅返回和过滤器相匹配的结果。该选项支持通配符。`^` 表示数据开头，`$` 表示数据结尾，`*` 表示任意数量的任意字符，`?` 表示一个任意字符

**函数返回值：** 返回存储内存区域的一个列表。每个元素是一个列表字段：state, start, end, type, name, internalName。

**例子：**
```lua
print(gg.getRangesList())
print(gg.getRangesList('libc.so'))
print(gg.getRangesList('lib*.so'))
print(gg.getRangesList('^/data/'))
print(gg.getRangesList('.so$'))
```

---

#### getResult

**函数原型：**
```lua
mixed getResults(int maxCount)
```

**函数功能：** 导入结果到结果列表，并作为表返回。

**函数参数：**
- `maxCount`：导入结果的最大数量

**函数返回值：** 成功返回结果列表，失败返回字符串 "error"，每个元素都是一个包含三个关键字的列表：address (long), value (string with a value), flags (one of the constants TYPE_*)。

**函数例子：**
```lua
gg.searchNumber('10', gg.TYPE_DWORD)
local r = gg.getResults(5)
print('First 5 results: ', r)
print('First result: ', r[1])
print('First result address: ', r[1].address)
print('First result value: ', r[1].value)
print('First result type: ', r[1].flags)
```

---

#### getResultCount

**函数原型：**
```lua
long getResultCount()
```

**函数功能：** 获取找到结果的数量。

**函数参数：** 无

**函数返回值：** 找到结果的数量。

**函数例子：**
```lua
gg.searchNumber('10', gg.TYPE_DWORD)
print('Found: ', gg.getResultCount())
```

---

#### getSpeed

**函数原型：**
```lua
double getSpeed()
```

**函数功能：** 从加速中获取当前速度。

**函数参数：** 无

**函数返回值：** 返回从加速中获取的当前速度。

---

#### getTargetInfo

**函数原型：**
```lua
mixed getTargetInfo()
```

**函数功能：** 如果可能的话，获取关于选择进程的信息列表。一系列字段可以是不同的。打印可见和可用字段的结果列表。

可能的字段：firstInstallTime, lastUpdateTime, packageName, sharedUserId, sharedUserLabel, versionCode, versionName, activities (name, label), installer, enabledSetting, backupAgentName, className, dataDir, descriptionRes, flags, icon, labelRes, logo, manageSpaceActivityName, name, nativeLibraryDir, packageName, permission, processName, publicSourceDir, sourceDir, targetSdkVersion, taskAffinity, theme, uid, label。谷歌应用会返回每个字段。

**函数参数：** 无

**函数返回值：** 返回选择进程的信息列表，或返回 nil。

**函数例子：**
```lua
-- check for game version
local v = gg.getTargetInfo()
if v.versionCode ~= 291 then
    print('This script only works with game version 291. You have game version ', v.versionCode, ' Please install version 291 and try again.')
    os.exit() -- 退出程序
end
```

---

#### getTargetPackage

**函数原型：**
```lua
mixed getTargetPackage()
```

**函数功能：** 获取选择进程的包名字。

**函数参数：** 无

**函数返回值：** 返回选择进程的名字字符串。或者返回 nil。例如：'com.blayzegames.iosfps'

---

#### getValues

**函数原型：**
```lua
mixed getValues(table values)
```

**函数功能：** 获取列表的项目的值。

**函数参数：**
- `values`：一个包含很多个列表的列表，该表中的每个表包含 address 和标志字段（其中一个就是 TYPE* 符号常量）

**函数返回值：** 成功返回一个新表，失败返回 "error"，表的每个元素是一个包含三个关键字的表，address (long), value (string with a value), flags (one of the constants TYPE_*)。

**函数例子：**
```lua
gg.searchNumber('10', gg.TYPE_DWORD)
local r = gg.getResults(5) -- load items
r = gg.getValues(r) -- refresh items values
print('First 5 results: ', r)
print('First result: ', r[1])
print('First result address: ', r[1].address)
print('First result value: ', r[1].value)
print('First result type: ', r[1].flags)

local t = {}
t[1] = {}
t[1].address = 0x18004030 -- some desired address
t[1].flags = gg.TYPE_DWORD
t[2] = {}
t[2].address = 0x18004040 -- another desired address
t[2].flags = gg.TYPE_BYTE
t = gg.getValues(t)
print(t)
```

---

#### getValuesRange

**函数原型：**
```lua
mixed getValuesRange(table values)
```

**函数功能：** 获取传递的表参数的值的内存区域。

**函数参数：**
- `values`：参数是表类型，该表既可以是地址列表，也可以是一个地址字段的列表

**函数返回值：** 返回一个列表，该列表的每个键，来源于参数的表值，将和一个短区域代码相联系（例如 ch），失败返回字符串 error。

**函数例子：**
```lua
print('1: ', gg.getValuesRange({0x9000, 0x9010, 0x9020, 0x9030}))
-- table as a list of addresses

gg.searchNumber('10', gg.TYPE_DWORD)
local r = gg.getResults(5)
print('2: ', r, gg.getValuesRange(r))
-- table as a list of tables with the address field
```

---

#### gotoAddress

**函数原型：**
```lua
gotoAddress(long address)
```

**函数功能：** 在内存编辑器中跳转到指定地址。

**函数参数：**
- `address`：希望跳转的地址

**返回值：** 无

---

#### isPackageInstalled

**函数原型：**
```lua
bool isPackageInstalled(string pkg)
```

**函数功能：** 通过安装包名字判断指定应用程序在操作系统上是否安装。

**函数参数：**
- `pkg`：安装包名字

**函数返回值：** 如果已安装返回 true，否则返回 false。

**函数例子：**
```lua
print('Game installed:', gg.isPackageInstalled('com.blayzegames.iosfps'))
```

---

#### isProcessPaused

**函数原型：**
```lua
bool isProcessPaused()
```

**函数功能：** 获取指定进程的暂停状态。

**函数返回值：** 如果进程已暂停返回 true，否则返回 false。

---

#### isVisible

**函数原型：**
```lua
bool isVisible()
```

**函数功能：** 检查 GameGuardian 的 UI 是否打开。

**函数参数：** 无

**返回值：** 如果 GameGuardian 修改器的 UI 是打开的返回 true，否则返回 false。

---

#### loadList

**函数原型：**
```lua
mixed loadList(
    string file,
    int flags = 0
)
```

**函数功能：** 从文件中导入保存列表。

**函数参数：**
- `file`：要导入到列表的文件
- `flags`：系列标志 LOAD_*

**函数返回值：** 成功 true，失败返回 "error"。

**例子：**
```lua
print('loadList:', gg.loadList('/sdcard/Notes/gg.victum.txt'))
print('loadList:', gg.loadList('/sdcard/Notes/gg.victum.txt', 0))
print('loadList:', gg.loadList('/sdcard/Notes/gg.victum.txt', gg.LOAD_APPEND))
print('loadList:', gg.loadList('/sdcard/Notes/gg.victum.txt', gg.LOAD_VALUES_FREEZE))
print('loadList:', gg.loadList('/sdcard/Notes/gg.victum.txt', gg.LOAD_APPEND | gg.LOAD_VALUES))
```

---

#### multiChoice

**函数原型：**
```lua
mixed multiChoice(
    table items,
    table selection = {},
    string message = nil
)
```

**函数功能：** 显示多项选择对话框。

**函数参数：**
- `items`：列表类型，需要显示选择项目
- `selection`：列表类型，给每个选择项指定同样的默认选择状态。如果 key 是未发现的那么元素将是未选择的
- `message`：多项选择框的标题

**函数返回值：** 如果不选择，直接取消选择对话框，会返回 nil，否则返回一个表，表里的元素是一个项目关键字和值 true。

**函数例子：**
```lua
print('1: ', gg.multiChoice({'A', 'B', 'C', 'D'}))
-- show list of 4 items without checked items

print('2: ', gg.multiChoice({'A', 'B', 'C', 'D'}, {[2]=true, [4]=true}))
-- show list of 4 items with checked 2 and 4 items

print('3: ', gg.multiChoice({'A', 'B', 'C', 'D'}, {[3]=true}, 'Select letter:'))
-- show list of 4 items with checked 3 item and message

print('4: ', gg.multiChoice({'A', 'B', 'C', 'D'}, {}, 'Select letter:'))
-- show list of 4 items without checked items and message
```

---

#### processKill

**函数原型：**
```lua
bool processKill()
```

**函数功能：** 暴力杀掉选择进程，注意该操作可能使得该进程的数据丢失。

**函数参数：** 无

**函数返回值：** 成功 true，失败 false。

---

#### processPause

**函数原型：**
```lua
bool processPause()
```

**函数功能：** 暂停选择程序进程。

**函数参数：** 无

**函数返回值：** 成功 true，失败 false。

---

#### processResume

**函数原型：**
```lua
bool processResume()
```

**函数功能：** 如果程序进程被暂停，该操作恢复程序进程。

**函数参数：** 无

**函数返回值：** 成功 true，失败 false。

---

#### processToggle

**函数原型：**
```lua
bool processToggle()
```

**函数功能：** 切换选择进程的暂停状态。如果进程是暂停的，就恢复进程，否则暂停进程。

**函数参数：** 无

**函数返回值：** 成功返回 true，失败返回 false。

---

#### prompt

**函数原型：**
```lua
mixed prompt(
    table prompts,
    table defaults = {},
    table types = {}
)
```

**函数功能：** 显示数据入口对话框。对于字段的顺序，提示必须是数字数组。

**函数参数：**
- `prompts`：列表类型，存放着指定键和对输入字段的描述
- `defaults`：列表类型，存放着，给每个键提示的默认值
- `types`：列表类型，每个键提示的指定数据类型，可用的值：'number', 'text', 'path', 'file', 'setting', 'speed', 'checkbox'，从类型依赖于输入字段附近的附加元素的输出

**函数返回值：** 对话框取消，返回 nil，否则返回一个表，存放着提示和 keys，和从输入字段中获得的值。

**代码例子：**
```lua
print('prompt 1: ', gg.prompt(
    {'ask any', 'ask num', 'ask text', 'ask path', 'ask file', 'ask set', 'ask speed', 'checked', 'not checked'},
    {[1]='any val', [7]=123, [6]=-0.34, [8]=true},
    {[2]='number', [3]='text', [4]='path', [5]='file', [6]='setting', [7]='speed', [8]='checkbox', [9]='checkbox'}
))

print('prompt 2: ', gg.prompt(
    {'ask any', 'ask num', 'ask text', 'ask path', 'ask file', 'ask set', 'ask speed', 'check'},
    {[1]='any val', [7]=123, [6]=-0.34}
))

print('prompt 3: ', gg.prompt(
    {'ask any', 'ask num', 'ask text', 'ask path', 'ask file', 'ask set', 'ask speed', 'check'}
))
```

---

#### removeResults

**函数原型：**
```lua
mixed removeResults(table results)
```

**函数功能：** 从列出的发现结果的列表中，移除结果值。

**函数参数：**
- `result`：表类型，每个元素也是一个表类型，该表包含了地址，flags 字段（其中之一是符号常量 TYPE_*）

**返回值：** 成功返回 true，其它返回 error。

**例子代码：**
```lua
gg.searchNumber('10', gg.TYPE_DWORD)
local r = gg.getResults(5)
print('Remove first 5 results: ', gg.removeResults(r))
```

---

#### require

**函数原型：**
```lua
require(
    string version = nil,
    int build = 0
)
```

**函数功能：** 检查 GameGuardian 的版本号。如果这个版本号低于需要的版本号，那么脚本将结束，并提示更新 GameGuardian 版本。

**函数参数：**
- `version`：最低需求版本号
- `build`：运行脚本的最小版本号

**函数返回值：** 无

**例子代码：**
```lua
gg.require('8.31.1')
gg.require('8.31.1', 5645)
gg.require(nil, 5645)
```

---

#### saveList

**函数原型：**
```lua
mixed saveList(
    string file,
    int flags = 0
)
```

**函数功能：** 将保存列表的内容保存到文件。

**函数参数：**
- `file`：将内容将要保存到的文件
- `flags`：SAVE_* 系列保存标志

**函数返回值：** 成功 true，失败 "error"。

**例子代码：**
```lua
print('saveList:', gg.saveList('/sdcard/Notes/gg.victum.txt'))
print('saveList:', gg.saveList('/sdcard/Notes/gg.victum.txt', 0))
print('saveList:', gg.saveList('/sdcard/Notes/gg.victum.txt', gg.SAVE_AS_TEXT))
```

---

#### searchAddress

**函数原型：**
```lua
mixed searchAddress(
    string text,
    long mask = -1,
    int type = gg.TYPE_AUTO,
    int sign = gg.SIGN_EQUAL,
    long memoryFrom = 0,
    long memoryTo = -1
)
```

**函数功能：** 根据指定参数，执行一个地址搜索。如果在结果列表中没有搜索到结果值，将执行一个新的搜索，否则重定义并重新搜索。

**函数参数：**
- `text`：搜索字符串，格式跟在 GameGuardian 的 GUI 中操作的字符串一样
- `mask`：遮罩掩
码默认是 -1 (0xFFFFFFFFFFFFFFFF)
- `type`：数据类型符号常量 TYPE_*
- `sign`：信号，SIGN_EQUAL 或 SIGN_NOT_EQUAL
- `memoryFrom`：搜索的起始内存地址
- `memoryTo`：搜索的内存的结束地址

**函数返回值：** True 或返回字符串 "error"。

**函数例子：**
```lua
gg.searchAddress('A20', 0xFFFFFFFF)
gg.searchAddress('B20', 0xFF0, gg.TYPE_DWORD, gg.SIGN_NOT_EQUAL)
gg.searchAddress('0B?0', 0xFFF, gg.TYPE_FLOAT)
gg.searchAddress('??F??', 0xBA0, gg.TYPE_BYTE, gg.SIGN_NOT_EQUAL, 0x9000, 0xA09000)
```

---

#### searchFuzzy

**函数原型：**
```lua
mixed searchFuzzy(
    string difference = '0',
    int sign = gg.SIGN_FUZZY_EQUAL,
    int type = gg.TYPE_AUTO,
    long memoryFrom = 0,
    long memoryTo = -1
)
```

**函数功能：** 用指定的参数优化模糊搜索。

**函数参数：**
- `difference`：old 值和 new 值的差值，默认是 "0"
- `sign`：信号常量，符号常量之一是 SIGN_FUZZY_*
- `type`：数据类型符号常量
- `memoryFrom`：搜索的内存单元起始地址
- `memoryTo`：搜索的内存单元的结束地址

**返回值：** 返回 True 或失败返回 "error"。

**例子代码：**
```lua
gg.searchFuzzy()
-- value not changed

gg.searchFuzzy('0', gg.SIGN_FUZZY_NOT_EQUAL)
-- value changed

gg.searchFuzzy('0', gg.SIGN_FUZZY_GREATER)
-- value increased

gg.searchFuzzy('0', gg.SIGN_FUZZY_LESS)
-- value decreased

gg.searchFuzzy('15')
-- value increased by 15

gg.searchFuzzy('-115')
-- value decreased by 115
```

---

#### searchNumber

**函数原型：**
```lua
mixed searchNumber(
    string text,
    int type = gg.TYPE_AUTO,
    bool encrypted = false,
    int sign = gg.SIGN_EQUAL,
    long memoryFrom = 0,
    long memoryTo = -1
)
```

**函数功能：** 根据指定参数，搜索一个数字。在结果列表中如果没有搜索到结果，那么将执行一个新的搜索，否则重新定义搜索。

**函数参数：**
- `text`：搜索字符串，该字符串格式跟 GameGuardian 的 GUI 格式相同
- `type`：数类型，符号常量表示 TYPE_*
- `encrypted`：是否加密搜索
- `sign`：信号，其中一个符号常量 SIGN_*
- `memoryFrom`：搜索内存单元的起始地址
- `memoryTo`：搜索内存单元的结束地址

**函数返回值：** 成功 true，失败返回字符串 error。

**代码例子：**
```lua
gg.searchNumber('10', gg.TYPE_DWORD)
-- number search

gg.searchNumber('-10', gg.TYPE_DWORD, true)
-- encrypted search

gg.searchNumber('10~20', gg.TYPE_DWORD, false, gg.SIGN_NOT_EQUAL)
-- range search

gg.searchNumber('6~7;7;1~2;0;0;0;0;6~8::29', gg.TYPE_DWORD)
-- group search with ranges
```

---

#### setRanges

**函数原型：**
```lua
setRanges(int ranges)
```

**函数功能：** 设置内存区域，通过设置 REGION_* 标志的遮罩（掩码）位。

**函数参数：**
- `ranges`：REGION_* 标志的遮罩位

---

#### setSpeed

**函数原型：**
```lua
mixed setSpeed(double speed)
```

**函数功能：** 根据加速功能设置加速。如果加速没有被导入，那么将会被导入。该调用将会被阻塞。脚本将等待直到加速完全导入为止。

**函数参数：**
- `speed`：你希望填入的速度。范围必须在 [1.0E-9; 1.0E9]

**函数返回值：** 成功返回真，失败返回 error。

---

#### setValues

**函数原型：**
```lua
mixed setValues(table values)
```

**函数参数：**
- `values`：表类型，该表的元素也是表类型，元素包含三个关键字：address (long), value (string with a value), flags (one of the constants TYPE_*)

**返回值：** true 或者字符串 error。

**例子代码：**
```lua
gg.searchNumber('10', gg.TYPE_DWORD)
local r = gg.getResults(5) -- load items
r[1].value = '15'
print('Edited: ', gg.setValues(r))

local t = {}
t[1] = {}
t[1].address = 0x18004030 -- some desired address
t[1].flags = gg.TYPE_DWORD
t[1].value = 12345
t[2] = {}
t[2].address = 0x18004040 -- another desired address
t[2].flags = gg.TYPE_BYTE
t[2].value = '7Fh'
print('Set', t, gg.setValues(t))
```

---

#### setVisible

**函数原型：**
```lua
setVisible(bool visible)
```

**函数功能：** 打开或关闭 GameGuardian 的 UI。

**函数参数：** 打开设置 true，关闭设置 false。

**函数返回值：** 无

---

#### skipRestoreState

**函数原型：**
```lua
skipRestoreState()
```

**函数功能：** 请不要在脚本执行完毕后存储 GameGuardian 的状态。例如，默认情况下，一组内存单元在脚本执行完毕过后保存，该函数调用就会阻止保存。

**函数参数：** 无

**函数返回值：** 无

**代码例子：**
```lua
gg.setRanges(bit32.bxor(gg.REGION_C_HEAP, gg.REGION_C_ALLOC, gg.REGION_ANONYMOUS))
-- do some things like search values
-- gg.skipRestoreState() -- if you uncomment this line -
-- memory ranges after end script stay same as we set in first line.
-- If not - it will be restored to state which be before script run.
```

---

#### sleep

**函数原型：**
```lua
sleep(int milliseconds)
```

**函数功能：** 指定一个数值（毫秒数）会使得脚本睡眠，以系统定时器和调度器的精确性和准确性为前提。

**函数参数：**
- `milliseconds`：指定睡眠毫秒数

**返回值：** 无

**例子代码：**
```lua
-- 200 ms
gg.sleep(200)

-- 300 ms
local v = 300
gg.sleep(v)
```

---

#### startFuzzy

**函数原型：**
```lua
mixed startFuzzy(
    int type = gg.TYPE_AUTO,
    long memoryFrom = 0,
    long memoryTo = -1
)
```

**函数功能：** 指定具体参数，进行模糊搜索。

**函数参数：**
- `type`：数据类型符号常量
- `memoryFrom`：搜索的内存单元起始地址
- `memoryTo`：搜索的内存单元的结束地址

**函数返回值：** 成功 True，失败 "error"。

**例子代码：**
```lua
gg.startFuzzy()
gg.startFuzzy(gg.TYPE_DWORD)
gg.startFuzzy(gg.TYPE_FLOAT)
gg.startFuzzy(gg.TYPE_BYTE, 0x9000, 0xA09000)
```

---

#### timeJump

**函数原型：**
```lua
mixed timeJump(string time)
```

**函数功能：** 时间跳跃。

**函数参数：**
- `time`：时间字符串，该字符串参数跟 GameGuardian 中时间跳跃的时间格式一致

**函数返回值：** 成功为 true，失败返回字符串 error。

**例子代码：**
```lua
print('jump 1:', gg.timeJump('42345678'))
-- jump for 1 year 125 days 2 hours 41 minutes 18 seconds

print('jump 2:', gg.timeJump('1:125:2:41:18'))
-- same as above

print('jump 3:', gg.timeJump('5:13'))
-- jump for 5 minutes 13 seconds

print('jump 4:', gg.timeJump('7:3:1'))
-- jump for 7 hours 3 minutes 1 seconds

print('jump 5:', gg.timeJump('3600'))
-- jump for 1 hour

print('jump 6:', gg.timeJump('2:15:54:32'))
-- jump for 2 days 15 hours 54 minutes 32 seconds

print('jump 7:', gg.timeJump('3600.15'))
-- jump for 1 hour 0.15 seconds

print('jump 8:', gg.timeJump('7:3:1.519'))
-- jump for 7 hours 3 minutes 1.519 seconds
```

---

#### toast

**函数原型：**
```lua
toast(
    string text,
    bool fast = false
)
```

**函数功能：** 在屏幕底部显示一段信息。如果第二个参数设置为 true，将短暂的显示信息。

**函数参数：**
- `text`：显示文本提示信息
- `fast`：短时间内，显示一段提示信息

**例子代码：**
```lua
gg.toast('This is toast')
-- Show text notification for a long period of time

gg.toast('This is toast', true)
-- Show text notification for a short period of time
```

---

## gg 符号常量

### BUILD
- **数据类型：** int
- **数据意义：** 由 GameGuardian 建立的数字

### CACHE_DIR
- **数据类型：** string
- **数据意义：** 在文件系统中，给 GameGuardian 指定的缓存目录的绝对路径。当设备运行，且存储空间不足时，首先被删除的文件。且无法保证这些文件何时被删除。
- **备注：** 不应该依赖于系统去自动删除这些文件。你们应该总是有充足的理由去设置它，比如将你缓存文件大小设置为 1MB，并在超过你分配的空间时，删除这些文件。如果你的应用需要更大的存储空间，你们应该使用 EXT_CACHE_DIR 代替。这部分数据替换到内存，不要让其它 app 可见，也不要让用户可以篡改。

### EXT_CACHE_DIR
- **数据类型：** string
- **数据意义：** 存储在 shared/external 存储设备的绝对目录路径，GameGuardian 能够支持持久化存储数据。

### FILES_DIR
- **数据类型：** string
- **数据意义：** 控制 GameGuardian 文件的目录路径，放置在内存中，对其它 app 不可见。

### LOAD_APPEND
- **数据类型：** int
- **数据意义：** 用于 loadList 的标志，附加到列表。

### LOAD_VALUES
- **数据类型：** int
- **数据意义：** 用于 loadList 的标志，导入值。

### LOAD_VALUES_FREEZE
- **数据类型：** int
- **数据意义：** 用于 loadList 的标志，导入值并冻结。

### PACKAGE
- **数据类型：** string
- **数据意义：** GameGuardian 的包名字。

### REGION_ANONYMOUS
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_ASHMEM
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_BAD
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_C_ALLOC
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_C_BSS
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_C_DATA
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_C_HEAP
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_CODE_APP
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_CODE_SYS
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_JAVA
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_JAVA_HEAP
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_OTHER
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_PPSSPP
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### REGION_STACK
- **数据类型：** int
- **数据意义：** 用于 getRanges, setRanges 的标志位。

### SAVE_AS_TEXT
- **数据类型：** int
- **数据意义：** 用于 saveList 的标志位。

### SIGN_EQUAL
- **数据类型：** int
- **数据意义：** 用于 searchAddress, searchNumber 的标志位。

### SIGN_FUZZY_EQUAL
- **数据类型：** int
- **数据意义：** 用于 searchFuzzy 的标志位。

### SIGN_FUZZY_GREATER
- **数据类型：** int
- **数据意义：** 用于 searchFuzzy 的标志位。

### SIGN_FUZZY_LESS
- **数据类型：** int
- **数据意义：** 用于 searchFuzzy 的标志位。

### SIGN_GREATER_OR_EQUAL
- **数据类型：** int
- **数据意义：** 用于 searchAddress, searchNumber 的标志位。

### SIGN_LESS_OR_EQUAL
- **数据类型：** int
- **数据意义：** 用于 searchAddress, searchNumber 的标志位。

### SIGN_NOT_EQUAL
- **数据类型：** int
- **数据意义：** 用于 searchAddress, searchNumber 的标志位。

### TYPE_AUTO
- **数据类型：** int
- **数据意义：** 表示数据类型 Auto。

### TYPE_BYTE
- **数据类型：** int
- **数据意义：** 表示数据类型 BYTE。

### TYPE_DOUBLE
- **数据类型：** int
- **数据意义：** 表示数据类型 DOUBLE。

### TYPE_DWORD
- **数据类型：** int
- **数据意义：** 表示数据类型 DWORD。

### TYPE_FLOAT
- **数据类型：** int
- **数据意义：** 表示数据类型 FLOAT。

### TYPE_QWORD
- **数据类型：** int
- **数据意义：** 表示数据类型 QWORD。

### TYPE_WORD
- **数据类型：** int
- **数据意义：** 表示数据类型 WORD。

### TYPE_XOR
- **数据类型：** int
- **数据意义：** 表示数据类型 XOR。

### VERSION
- **数据类型：** string
- **数据意义：** GameGuardian 的版本号字符串。

### VERSION_INT
- **数据类型：** int
- **数据意义：** GameGuardian 的版本数字。

---

**文档结束**
"""));
//TODO:文档结束
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
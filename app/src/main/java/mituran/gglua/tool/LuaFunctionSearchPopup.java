package mituran.gglua.tool;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import mituran.gglua.tool.model.LuaFunction;


public class LuaFunctionSearchPopup {

    private Activity activity;
    private PopupWindow popupWindow;
    private LuaFunctionAdapter adapter;
    private EditText etSearch;
    private RecyclerView rvFunctions; // 将RecyclerView提升为成员变量

    public LuaFunctionSearchPopup(Activity activity) {
        this.activity = activity;
        initPopup();
    }

    private void initPopup() {
        View popupView = LayoutInflater.from(activity)
                .inflate(R.layout.popup_lua_function_search, null);

        // 初始化控件
        etSearch = popupView.findViewById(R.id.et_search);
        ImageView ivHelp = popupView.findViewById(R.id.iv_help);
        rvFunctions = popupView.findViewById(R.id.rv_functions); // 赋值给成员变量

        // 设置RecyclerView
        rvFunctions.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new LuaFunctionAdapter(getLuaFunctions());
        rvFunctions.setAdapter(adapter);

        // *** 新增：为列表添加默认分割线 ***
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(activity,
                DividerItemDecoration.VERTICAL);
        rvFunctions.addItemDecoration(dividerItemDecoration);

        // *** 修改：搜索功能，并控制列表的显示/隐藏 ***
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 如果输入内容为空，隐藏列表；否则显示列表
                if (s.toString().isEmpty()) {
                    rvFunctions.setVisibility(View.GONE);
                } else {
                    rvFunctions.setVisibility(View.VISIBLE);
                }
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 帮助按钮
        ivHelp.setOnClickListener(v -> showHelpDialog());

        // 函数点击事件
        adapter.setOnFunctionClickListener(function -> {
            insertFunctionToEditor(function.getName());
            dismiss();
        });

        // 创建PopupWindow
        popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        // 注意：这里我们使用XML里的背景，所以可以设置为透明
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // 如果XML里的elevation不生效，可以在这里设置
        popupWindow.setElevation(20);
    }

    // 显示弹窗时，重置状态
    public void show(View anchorView) {
        if (popupWindow != null && !popupWindow.isShowing()) {
            // 重置搜索框和列表状态
            etSearch.setText("");
            rvFunctions.setVisibility(View.GONE);

            // 之前的显示逻辑
            popupWindow.getContentView().measure(
                    View.MeasureSpec.UNSPECIFIED,
                    View.MeasureSpec.UNSPECIFIED
            );
            int[] location = new int[2];
            anchorView.getLocationOnScreen(location);
            popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY,
                    0, location[1] - popupWindow.getContentView().getMeasuredHeight());
        }
    }


    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    // 显示帮助对话框
    private void showHelpDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("代码帮助")
                .setMessage("在搜索框中输入函数名或关键词即可快速查找Lua函数。\n\n" +
                        "用于参考，具体使用方法和功能详细介绍请返回“主页”查看官方文档。\n\n")
                .setPositiveButton("确定", null)
                .show();
    }

    // 插入函数到编辑器
    private void insertFunctionToEditor(String functionName) {
        // TODO:
    }

    // 获取Lua函数列表（示例数据）
    private List<LuaFunction> getLuaFunctions() {
        List<LuaFunction> functions = new ArrayList<>();

        // 基本函数
        functions.add(new LuaFunction("print()", "输出内容到控制台", "print(...)"));
        functions.add(new LuaFunction("type()", "返回变量的类型", "type(v)"));
        functions.add(new LuaFunction("tonumber()", "将参数转换为数字", "tonumber(e [, base])"));
        functions.add(new LuaFunction("tostring()", "将参数转换为字符串", "tostring(v)"));
        functions.add(new LuaFunction("pairs()", "遍历表的所有键值对", "pairs(t)"));
        functions.add(new LuaFunction("ipairs()", "遍历数组部分", "ipairs(t)"));
        functions.add(new LuaFunction("next()", "返回表的下一个键值对", "next(table [, index])"));
        functions.add(new LuaFunction("select()", "返回指定位置后的所有参数", "select(index, ...)"));

        // 字符串函数
        functions.add(new LuaFunction("string.len()", "返回字符串长度", "string.len(s)"));
        functions.add(new LuaFunction("string.sub()", "截取子字符串", "string.sub(s, i [, j])"));
        functions.add(new LuaFunction("string.find()", "查找字符串", "string.find(s, pattern [, init])"));
        functions.add(new LuaFunction("string.format()", "格式化字符串", "string.format(formatstring, ...)"));
        functions.add(new LuaFunction("string.gsub()", "字符串替换", "string.gsub(s, pattern, repl)"));
        functions.add(new LuaFunction("string.upper()", "转换为大写", "string.upper(s)"));
        functions.add(new LuaFunction("string.lower()", "转换为小写", "string.lower(s)"));

        // 表函数
        functions.add(new LuaFunction("table.insert()", "插入元素到表", "table.insert(list, [pos,] value)"));
        functions.add(new LuaFunction("table.remove()", "移除表中元素", "table.remove(list [, pos])"));
        functions.add(new LuaFunction("table.sort()", "对表进行排序", "table.sort(list [, comp])"));
        functions.add(new LuaFunction("table.concat()", "连接表中字符串", "table.concat(list [, sep])"));

        // 数学函数
        functions.add(new LuaFunction("math.abs()", "绝对值", "math.abs(x)"));
        functions.add(new LuaFunction("math.ceil()", "向上取整", "math.ceil(x)"));
        functions.add(new LuaFunction("math.floor()", "向下取整", "math.floor(x)"));
        functions.add(new LuaFunction("math.max()", "返回最大值", "math.max(x, ...)"));
        functions.add(new LuaFunction("math.min()", "返回最小值", "math.min(x, ...)"));
        functions.add(new LuaFunction("math.random()", "生成随机数", "math.random([m [, n]])"));

        // IO函数
        functions.add(new LuaFunction("io.open()", "打开文件", "io.open(filename [, mode])"));
        functions.add(new LuaFunction("io.read()", "读取文件内容", "io.read(...)"));
        functions.add(new LuaFunction("io.write()", "写入文件", "io.write(...)"));

        // GG函数
        functions.add(new LuaFunction("gg.alert()", "显示一个包含几个按钮的对话框", "gg.alert(text, positive='ok', negative=nil, neutral=nil)"));

        functions.add(new LuaFunction("gg.bytes()", "获得指定编码文本的文本字节", "gg.bytes(text, encoding='UTF-8')"));

        functions.add(new LuaFunction("gg.choice()", "从列表中显示一个选择对话框", "gg.choice(items, selected=nil, message=nil)"));

        functions.add(new LuaFunction("gg.clearResults()", "清空搜索到的值的列表", "gg.clearResults()"));

        functions.add(new LuaFunction("gg.copyMemory()", "复制内存", "gg.copyMemory(from, to, bytes)"));

        functions.add(new LuaFunction("gg.copyText()", "复制文本到剪贴板", "gg.copyText(text, fixLocale=true)"));

        functions.add(new LuaFunction("gg.dumpMemory()", "复制指定内存单元到文件", "gg.dumpMemory(from, to, dir)"));

        functions.add(new LuaFunction("gg.editAll()", "编辑搜索的结果值，编辑全部结果值", "gg.editAll(value, type)"));

        functions.add(new LuaFunction("gg.getFile()", "获取当前正在运行的脚本的名字", "gg.getFile()"));

        functions.add(new LuaFunction("gg.getLine()", "获取将要执行的脚本的当前行的行号", "gg.getLine()"));

        functions.add(new LuaFunction("gg.getLocale()", "获取在GameGuardian中的当前选择的字符串本地化情况", "gg.getLocale()"));

        functions.add(new LuaFunction("gg.getRanges()", "内存区域作为一个REGION_*指针标志位遮罩返回", "gg.getRanges()"));

        functions.add(new LuaFunction("gg.getRangesList()", "获取选择进程的内存区域的列表", "gg.getRangesList(filter='')"));

        functions.add(new LuaFunction("gg.getResults()", "导入结果到结果列表，并作为表返回", "gg.getResults(maxCount)"));

        functions.add(new LuaFunction("gg.getResultCount()", "获取找到结果的数量", "gg.getResultCount()"));

        functions.add(new LuaFunction("gg.getSpeed()", "从加速中获取当前速度", "gg.getSpeed()"));

        functions.add(new LuaFunction("gg.getTargetInfo()", "获取关于选择进程的信息列表", "gg.getTargetInfo()"));

        functions.add(new LuaFunction("gg.getTargetPackage()", "获取选择进程的包名字", "gg.getTargetPackage()"));

        functions.add(new LuaFunction("gg.getValues()", "获取列表的项目的值", "gg.getValues(values)"));

        functions.add(new LuaFunction("gg.getValuesRange()", "获取传递的表参数的值的内存区域", "gg.getValuesRange(values)"));

        functions.add(new LuaFunction("gg.gotoAddress()", "在内存编辑器中跳转到指定地址", "gg.gotoAddress(address)"));

        functions.add(new LuaFunction("gg.isPackageInstalled()", "通过安装包名字判断指定应用程序在操作系统上是否安装", "gg.isPackageInstalled(pkg)"));

        functions.add(new LuaFunction("gg.isProcessPaused()", "获取指定进程的暂停状态", "gg.isProcessPaused()"));

        functions.add(new LuaFunction("gg.isVisible()", "检查GameGuardian的UI是否打开", "gg.isVisible()"));

        functions.add(new LuaFunction("gg.loadList()", "从文件中导入保存列表", "gg.loadList(file, flags=0)"));

        functions.add(new LuaFunction("gg.multiChoice()", "显示多项选择对话框", "gg.multiChoice(items, selection={}, message=nil)"));

        functions.add(new LuaFunction("gg.processKill()", "暴力杀掉选择进程", "gg.processKill()"));

        functions.add(new LuaFunction("gg.processPause()", "暂停选择程序进程", "gg.processPause()"));

        functions.add(new LuaFunction("gg.processResume()", "如果程序进程被暂停，该操作恢复程序进程", "gg.processResume()"));

        functions.add(new LuaFunction("gg.processToggle()", "切换选择进程的暂停状态", "gg.processToggle()"));

        functions.add(new LuaFunction("gg.prompt()", "显示数据入口对话框", "gg.prompt(prompts, defaults={}, types={})"));

        functions.add(new LuaFunction("gg.removeResults()", "从列出的发现结果的列表中，移除结果值", "gg.removeResults(results)"));

        functions.add(new LuaFunction("gg.require()", "检查GameGuardian的版本号", "gg.require(version=nil, build=0)"));

        functions.add(new LuaFunction("gg.saveList()", "将保存列表的内容保存到文件", "gg.saveList(file, flags=0)"));

        functions.add(new LuaFunction("gg.searchAddress()", "根据指定参数，执行一个地址搜索", "gg.searchAddress(text, mask=-1, type=gg.TYPE_AUTO, sign=gg.SIGN_EQUAL, memoryFrom=0, memoryTo=-1)"));

        functions.add(new LuaFunction("gg.searchFuzzy()", "用指定的参数优化模糊搜索", "gg.searchFuzzy(difference='0', sign=gg.SIGN_FUZZY_EQUAL, type=gg.TYPE_AUTO, memoryFrom=0, memoryTo=-1)"));

        functions.add(new LuaFunction("gg.searchNumber()", "根据指定参数，搜索一个数字", "gg.searchNumber(text, type=gg.TYPE_AUTO, encrypted=false, sign=gg.SIGN_EQUAL, memoryFrom=0, memoryTo=-1)"));

        functions.add(new LuaFunction("gg.setRanges()", "设置内存区域，通过设置REGION_*标志的遮罩位", "gg.setRanges(ranges)"));

        functions.add(new LuaFunction("gg.setSpeed()", "根据加速功能设置加速", "gg.setSpeed(speed)"));

        functions.add(new LuaFunction("gg.setValues()", "设置值", "gg.setValues(values)"));

        functions.add(new LuaFunction("gg.setVisible()", "打开或关闭GameGuardian的UI", "gg.setVisible(visible)"));

        functions.add(new LuaFunction("gg.skipRestoreState()", "请不要在脚本执行完毕后存储GameGuardian的状态", "gg.skipRestoreState()"));

        functions.add(new LuaFunction("gg.sleep()", "指定一个数值(毫秒数)会使得脚本睡眠", "gg.sleep(milliseconds)"));

        functions.add(new LuaFunction("gg.startFuzzy()", "指定具体参数，进行模糊搜索", "gg.startFuzzy(type=gg.TYPE_AUTO, memoryFrom=0, memoryTo=-1)"));

        functions.add(new LuaFunction("gg.timeJump()", "时间跳跃", "gg.timeJump(time)"));

        functions.add(new LuaFunction("gg.toast()", "在屏幕底部显示一段信息", "gg.toast(text, fast=false)"));

        return functions;
    }
}
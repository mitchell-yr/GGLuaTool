package mituran.gglua.tool;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;  // 添加 Toast 导入

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class LuaEngine {

    private Globals globals;
    private final Activity activity;
    private final TextView tvLogOutput;

    // 自定义异常，用于安全地处理 os.exit
    public static class LuaExitException extends RuntimeException {
        private final int exitCode;

        public LuaExitException(int exitCode) {
            super("Lua script exited with code: " + exitCode);
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }

    public LuaEngine(Activity activity, TextView tvLogOutput) {
        this.activity = activity;
        this.tvLogOutput = tvLogOutput;
    }

    /**
     * 初始化 LuaJ 虚拟机环境
     */
    public void initialize() {
        // 1. 创建一个标准的全局环境
        globals = JsePlatform.standardGlobals();

        // 2. 重写 print 函数，使其输出到我们的 TextView
        globals.set("print", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                // 如果没有参数，也打印一个换行符，行为和标准 print 一致
                if (args.narg() == 0) {
                    logToTextView("\n");
                    return NIL;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    // 注意：Varargs 的索引从 1 开始，不是从 0 开始
                    LuaValue v = args.arg(i);
                    sb.append(v.tojstring());
                    if (i < args.narg()) {
                        sb.append("\t"); // 参数之间用制表符分隔
                    }
                }
                sb.append("\n"); // 最后加上换行符
                logToTextView("[print]"+sb.toString());
                return NIL;
            }
        });

        // 3. [关键修复] 重写 os.exit 函数，防止应用闪退
        overrideOsExit();

        // 4. 添加 gg.toast() 函数
        createGgLibrary();
    }

    /**
     * 创建 gg 库并添加所有 GG 函数
     */
    private void createGgLibrary() {
        // 创建 gg table
        LuaTable ggTable = new LuaTable();

        // ============ UI 相关函数 ============

        // gg.toast(text, fast) - 显示 Toast 消息
        ggTable.set("toast", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 1) {
                    throw new LuaError("gg.toast() requires at least 1 argument (text)");
                }
                String text = args.arg1().checkjstring();
                boolean fast = args.narg() >= 2 ? args.arg(2).checkboolean() : false;

                logToTextView("[gg.toast] " + text + " (fast=" + fast + ")\n");

                final boolean isFast = fast;
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        int duration = isFast ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
                        Toast.makeText(activity, text, duration).show();
                    });
                }
                return NIL;
            }
        });

        // gg.alert(text, positive, negative, neutral) - 显示警告对话框
        ggTable.set("alert", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String text = args.narg() >= 1 ? args.arg1().checkjstring() : "";
                String positive = args.narg() >= 2 ? args.arg(2).optjstring("ok") : "ok";
                String negative = args.narg() >= 3 ? args.arg(3).optjstring(null) : null;
                String neutral = args.narg() >= 4 ? args.arg(4).optjstring(null) : null;

                logToTextView("[gg.alert] text=\"" + text + "\"\n");

                // 使用 CountDownLatch 等待对话框结果
                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<Integer> result = new AtomicReference<>(0);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setMessage(text);
                        builder.setCancelable(true);

                        // Positive 按钮 (最右边) - 返回 1
                        builder.setPositiveButton(positive, (dialog, which) -> {
                            result.set(1);
                            latch.countDown();
                        });

                        // Negative 按钮 (右边第二个) - 返回 2
                        if (negative != null) {
                            builder.setNegativeButton(negative, (dialog, which) -> {
                                result.set(2);
                                latch.countDown();
                            });
                        }

                        // Neutral 按钮 (最左边) - 返回 3
                        if (neutral != null) {
                            builder.setNeutralButton(neutral, (dialog, which) -> {
                                result.set(3);
                                latch.countDown();
                            });
                        }

                        // 取消对话框 - 返回 0
                        builder.setOnCancelListener(dialog -> {
                            result.set(0);
                            latch.countDown();
                        });

                        builder.show();
                    });

                    try {
                        latch.await(); // 等待用户操作
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                return LuaValue.valueOf(result.get());
            }
        });

        // gg.choice(items, selected, message) - 显示单选对话框
        ggTable.set("choice", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaTable items = args.arg1().checktable();
                int selectedIndex = args.narg() >= 2 ? args.arg(2).optint(-1) : -1;
                String message = args.narg() >= 3 ? args.arg(3).optjstring("请选择") : "请选择";

                // 转换为字符串数组
                int itemCount = items.length();
                String[] itemArray = new String[itemCount];
                for (int i = 1; i <= itemCount; i++) {
                    itemArray[i - 1] = items.get(i).tojstring();
                }

                logToTextView("[gg.choice] items count=" + itemCount + ", message=\"" + message + "\"\n");

                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<Integer> result = new AtomicReference<>(-1);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(message);

                        // 设置单选列表
                        builder.setSingleChoiceItems(itemArray, selectedIndex - 1, (dialog, which) -> {
                            result.set(which + 1); // Lua 索引从 1 开始
                            dialog.dismiss();
                            latch.countDown();
                        });

                        // 取消按钮
                        builder.setNegativeButton("取消", (dialog, which) -> {
                            result.set(-1);
                            latch.countDown();
                        });

                        builder.setOnCancelListener(dialog -> {
                            result.set(-1);
                            latch.countDown();
                        });

                        builder.show();
                    });

                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                int resultValue = result.get();
                return resultValue > 0 ? LuaValue.valueOf(resultValue) : NIL;
            }
        });

        // gg.multiChoice(items, selection, message) - 显示多选对话框
        ggTable.set("multiChoice", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaTable items = args.arg1().checktable();
                LuaTable selection = args.narg() >= 2 ? args.arg(2).checktable() : new LuaTable();
                String message = args.narg() >= 3 ? args.arg(3).optjstring("请选择") : "请选择";

                // 转换为字符串数组
                int itemCount = items.length();
                String[] itemArray = new String[itemCount];
                boolean[] checkedItems = new boolean[itemCount];

                for (int i = 1; i <= itemCount; i++) {
                    itemArray[i - 1] = items.get(i).tojstring();
                    checkedItems[i - 1] = selection.get(i).toboolean();
                }

                logToTextView("[gg.multiChoice] items count=" + itemCount + ", message=\"" + message + "\"\n");

                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<LuaTable> result = new AtomicReference<>(null);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(message);

                        // 设置多选列表
                        builder.setMultiChoiceItems(itemArray, checkedItems, (dialog, which, isChecked) -> {
                            checkedItems[which] = isChecked;
                        });

                        // 确定按钮
                        builder.setPositiveButton("确定", (dialog, which) -> {
                            LuaTable resultTable = new LuaTable();
                            for (int i = 0; i < checkedItems.length; i++) {
                                if (checkedItems[i]) {
                                    resultTable.set(i + 1, LuaValue.TRUE);
                                }
                            }
                            result.set(resultTable);
                            latch.countDown();
                        });

                        // 取消按钮
                        builder.setNegativeButton("取消", (dialog, which_btn) -> {
                            result.set(null);
                            latch.countDown();
                        });

                        builder.setOnCancelListener(dialog -> {
                            result.set(null);
                            latch.countDown();
                        });

                        builder.show();
                    });

                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                LuaTable resultTable = result.get();
                return resultTable != null ? resultTable : NIL;
            }
        });

        // gg.prompt(prompts, defaults, types) - 显示输入对话框
        ggTable.set("prompt", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaTable prompts = args.arg1().checktable();
                LuaTable defaults = args.narg() >= 2 ? args.arg(2).checktable() : new LuaTable();
                LuaTable types = args.narg() >= 3 ? args.arg(3).checktable() : new LuaTable();

                int promptCount = prompts.length();
                logToTextView("[gg.prompt] prompts count=" + promptCount + "\n");

                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<LuaTable> result = new AtomicReference<>(null);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle("请输入");

                        // 创建输入布局
                        LinearLayout layout = new LinearLayout(activity);
                        layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setPadding(50, 40, 50, 10);

                        // 存储输入控件
                        final Object[] inputViews = new Object[promptCount];

                        for (int i = 1; i <= promptCount; i++) {
                            String promptText = prompts.get(i).tojstring();
                            LuaValue defaultValue = defaults.get(i);
                            String typeStr = types.get(i).optjstring("text");

                            // 添加标签
                            TextView label = new TextView(activity);
                            label.setText(promptText);
                            label.setPadding(0, 20, 0, 10);
                            layout.addView(label);

                            // 根据类型创建输入控件
                            if (typeStr.equals("checkbox")) {
                                CheckBox checkBox = new CheckBox(activity);
                                checkBox.setChecked(defaultValue.toboolean());
                                layout.addView(checkBox);
                                inputViews[i - 1] = checkBox;
                            } else {
                                EditText editText = new EditText(activity);
                                editText.setText(defaultValue.isnil() ? "" : defaultValue.tojstring());

                                // 设置输入类型
                                if (typeStr.equals("number") || typeStr.equals("speed") || typeStr.equals("setting")) {
                                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                }

                                layout.addView(editText);
                                inputViews[i - 1] = editText;
                            }
                        }

                        builder.setView(layout);

                        // 确定按钮
                        builder.setPositiveButton("确定", (dialog, which) -> {
                            LuaTable resultTable = new LuaTable();
                            for (int i = 0; i < promptCount; i++) {
                                Object view = inputViews[i];
                                if (view instanceof CheckBox) {
                                    resultTable.set(i + 1, LuaValue.valueOf(((CheckBox) view).isChecked()));
                                } else if (view instanceof EditText) {
                                    String text = ((EditText) view).getText().toString();
                                    resultTable.set(i + 1, LuaValue.valueOf(text));
                                }
                            }
                            result.set(resultTable);
                            latch.countDown();
                        });

                        // 取消按钮
                        builder.setNegativeButton("取消", (dialog, which) -> {
                            result.set(null);
                            latch.countDown();
                        });

                        builder.setOnCancelListener(dialog -> {
                            result.set(null);
                            latch.countDown();
                        });

                        builder.show();
                    });

                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                LuaTable resultTable = result.get();
                return resultTable != null ? resultTable : NIL;
            }
        });

        // gg.setVisible(visible) - 设置 GG 可见性
        ggTable.set("setVisible", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                boolean visible = args.arg1().checkboolean();
                logToTextView("[gg.setVisible] visible=" + visible + "\n");
                return NIL;
            }
        });

        // gg.isVisible() - 检查 GG 是否可见
        ggTable.set("isVisible", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.isVisible] -> true\n");
                return TRUE;
            }
        });

        // ============ 搜索相关函数 ============

        // gg.searchNumber(text, type, encrypted, sign, memoryFrom, memoryTo)
        ggTable.set("searchNumber", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String text = args.arg1().checkjstring();
                int type = args.narg() >= 2 ? args.arg(2).checkint() : 127; // TYPE_AUTO
                boolean encrypted = args.narg() >= 3 ? args.arg(3).checkboolean() : false;
                int sign = args.narg() >= 4 ? args.arg(4).checkint() : 536870912; // SIGN_EQUAL

                logToTextView("[gg.searchNumber] text=\"" + text + "\", type=" + type +
                        ", encrypted=" + encrypted + ", sign=" + sign + "\n");

                return TRUE;
            }
        });

        // gg.searchAddress(text, mask, type, sign, memoryFrom, memoryTo)
        ggTable.set("searchAddress", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String text = args.arg1().checkjstring();
                long mask = args.narg() >= 2 ? args.arg(2).checklong() : -1L;

                logToTextView("[gg.searchAddress] text=\"" + text + "\", mask=" +
                        Long.toHexString(mask) + "\n");

                return TRUE;
            }
        });

        // gg.startFuzzy(type, memoryFrom, memoryTo) - 开始模糊搜索
        ggTable.set("startFuzzy", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int type = args.narg() >= 1 ? args.arg1().checkint() : 127;
                logToTextView("[gg.startFuzzy] type=" + type + "\n");
                return TRUE;
            }
        });

        // gg.searchFuzzy(difference, sign, type, memoryFrom, memoryTo)
        ggTable.set("searchFuzzy", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String difference = args.narg() >= 1 ? args.arg(1).optjstring("0") : "0";
                int sign = args.narg() >= 2 ? args.arg(2).checkint() : 536870912;

                logToTextView("[gg.searchFuzzy] difference=\"" + difference + "\", sign=" + sign + "\n");
                return TRUE;
            }
        });

        // gg.clearResults() - 清空搜索结果
        ggTable.set("clearResults", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.clearResults]\n");
                return NIL;
            }
        });

        // gg.getResultCount() - 获取结果数量
        ggTable.set("getResultCount", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.getResultCount] -> 10\n");
                return LuaValue.valueOf(10); // 模拟返回10个结果
            }
        });

        // gg.getResults(maxCount) - 获取搜索结果
        ggTable.set("getResults", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int maxCount = args.arg1().checkint();
                logToTextView("[gg.getResults] maxCount=" + maxCount + "\n");

                // 模拟返回结果列表
                LuaTable results = new LuaTable();
                for (int i = 1; i <= Math.min(maxCount, 3); i++) {
                    LuaTable item = new LuaTable();
                    item.set("address", LuaValue.valueOf(0x10000000L + i * 0x1000));
                    item.set("value", LuaValue.valueOf(String.valueOf(100 + i)));
                    item.set("flags", LuaValue.valueOf(4)); // TYPE_DWORD
                    results.set(i, item);
                }
                return results;
            }
        });

        // gg.removeResults(results) - 移除指定结果
        ggTable.set("removeResults", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaTable results = args.arg1().checktable();
                logToTextView("[gg.removeResults] count=" + results.length() + "\n");
                return TRUE;
            }
        });

        // ============ 内存编辑函数 ============

        // gg.editAll(value, type) - 编辑所有结果
        ggTable.set("editAll", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String value = args.arg1().checkjstring();
                int type = args.arg(2).checkint();

                logToTextView("[gg.editAll] value=\"" + value + "\", type=" + type + "\n");
                return LuaValue.valueOf(5); // 模拟修改了5个值
            }
        });

        // gg.getValues(values) - 获取指定地址的值
        ggTable.set("getValues", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaTable values = args.arg1().checktable();
                logToTextView("[gg.getValues] count=" + values.length() + "\n");

                // 模拟返回值
                LuaTable results = new LuaTable();
                for (int i = 1; i <= values.length(); i++) {
                    LuaTable item = values.get(i).checktable();
                    LuaTable result = new LuaTable();
                    result.set("address", item.get("address"));
                    result.set("value", LuaValue.valueOf("100"));
                    result.set("flags", item.get("flags"));
                    results.set(i, result);
                }
                return results;
            }
        });

        // gg.setValues(values) - 设置指定地址的值
        ggTable.set("setValues", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaTable values = args.arg1().checktable();
                logToTextView("[gg.setValues] count=" + values.length() + "\n");
                return TRUE;
            }
        });

        // gg.getValuesRange(values) - 获取地址所在内存区域
        ggTable.set("getValuesRange", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.getValuesRange]\n");

                LuaTable result = new LuaTable();
                result.set(1, LuaValue.valueOf("Ca")); // 模拟返回 C_ALLOC 区域
                return result;
            }
        });

        // ============ 内存操作函数 ============

        // gg.copyMemory(from, to, bytes) - 复制内存
        ggTable.set("copyMemory", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                long from = args.arg1().checklong();
                long to = args.arg(2).checklong();
                int bytes = args.arg(3).checkint();

                logToTextView(String.format("[gg.copyMemory] from=0x%X, to=0x%X, bytes=%d\n",
                        from, to, bytes));
                return TRUE;
            }
        });

        // gg.dumpMemory(from, to, dir) - 导出内存到文件
        ggTable.set("dumpMemory", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                long from = args.arg1().checklong();
                long to = args.arg(2).checklong();
                String dir = args.arg(3).checkjstring();

                logToTextView(String.format("[gg.dumpMemory] from=0x%X, to=0x%X, dir=\"%s\"\n",
                        from, to, dir));
                return TRUE;
            }
        });

        // gg.gotoAddress(address) - 跳转到地址
        ggTable.set("gotoAddress", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                long address = args.arg1().checklong();
                logToTextView(String.format("[gg.gotoAddress] address=0x%X\n", address));
                return NIL;
            }
        });

        // ============ 内存区域函数 ============

        // gg.setRanges(ranges) - 设置内存区域
        ggTable.set("setRanges", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int ranges = args.arg1().checkint();
                logToTextView("[gg.setRanges] ranges=0x" + Integer.toHexString(ranges) + "\n");
                return NIL;
            }
        });

        // gg.getRanges() - 获取当前内存区域
        ggTable.set("getRanges", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.getRanges] -> 0xFFFFFFFF\n");
                return LuaValue.valueOf(0xFFFFFFFF); // 模拟返回所有区域
            }
        });

        // gg.getRangesList(filter) - 获取内存区域列表
        ggTable.set("getRangesList", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String filter = args.narg() >= 1 ? args.arg1().optjstring("") : "";
                logToTextView("[gg.getRangesList] filter=\"" + filter + "\"\n");

                // 模拟返回内存区域列表
                LuaTable results = new LuaTable();
                LuaTable region = new LuaTable();
                region.set("state", LuaValue.valueOf("Xa"));
                region.set("start", LuaValue.valueOf(0x10000000L));
                region.set("end", LuaValue.valueOf(0x20000000L));
                region.set("type", LuaValue.valueOf("Ca"));
                region.set("name", LuaValue.valueOf("[anon:libc_malloc]"));
                results.set(1, region);
                return results;
            }
        });

        // ============ 列表保存/加载函数 ============

        // gg.saveList(file, flags) - 保存列表到文件
        ggTable.set("saveList", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String file = args.arg1().checkjstring();
                int flags = args.narg() >= 2 ? args.arg(2).checkint() : 0;

                logToTextView("[gg.saveList] file=\"" + file + "\", flags=" + flags + "\n");
                return TRUE;
            }
        });

        // gg.loadList(file, flags) - 从文件加载列表
        ggTable.set("loadList", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String file = args.arg1().checkjstring();
                int flags = args.narg() >= 2 ? args.arg(2).checkint() : 0;

                logToTextView("[gg.loadList] file=\"" + file + "\", flags=" + flags + "\n");
                return TRUE;
            }
        });

        // ============ 进程控制函数 ============

        // gg.processKill() - 终止进程
        ggTable.set("processKill", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.processKill]\n");
                return TRUE;
            }
        });

        // gg.processPause() - 暂停进程
        ggTable.set("processPause", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.processPause]\n");
                return TRUE;
            }
        });

        // gg.processResume() - 恢复进程
        ggTable.set("processResume", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.processResume]\n");
                return TRUE;
            }
        });

        // gg.processToggle() - 切换进程暂停状态
        ggTable.set("processToggle", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.processToggle]\n");
                return TRUE;
            }
        });

        // gg.isProcessPaused() - 检查进程是否暂停
        ggTable.set("isProcessPaused", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.isProcessPaused] -> false\n");
                return FALSE;
            }
        });

        // ============ 速度控制函数 ============

        // gg.setSpeed(speed) - 设置游戏速度
        ggTable.set("setSpeed", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                double speed = args.arg1().checkdouble();
                logToTextView("[gg.setSpeed] speed=" + speed + "\n");
                return TRUE;
            }
        });

        // gg.getSpeed() - 获取当前速度
        ggTable.set("getSpeed", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.getSpeed] -> 1.0\n");
                return LuaValue.valueOf(1.0);
            }
        });

        // gg.timeJump(time) - 时间跳跃
        ggTable.set("timeJump", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String time = args.arg1().checkjstring();
                logToTextView("[gg.timeJump] time=\"" + time + "\"\n");
                return TRUE;
            }
        });

        // ============ 工具函数 ============

        // gg.sleep(milliseconds) - 休眠
        ggTable.set("sleep", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int ms = args.arg1().checkint();
                logToTextView("[gg.sleep] milliseconds=" + ms + "\n");
                try {
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return NIL;
            }
        });

        // gg.copyText(text, fixLocale) - 复制文本到剪贴板
        ggTable.set("copyText", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String text = args.arg1().checkjstring();
                boolean fixLocale = args.narg() >= 2 ? args.arg(2).checkboolean() : true;

                logToTextView("[gg.copyText] text=\"" + text + "\", fixLocale=" + fixLocale + "\n");
                return NIL;
            }
        });

        // gg.bytes(text, encoding) - 获取文本字节
        ggTable.set("bytes", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String text = args.arg1().checkjstring();
                String encoding = args.narg() >= 2 ? args.arg(2).optjstring("UTF-8") : "UTF-8";

                logToTextView("[gg.bytes] text=\"" + text + "\", encoding=\"" + encoding + "\"\n");

                try {
                    byte[] bytes = text.getBytes(encoding);
                    String result = "";
                    for (int i = 0; i < bytes.length; i++) {
                        result += (i > 0 ? ", " : "") + (bytes[i] & 0xFF);
                    }
                    return LuaValue.valueOf(result);
                } catch (Exception e) {
                    return LuaValue.valueOf("encoding error");
                }
            }
        });

        // ============ 信息获取函数 ============

        // gg.getFile() - 获取当前脚本文件名
        ggTable.set("getFile", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String filename = "/sdcard/script.lua";
                logToTextView("[gg.getFile] -> \"" + filename + "\"\n");
                return LuaValue.valueOf(filename);
            }
        });

        // gg.getLine() - 获取当前行号
        ggTable.set("getLine", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int line = 5; // 简化实现
                logToTextView("[gg.getLine] -> " + line + "\n");
                return LuaValue.valueOf(line);
            }
        });

        // gg.getLocale() - 获取语言环境
        ggTable.set("getLocale", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String locale = "zh_CN";
                logToTextView("[gg.getLocale] -> \"" + locale + "\"\n");
                return LuaValue.valueOf(locale);
            }
        });

        // gg.getTargetPackage() - 获取目标包名
        ggTable.set("getTargetPackage", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String pkg = "com.example.game";
                logToTextView("[gg.getTargetPackage] -> \"" + pkg + "\"\n");
                return LuaValue.valueOf(pkg);
            }
        });

        // gg.getTargetInfo() - 获取目标应用信息
        ggTable.set("getTargetInfo", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.getTargetInfo]\n");

                LuaTable info = new LuaTable();
                info.set("packageName", LuaValue.valueOf("com.example.game"));
                info.set("versionCode", LuaValue.valueOf(100));
                info.set("versionName", LuaValue.valueOf("1.0.0"));
                info.set("label", LuaValue.valueOf("Game"));
                return info;
            }
        });

        // gg.isPackageInstalled(pkg) - 检查应用是否安装
        ggTable.set("isPackageInstalled", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String pkg = args.arg1().checkjstring();
                logToTextView("[gg.isPackageInstalled] pkg=\"" + pkg + "\" -> true\n");
                return TRUE;
            }
        });

        // gg.require(version, build) - 检查 GG 版本
        ggTable.set("require", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String version = args.narg() >= 1 ? args.arg1().optjstring(null) : null;
                int build = args.narg() >= 2 ? args.arg(2).optint(0) : 0;

                logToTextView("[gg.require] version=\"" + version + "\", build=" + build + "\n");
                return NIL;
            }
        });

        // gg.skipRestoreState() - 跳过状态恢复
        ggTable.set("skipRestoreState", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                logToTextView("[gg.skipRestoreState]\n");
                return NIL;
            }
        });

        // 添加 refineAddress 函数
        ggTable.set("refineAddress", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                // 检查必需参数
                if (args.narg() < 1) {
                    return LuaValue.valueOf("refineAddress() requires at least 1 argument (text)");
                }

                    return LuaValue.TRUE;
            }
        });

        // 添加 refineNumber 函数
        ggTable.set("refineNumber", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                // 检查必需参数
                if (args.narg() < 1) {
                    return LuaValue.valueOf("refineNumber() requires at least 1 argument (text)");
                }
                    return LuaValue.TRUE;
            }
        });

        // ============ 常量定义 ============
        addGgConstants(ggTable);

        // 将 gg table 设置为全局变量
        globals.set("gg", ggTable);
    }

    /**
     * 添加 GG 常量
     */
    private void addGgConstants(LuaTable ggTable) {
        // 数据类型常量
        ggTable.set("TYPE_AUTO", LuaValue.valueOf(127));
        ggTable.set("TYPE_BYTE", LuaValue.valueOf(1));
        ggTable.set("TYPE_WORD", LuaValue.valueOf(2));
        ggTable.set("TYPE_DWORD", LuaValue.valueOf(4));
        ggTable.set("TYPE_QWORD", LuaValue.valueOf(8));
        ggTable.set("TYPE_FLOAT", LuaValue.valueOf(16));
        ggTable.set("TYPE_DOUBLE", LuaValue.valueOf(32));
        ggTable.set("TYPE_XOR", LuaValue.valueOf(64));

        // 比较符号常量
        ggTable.set("SIGN_EQUAL", LuaValue.valueOf(536870912));
        ggTable.set("SIGN_NOT_EQUAL", LuaValue.valueOf(536870913));
        ggTable.set("SIGN_GREATER_OR_EQUAL", LuaValue.valueOf(536870914));
        ggTable.set("SIGN_LESS_OR_EQUAL", LuaValue.valueOf(536870915));
        ggTable.set("SIGN_FUZZY_EQUAL", LuaValue.valueOf(536870912));
        ggTable.set("SIGN_FUZZY_NOT_EQUAL", LuaValue.valueOf(536870913));
        ggTable.set("SIGN_FUZZY_GREATER", LuaValue.valueOf(536870914));
        ggTable.set("SIGN_FUZZY_LESS", LuaValue.valueOf(536870915));

        // 内存区域常量
        ggTable.set("REGION_JAVA_HEAP", LuaValue.valueOf(1));
        ggTable.set("REGION_C_HEAP", LuaValue.valueOf(2));
        ggTable.set("REGION_C_ALLOC", LuaValue.valueOf(4));
        ggTable.set("REGION_C_DATA", LuaValue.valueOf(8));
        ggTable.set("REGION_C_BSS", LuaValue.valueOf(16));
        ggTable.set("REGION_ANONYMOUS", LuaValue.valueOf(32));
        ggTable.set("REGION_STACK", LuaValue.valueOf(64));
        ggTable.set("REGION_ASHMEM", LuaValue.valueOf(128));
        ggTable.set("REGION_PPSSPP", LuaValue.valueOf(256));
        ggTable.set("REGION_JAVA", LuaValue.valueOf(512));
        ggTable.set("REGION_CODE_APP", LuaValue.valueOf(1024));
        ggTable.set("REGION_CODE_SYS", LuaValue.valueOf(2048));
        ggTable.set("REGION_BAD", LuaValue.valueOf(4096));
        ggTable.set("REGION_OTHER", LuaValue.valueOf(8192));

        // 加载/保存标志常量
        ggTable.set("LOAD_APPEND", LuaValue.valueOf(1));
        ggTable.set("LOAD_VALUES", LuaValue.valueOf(2));
        ggTable.set("LOAD_VALUES_FREEZE", LuaValue.valueOf(4));
        ggTable.set("SAVE_AS_TEXT", LuaValue.valueOf(1));

        // 版本信息
        ggTable.set("VERSION", LuaValue.valueOf("101.1"));
        ggTable.set("VERSION_INT", LuaValue.valueOf(16809));
        ggTable.set("BUILD", LuaValue.valueOf(16809));
        ggTable.set("PACKAGE", LuaValue.valueOf("catch_.me_.if_.you_.can_"));

        // 目录路径
        ggTable.set("CACHE_DIR", LuaValue.valueOf("/data/data/catch_.me_.if_.you_.can_/cache"));
        ggTable.set("FILES_DIR", LuaValue.valueOf("/data/data/catch_.me_.if_.you_.can_/files"));
        ggTable.set("EXT_CACHE_DIR", LuaValue.valueOf("/sdcard/Android/data/catch_.me_.if_.you_.can_/cache"));
    }

    /**
     * 重写 os.exit 函数，使其不会真正退出应用
     */
    private void overrideOsExit() {
        LuaValue osLib = globals.get("os");
        if (osLib != null && !osLib.isnil()) {
            LuaTable osTable = osLib.checktable();

            // 重写 os.exit 函数
            osTable.set("exit", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    int exitCode = 0;
                    if (args.narg() > 0) {
                        exitCode = args.arg1().optint(0);
                    }

                    // 记录退出信息
                    logToTextView("[os.exit]调用os.exit(" + exitCode + ")\n");

                    // 抛出自定义异常来中断脚本执行，而不是真正退出应用
                    throw new LuaExitException(exitCode);
                }
            });

        }
    }

    /**
     * 执行字符串形式的 Lua 脚本
     * @param script Lua 脚本代码
     */
    public void executeString(String script) {
        try {
            if (globals == null) {
                logToTextView("[lua虚拟机]错误: Lua 引擎未初始化！\n");
                return;
            }
            // 加载并执行脚本
            globals.load(script, "script.lua").call();
        } catch (LuaExitException e) {
            // 处理 os.exit 调用，这是正常的脚本退出
            // 退出码已经在 overrideOsExit 中记录了
        } catch (LuaError e) {
            // 捕获 Lua 运行时错误
            //logToTextView("[lua虚拟机]Lua错误: " + e.getMessage() + "\n");
        } catch (Exception e) {
            // 捕获其他 Java 异常
            logToTextView("[lua虚拟机]Java错误: " + e.getMessage() + "\n");
        }
    }

    /**
     * 执行字节码形式的 Lua 脚本 (.luac)
     * @param bytecode .luac 文件的字节数组
     */
    public void executeBytecode(byte[] bytecode) {
        try {
            if (globals == null) {
                logToTextView("错误: Lua 引擎未初始化！\n");
                return;
            }
            InputStream is = new ByteArrayInputStream(bytecode);
            // 加载并执行字节码。"b" 代表 binary mode
            globals.load(is, "script.luac", "b", globals).call();
        } catch (LuaExitException e) {
            // 处理 os.exit 调用，这是正常的脚本退出
            // 退出码已经在 overrideOsExit 中记录了
        } catch (LuaError e) {
            logToTextView("Lua 错误: " + e.getMessage() + "\n");
        } catch (Exception e) {
            logToTextView("Java 错误: " + e.getMessage() + "\n");
        }
    }

    /**
     * 清空日志 TextView
     */
    public void clearLog() {
        if (activity != null && tvLogOutput != null) {
            activity.runOnUiThread(() -> tvLogOutput.setText(""));
        }
    }

    /**
     * 释放资源
     */
    public void close() {
        globals = null;
    }

    /**
     * 安全地在 UI 线程上更新 TextView
     * @param message 要输出到日志的信息
     */
    private void logToTextView(String message) {
        if (activity != null && tvLogOutput != null) {
            activity.runOnUiThread(() -> tvLogOutput.append(message));
        }
    }

}
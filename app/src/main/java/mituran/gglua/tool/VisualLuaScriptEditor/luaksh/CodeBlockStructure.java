package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import java.util.ArrayList;
import java.util.List;

public class CodeBlockStructure {

    public enum PartType {
        LABEL,
        INPUT
    }

    public static class Part {
        public PartType type;
        public String text;
        public String value;

        public Part(PartType type, String text) {
            this.type = type;
            this.text = text;
            this.value = "";
        }

        public Part(PartType type, String text, String value) {
            this.type = type;
            this.text = text;
            this.value = value;
        }
    }

    private List<Part> parts;

    public CodeBlockStructure() {
        this.parts = new ArrayList<>();
    }

    public CodeBlockStructure addLabel(String text) {
        parts.add(new Part(PartType.LABEL, text));
        return this;
    }

    public CodeBlockStructure addInput(String hint) {
        parts.add(new Part(PartType.INPUT, hint));
        return this;
    }

    public CodeBlockStructure addInput(String hint, String defaultValue) {
        parts.add(new Part(PartType.INPUT, hint, defaultValue));
        return this;
    }

    public List<Part> getParts() {
        return parts;
    }

    public static CodeBlockStructure getStructure(CodeBlockType type) {
        CodeBlockStructure structure = new CodeBlockStructure();

        switch (type) {
            case MAIN_START:
                return structure.addLabel("📝 主程序入口");

            case FUNCTION_START:
                return structure.addLabel("⚙️ 函数参数：(")
                        .addInput("参数列表", "")
                        .addLabel(")");

            case COMMENT:
                return structure.addLabel("💬").addInput("注释内容");

            case PRINT:
                return structure.addLabel("打印").addInput("内容");

            case VARIABLE_ASSIGN:
                return structure.addLabel("变量").addInput("变量名")
                        .addLabel("赋值为").addInput("值");

            case VARIABLE_DECLARE:
                return structure.addLabel("声明变量").addInput("变量名");

            case LOCAL_VARIABLE:
                return structure.addLabel("局部变量").addInput("变量名")
                        .addLabel("=").addInput("初始值");

            case IF:
                return structure.addLabel("如果").addInput("条件")
                        .addLabel("成立");

            case ELSEIF:
                return structure.addLabel("否则如果").addInput("条件")
                        .addLabel("成立");

            case ELSE:
                return structure.addLabel("否则");

            case END:
                return structure.addLabel("结束");

            case FOR:
                return structure.addLabel("循环").addInput("变量名")
                        .addLabel("从").addInput("起始值")
                        .addLabel("到").addInput("结束值");

            case WHILE:
                return structure.addLabel("当").addInput("条件")
                        .addLabel("成立时循环");

            case REPEAT:
                return structure.addLabel("重复执行");

            case UNTIL:
                return structure.addLabel("直到").addInput("条件")
                        .addLabel("成立");

            case BREAK:
                return structure.addLabel("跳出循环");

            case FUNCTION:
                return structure.addLabel("函数").addInput("函数名(参数)");

            case RETURN:
                return structure.addLabel("返回").addInput("值");

            case FUNCTION_CALL:
                return structure.addLabel("调用").addInput("函数名(参数)");

            case TABLE_CREATE:
                return structure.addLabel("创建表").addInput("表名");

            case TABLE_INSERT:
                return structure.addLabel("向表").addInput("表名")
                        .addLabel("插入").addInput("值");

            case TABLE_ACCESS:
                return structure.addLabel("表").addInput("表名")
                        .addLabel("[").addInput("索引").addLabel("]");

            // ===== GG 搜索 =====
            case GG_SEARCH_NUMBER:
                return structure.addLabel("搜索数值").addInput("搜索值")
                        .addLabel("类型").addInput("类型", "gg.TYPE_AUTO")
                        .addLabel("加密").addInput("false/true", "false")
                        .addLabel("符号").addInput("符号", "gg.SIGN_EQUAL");

            case GG_SEARCH_ADDRESS:
                return structure.addLabel("搜索地址").addInput("地址文本")
                        .addLabel("掩码").addInput("掩码", "-1")
                        .addLabel("类型").addInput("类型", "gg.TYPE_AUTO")
                        .addLabel("符号").addInput("符号", "gg.SIGN_EQUAL");

            case GG_SEARCH_FUZZY:
                return structure.addLabel("模糊搜索精炼").addLabel("差值").addInput("差值", "'0'")
                        .addLabel("符号").addInput("符号", "gg.SIGN_FUZZY_EQUAL")
                        .addLabel("类型").addInput("类型", "gg.TYPE_AUTO");

            case GG_START_FUZZY:
                return structure.addLabel("开始模糊搜索").addLabel("类型").addInput("类型", "gg.TYPE_AUTO");

            case GG_SEARCH_POINTER:
                return structure.addLabel("搜索指针").addLabel("最大偏移").addInput("偏移", "0");

            case GG_REFINE_NUMBER:
                return structure.addLabel("精炼数值").addInput("搜索值")
                        .addLabel("类型").addInput("类型", "gg.TYPE_AUTO")
                        .addLabel("加密").addInput("false/true", "false")
                        .addLabel("符号").addInput("符号", "gg.SIGN_EQUAL");

            case GG_REFINE_ADDRESS:
                return structure.addLabel("精炼地址").addInput("地址文本")
                        .addLabel("掩码").addInput("掩码", "-1")
                        .addLabel("类型").addInput("类型", "gg.TYPE_AUTO")
                        .addLabel("符号").addInput("符号", "gg.SIGN_EQUAL");

            // ===== GG 结果 =====
            case GG_GET_RESULTS:
                return structure.addLabel("获取结果到").addInput("变量名", "results")
                        .addLabel("最大数量").addInput("数量", "100");

            case GG_GET_RESULTS_COUNT:
                return structure.addLabel("获取结果数量到").addInput("变量名", "count");

            case GG_CLEAR_RESULTS:
                return structure.addLabel("清空搜索结果");

            case GG_LOAD_RESULTS:
                return structure.addLabel("加载结果表").addInput("结果表变量");

            case GG_REMOVE_RESULTS:
                return structure.addLabel("移除结果").addInput("结果表变量");

            case GG_EDIT_ALL:
                return structure.addLabel("编辑全部结果").addLabel("值").addInput("新值")
                        .addLabel("类型").addInput("类型", "gg.TYPE_DWORD");

            case GG_GET_SELECTED_RESULTS:
                return structure.addLabel("获取已选结果到").addInput("变量名", "selected");

            // ===== GG 内存读写 =====
            case GG_GET_VALUES:
                return structure.addLabel("读取值到").addInput("变量名", "values")
                        .addLabel("值表").addInput("值表变量");

            case GG_SET_VALUES:
                return structure.addLabel("写入值").addInput("值表变量");

            case GG_COPY_MEMORY:
                return structure.addLabel("复制内存 从").addInput("源地址")
                        .addLabel("到").addInput("目标地址")
                        .addLabel("字节数").addInput("字节数");

            case GG_ALLOCATE_PAGE:
                return structure.addLabel("分配内存页到").addInput("变量名", "page")
                        .addLabel("模式").addInput("模式", "gg.PROT_READ | gg.PROT_EXEC")
                        .addLabel("地址").addInput("地址", "0");

            case GG_DUMP_MEMORY:
                return structure.addLabel("转储内存 从").addInput("起始地址")
                        .addLabel("到").addInput("结束地址")
                        .addLabel("目录").addInput("保存目录");

            case GG_GET_VALUES_RANGE:
                return structure.addLabel("获取值的内存区域到").addInput("变量名", "ranges")
                        .addLabel("值表").addInput("值表变量");

            // ===== GG 保存列表 =====
            case GG_ADD_LIST_ITEMS:
                return structure.addLabel("添加到保存列表").addInput("项目表变量");

            case GG_GET_LIST_ITEMS:
                return structure.addLabel("获取保存列表到").addInput("变量名", "listItems");

            case GG_REMOVE_LIST_ITEMS:
                return structure.addLabel("从保存列表移除").addInput("项目表变量");

            case GG_CLEAR_LIST:
                return structure.addLabel("清空保存列表");

            case GG_SAVE_LIST:
                return structure.addLabel("保存列表到文件").addInput("文件路径")
                        .addLabel("标志").addInput("标志", "0");

            case GG_LOAD_LIST:
                return structure.addLabel("从文件加载列表").addInput("文件路径")
                        .addLabel("标志").addInput("标志", "0");

            case GG_GET_SELECTED_LIST_ITEMS:
                return structure.addLabel("获取已选列表项到").addInput("变量名", "selected");

            // ===== GG 进程 =====
            case GG_GET_TARGET_INFO:
                return structure.addLabel("获取目标进程信息到").addInput("变量名", "info");

            case GG_GET_TARGET_PACKAGE:
                return structure.addLabel("获取目标包名到").addInput("变量名", "pkg");

            case GG_PROCESS_PAUSE:
                return structure.addLabel("暂停进程");

            case GG_PROCESS_RESUME:
                return structure.addLabel("恢复进程");

            case GG_PROCESS_TOGGLE:
                return structure.addLabel("切换进程暂停状态");

            case GG_PROCESS_KILL:
                return structure.addLabel("强制结束进程");

            case GG_IS_PROCESS_PAUSED:
                return structure.addLabel("进程是否暂停到").addInput("变量名", "paused");

            // ===== GG UI/对话框 =====
            case GG_ALERT:
                return structure.addLabel("弹出对话框到").addInput("变量名", "ret")
                        .addLabel("文本").addInput("提示文本")
                        .addLabel("确定按钮").addInput("按钮文本", "'ok'")
                        .addLabel("取消按钮").addInput("按钮文本", "nil")
                        .addLabel("中立按钮").addInput("按钮文本", "nil");

            case GG_TOAST:
                return structure.addLabel("显示提示").addInput("提示文本")
                        .addLabel("快速").addInput("false/true", "false");

            case GG_PROMPT:
                return structure.addLabel("输入对话框到").addInput("变量名", "input")
                        .addLabel("提示表").addInput("提示表")
                        .addLabel("默认值表").addInput("默认值表", "{}")
                        .addLabel("类型表").addInput("类型表", "{}");

            case GG_CHOICE:
                return structure.addLabel("选择对话框到").addInput("变量名", "sel")
                        .addLabel("选项表").addInput("选项表")
                        .addLabel("默认选中").addInput("索引", "nil")
                        .addLabel("标题").addInput("标题", "nil");

            case GG_MULTI_CHOICE:
                return structure.addLabel("多选对话框到").addInput("变量名", "sel")
                        .addLabel("选项表").addInput("选项表")
                        .addLabel("默认选中表").addInput("选中表", "{}")
                        .addLabel("标题").addInput("标题", "nil");

            case GG_SET_VISIBLE:
                return structure.addLabel("设置GG可见性").addInput("true/false", "true");

            case GG_IS_VISIBLE:
                return structure.addLabel("GG是否可见到").addInput("变量名", "visible");

            case GG_SHOW_UI_BUTTON:
                return structure.addLabel("显示脚本UI按钮");

            case GG_HIDE_UI_BUTTON:
                return structure.addLabel("隐藏脚本UI按钮");

            case GG_IS_CLICKED_UI_BUTTON:
                return structure.addLabel("UI按钮是否被点击到").addInput("变量名", "clicked");

            // ===== GG 速度/时间 =====
            case GG_SET_SPEED:
                return structure.addLabel("设置速度").addInput("速度值", "1.0");

            case GG_GET_SPEED:
                return structure.addLabel("获取当前速度到").addInput("变量名", "speed");

            case GG_TIME_JUMP:
                return structure.addLabel("时间跳跃").addInput("时间字符串");

            case GG_UNRANDOMIZER:
                return structure.addLabel("反随机化").addLabel("qword").addInput("qword值", "nil")
                        .addLabel("qincr").addInput("增量", "nil")
                        .addLabel("double").addInput("double值", "nil")
                        .addLabel("dincr").addInput("增量", "nil");

            // ===== GG 内存区域 =====
            case GG_SET_RANGES:
                return structure.addLabel("设置内存区域").addInput("区域掩码", "gg.REGION_ANONYMOUS");

            case GG_GET_RANGES:
                return structure.addLabel("获取内存区域掩码到").addInput("变量名", "ranges");

            case GG_GET_RANGES_LIST:
                return structure.addLabel("获取内存区域列表到").addInput("变量名", "rangesList")
                        .addLabel("过滤").addInput("过滤字符串", "''");

            // ===== GG 工具/其他 =====
            case GG_SLEEP:
                return structure.addLabel("休眠").addInput("毫秒数", "1000").addLabel("毫秒");

            case GG_REQUIRE:
                return structure.addLabel("要求GG版本").addInput("版本号", "nil")
                        .addLabel("构建号").addInput("构建号", "0");

            case GG_COPY_TEXT:
                return structure.addLabel("复制到剪贴板").addInput("文本内容");

            case GG_MAKE_REQUEST:
                return structure.addLabel("HTTP请求到").addInput("变量名", "resp")
                        .addLabel("URL").addInput("URL地址")
                        .addLabel("头部表").addInput("头部表", "{}")
                        .addLabel("POST数据").addInput("数据", "nil");

            case GG_BYTES:
                return structure.addLabel("获取字节到").addInput("变量名", "b")
                        .addLabel("文本").addInput("文本内容")
                        .addLabel("编码").addInput("编码", "'UTF-8'");

            case GG_DISASM:
                return structure.addLabel("反汇编到").addInput("变量名", "asm")
                        .addLabel("类型").addInput("ASM类型")
                        .addLabel("地址").addInput("地址")
                        .addLabel("操作码").addInput("操作码");

            case GG_NUMBER_FROM_LOCALE:
                return structure.addLabel("本地化数字转英文到").addInput("变量名", "num")
                        .addLabel("数字").addInput("数字字符串");

            case GG_NUMBER_TO_LOCALE:
                return structure.addLabel("数字转本地化到").addInput("变量名", "num")
                        .addLabel("数字").addInput("数字字符串");

            case GG_IS_PACKAGE_INSTALLED:
                return structure.addLabel("应用是否安装到").addInput("变量名", "installed")
                        .addLabel("包名").addInput("包名");

            case GG_SAVE_VARIABLE:
                return structure.addLabel("保存变量到文件").addInput("变量")
                        .addLabel("文件名").addInput("文件路径");

            case GG_GET_FILE:
                return structure.addLabel("获取脚本文件名到").addInput("变量名", "file");

            case GG_GET_LINE:
                return structure.addLabel("获取脚本行号到").addInput("变量名", "line");

            case GG_GET_LOCALE:
                return structure.addLabel("获取GG语言到").addInput("变量名", "locale");

            case GG_GET_ACTIVE_TAB:
                return structure.addLabel("获取GG活动标签到").addInput("变量名", "tab");

            case GG_GOTO_ADDRESS:
                return structure.addLabel("跳转到地址").addInput("地址");

            case GG_GET_SELECTED_ELEMENTS:
                return structure.addLabel("获取内存编辑器选中地址到").addInput("变量名", "elements");

            case GG_SKIP_RESTORE_STATE:
                return structure.addLabel("跳过恢复GG状态");

            default:
                return structure.addInput("内容");
        }
    }

    public static String generateCode(CodeBlockType type, List<Part> parts) {
        StringBuilder code = new StringBuilder();

        switch (type) {
            case MAIN_START:
            case FUNCTION_START:
                return null;

            case COMMENT:
                code.append("-- ").append(getInputValue(parts, 0));
                break;

            case PRINT:
                code.append("print(").append(getInputValue(parts, 0)).append(")");
                break;

            case VARIABLE_ASSIGN:
                code.append(getInputValue(parts, 0)).append(" = ")
                        .append(getInputValue(parts, 1));
                break;

            case VARIABLE_DECLARE:
                code.append(getInputValue(parts, 0)).append(" = nil");
                break;

            case LOCAL_VARIABLE:
                code.append("local ").append(getInputValue(parts, 0))
                        .append(" = ").append(getInputValue(parts, 1));
                break;

            case IF:
                code.append("if ").append(getInputValue(parts, 0)).append(" then");
                break;

            case ELSEIF:
                code.append("elseif ").append(getInputValue(parts, 0)).append(" then");
                break;

            case ELSE:
                code.append("else");
                break;

            case END:
                code.append("end");
                break;

            case FOR:
                code.append("for ").append(getInputValue(parts, 0))
                        .append(" = ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(" do");
                break;

            case WHILE:
                code.append("while ").append(getInputValue(parts, 0)).append(" do");
                break;

            case REPEAT:
                code.append("repeat");
                break;

            case UNTIL:
                code.append("until ").append(getInputValue(parts, 0));
                break;

            case BREAK:
                code.append("break");
                break;

            case FUNCTION:
                code.append("function ").append(getInputValue(parts, 0));
                break;

            case RETURN: {
                String returnValue = getInputValue(parts, 0);
                if (returnValue.isEmpty()) {
                    code.append("return");
                } else {
                    code.append("return ").append(returnValue);
                }
                break;
            }

            case FUNCTION_CALL:
                code.append(getInputValue(parts, 0));
                break;

            case TABLE_CREATE:
                code.append(getInputValue(parts, 0)).append(" = {}");
                break;

            case TABLE_INSERT:
                code.append("table.insert(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1)).append(")");
                break;

            case TABLE_ACCESS:
                code.append(getInputValue(parts, 0)).append("[")
                        .append(getInputValue(parts, 1)).append("]");
                break;

            // ===== GG 搜索 =====
            case GG_SEARCH_NUMBER:
                code.append("gg.searchNumber(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            case GG_SEARCH_ADDRESS:
                code.append("gg.searchAddress(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            case GG_SEARCH_FUZZY:
                code.append("gg.searchFuzzy(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2)).append(")");
                break;

            case GG_START_FUZZY:
                code.append("gg.startFuzzy(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_SEARCH_POINTER:
                code.append("gg.searchPointer(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_REFINE_NUMBER:
                code.append("gg.refineNumber(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            case GG_REFINE_ADDRESS:
                code.append("gg.refineAddress(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            // ===== GG 结果 =====
            case GG_GET_RESULTS:
                code.append(getInputValue(parts, 0)).append(" = gg.getResults(")
                        .append(getInputValue(parts, 1)).append(")");
                break;

            case GG_GET_RESULTS_COUNT:
                code.append(getInputValue(parts, 0)).append(" = gg.getResultsCount()");
                break;

            case GG_CLEAR_RESULTS:
                code.append("gg.clearResults()");
                break;

            case GG_LOAD_RESULTS:
                code.append("gg.loadResults(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_REMOVE_RESULTS:
                code.append("gg.removeResults(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_EDIT_ALL:
                code.append("gg.editAll(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1)).append(")");
                break;

            case GG_GET_SELECTED_RESULTS:
                code.append(getInputValue(parts, 0)).append(" = gg.getSelectedResults()");
                break;

            // ===== GG 内存读写 =====
            case GG_GET_VALUES:
                code.append(getInputValue(parts, 0)).append(" = gg.getValues(")
                        .append(getInputValue(parts, 1)).append(")");
                break;

            case GG_SET_VALUES:
                code.append("gg.setValues(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_COPY_MEMORY:
                code.append("gg.copyMemory(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2)).append(")");
                break;

            case GG_ALLOCATE_PAGE:
                code.append(getInputValue(parts, 0)).append(" = gg.allocatePage(")
                        .append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2)).append(")");
                break;

            case GG_DUMP_MEMORY:
                code.append("gg.dumpMemory(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2)).append(")");
                break;

            case GG_GET_VALUES_RANGE:
                code.append(getInputValue(parts, 0)).append(" = gg.getValuesRange(")
                        .append(getInputValue(parts, 1)).append(")");
                break;

            // ===== GG 保存列表 =====
            case GG_ADD_LIST_ITEMS:
                code.append("gg.addListItems(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_GET_LIST_ITEMS:
                code.append(getInputValue(parts, 0)).append(" = gg.getListItems()");
                break;

            case GG_REMOVE_LIST_ITEMS:
                code.append("gg.removeListItems(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_CLEAR_LIST:
                code.append("gg.clearList()");
                break;

            case GG_SAVE_LIST:
                code.append("gg.saveList(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1)).append(")");
                break;

            case GG_LOAD_LIST:
                code.append("gg.loadList(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1)).append(")");
                break;

            case GG_GET_SELECTED_LIST_ITEMS:
                code.append(getInputValue(parts, 0)).append(" = gg.getSelectedListItems()");
                break;

            // ===== GG 进程 =====
            case GG_GET_TARGET_INFO:
                code.append(getInputValue(parts, 0)).append(" = gg.getTargetInfo()");
                break;

            case GG_GET_TARGET_PACKAGE:
                code.append(getInputValue(parts, 0)).append(" = gg.getTargetPackage()");
                break;

            case GG_PROCESS_PAUSE:
                code.append("gg.processPause()");
                break;

            case GG_PROCESS_RESUME:
                code.append("gg.processResume()");
                break;

            case GG_PROCESS_TOGGLE:
                code.append("gg.processToggle()");
                break;

            case GG_PROCESS_KILL:
                code.append("gg.processKill()");
                break;

            case GG_IS_PROCESS_PAUSED:
                code.append(getInputValue(parts, 0)).append(" = gg.isProcessPaused()");
                break;

            // ===== GG UI/对话框 =====
            case GG_ALERT:
                code.append(getInputValue(parts, 0)).append(" = gg.alert(")
                        .append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3))
                        .append(", ").append(getInputValue(parts, 4)).append(")");
                break;

            case GG_TOAST:
                code.append("gg.toast(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1)).append(")");
                break;

            case GG_PROMPT:
                code.append(getInputValue(parts, 0)).append(" = gg.prompt(")
                        .append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            case GG_CHOICE:
                code.append(getInputValue(parts, 0)).append(" = gg.choice(")
                        .append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            case GG_MULTI_CHOICE:
                code.append(getInputValue(parts, 0)).append(" = gg.multiChoice(")
                        .append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            case GG_SET_VISIBLE:
                code.append("gg.setVisible(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_IS_VISIBLE:
                code.append(getInputValue(parts, 0)).append(" = gg.isVisible()");
                break;

            case GG_SHOW_UI_BUTTON:
                code.append("gg.showUiButton()");
                break;

            case GG_HIDE_UI_BUTTON:
                code.append("gg.hideUiButton()");
                break;

            case GG_IS_CLICKED_UI_BUTTON:
                code.append(getInputValue(parts, 0)).append(" = gg.isClickedUiButton()");
                break;

            // ===== GG 速度/时间 =====
            case GG_SET_SPEED:
                code.append("gg.setSpeed(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_GET_SPEED:
                code.append(getInputValue(parts, 0)).append(" = gg.getSpeed()");
                break;

            case GG_TIME_JUMP:
                code.append("gg.timeJump(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_UNRANDOMIZER:
                code.append("gg.unrandomizer(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            // ===== GG 内存区域 =====
            case GG_SET_RANGES:
                code.append("gg.setRanges(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_GET_RANGES:
                code.append(getInputValue(parts, 0)).append(" = gg.getRanges()");
                break;

            case GG_GET_RANGES_LIST:
                code.append(getInputValue(parts, 0)).append(" = gg.getRangesList(")
                        .append(getInputValue(parts, 1)).append(")");
                break;

            // ===== GG 工具/其他 =====
            case GG_SLEEP:
                code.append("gg.sleep(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_REQUIRE:
                code.append("gg.require(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1)).append(")");
                break;

            case GG_COPY_TEXT:
                code.append("gg.copyText(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_MAKE_REQUEST:
                code.append(getInputValue(parts, 0)).append(" = gg.makeRequest(")
                        .append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            case GG_BYTES:
                code.append(getInputValue(parts, 0)).append(" = gg.bytes(")
                        .append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2)).append(")");
                break;

            case GG_DISASM:
                code.append(getInputValue(parts, 0)).append(" = gg.disasm(")
                        .append(getInputValue(parts, 1))
                        .append(", ").append(getInputValue(parts, 2))
                        .append(", ").append(getInputValue(parts, 3)).append(")");
                break;

            case GG_NUMBER_FROM_LOCALE:
                code.append(getInputValue(parts, 0)).append(" = gg.numberFromLocale(")
                        .append(getInputValue(parts, 1)).append(")");
                break;

            case GG_NUMBER_TO_LOCALE:
                code.append(getInputValue(parts, 0)).append(" = gg.numberToLocale(")
                        .append(getInputValue(parts, 1)).append(")");
                break;

            case GG_IS_PACKAGE_INSTALLED:
                code.append(getInputValue(parts, 0)).append(" = gg.isPackageInstalled(")
                        .append(getInputValue(parts, 1)).append(")");
                break;

            case GG_SAVE_VARIABLE:
                code.append("gg.saveVariable(").append(getInputValue(parts, 0))
                        .append(", ").append(getInputValue(parts, 1)).append(")");
                break;

            case GG_GET_FILE:
                code.append(getInputValue(parts, 0)).append(" = gg.getFile()");
                break;

            case GG_GET_LINE:
                code.append(getInputValue(parts, 0)).append(" = gg.getLine()");
                break;

            case GG_GET_LOCALE:
                code.append(getInputValue(parts, 0)).append(" = gg.getLocale()");
                break;

            case GG_GET_ACTIVE_TAB:
                code.append(getInputValue(parts, 0)).append(" = gg.getActiveTab()");
                break;

            case GG_GOTO_ADDRESS:
                code.append("gg.gotoAddress(").append(getInputValue(parts, 0)).append(")");
                break;

            case GG_GET_SELECTED_ELEMENTS:
                code.append(getInputValue(parts, 0)).append(" = gg.getSelectedElements()");
                break;

            case GG_SKIP_RESTORE_STATE:
                code.append("gg.skipRestoreState()");
                break;

            default:
                code.append(getInputValue(parts, 0));
                break;
        }

        return code.toString();
    }

    private static String getInputValue(List<Part> parts, int inputIndex) {
        int currentInputIndex = 0;
        for (Part part : parts) {
            if (part.type == PartType.INPUT) {
                if (currentInputIndex == inputIndex) {
                    return part.value != null && !part.value.isEmpty()
                            ? part.value : "";
                }
                currentInputIndex++;
            }
        }
        return "";
    }

    public static String getInputValueWithDefault(List<Part> parts, int inputIndex, String defaultValue) {
        int currentInputIndex = 0;
        for (Part part : parts) {
            if (part.type == PartType.INPUT) {
                if (currentInputIndex == inputIndex) {
                    return part.value != null && !part.value.isEmpty()
                            ? part.value : defaultValue;
                }
                currentInputIndex++;
            }
        }
        return defaultValue;
    }

    public static List<Part> parseOldValue(CodeBlockType type, String oldValue) {
        CodeBlockStructure structure = getStructure(type);
        List<Part> parts = new ArrayList<>();

        for (Part p : structure.getParts()) {
            parts.add(new Part(p.type, p.text, p.value));
        }

        if (oldValue == null || oldValue.isEmpty()) {
            return parts;
        }

        for (Part part : parts) {
            if (part.type == PartType.INPUT) {
                part.value = oldValue;
                break;
            }
        }

        return parts;
    }
}
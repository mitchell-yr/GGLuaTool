package mituran.gglua.tool.luaTool;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaFunctionAndVariableObfuscate {

    // Lua关键字，不应该被混淆
    private static final Set<String> LUA_KEYWORDS = new HashSet<>(Arrays.asList(
            "and", "break", "do", "else", "elseif", "end", "false", "for",
            "function", "if", "in", "local", "nil", "not", "or", "repeat",
            "return", "then", "true", "until", "while", "goto",
            // Lua内置函数
            "print", "type", "tonumber", "tostring", "pairs", "ipairs", "next",
            "select", "rawget", "rawset", "setmetatable", "getmetatable",
            "require", "module", "package", "string", "table", "math", "io",
            "os", "debug", "coroutine", "assert", "error", "pcall", "xpcall",
            "loadstring", "load", "loadfile", "dofile", "_G", "_VERSION"
    ));

    // 自定义的gg库函数
    private static final Set<String> GG_FUNCTIONS = new HashSet<>(Arrays.asList(
            "alert", "bytes", "choice", "clearResults", "copyMemory", "copyText",
            "dumpMemory", "editAll", "getFile", "getLine", "getLocale", "getRanges",
            "getRangesList", "getResults", "getResultCount", "getSpeed", "getTargetInfo",
            "getTargetPackage", "getValues", "getValuesRange", "gotoAddress",
            "isPackageInstalled", "isProcessPaused", "isVisible", "loadList",
            "multiChoice", "processKill", "processPause", "processResume",
            "processToggle", "prompt", "removeResults", "require", "saveList",
            "searchAddress", "searchFuzzy", "searchNumber", "setRanges", "setSpeed",
            "setValues", "setVisible", "skipRestoreState", "sleep", "startFuzzy",
            "timeJump", "toast","refineAddress"
    ));

    // 存储变量名/函数名的映射关系
    private Map<String, String> nameMapping;
    // 存储gg函数的映射关系
    private Map<String, String> ggFunctionMapping;
    private int obfuscateCounter;
    private boolean useRandomNames;
    private boolean obfuscateGGFunctions;

    public LuaFunctionAndVariableObfuscate() {
        this.nameMapping = new HashMap<>();
        this.ggFunctionMapping = new HashMap<>();
        this.obfuscateCounter = 0;
        this.useRandomNames = true;
        this.obfuscateGGFunctions = true; // 默认混淆gg函数
    }

    /**
     * 主要的加密函数
     * @param luaScript 原始Lua脚本
     * @return 混淆后的Lua脚本
     */
    public String encryptFunction(String luaScript) {
        if (luaScript == null || luaScript.isEmpty()) {
            return luaScript;
        }

        // 1. 提取所有的标识符（函数名和变量名）
        Set<String> identifiers = extractIdentifiers(luaScript);

        // 2. 提取所有使用的gg函数
        Set<String> usedGGFunctions = extractGGFunctions(luaScript);

        // 3. 为每个标识符生成混淆名称
        generateObfuscatedNames(identifiers);

        // 4. 为gg函数生成混淆名称
        if (obfuscateGGFunctions) {
            generateGGFunctionMappings(usedGGFunctions);
        }

        // 5. 替换脚本中的标识符
        String obfuscatedScript = replaceIdentifiers(luaScript);

        // 6. 如果混淆了gg函数，需要在脚本开头添加映射代码
        if (obfuscateGGFunctions && !ggFunctionMapping.isEmpty()) {
            obfuscatedScript = addGGFunctionMapping(obfuscatedScript);
        }

        return obfuscatedScript;
    }

    /**
     * 提取脚本中使用的所有gg函数
     */
    private Set<String> extractGGFunctions(String script) {
        Set<String> usedFunctions = new HashSet<>();

        // 匹配 gg.functionName 模式
        Pattern ggPattern = Pattern.compile("gg\\.([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher ggMatcher = ggPattern.matcher(script);

        while (ggMatcher.find()) {
            String funcName = ggMatcher.group(1);
            if (GG_FUNCTIONS.contains(funcName)) {
                usedFunctions.add(funcName);
            }
        }

        return usedFunctions;
    }

    /**
     * 为gg函数生成混淆映射
     */
    private void generateGGFunctionMappings(Set<String> usedFunctions) {
        for (String funcName : usedFunctions) {
            if (!ggFunctionMapping.containsKey(funcName)) {
                String obfuscatedName = generateObfuscatedFunctionName();
                ggFunctionMapping.put(funcName, obfuscatedName);
            }
        }
    }

    /**
     * 生成混淆的函数名
     */
    private String generateObfuscatedFunctionName() {
        if (useRandomNames) {
            // 使用混淆字符
            String[] chars = {"l", "I", "1", "O", "0", "_"};
            StringBuilder name = new StringBuilder("_");
            Random random = new Random();

            for (int i = 0; i < 8; i++) {
                name.append(chars[random.nextInt(chars.length)]);
            }
            name.append(Integer.toHexString(obfuscateCounter++));
            return name.toString();
        } else {
            return "_gg" + Integer.toHexString(obfuscateCounter++);
        }
    }

    /**
     * 添加gg函数映射代码到脚本开头
     */
    private String addGGFunctionMapping(String script) {
        StringBuilder mappingCode = new StringBuilder();

        // 添加注释
        mappingCode.append("-- Obfuscated by LuaObfuscator\n");

        // 保存原始gg表
        mappingCode.append("local _gg = gg\n");

        // 创建新的gg表
        mappingCode.append("gg = {}\n");

        // 添加所有的函数映射
        for (Map.Entry<String, String> entry : ggFunctionMapping.entrySet()) {
            String original = entry.getKey();
            String obfuscated = entry.getValue();
            mappingCode.append(String.format("gg.%s = _gg.%s\n", obfuscated, original));
        }

        // 添加未使用但可能需要的常量（如gg.TYPE_DWORD等）
        mappingCode.append("-- Copy constants\n");
        mappingCode.append("for k, v in pairs(_gg) do\n");
        mappingCode.append("    if type(v) ~= 'function' then\n");
        mappingCode.append("        gg[k] = v\n");
        mappingCode.append("    end\n");
        mappingCode.append("end\n\n");

        return mappingCode.toString() + script;
    }

    /**
     * 提取脚本中的所有标识符
     */
    private Set<String> extractIdentifiers(String script) {
        Set<String> identifiers = new HashSet<>();

        // 移除字符串和注释，避免误匹配
        String cleanScript = removeStringsAndComments(script);

        // 匹配函数定义
        Pattern funcPattern = Pattern.compile("function\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
        Matcher funcMatcher = funcPattern.matcher(cleanScript);
        while (funcMatcher.find()) {
            String funcName = funcMatcher.group(1);
            if (!LUA_KEYWORDS.contains(funcName)) {
                identifiers.add(funcName);
            }
        }

        // 匹配local函数定义
        Pattern localFuncPattern = Pattern.compile("local\\s+function\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
        Matcher localFuncMatcher = localFuncPattern.matcher(cleanScript);
        while (localFuncMatcher.find()) {
            String funcName = localFuncMatcher.group(1);
            if (!LUA_KEYWORDS.contains(funcName)) {
                identifiers.add(funcName);
            }
        }

        // 匹配变量声明
        Pattern varPattern = Pattern.compile("local\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\s*,\\s*[a-zA-Z_][a-zA-Z0-9_]*)*)");
        Matcher varMatcher = varPattern.matcher(cleanScript);
        while (varMatcher.find()) {
            String varList = varMatcher.group(1);
            String[] vars = varList.split("\\s*,\\s*");
            for (String var : vars) {
                var = var.trim();
                if (!var.isEmpty() && !LUA_KEYWORDS.contains(var) && !var.equals("gg")) {
                    identifiers.add(var);
                }
            }
        }

        // 匹配函数参数
        Pattern paramPattern = Pattern.compile("function[^(]*\\(([^)]*)\\)");
        Matcher paramMatcher = paramPattern.matcher(cleanScript);
        while (paramMatcher.find()) {
            String params = paramMatcher.group(1);
            if (!params.trim().isEmpty()) {
                String[] paramArray = params.split("\\s*,\\s*");
                for (String param : paramArray) {
                    param = param.trim();
                    if (!param.isEmpty() && !LUA_KEYWORDS.contains(param)) {
                        identifiers.add(param);
                    }
                }
            }
        }

        // 匹配for循环变量
        Pattern forPattern = Pattern.compile("for\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*[=,]");
        Matcher forMatcher = forPattern.matcher(cleanScript);
        while (forMatcher.find()) {
            String varName = forMatcher.group(1);
            if (!LUA_KEYWORDS.contains(varName)) {
                identifiers.add(varName);
            }
        }

        return identifiers;
    }

    /**
     * 移除字符串和注释，避免误匹配
     */
    private String removeStringsAndComments(String script) {
        // 移除单行注释
        script = script.replaceAll("--[^\n]*", "");

        // 移除多行注释 - 使用(?s)标志让.匹配包括换行符
        script = script.replaceAll("(?s)--\\[\\[.*?\\]\\]", "");

        // 替换字符串为占位符
        script = script.replaceAll("\"[^\"]*\"", "\"\"");
        script = script.replaceAll("'[^']*'", "''");

        // 替换多行字符串 - 使用(?s)标志
        script = script.replaceAll("(?s)\\[\\[.*?\\]\\]", "[[]]");

        return script;
    }

    /**
     * 为标识符生成混淆名称
     */
    private void generateObfuscatedNames(Set<String> identifiers) {
        for (String identifier : identifiers) {
            if (!nameMapping.containsKey(identifier)) {
                String obfuscatedName = generateRandomName();
                nameMapping.put(identifier, obfuscatedName);
            }
        }
    }

    /**
     * 生成随机混淆名称
     */
    private String generateRandomName() {
        String chars = "ilIloO01";
        StringBuilder name = new StringBuilder("_");
        Random random = new Random();

        int length = 6 + random.nextInt(4);
        for (int i = 0; i < length; i++) {
            name.append(chars.charAt(random.nextInt(chars.length())));
        }

        name.append(Integer.toHexString(obfuscateCounter++));
        return name.toString();
    }

    /**
     * 替换脚本中的标识符
     */
    private String replaceIdentifiers(String script) {
        String result = script;

        // 先收集所有字符串内容
        List<String> strings = new ArrayList<>();
        // 使用(?s)标志处理多行字符串
        Pattern stringPattern = Pattern.compile("(\"[^\"]*\"|'[^']*'|(?s)\\[\\[.*?\\]\\])");
        Matcher stringMatcher = stringPattern.matcher(script);

        while (stringMatcher.find()) {
            strings.add(stringMatcher.group());
        }

        // 临时替换字符串为占位符
        int stringIndex = 0;
        for (String str : strings) {
            result = result.replace(str, "<<<STRING_" + (stringIndex++) + ">>>");
        }

        // 替换gg函数调用
        if (obfuscateGGFunctions) {
            for (Map.Entry<String, String> entry : ggFunctionMapping.entrySet()) {
                String original = entry.getKey();
                String obfuscated = entry.getValue();
                result = result.replaceAll("gg\\." + Pattern.quote(original) + "\\b", "gg." + obfuscated);
            }
        }

        // 替换普通标识符
        List<Map.Entry<String, String>> sortedMappings = new ArrayList<>(nameMapping.entrySet());
        sortedMappings.sort((a, b) -> b.getKey().length() - a.getKey().length());

        for (Map.Entry<String, String> entry : sortedMappings) {
            String original = entry.getKey();
            String obfuscated = entry.getValue();
            result = result.replaceAll("\\b" + Pattern.quote(original) + "\\b", obfuscated);
        }

        // 恢复字符串
        stringIndex = 0;
        for (String str : strings) {
            result = result.replace("<<<STRING_" + (stringIndex++) + ">>>", str);
        }

        return result;
    }

    /**
     * 设置是否混淆gg函数
     */
    public void setObfuscateGGFunctions(boolean obfuscate) {
        this.obfuscateGGFunctions = obfuscate;
    }

    /**
     * 获取所有映射表
     */
    public Map<String, String> getAllMappings() {
        Map<String, String> allMappings = new HashMap<>();
        allMappings.putAll(nameMapping);
        allMappings.putAll(ggFunctionMapping);
        return allMappings;
    }

    /**
     * 清除映射表
     */
    public void clearMapping() {
        nameMapping.clear();
        ggFunctionMapping.clear();
        obfuscateCounter = 0;
    }

    /**
     * 设置是否使用随机名称
     */
    public void setUseRandomNames(boolean useRandom) {
        this.useRandomNames = useRandom;
    }

    // 测试主函数
    public static void main(String[] args) {
        LuaFunctionAndVariableObfuscate obfuscator = new LuaFunctionAndVariableObfuscate();

        String testScript = "-- Game Guardian Script\n" +
                "local function searchValue(value)\n" +
                "    gg.clearResults()\n" +
                "    gg.searchNumber(value, gg.TYPE_DWORD)\n" +
                "    local results = gg.getResultCount()\n" +
                "    if results > 0 then\n" +
                "        gg.toast(\"Found \" .. results .. \" results\")\n" +
                "    end\n" +
                "    return results\n" +
                "end\n" +
                "\n" +
                "function modifyMemory(address, newValue)\n" +
                "    local values = {{\n" +
                "        address = address,\n" +
                "        value = newValue,\n" +
                "        flags = gg.TYPE_DWORD\n" +
                "    }}\n" +
                "    gg.setValues(values)\n" +
                "    gg.alert(\"Memory modified!\")\n" +
                "end\n" +
                "\n" +
                "local targetPackage = gg.getTargetPackage()\n" +
                "gg.alert(\"Target: \" .. targetPackage)\n" +
                "\n" +
                "local searchInput = gg.prompt({\"Enter value:\"}, {0}, {\"number\"})\n" +
                "if searchInput then\n" +
                "    local count = searchValue(searchInput[1])\n" +
                "    if count > 0 then\n" +
                "        local results = gg.getResults(100)\n" +
                "        for i, result in ipairs(results) do\n" +
                "            modifyMemory(result.address, 999999)\n" +
                "        end\n" +
                "    end\n" +
                "end\n" +
                "\n" +
                "gg.setVisible(false)\n" +
                "gg.sleep(1000)\n";

        System.out.println("原始脚本:");
        System.out.println(testScript);
        System.out.println("\n" + "=".repeat(50) + "\n");

        //String obfuscated = obfuscator.encryptFunction(testScript);

        System.out.println("混淆后的脚本:");
        //System.out.println(obfuscated);
        System.out.println("\n" + "=".repeat(50) + "\n");

        System.out.println("所有映射表:");
        //for (Map.Entry<String, String> entry : obfuscator.getAllMappings().entrySet()) {
        //    System.out.println(entry.getKey() + " -> " + entry.getValue());
        //}
    }
}
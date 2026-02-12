package mituran.gglua.tool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mituran.gglua.tool.luaTool.LuaFunctionAndVariableObfuscate;
import mituran.gglua.tool.luaTool.LuaStringEncryptor;

public class LuaEncryptionModule {
    

    /**
     * 加密选项枚举
     */
    public enum EncryptionOption {
        ANTI_UNLUAC("防unluac/TD反编译", "添加反编译保护"),
        STRING_ENCRYPT("字符串加密", "对字符串进行混淆加密"),
        ANTI_LOG("防log出源码", "防止函数调用记录暴露调用"),
        VARIABLE_OBFUSCATE("变量名和函数名混淆", "混淆局部变量名"),
        ANTI_LOAD("防load", ""),
        GARBAGE_CODE("花指令", "注入无用代码增加分析难度");

        private String name;
        private String description;

        EncryptionOption(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 应用所有选中的加密选项
     */
    public static String applyEncryption(String luaCode, boolean[] selectedOptions) {
        String result = luaCode;

        // 按顺序应用加密
        if (selectedOptions[2]) { // 防log出源码 - 需要先执行
            result = addAntiLogProtection(result);
        }

        if (selectedOptions[0]) { // 防unluac反编译
            result = addAntiDecompileProtection(result);
        }


        if (selectedOptions[1]) { // 字符串加密
            result = encryptStrings(result);
        }


        if (selectedOptions[4]) { // 防load
            result = antiLoad(result);
        }

        if (selectedOptions[5]) { //
            result = injectGarbageCode(result);
        }
        if (selectedOptions[3]) { // 变量名混淆
            result = obfuscateVariables(result);
        }
        return result;
    }

    /**
     * 添加反编译保护
     */
    private static String addAntiDecompileProtection(String code) {
        StringBuilder protection = new StringBuilder();

        protection.append("function a() end a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a()))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))) for i=1,0 do _() local _={} _._=_ _._=_._ _._={} for i in (_) do _[_]=_ end _()  local i={(BYQ or BYQ)} if i.i==i.i then i.i=i.i() end end while(true) do while(true) do break return end break end \n");

        return protection.toString() + code;
    }

    /**
     * 字符串加密
     */
    private static String encryptStrings(String code) {
        LuaStringEncryptor encryptor = new LuaStringEncryptor();
        String encryptedScript = encryptor.encryptString(code);
        return encryptedScript;
    }


    /**
     * 添加防log保护
     */
    private static String addAntiLogProtection(String code) {
        StringBuilder protection = new StringBuilder();

        protection.append("\nfrep=string.rep(\"mituran\",514514) for i=0,101 do gg.refineAddress(frep,0xFFFFFFFF) end \n");


        return protection.toString() + code;
    }

    /**
     * 变量名混淆
     */
    private static String obfuscateVariables(String code) {
        // 创建混淆器实例
        LuaFunctionAndVariableObfuscate obfuscator = new LuaFunctionAndVariableObfuscate();

// 启用gg函数混淆（默认开启）
        obfuscator.setObfuscateGGFunctions(true);

// 混淆Lua脚本
        String obfuscatedScript = obfuscator.encryptFunction(code);



        return obfuscatedScript;
    }

    /**
     * 防load
     */
    private static String antiLoad(String code) {
        StringBuilder protection = new StringBuilder();

        protection.append("""
                local timeinit=os.clock()
                for i=1,81 do
                loadfile("/system/priv-app/SystemUI/SystemUI.apk")
                loadfile("/system/priv-app/Settings/Settings.apk")
                end
                while os.clock()-timeinit>6 do
                gg.setVisible(true)
                print("请勿进行load操作")
                os.exit()
                end
                abc ={}\s
                abc.last = gg.getFile()\s
                abc.data = loadfile(abc.last)
                abc.cpp = abc.data \s
                if abc.cpp ~= nil then \s
                abc.data = nil \s
                ppb = abc.last:match("[^/]+$")
                ppi = "lohhhggg"\s
                pu = gg.getResults(5000) \s
                os.rename("" .. abc.last .. "", "" .. abc.last:gsub("/[^/]+$", "") .. "/" .. ppi .. "") \s
                prt = loadfile("" .. abc.last:gsub("/[^/]+$", "") .. "/" .. ppi .. "") \s
                if prt ~= nil then   \s
                os.rename("" .. abc.last:gsub("/[^/]+$", "") .. "/" .. ppi .. "", "" .. abc.last:gsub("/[^/]+$", "") .. "/" .. ppb .. "")   \s
                gg.alert("请勿LOAD")  \s
                while true do  \s
                os.exit()\s
                end
                end
                end
                """);

        return protection.toString() + code;
    }

    /**
     * 注入垃圾代码
     */
    private static String injectGarbageCode(String code) {
        //TODO:

        return code;
    }
}
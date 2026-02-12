package mituran.gglua.tool.luaTool;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.compiler.LuaC;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaSyntaxChecker {

    /**
     * 检查结果类
     */
    public static class CheckResult {
        private boolean valid;
        private String errorMessage;
        private int errorLine;
        private int errorColumn;

        public CheckResult(boolean valid) {
            this.valid = valid;
            this.errorMessage = "";
            this.errorLine = -1;
            this.errorColumn = -1;
        }

        public CheckResult(boolean valid, String errorMessage, int errorLine) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.errorLine = errorLine;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public int getErrorLine() {
            return errorLine;
        }

        public int getErrorColumn() {
            return errorColumn;
        }

        @Override
        public String toString() {
            if (valid) {
                return "语法正确";
            } else {
                String result = "语法错误: " + errorMessage;
                if (errorLine > 0) {
                    result += " (第 " + errorLine + " 行)";
                }
                return result;
            }
        }
    }

    private Globals globals;

    public LuaSyntaxChecker() {
        globals = JsePlatform.standardGlobals();
        LuaC.install(globals);
    }

    /**
     * 检查 Lua 脚本语法
     */
    public CheckResult checkSyntax(String luaScript) {
        if (luaScript == null || luaScript.trim().isEmpty()) {
            return new CheckResult(false, "脚本内容为空", 0);
        }

        try {
            globals.load(luaScript);
            return new CheckResult(true);
        } catch (LuaError e) {
            return parseError(e);
        } catch (Exception e) {
            return new CheckResult(false, e.getMessage(), -1);
        }
    }

    /**
     * 解析 LuaError 获取详细错误信息（简化版）
     * 去除 [string "script"]:line: 前缀
     */
    private CheckResult parseError(LuaError error) {
        String rawMessage = error.getMessage();
        if (rawMessage == null || rawMessage.isEmpty()) {
            return new CheckResult(false, "未知错误", -1);
        }

        int line = -1;
        String cleanMessage = rawMessage;

        // 查找 [string "..."]： 格式
        int stringPrefixEnd = -1;
        if (rawMessage.startsWith("[string ")) {
            stringPrefixEnd = rawMessage.indexOf("]:");
            if (stringPrefixEnd != -1) {
                stringPrefixEnd += 2; // 跳过 ]:
            }
        }

        if (stringPrefixEnd > 0) {
            // 从 ]: 之后开始解析
            String remaining = rawMessage.substring(stringPrefixEnd);

            // 查找第一个冒号，前面应该是行号
            int colonPos = remaining.indexOf(":");
            if (colonPos > 0) {
                String lineStr = remaining.substring(0, colonPos).trim();
                try {
                    line = Integer.parseInt(lineStr);
                    // 获取错误信息（冒号后面的部分）
                    cleanMessage = remaining.substring(colonPos + 1).trim();
                } catch (NumberFormatException e) {
                    // 如果解析失败，使用原始信息
                    cleanMessage = remaining;
                }
            } else {
                cleanMessage = remaining;
            }
        } else {
            // 尝试简单格式: 任意内容:行号:错误信息
            String[] parts = rawMessage.split(":", 3);
            if (parts.length >= 3) {
                try {
                    line = Integer.parseInt(parts[1].trim());
                    cleanMessage = parts[2].trim();
                } catch (NumberFormatException e) {
                    cleanMessage = rawMessage;
                }
            }
        }

        // 如果清理后的消息为空，使用原始消息
        if (cleanMessage == null || cleanMessage.trim().isEmpty()) {
            cleanMessage = rawMessage;
        }

        return new CheckResult(false, cleanMessage.trim(), line);
    }

    /**
     * 检查 Lua 脚本文件
     */
    public CheckResult checkSyntaxFromFile(String filePath) {
        try {
            String content = readFile(filePath);
            return checkSyntax(content);
        } catch (Exception e) {
            return new CheckResult(false, "读取文件失败: " + e.getMessage(), -1);
        }
    }

    /**
     * 读取文件内容
     */
    private String readFile(String filePath) throws Exception {
        java.io.File file = new java.io.File(filePath);
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return new String(data, "UTF-8");
    }
}
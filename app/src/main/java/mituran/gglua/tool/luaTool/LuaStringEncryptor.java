package mituran.gglua.tool.luaTool;

import java.util.Base64;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaStringEncryptor {

    private final Random random;
    private final String customBase64Table;

    public LuaStringEncryptor() {
        this.random = new Random();
        this.customBase64Table = generateCustomBase64Table();
    }

    public LuaStringEncryptor(long seed) {
        this.random = new Random(seed);
        this.customBase64Table = generateCustomBase64Table();
    }

    /**
     * 生成自定义的Base64字符表（打乱标准表）
     */
    private String generateCustomBase64Table() {
        String standard = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        char[] chars = standard.toCharArray();

        // Fisher-Yates 洗牌算法
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    /**
     * 生成解密函数（使用自定义字符表和密钥拆分）
     */
    private String generateDecryptFunction() {
        // 将自定义Base64表拆分成多个部分
        String part1 = customBase64Table.substring(0, 21);
        String part2 = customBase64Table.substring(21, 42);
        String part3 = customBase64Table.substring(42);

        // 转义单引号
        part1 = part1.replace("'", "\\'");
        part2 = part2.replace("'", "\\'");
        part3 = part3.replace("'", "\\'");

        return "local function _d(s)\n" +
                "    local b='" + part1 + "'" + "..'" + part2 + "'" + "..'" + part3 + "'\n" +
                "    s=string.gsub(s,'[^'..b..'=]','')\n" +
                "    return (s:gsub('.',function(x)\n" +
                "        if(x=='=')then return '' end\n" +
                "        local r,f='',(b:find(x)-1)\n" +
                "        for i=6,1,-1 do r=r..(f%2^i-f%2^(i-1)>0 and '1' or '0')end\n" +
                "        return r\n" +
                "    end):gsub('%d%d%d?%d?%d?%d?%d?%d?',function(x)\n" +
                "        if(#x~=8)then return '' end\n" +
                "        local c=0\n" +
                "        for i=1,8 do c=c+(x:sub(i,i)=='1' and 2^(8-i) or 0)end\n" +
                "        return string.char(c)\n" +
                "    end))\n" +
                "end\n\n";
    }

    /**
     * 使用自定义字符表进行Base64编码
     */
    private String encryptSingleString(String str) {
        String standard = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

        // 使用标准Base64编码
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String encoded = Base64.getEncoder().encodeToString(bytes);

        // 将标准Base64字符替换为自定义字符表
        StringBuilder result = new StringBuilder();
        for (char c : encoded.toCharArray()) {
            int index = standard.indexOf(c);
            if (index >= 0) {
                result.append(customBase64Table.charAt(index));
            } else {
                result.append(c); // 保留 '='
            }
        }

        return result.toString();
    }

    /**
     * 处理转义字符，将Lua字符串转换为实际字符串
     */
    private String unescapeLuaString(String str) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case 'n': result.append('\n'); i += 2; break;
                    case 'r': result.append('\r'); i += 2; break;
                    case 't': result.append('\t'); i += 2; break;
                    case '\\': result.append('\\'); i += 2; break;
                    case '"': result.append('"'); i += 2; break;
                    case '\'': result.append('\''); i += 2; break;
                    case '0': result.append('\0'); i += 2; break;
                    default:
                        // 处理 \ddd 数字转义
                        if (Character.isDigit(next)) {
                            int end = i + 1;
                            while (end < str.length() && end < i + 4 && Character.isDigit(str.charAt(end))) {
                                end++;
                            }
                            int code = Integer.parseInt(str.substring(i + 1, end));
                            result.append((char) code);
                            i = end;
                        } else {
                            result.append(c);
                            i++;
                        }
                        break;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    /**
     * 加密Lua脚本中的所有字符串
     */
    public String encryptString(String luaScript) {
        StringBuilder result = new StringBuilder();

        // 正则表达式匹配Lua字符串
        // 匹配双引号字符串、单引号字符串，但排除多行字符串 [[...]]
        String regex = "(?<!\\[)(?<!\\])(['\"])(?:(?=(\\\\?))\\2.)*?\\1";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(luaScript);

        int lastEnd = 0;
        boolean hasEncryption = false;

        while (matcher.find()) {
            String matched = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            // 获取引号类型
            char quote = matched.charAt(0);

            // 提取字符串内容（去掉引号）
            String content = matched.substring(1, matched.length() - 1);

            // 检查是否是多行字符串标记（跳过 [[ 或 ]]）
            if (content.startsWith("[") || content.startsWith("]")) {
                continue;
            }

            // 解析转义字符
            String unescaped = unescapeLuaString(content);

            // 加密字符串
            String encrypted = encryptSingleString(unescaped);

            // 添加未匹配的部分
            result.append(luaScript.substring(lastEnd, start));

            // 替换为解密函数调用
            result.append("_d(\"").append(encrypted).append("\")");

            lastEnd = end;
            hasEncryption = true;
        }

        // 添加剩余部分
        result.append(luaScript.substring(lastEnd));

        // 如果有加密的字符串，在开头添加解密函数
        if (hasEncryption) {
            return generateDecryptFunction() + result.toString();
        }

        return result.toString();
    }

    /**
     * 测试示例
     */
    public static void main(String[] args) {
        LuaStringEncryptor encryptor = new LuaStringEncryptor();

        // 测试脚本
        String testScript =
                "local name = \"Hello World\"\n" +
                        "local msg = 'This is a test'\n" +
                        "print(\"Name: \" .. name)\n" +
                        "local escape = \"Line1\\nLine2\\tTabbed\"\n" +
                        "local quote = 'It\\'s working'\n" +
                        "-- 注释中的 \"字符串\" 不会被加密\n" +
                        "local multiline = [[这是多行字符串\n不会被加密]]\n" +
                        "function test()\n" +
                        "    return \"Result\"\n" +
                        "end";

        System.out.println("原始脚本:");
        System.out.println(testScript);
        System.out.println("\n" + "=".repeat(60) + "\n");

        String encrypted = encryptor.encryptString(testScript);
        System.out.println("加密后的脚本:");
        System.out.println(encrypted);

        System.out.println("\n" + "=".repeat(60) + "\n");
        System.out.println("自定义Base64字符表: " + encryptor.customBase64Table);
    }
}
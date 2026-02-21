# GGLuaTool

# *寻找合作者中*！目前一个人周末抽空写效率低下，急需合作者。

# 欢迎联系：

# qq:2185125049

# email:mitchell0yr@gmail.com

## 简介

这是一个悠然制作的GGlua脚本工具。用于编写、编译与反编译、加密适用于GameGuardian的lua脚本的工具。  
此软件目前只有部分功能，将在近期加入更多功能。正在持续更新！

此软件由AndroidStudio使用了java制作。

## 主要事项

请遵守开源协议！此软件仅供学习交流使用，**禁止用于违法用途**(如制作游戏外挂等非法用途)，若造成任何后果与开发者无关
此项目使用了deepseek、claude、gemini辅助，请按照《人工智能生成合成内容标识办法》等法规标识生成式人工智能使用

## 使用方法

> - 开袋即食（bushi）
> - 在Android Studio打开，注意gradle和java版本

#### 目前功能

- 脚本编辑器（基于sora-editor，支持代码补全、语法高亮等等）
- 支持一件插入脚本模板
- 插件正在重写支持
- 两种lua字节码反编译：unluac和TD（unluac包遇到技术性问题正在解决中）
- 支持编译脚本，添加加密
- 语法检查
- 可视化lua编辑
- 内置支持gg函数的lua虚拟机（可用于过防御、反检测的脚本函数调用自吐和动态调试，未来会加入插桩、变量追踪等）
- #### 注意：这些更新随时可能变动
  
#### 即将支持功能
  
  - cpp一键生成、编译（鉴于体量，可能会取消一件编译）
    
  - 构建直装（长期）
    
  - 构建发行品（生成内置脚本的GG客户端，且可自选附加函数）（在计划中）
    
  - 更高等级的加密
  
  - 基于内置luaj的即时动态调试 

  - 内置框架（基于spacecore（安卓9到14支持）。由于blackbox没做好gg适配，会在后续版本加入。如果有更好的开源免费框架欢迎推荐）

### 如何添加新的代码块类型


#### 1. 在 `CodeBlockType.java` 中添加新枚举

```java
public enum CodeBlockType {
    // ... 现有代码块 ...
    
    // 添加内容

    
    
    // ... 构造函数和其他方法保持不变 ...
}
```

#### 2. 在 `CodeBlockStructure.java` 中定义结构

在 `getStructure()` 方法的 `switch` 语句中添加：

```java
public static CodeBlockStructure getStructure(CodeBlockType type) {
    CodeBlockStructure structure = new CodeBlockStructure();

    switch (type) {
        // ... 现有case ...
        
        // IO操作
        case FILE_OPEN:
            return structure.addLabel("打开文件")
                    .addInput("文件名")
                    .addLabel("模式")
                    .addInput("r/w/a");

        case FILE_READ:
            return structure.addLabel("读取文件内容到")
                    .addInput("变量名");

        case FILE_WRITE:
            return structure.addLabel("写入内容")
                    .addInput("内容");

        case FILE_CLOSE:
            return structure.addLabel("关闭文件");

        default:
            return structure.addInput("内容");
    }
}
```

#### 3. 在 `generateCode()` 方法中添加代码生成逻辑

```java
public static String generateCode(CodeBlockType type, List<Part> parts) {
    StringBuilder code = new StringBuilder();

    switch (type) {
        // ... 现有case ...
        
        // IO操作
        case FILE_OPEN:
            code.append("file, err = io.open(")
                    .append(getInputValue(parts, 0))
                    .append(", \"")
                    .append(getInputValue(parts, 1))
                    .append("\")");
            break;

        case FILE_READ:
            code.append(getInputValue(parts, 0))
                    .append(" = file:read(\"*a\")");
            break;

        case FILE_WRITE:
            code.append("file:write(")
                    .append(getInputValue(parts, 0))
                    .append(")");
            break;

        case FILE_CLOSE:
            code.append("file:close()");
            break;
            

        default:
            code.append(getInputValue(parts, 0));
            break;
    }

    return code.toString();
}
```

#### 4. 在 `CodeBlockTypeItem.java` 中添加分类

在 `createAllCategories()` 方法中添加新分类：

```java
public static List<CodeBlockTypeItem> createAllCategories() {
    List<CodeBlockTypeItem> categories = new ArrayList<>();

    // ... 现有分类 ...

    // IO操作分类
    CodeBlockTypeItem ioOps = new CodeBlockTypeItem("📁 文件IO操作");
    ioOps.addBlockType(CodeBlockType.FILE_OPEN);
    ioOps.addBlockType(CodeBlockType.FILE_READ);
    ioOps.addBlockType(CodeBlockType.FILE_WRITE);
    ioOps.addBlockType(CodeBlockType.FILE_CLOSE);
    categories.add(ioOps);


    return categories;
}
```
#### 注意事项

1. **颜色选择**：为同一类操作使用相同颜色便于识别
2. **默认值**：提供合理的默认值帮助用户理解
3. **提示文本**：使用清晰的中文提示
4. **代码生成**：确保生成的Lua代码语法正确
5. **测试**：添加后要测试生成的代码是否可用

这样就可以无限扩展你的可视化编辑器了！

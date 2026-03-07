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

各个文件功能及项目结构见“文件结构fileStructure”

## 使用方法

> - 开袋即食（bushi）
> - 在Android Studio打开，注意gradle和java版本

#### 目前功能

- 脚本编辑器（基于sora-editor，支持代码补全、语法高亮等等）
- 支持一件插入脚本模板
- 构建发行品（可以一键生成定制GG客户端，且可自选添加附加函数和内置脚本）（请强大的函数添加将支持，添加函数包具体格式见代码，目前支持script添加法，提供了测试函数包供参考）
- 两种lua字节码反编译：unluac和TD（unluac包遇到技术性问题正在解决中）（未来将加入更多工具）
- 支持编译脚本，添加加密
- 语法检查
- 可视化lua编辑
- 内置支持gg函数的lua虚拟机（可用于过防御、反检测的脚本函数调用自吐和动态调试，未来会加入插桩、变量追踪等）
- #### 注意：这些更新随时可能变动
  
#### 即将支持功能
  
  - cpp一键生成、编译（鉴于体量，可能会取消一件编译）
    
  - 一键构建直装（长期）

  - 更高等级的加密
  
  - 基于内置luaj的即时动态调试 

  - 内置框架（基于spacecore（安卓9到14支持）。由于blackbox没做好gg适配，会在后续版本加入。如果有更好的开源免费框架欢迎推荐）


### 如何给luaj虚拟机添加\修改函数
### 如何制作GG修改器函数包

函数包（.ggfunc）是`定制GG修改器`功能中用于给修改器的luaj虚拟机添加用户自定义函数的，一个正常的函数包（ZIP 结构）应该包含：  

- funcpack.json          —— 元数据（函数名、描述、适用版本等）
- inner_96.smali         —— Script$xxx.smali（96.0 版，可选）（xxx内容和名称一致）
- inner_101.smali        —— Script$xxx.smali（101.1 版，可选）
- registration_96.smali  —— sleep 下方插入的注册代码（96.0 版，可选）
- registration_101.smali —— sleep 下方插入的注册代码（101.1 版，可选）

funcpack.json 示例：  

```json
 {
   "name": "名称",
   "description": "描述",
   "version": "使用版本",
   "format_version": 1
 }
```

> version 字段含义：  
> "96"        — 仅有 96.0 版 smali  
> "101"       — 仅有 101.1 版 smali  
> "universal" — 同时包含两个版本的 smali  

我提供了测试函数包供参考。

运行时流程如下：  
将inner添加到class内  
在Script类中定位到sleep方法注册处，并在下方添加registration内的代码片段。会自动判断版本并注入对应版本代码
### 社区分页（communityFragment）
服务端接口文档（方便搭建后端）  

轮播图接口 GET /api/community/banners  
```JSON
{
"code": 0,
"data": [
{
"imageUrl": "http://你的服务器/images/banner1.jpg",
"linkUrl": "http://跳转链接",
"title": "轮播图标题"
},
{
"imageUrl": "http://你的服务器/images/banner2.jpg",
"linkUrl": "",
"title": "第二张轮播图"
}
]
}
```
脚本分享接口 GET /api/community/scripts
```JSON
{
"code": 0,
"data": [
{
"id": 1,
"title": "某游戏脚本",
"desc": "支持最新版本，一键修改金币数量",
"author": "开发者A",
"tag": "热门",
"date": "2024-01-15",
"url": "http://详情链接"
}
]
}
```
资源详情页接口 GET /api/community/resource/detail?id=1
```JSON

{
"code": 0,
"data": {
"id": 1,
"title": "某某修改器",
"desc": "功能强大的游戏修改工具",
"iconUrl": "http://服务器/icons/tool.png",
"size": "10.5MB",
"downloadUrl": "http://服务器/download/tool.apk",
"version": "v3.2.1",
"author": "开发者名称",
"updateDate": "2024-06-15",
"category": "工具",
"downloadCount": 15000,
"rating": 4.8,
"markdownContent": "# 工具详细介绍\n\n## 功能特点\n\n- 功能一\n- 功能二\n\n## 使用方法\n\n1. 步骤一\n2. 步骤二\n\n## ⚠️ 注意事项\n\n> 重要提示内容\n\n![截图](http://服务器/images/screenshot.png)"
}
}
```
资源接口 GET /api/community/resources
```JSON

{
"code": 0,
"data": [
{
"id": 1,
"title": "某某修改器",
"desc": "功能强大的游戏修改工具",
"iconUrl": "http://服务器/icons/tool.png",
"size": "10.5MB",
"downloadUrl": "http://服务器/download/tool.apk",
"version": "v3.2.1",
"author": "开发者名称",
"updateDate": "2024-06-15",
"category": "工具",
"detailUrl": "http://服务器/api/community/resource/detail?id=1",
"downloadCount": 15000,
"rating": 4.8
}
]
}
```
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


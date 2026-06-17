# GGLuaTool

！！！ *寻找合作者中* ！！！

# 欢迎联系：

# qq:2185125049

# email:mitchell0yr@gmail.com

## 简介

这是一个悠然制作的GGlua脚本工具。用于编写、编译与反编译、加密适用于GameGuardian的lua脚本的工具。   
此软件将加入更多功能，正在持续更新！

此软件使用了AndroidStudio由java制作。

## 注意事项

请遵守开源协议！此软件仅供学习交流使用，**禁止用于违法用途**(如制作游戏外挂等非法用途)，若造成任何后果与开发者无关  
此项目使用了deepseek、claude等辅助，请按照《人工智能生成合成内容标识办法》等您所在地当地法规标识生成式人工智能使用  

由于GG的luaj虚拟机经过了魔改（jse3.0.1但是支持lua5.3特性），导致一些技术问题还待解决。作为替代方案，我们使用了api调用GG修改器的原生功能，详情见下方。

## 项目结构

```
app/src/main/java/mituran/gglua/tool/    # 主包
├── apktools/          # APK修改（GG安装包编辑、smali注入、签名）
├── communityFragment/ # 社区分页（轮播图、脚本分享、教程、资源下载）
├── licenseModel/      # 开源许可证
├── luaTool/           # Lua工具（格式化、混淆、加密、语法检查）
├── model/             # 数据模型
├── plugin/            # 插件系统
├── template/          # 脚本模板系统
├── tutorial/          # 教程文档查看器
├── util/              # 工具类（网络、剪贴板、资源复制）
├── VisualLuaScriptEditor/ # 可视化积木式脚本编辑器
└── *.java             # 核心Activity/Fragment
```

> 完整文件结构详见 [文件结构fileStructure.md](../GGtool/fileStructure.md)

## 技术栈

| 类别    | 技术                              |
| ----- | ------------------------------- |
| 语言    | Java                            |
| 构建工具  | Gradle                          |
| 代码编辑器 | sora-editor                     |
| Lua引擎 | Luaj（魔改版，jse3.0.1） |
| 反编译   | unluac、TD                       |
| APK修改 | smali注入、ARSC编辑、APK签名            |
| 加密    | 自定义Lua加密（字符串加密、函数/变量混淆、垃圾代码注入）  |

## 构建说明

| 环境要求                        | 开发时所用版本        |
| --------------------------- |----------------|
| Gradle                      | 8.10.2         |
| AGP (Android Gradle Plugin) | 8.8.0          |
| JDK                         | 17             |
| compileSdk                  | 35             |
| minSdk                      | 28 (Android 9) |
| targetSdk                   | 35             |

## 模块说明

| 模块                    | 路径                       | 用途                            |
|-----------------------|--------------------------|-------------------------------|
| apktools              | `apktools/`              | 解析/修改GG安装包，注入smali代码及脚本，重签名   |
| communityFragment     | `communityFragment/`     | 社区分页：轮播图、脚本分享、教程、资源下载，支持本地/远程 |
| luaTool               | `luaTool/`               | Lua脚本加密、混淆、格式化、语法检查           |
| plugin                | `plugin/`                | 编辑器插件框架，支持加载和管理外部插件           |
| template              | `template/`              | Lua脚本模板，一键插入常用代码结构            |
| tutorial              | `tutorial/`              | 各类教程文档的Markdown查看器            |
| util                  | `util/`                  | 网络请求、剪贴板、资源复制等通用工具            |
| VisualLuaScriptEditor | `VisualLuaScriptEditor/` | 积木式可视化Lua脚本编辑器                |

## 功能

### 目前支持

| 功能     | 说明                                             |
|--------|------------------------------------------------|
| 脚本编辑器  | 基于sora-editor，支持代码补全、语法高亮                      |
| 脚本模板   | 一键插入常用脚本模板                                     |
| 构建发行品  | 一键生成定制GG客户端，可自选添加自定义函数和内置脚本                    |
| Lua反编译 | unluac和TD两种字节码反编译方式（unluac遇到技术问题解决中）           |
| 脚本编译   | 编译脚本并添加加密                                      |
| 语法检查   | Lua语法检查                                        |
| 可视化编辑  | 积木式可视化Lua脚本编辑                                  |
| Lua虚拟机 | 内置支持GG函数的lua虚拟机（用于过防御、反检测、函数调用自吐和动态调试），代码一键试运行 |
| 调用GG接口 | 对接带有api的GG实现运行                                 |

> 注意：上述功能随时可能变动
> 调用GG接口依赖于开源项目[GameGuardian-Api](https://github.com/mitchell-yr/GameGuardian-Api)，需要安装这个项目内指定版本GG
### 即将支持

| 功能    | 说明                                 |
| ----- | ---------------------------------- |
| C++编译 | 一键生成、编译（可能取消一键编译）                  |
| 直装构建  | 一键构建直装（长期目标）                       |
| 高级加密  | 更高等级的加密方案                          |
| 动态调试  | 基于内置luaj的即时动态调试（未来加入插桩、变量追踪）       |
| 内置框架  | 基于spacecore（安卓9-14），后续加入blackbox适配 |

### 功能测试
已通过小米8(安卓10)、红米k40(安卓11)、一加15(安卓16)、iqoo15ultra(安卓16)的真机测试，通过pixel9(安卓14)模拟机测试

## 使用方法

### 脚本执行
我们采用了内置lua虚拟机和对接GG接口(GameGuardian-Api)两套方案
#### 虚拟机函数添加
在`LuaEngine.java`中`createGgLibrary`方法中按照示例添加GG函数（即`gg.xxx()`）:
```lua
ggTable.set("方法名", new VarArgFunction() {
    @Override
    public Varargs invoke(Varargs args) {
        //此处写具体实现
        logToTextView("[gg.方法名] 日志信息\n");//向运行日志回调的信息
        return 返回值;
    }
    });
```

#### GameGuardian-Api相关api
| 端点                     | 方法   | 说明                                                        |
| ---------------------- | ---- | --------------------------------------------------------- |
| `/api/status`          | GET  | 服务状态                                                      |
| `/api/test`            | GET  | 连接测试                                                      |
| `/api/gg/info`         | GET  | 目标进程信息（PID、包名）                                            |
| `/api/gg/status`       | GET  | 搜索结果数量                                                    |
| `/api/gg/searchNumber` | POST | 搜索数值 `{"value":"100","type":1}`（实验api，不稳定，请正式使用runScript） |
| `/api/gg/getResults`   | POST | 获取搜索结果（最多100条）（实验api，不稳定，请正式使用runScript）                  |
| `/api/gg/editAll`      | POST | 批量修改 `{"value":"999"}`（实验api，不稳定，请正式使用runScript）          |
| `/api/gg/runScript`    | POST | **执行任意 GG Lua 脚本（请主要使用这个）**                               |
>注意，调用api要防止阻塞线程
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

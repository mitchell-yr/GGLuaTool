## 目录结构

```
app/src/main/
├── java/
│   ├── mituran/gglua/tool/              # 主包目录
│   │   ├── apktools/                    # APK工具相关，用于GameGuardian安装包编辑
│   │   │   ├── apksign/                 # apk签名相关
│   │   │   │   ├── ApkSignerWrapper.java          # 签名封装
│   │   │   │   ├── KeyStoreGenerator.java         # 密钥库生成
│   │   │   │   ├── SignatureConfig.java           # 签名配置
│   │   │   │   └── SignatureSelectionActivity.java # 签名选择界面
│   │   │   ├── ApkModifier.java          # apk修改器，负责具体的修改逻辑
│   │   │   ├── ModifierActivity.java     # apk修改器界面，负责界面控件管理及签名弹窗
│   │   │   ├── ArscEditor.java           # ARSC资源编辑器
│   │   │   ├── CustomFunctionPackage.java # 自定义函数包处理
│   │   │   ├── DexModifier.java          # DEX修改
│   │   │   ├── FunctionManagerActivity.java # 函数管理器界面
│   │   │   ├── GgFunctionAdder.java      # GG函数添加器
│   │   │   ├── ManifestModifier.java     # AndroidManifest修改
│   │   │   ├── ModifyPreset.java          # 修改预设配置
│   │   │   └── ScriptEmbedder.java       # 脚本嵌入
│   │   ├── communityFragment/           # community分页
│   │   │   ├── CommunityFragment.java      # 主Fragment
│   │   │   ├── ApiConfig.java              # 网络配置（改IP等配置改这里）
│   │   │   ├── BannerItem.java             # 轮播图数据模型
│   │   │   ├── ResourceDetailActivity.java # "资源"详情页
│   │   │   ├── TutorialItem.java           # 教程数据模型
│   │   │   ├── ScriptItem.java             # 脚本分享数据模型
│   │   │   ├── ResourceItem.java           # 资源数据模型
│   │   │   ├── BannerAdapter.java          # 轮播图适配器
│   │   │   ├── TutorialAdapter.java        # "教程"适配器
│   │   │   ├── ScriptAdapter.java          # "脚本分享"适配器
│   │   │   └── ResourceAdapter.java        # "资源"适配器。支持本地"资源"，注册本地"资源"在CommunityFragment添加ResourceItem.createBuiltIn(...)，并在assets/resources内添加相应的md
│   │   ├── licenseModel/                # 开源许可证
│   │   │   ├── LicenseAdapter.java
│   │   │   ├── LicenseInfo.java
│   │   │   └── OpenSourceActivity.java
│   │   ├── luaTool/                     # Lua工具类
│   │   │   ├── LuaFormatterWrapper.java           # Lua格式化
│   │   │   ├── LuaFunctionAndVariableObfuscate.java # 函数及变量混淆
│   │   │   ├── LuaJunkCodeObfuscator.java         # 垃圾代码混淆
│   │   │   ├── LuaStringEncryptor.java            # 字符串加密
│   │   │   └── LuaSyntaxChecker.java              # 语法检查
│   │   ├── model/                       # 数据模型
│   │   │   ├── LuaFunction.java
│   │   │   ├── Plugin.java
│   │   │   └── Template.java
│   │   ├── plugin/                      # 编辑器插件系统
│   │   │   ├── PluginAdapter.java
│   │   │   ├── PluginManager.java
│   │   │   └── PluginManagerActivity.java
│   │   ├── template/                    # lua脚本模板系统
│   │   │   ├── TemplateAdapter.java
│   │   │   ├── TemplateEditorActivity.java
│   │   │   └── TemplateManagerActivity.java
│   │   ├── tutorial/                    # 教程相关
│   │   │   ├── EncryptTutorialActivity.java          # lua脚本加密教程文档md查看器
│   │   │   ├── GGFunctionAddingTutorialActivity.java # GG函数添加教程
│   │   │   ├── GGFunctionDocumentViewActivity.java   # GG函数文档md查看器
│   │   │   ├── GGTutorialActivity.java               # GG修改器使用教程文档md查看器
│   │   │   ├── TutorialChapter.java                  # 教程章节数据模型
│   │   │   └── TutorialChapterAdapter.java           # 教程章节适配器
│   │   ├── util/                        # 工具类
│   │   │   ├── AssetsCopyUtil.java       # 资源复制工具
│   │   │   ├── ClipboardHelper.java      # 剪贴板工具
│   │   │   ├── HttpHelper.java           # 网络请求工具
│   │   │   └── UnluacWrapper.java        # Unluac反编译封装
│   │   ├── VisualLuaScriptEditor/       # Lua可视化脚本编辑器
│   │   │   └── luaksh/
│   │   │       ├── MainActivity.java               # 可视化编辑器主Activity
│   │   │       ├── CodeBlock.java                  # 代码块模型
│   │   │       ├── CodeBlockAdapter.java           # 代码块列表适配器
│   │   │       ├── CodeBlockStructure.java         # 代码块结构定义 # 添加新代码块类型需在此定义
│   │   │       ├── CodeBlockType.java              # 代码块类型枚举 # 添加新代码块类型需在此注册
│   │   │       ├── CodeBlockTypeItem.java          # 代码块分类 # 添加新代码块类型需在此归类
│   │   │       ├── CodeTab.java                    # 代码标签页
│   │   │       ├── CustomActionBar.java            # 自定义操作栏
│   │   │       ├── DynamicCodeBlockType.java       # 动态代码块类型
│   │   │       ├── ExpandableBlockTypeAdapter.java # 可展开代码块类型适配器
│   │   │       ├── FixedWidthRecyclerView.java     # 固定宽度RecyclerView
│   │   │       ├── GeneratedLuaCode.java           # 生成Lua代码管理（封装生成的Lua代码及其行号到代码块的映射关系）
│   │   │       ├── ProjectManager.java             # 项目管理
│   │   │       └── TabAdapter.java                 # 标签页适配器
│   │   ├── BuildOutputLogManager.java   # 构建输出日志管理器（用于lua脚本编译）
│   │   ├── CodeEditorLua.java           # Lua代码编辑器
│   │   ├── DraggableScrollBar.java      # 可拖动滚动条组件（用于tutorial内各个独立的md查看器）
│   │   ├── EncryptionOptionsDialog.java # lua脚本加密选项对话框（在代码编辑器内点击编译后用于选择启用哪些加密）
│   │   ├── FilePreviewActivity.java     # 文件预览Activity（用于GG自带log和反编译所产生文件）
│   │   ├── HomeFragment.java            # 主页页面
│   │   ├── LuaCheckResultDialog.java    # Lua语法检查结果对话框（脚本编辑器内）
│   │   ├── LuaCompiler.java             # Lua编译器（在脚本编辑器内点编译后）
│   │   ├── LogDecompileActivity.java    # 构建日志反编译Activity
│   │   ├── LuaEncryptionModule.java     # Lua脚本加密模块，内定义了部分加密的方法，其他加密方法在luaTool里调用
│   │   ├── LuaEngine.java               # Lua引擎，内部定义了添加的GG函数
│   │   ├── LuaExecutorActivity.java     # Lua执行器Activity（用于主页"脚本解密"card内的"Lua虚拟机"）
│   │   ├── LuaFunctionAdapter.java      # Lua函数搜索弹窗适配器
│   │   ├── LuaFunctionSearchPopup.java  # Lua函数搜索弹窗（用在代码编辑器内）
│   │   ├── MainActivity.java            # 主Activity，软件入口
│   │   ├── MarkdownViewerActivity.java  # Markdown查看器（用于插件文档显示）
│   │   ├── ProjectFragment.java         # 项目页面
│   │   ├── ProjectTypeDialog.java       # 选择项目类型对话框（在新建项目时弹出）
│   │   └── SettingsActivity.java        # 主页设置Activity
│   ├── unluac/                          # Unluac反编译工具
│   └── zhao/arsceditor/                 # resource.ARSC修改工具
├── res/                                 # Android资源目录
│   ├── layout/                          # 布局文件
│   ├── values/                          # 值资源（strings、colors、styles、themes）
│   ├── drawable/                        # 图片/图形资源
│   ├── menu/                            # 菜单资源
│   ├── xml/                             # XML配置（backup、file_paths等）
│   └── mipmap-*/                        # 应用图标（多分辨率）
└── assets/                              # 应用资产
    ├── resources/                       # 社区分页内置资源（.md文档）
    ├── smali/                           # smali注入代码（96/101版本）
    ├── templates/                       # lua脚本模板（JSON格式）
    ├── textmate/                        # 编辑器语法高亮主题
    ├── GGtutorial/                      # GG修改器教程文档
    ├── GameGuardianDocument.pdf         # GG官方文档
    ├── luaformatter.jar                 # Lua格式化jar包
    └── user_agreement.txt               # 用户协议
```

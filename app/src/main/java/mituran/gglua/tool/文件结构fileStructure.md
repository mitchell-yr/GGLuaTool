## 目录结构

```
java/
├── mituran.gglua.tool/          # 主包目录
│   ├── apktools/                # APK工具相关，用于gameguardian安装包编辑
│   │   ├── apksign/  # apk签名相关
│   │   ├── apkmodifier.java  # apk修改器，负责具体的修改逻辑
│   │   ├── modifieractivity.java  # apk修改器界面，负责界面控件管理，以及签名弹窗等等
│   │   └── 其他 #对应具体部分的修改
│   ├── licenseModel/            # 开源许可证
│   ├── luaTool/                 # Lua工具类
│   ├── model/                   # 数据模型
│   ├── plugin/                  # 编辑器插件系统
│   ├── template/                # lua脚本模板系统
│   ├── tutorial/                # 教程相关
│   │   ├── EncryptTutorialActivity.java  # lua脚本加密教程文档md查看器
│   │   ├── GGFunctionDocumentViewActivity.java  # GG函数文档md查看器
│   │   └── GGTutorialActivity.java  # GG修改器使用教程文档md查看器
│   ├── util/                    # 工具类
│   └── VisualLuaScriptEditor.luash  # Lua可使化脚本编辑器
│
├── BannerAdapter.java           # 首页轮播图横幅适配器
├── BuildOutputLogManager.java   # 构建输出日志管理器（用于lua脚本编译）
├── CodeEditorLua.java           # Lua代码编辑器
├── CommunityFragment.java       # 社区页面
├── DraggableScrollBar.java      # 可拖动滚动条组件（用于tutorial内各个独立的md查看器）
├── EncryptionOptionsDialog.java # lua脚本加密选项对话框（在代码编辑器内点击编译后用于选择启用哪些加密）
├── HomeFragment.java            # 主页页面
├── LuaCheckResultDialog.java    # Lua语法检查结果对话框（脚本编辑器内）
├── LuaCompiler.java             # Lua编译器（在脚本编辑器内点编译后）
├── LuaEncryptionModule.java     # Lua脚本加密模块，内定义了部分加密的方法，其他加密方法在luatool里调用
├── LuaEngine.java               # Lua引擎，内部定义了添加的GG函数
├── LuaExecutorActivity.java     # Lua执行器活动（只用于主页“脚本解密”card内的“Lua虚拟机”）
├── LuaFunctionAdapter.java      # Lua函数搜索弹窗适配器
├── LuaFunctionSearchPopup.java  # Lua函数搜索弹窗（用在代码编辑器内）
├── MainActivity.java            # 主活动，软件入口
├── MarkdownViewerActivity.java  # Markdown查看器（简单markdown查看，用于插件文档显示。其他的一些md没有用它显示）
├── ProjectFragment.java         # 项目页面
├── ProjectTypeDialog.java       # 选择项目类型对话框（在新建项目时弹出）
│
├── unluac/                          # Unluac反编译工具
└── zhao.arsceditor/                 # resource.ARSC修改工具
```
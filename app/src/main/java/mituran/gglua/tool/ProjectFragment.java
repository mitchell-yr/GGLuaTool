package mituran.gglua.tool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import mituran.gglua.tool.VisualLuaScriptEditor.luaksh.MainActivity;
import mituran.gglua.tool.plugin.PluginManagerActivity;
import mituran.gglua.tool.template.TemplateManagerActivity;


public class ProjectFragment extends Fragment {

    private EditText searchEditText;
    private Button btnManageTop;
    private LinearLayout cardsContainer;
    private List<ProjectItem> projectList;
    private List<ProjectItem> filteredList;

    //project文件夹
    private JSONArray projectsJsonArray;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project, container, false);

        initViews(view);
        initData();
        setupListeners();
        displayCards();

//        View topBar = view.findViewById(R.id.rootLinear);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            int statusBarHeight = getStatusBarHeight(getContext());
//            topBar.setPadding(0, statusBarHeight, 0, 0);
//        }

        return view;
    }

    private void initViews(View view) {
        searchEditText = view.findViewById(R.id.search_edit_text);
        btnManageTop = view.findViewById(R.id.btn_manage_top);
        cardsContainer = view.findViewById(R.id.cards_container);
    }

    private void initData() {

        if (! isDirExist()){
            Toast.makeText(getContext(),"必要目录缺失",Toast.LENGTH_SHORT).show();
            return;
        }
        // 初始化项目数据
        projectList = new ArrayList<>();
        filteredList = new ArrayList<>();

        //加载project目录
        try{
            projectsJsonArray = scanProjects();
            for (int i = 0; i < projectsJsonArray.length(); i++) {
                JSONObject project = projectsJsonArray.getJSONObject(i);

                String name = project.getString("name");
                String path = project.getString("path");
                String type = project.optJSONObject("information").getString("type");

                projectList.add(new ProjectItem(name, "项目类型：" +type,path,type));


            }
        } catch (JSONException e) {
            projectList.add(new ProjectItem("损坏的项目", "项目文件损坏无法读取","",""));
            Log.e("TAG", "解析项目数据失败: " + e.getMessage());
        }

        filteredList.addAll(projectList);
    }

    private void setupListeners() {
        // 顶部管理按钮点击事件
        btnManageTop.setOnClickListener(v -> {
            showTopManageDialog();
        });

        // 搜索框文本变化监听
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProjects(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    //顶部管理按钮弹窗
    private void showTopManageDialog() {
        String[] options = {"新建项目", "导入项目", "插件管理", "模板管理"};


        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("管理")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 新建项目
                            // 显示创建项目对话框

                            ProjectTypeDialog.showProjectNameDialog(getContext(), new ProjectTypeDialog.OnProjectCreatedListener() {
                                @Override
                                public void onProjectCreated(String projectName, ProjectTypeDialog.ProjectType type) {
//                                        Toast.makeText(getContext(),
//                                                "创建项目：" + projectName + "\n类型：" + type.getDisplayName(),
//                                                Toast.LENGTH_SHORT).show();
                                    // TODO:在这里处理项目创建逻辑
                                    File baseDir = new File(Environment.getExternalStorageDirectory(),"/GGtool/project/"+projectName);
                                    File targetDir1 = new File(Environment.getExternalStorageDirectory(), "/GGtool/project/"+projectName+"/project_info.json");
                                    File targetDir2 = new File(Environment.getExternalStorageDirectory(), "/GGtool/project/"+projectName+"/code.lua");
                                    try {
                                        //Toast.makeText(getContext(), targetDir1.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                        try {
                                            baseDir.mkdirs();
                                            targetDir1.createNewFile();
                                            targetDir2.createNewFile();
                                            FileOutputStream fos = new FileOutputStream(targetDir1);
                                            JSONObject tempJson = new JSONObject();
                                            tempJson.put("version","");
                                            tempJson.put("type",type.getDisplayName());
                                            fos.write(tempJson.toString().getBytes());
                                            fos.close();
                                            addProject(projectName,"项目类型：" + type.getDisplayName(),Environment.getExternalStorageDirectory().getAbsolutePath()+"/GGtool/project/"+projectName,type.getDisplayName());
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(getContext(), "创建异常: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }

                                    //Toast.makeText(getContext(),projectName+"项目类型：" + type.getDisplayName()+baseDir.getPath()+type.getDisplayName(),Toast.LENGTH_SHORT).show();
                                }
                                @Override
                                public boolean isProjectNameExists2(String projectName) {
                                    // 返回 true 表示名称已存在，false 表示名称可用
                                    return isProjectNameExists(projectName);
                                }
                            });
                            break;
                        case 1:
                            // 导入项目
                            importProject();
                            break;
                        case 2:
                            // 插件管理
                            managePlugins();
                            break;
                        case 3:
                            // 模板管理
                            manageTemplates();
                            break;
                    }
                })
                .show();
    }

    //导入项目
    private void importProject() {
        // 启动文件选择器选择zip文件
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "选择项目zip文件"), REQUEST_CODE_IMPORT_PROJECT);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "请安装文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    // 添加常量定义
    private static final int REQUEST_CODE_IMPORT_PROJECT = 1001;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMPORT_PROJECT && resultCode == android.app.Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                android.net.Uri uri = data.getData();
                importProjectFromZip(uri);
            }
        }
    }

    /**
     * 从zip文件导入项目
     * @param uri zip文件的URI
     */
    private void importProjectFromZip(android.net.Uri uri) {
        try {
            // 获取输入流
            java.io.InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(getContext(), "无法读取文件", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建临时文件
            File tempZipFile = new File(getContext().getCacheDir(), "temp_import.zip");
            FileOutputStream fos = new FileOutputStream(tempZipFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.close();
            inputStream.close();

            // 解压到临时目录
            File tempExtractDir = new File(getContext().getCacheDir(), "temp_extract");
            if (tempExtractDir.exists()) {
                deleteFolderRecursively(tempExtractDir);
            }
            tempExtractDir.mkdirs();

            // 解压zip文件
            unzipFile(tempZipFile, tempExtractDir);

            // 获取解压后的项目文件夹（应该只有一个根文件夹）
            File[] extractedFiles = tempExtractDir.listFiles();
            if (extractedFiles == null || extractedFiles.length == 0) {
                Toast.makeText(getContext(), "zip文件为空", Toast.LENGTH_SHORT).show();
                return;
            }

            File projectFolder = extractedFiles[0];
            if (!projectFolder.isDirectory()) {
                Toast.makeText(getContext(), "zip文件格式不正确", Toast.LENGTH_SHORT).show();
                return;
            }

            // 验证是否是有效的项目（检查project_info.json是否存在）
            File projectInfoFile = new File(projectFolder, "project_info.json");
            if (!projectInfoFile.exists()) {
                Toast.makeText(getContext(), "不是有效的项目文件", Toast.LENGTH_SHORT).show();
                return;
            }

            // 读取项目信息
            String projectName = projectFolder.getName();
            String jsonContent = readJsonFile(projectInfoFile);
            String projectType = "未知类型";

            if (jsonContent != null) {
                try {
                    JSONObject projectInfo = new JSONObject(jsonContent);
                    projectType = projectInfo.optString("type", "未知类型");
                } catch (JSONException e) {
                    Log.e("ProjectFragment", "解析项目信息失败", e);
                }
            }

            // 检查项目名是否已存在
            String finalProjectName = projectName;
            int suffix = 1;
            while (isProjectNameExists(finalProjectName)) {
                finalProjectName = projectName + "_" + suffix;
                suffix++;
            }

            // 复制项目到projects目录
            File targetDir = new File(Environment.getExternalStorageDirectory(), "GGtool/project/" + finalProjectName);
            copyDirectory(projectFolder, targetDir);

            // 添加到项目列表
            addProject(finalProjectName, "项目类型：" + projectType, targetDir.getAbsolutePath(), projectType);

            // 清理临时文件
            tempZipFile.delete();
            deleteFolderRecursively(tempExtractDir);

            Toast.makeText(getContext(), "导入成功: " + finalProjectName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("ProjectFragment", "导入项目失败", e);
            Toast.makeText(getContext(), "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 解压zip文件
     * @param zipFile zip文件
     * @param destDir 目标目录
     */
    private void unzipFile(File zipFile, File destDir) throws IOException {
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(zipFile));
        java.util.zip.ZipEntry zipEntry;

        while ((zipEntry = zis.getNextEntry()) != null) {
            File newFile = new File(destDir, zipEntry.getName());

            if (zipEntry.isDirectory()) {
                newFile.mkdirs();
            } else {
                // 确保父目录存在
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.close();
            }
            zis.closeEntry();
        }
        zis.close();
    }

    /**
     * 复制目录
     * @param sourceDir 源目录
     * @param destDir 目标目录
     */
    private void copyDirectory(File sourceDir, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, destFile);
                } else {
                    copyFile(file, destFile);
                }
            }
        }
    }

    /**
     * 复制文件
     * @param sourceFile 源文件
     * @param destFile 目标文件
     */
    private void copyFile(File sourceFile, File destFile) throws IOException {
        FileInputStream fis = new FileInputStream(sourceFile);
        FileOutputStream fos = new FileOutputStream(destFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }

        fis.close();
        fos.close();
    }

    //插件管理
    private void managePlugins() {
        Intent intent=new Intent();
        intent.setClass(getContext(), PluginManagerActivity.class);
        startActivity(intent);
    }

    //模板管理
    private void manageTemplates() {
        Intent intent = new Intent();
        intent.setClass(getContext(), TemplateManagerActivity.class);
        startActivity(intent);
    }

    //检查项目名称是否已存在
    private boolean isProjectNameExists(String projectName) {
        for (ProjectItem item : projectList) {
            if (item.getTitle().equals(projectName)) {
                return true;
            }
        }
        return false;
    }

    //检查项目名称是否已存在（排除当前项目）
    private boolean isProjectNameExists(String projectName, ProjectItem excludeItem) {
        for (ProjectItem item : projectList) {
            if (item != excludeItem && item.getTitle().equals(projectName)) {
                return true;
            }
        }
        return false;
    }

    //扫描项目目录
    private JSONArray scanProjects() {
        try {
            // 创建结果JSON数组
            JSONArray projectsArray = new JSONArray();

            // 获取目标目录路径
            File sdcard = Environment.getExternalStorageDirectory();
            File projectsDir = new File(sdcard, "GGtool/project");

//            // 检查目录是否存在
//            if (!projectsDir.exists() || !projectsDir.isDirectory()) {
//                Log.e("TAG", "目录不存在: " + projectsDir.getAbsolutePath());
//                return;
//            }

            // 获取所有子目录
            File[] subDirs = projectsDir.listFiles();
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    // 只处理目录
                    if (subDir.isDirectory()) {
                        // 检查是否存在 project_info.json 文件
                        File jsonFile = new File(subDir, "project_info.json");
                        if (jsonFile.exists() && jsonFile.isFile()) {
                            // 创建项目对象
                            JSONObject projectObj = new JSONObject();

                            // 设置name（文件夹名）
                            projectObj.put("name", subDir.getName());

                            // 设置path（子目录路径）
                            projectObj.put("path", subDir.getAbsolutePath());

                            // 读取并设置information（project_info.json内容）
                            String jsonContent = readJsonFile(jsonFile);
                            if (jsonContent != null) {
                                try {
                                    JSONObject infoJson = new JSONObject(jsonContent);
                                    projectObj.put("information", infoJson);
                                } catch (JSONException e) {
                                    // 如果JSON解析失败，直接存储字符串
                                    projectObj.put("information", jsonContent);
                                }
                            }

                            // 添加到数组
                            projectsArray.put(projectObj);
                        }
                    }
                }
            }

            // 输出结果
            Log.d("TAG", "扫描结果: " + projectsArray.toString(2));

            // 使用扫描结果
            return projectsArray;

        } catch (JSONException e) {
            Log.e("TAG", "JSON处理错误: " + e.getMessage());
            return new JSONArray();
        }
    }

    //读取JSON文件内容
    private String readJsonFile(File file) {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;

        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            reader = new BufferedReader(isr);

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
                content.append("\n");
            }

            return content.toString();

        } catch (IOException e) {
            Log.e("TAG", "读取文件失败: " + file.getAbsolutePath(), e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void filterProjects(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(projectList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ProjectItem item : projectList) {
                if (item.getTitle().toLowerCase().contains(lowerQuery) ||
                        item.getDescription().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(item);
                }
            }
        }

        displayCards();
    }

    private void displayCards() {
        cardsContainer.removeAllViews();

        for (ProjectItem item : filteredList) {
            View cardView = createCardView(item);
            cardsContainer.addView(cardView);
        }
    }

    private View createCardView(ProjectItem item) {
        View cardView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_project_card, cardsContainer, false);

        TextView tvTitle = cardView.findViewById(R.id.tv_title);
        TextView tvDescription = cardView.findViewById(R.id.tv_description);
        Button btnOperate = cardView.findViewById(R.id.btn_operate);
        Button btnOpen = cardView.findViewById(R.id.btn_open);

        tvTitle.setText(item.getTitle());
        tvDescription.setText(item.getDescription());

        btnOperate.setOnClickListener(v -> {
            showManageDialog(item);
        });

        btnOpen.setOnClickListener(v -> {
            if(Objects.equals(item.type, "可视化lua项目")){
                // 启动lua可视化脚本编辑器界面
                //Toast.makeText(getContext(), "1"+item.type, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getContext(), MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("path", item.path);
                intent.putExtras(bundle);
                startActivity(intent);
            }else {
                // 启动lua脚本编辑器界面
                //Toast.makeText(getContext(), "2"+item.type, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getContext(), CodeEditorLua.class);
                Bundle bundle = new Bundle();
                bundle.putString("path", item.path);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        return cardView;
    }

    private void showManageDialog(ProjectItem item) {
        // 使用 BottomSheetDialog 创建底部弹窗
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View bottomSheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_project_manage, null);

        // 获取弹窗中的各个选项视图
        LinearLayout renameOption = bottomSheetView.findViewById(R.id.option_rename);
        LinearLayout shareOption = bottomSheetView.findViewById(R.id.option_share);
        LinearLayout exportOption = bottomSheetView.findViewById(R.id.option_export);
        LinearLayout deleteOption = bottomSheetView.findViewById(R.id.option_delete);
        TextView projectTitle = bottomSheetView.findViewById(R.id.dialog_project_title);

        // 设置项目标题
        projectTitle.setText(item.getTitle());

        // 设置各选项的点击事件
        renameOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showRenameDialog(item);
        });

        shareOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            shareProject(item);
        });

        exportOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            exportProject(item);
        });

        deleteOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showDeleteConfirmDialog(item);
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    //添加重名检测
    private void showRenameDialog(ProjectItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_rename_project, null);

        EditText editTextName = dialogView.findViewById(R.id.edit_text_project_name);
        editTextName.setText(item.getTitle());
        editTextName.setSelection(item.getTitle().length());

        builder.setView(dialogView)
                .setTitle("重命名项目")
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = editTextName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        // 检查新名称是否与其他项目重复
                        if (isProjectNameExists(newName, item)) {
                            Toast.makeText(getContext(), "项目名称已存在，请使用其他名称", Toast.LENGTH_SHORT).show();
                        } else {
                            item.setTitle(newName);
                            displayCards();
                            Toast.makeText(getContext(), "项目已重命名", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "项目名称不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void shareProject(ProjectItem item) {
//        Toast.makeText(getContext(), "分享项目: " + item.getTitle(), Toast.LENGTH_SHORT).show();
//        Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        shareIntent.setType("text/plain");
//        shareIntent.putExtra(Intent.EXTRA_TEXT, "分享项目: " + item.getTitle());
//        startActivity(Intent.createChooser(shareIntent, "分享项目"));
    }

    private void exportProject(ProjectItem item) {
        try {
            // 创建导出目录
            File exportDir = new File(Environment.getExternalStorageDirectory(), "GGtool/export");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // 设置导出文件路径
            String timestamp = String.valueOf(System.currentTimeMillis());
            File zipFile = new File(exportDir, item.getTitle() + "_" + timestamp + ".zip");

            // 获取项目目录
            File projectDir = new File(item.getPath());
            if (!projectDir.exists() || !projectDir.isDirectory()) {
                Toast.makeText(getContext(), "项目目录不存在", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建zip文件
            boolean success = zipDirectory(projectDir, zipFile);

            if (success) {
                Toast.makeText(getContext(), "导出成功: " + zipFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

                // 可选：分享导出的文件
                shareExportedFile(zipFile);
            } else {
                Toast.makeText(getContext(), "导出失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("ProjectFragment", "导出项目失败", e);
            Toast.makeText(getContext(), "导出异常: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 将目录压缩成zip文件
     * @param sourceDir 源目录
     * @param zipFile 目标zip文件
     * @return 是否成功
     */
    private boolean zipDirectory(File sourceDir, File zipFile) {
        try {
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new FileOutputStream(zipFile));
            zipDirectoryHelper(sourceDir, sourceDir.getName(), zos);
            zos.close();
            return true;
        } catch (IOException e) {
            Log.e("ProjectFragment", "压缩文件失败", e);
            return false;
        }
    }

    /**
     * 递归压缩目录辅助方法
     * @param fileToZip 要压缩的文件或目录
     * @param fileName 文件名
     * @param zos zip输出流
     */
    private void zipDirectoryHelper(File fileToZip, String fileName, java.util.zip.ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }

        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new java.util.zip.ZipEntry(fileName));
            } else {
                zos.putNextEntry(new java.util.zip.ZipEntry(fileName + "/"));
            }
            zos.closeEntry();

            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipDirectoryHelper(childFile, fileName + "/" + childFile.getName(), zos);
                }
            }
        } else {
            FileInputStream fis = new FileInputStream(fileToZip);
            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            fis.close();
        }
    }

    /**
     * 分享导出的文件
     * @param file 要分享的文件
     */
    private void shareExportedFile(File file) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/zip");

            // 使用FileProvider处理文件URI（Android 7.0+需要）
            android.net.Uri fileUri;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                fileUri = androidx.core.content.FileProvider.getUriForFile(
                        getContext(),
                        getContext().getPackageName() + ".fileprovider",
                        file
                );
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                fileUri = android.net.Uri.fromFile(file);
            }

            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "项目导出: " + file.getName());
            startActivity(Intent.createChooser(shareIntent, "分享项目文件"));
        } catch (Exception e) {
            Log.e("ProjectFragment", "分享文件失败", e);
            Toast.makeText(getContext(), "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmDialog(ProjectItem item) {
        new AlertDialog.Builder(getContext())
                .setTitle("删除项目")
                .setMessage("确定要删除项目 \"" + item.getTitle() + "\" 吗？此操作不可恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    boolean result = deleteFolderRecursively(new File(item.getPath()));
                    if (result) {
                        Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                    }
                    //在项目列表中删除
                    deleteProject(item);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 递归删除文件夹及其内容
     * folder:要删除的文件夹
     * return删除是否成功
     */
    private boolean deleteFolderRecursively(File folder) {
        if (folder == null || !folder.exists()) {
            return false;
        }

        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 递归删除子文件夹
                        deleteFolderRecursively(file);
                    } else {
                        // 删除文件
                        file.delete();
                    }
                }
            }
        }
        // 删除空文件夹或文件
        return folder.delete();
    }

    private void deleteProject(ProjectItem item) {
        projectList.remove(item);
        filteredList.remove(item);
        displayCards();
        //Toast.makeText(getContext(), "项目已删除", Toast.LENGTH_SHORT).show();
    }

    // 添加新项目的方法
    public void addProject(String title, String description,String path,String type) {
        ProjectItem newItem = new ProjectItem(title, description,path,type);
        projectList.add(newItem);
        filteredList.add(newItem);
        displayCards();
    }

    // 移除项目的方法
    public void removeProject(int position) {
        if (position >= 0 && position < filteredList.size()) {
            ProjectItem item = filteredList.get(position);
            projectList.remove(item);
            filteredList.remove(position);
            displayCards();
        }
    }

    //检查必要目录是否存在
    private boolean isDirExist(){
        File sdCard = Environment.getExternalStorageDirectory();
        File baseDir = new File(sdCard, "GGtool");

        // 基础目录
        if (!baseDir.exists()) {
            return false;
        }else{
            // 子目录
            String[] subDirs = {"project", "templates", "plugins"};
            for (String dirName : subDirs) {
                File subDir = new File(baseDir, dirName);
                if (!subDir.exists()) {
                    return false;
                }
            }
        }
        return true;
    }

    //获取系统自带状态栏高度
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // 内部类：项目数据模型
    private static class ProjectItem {
        private String title;
        private String description;

        private String type;

        private String path;

        public ProjectItem(String title, String description,String path,String type) {
            this.title = title;
            this.description = description;
            this.path = path;
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getPath() {
            return path;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
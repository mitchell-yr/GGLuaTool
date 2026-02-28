package mituran.gglua.tool;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import mituran.gglua.tool.apktools.ModifierActivity;
import mituran.gglua.tool.licenseModel.OpenSourceActivity;
import mituran.gglua.tool.tutorial.EncryptTutorialActivity;
import mituran.gglua.tool.tutorial.GGFunctionAddingTutorialActivity;
import mituran.gglua.tool.tutorial.GGFunctionDocumentViewActivity;
import mituran.gglua.tool.tutorial.GGTutorialActivity;
import mituran.gglua.tool.util.ClipboardHelper;
import mituran.gglua.tool.util.UnluacWrapper;

public class HomeFragment extends Fragment implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    // 卡片视图
    private CardView card_downloadGG, card2, card3, card4;

    private Button btn_downloadGG,btn_gg_apk_generate;

    private LinearLayout  btn_virtualmachine_decrypt,btn_unluac,btn_tdecompile,btn_gg_decompile,btn_gg_addingfunction;

    //TDEcompile下载链接
    private String tdecompileDownloadUrl="https://mit0yr.lanzout.com/iDyVa37jjfji";

    // 反编译工具类型
    private enum DecompilerType {
        UNLUAC,
        TDECOMPILE,
        GG_MODIFIER
    }

    private ActivityResultLauncher<String> filePickerLauncher;
    //加解密card2用
    private Dialog currentDialog;
    private DecompilerType currentDecompilerType;

    // Unluac 工具
    private UnluacWrapper unluacWrapper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // 初始化 Unluac
        unluacWrapper = new UnluacWrapper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 初始化视图
        drawerLayout = view.findViewById(R.id.home_drawer_layout);
        navigationView = view.findViewById(R.id.home_nav_view);
        toolbar = view.findViewById(R.id.home_toolbar);

        // 初始化卡片
        card_downloadGG = view.findViewById(R.id.card1);
        card2 = view.findViewById(R.id.card2);
        card3 = view.findViewById(R.id.card3);
        card4 = view.findViewById(R.id.card4);

        //卡片1
        btn_downloadGG = view.findViewById(R.id.btn_downloadGG);//GG下载卡片的按钮

        //卡片2
        btn_gg_decompile=view.findViewById(R.id.btn_gg_decompile);
        btn_tdecompile=view.findViewById(R.id.btn_tdecompile);
        btn_unluac=view.findViewById(R.id.btn_unluac);
        btn_virtualmachine_decrypt=view.findViewById(R.id.btn_virtualmachine_decrypt);

        //卡片3
        btn_gg_apk_generate=view.findViewById(R.id.btn_auto_generate);

        // 设置工具栏
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        // 设置导航视图监听器
        navigationView.setNavigationItemSelectedListener(this);

        // 设置卡片点击事件
        setupCardClickListeners();

        // 初始化文件选择器
        initializeFilePicker();

        return view;
    }

    private void setupCardClickListeners() {
        card_downloadGG.setOnClickListener(v -> {
            //点击下载GG卡片
        });
        btn_downloadGG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://gameguardian.net/forum/files/file/2-gameguardian/"));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Unluac按钮
        if (btn_unluac != null) {
            btn_unluac.setOnClickListener(v -> {
                currentDecompilerType = DecompilerType.UNLUAC;
                showUnluacDialog();
            });
        }

        // TDECOMPILE按钮
        if (btn_tdecompile != null) {
            btn_tdecompile.setOnClickListener(v -> {
                currentDecompilerType = DecompilerType.TDECOMPILE;
                showTdecompileDialog();
            });
        }

        // GG修改器内置反编译按钮
        if (btn_gg_decompile != null) {
            btn_gg_decompile.setOnClickListener(v -> {
                currentDecompilerType = DecompilerType.GG_MODIFIER;
                Toast.makeText(getContext(), "GG修改器反编译功能开发中...", Toast.LENGTH_SHORT).show();
            });
        }
        //解密教程
        if (btn_virtualmachine_decrypt != null) {
            btn_virtualmachine_decrypt.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setClass(getContext(), LuaExecutorActivity.class);
                startActivity(intent);
            });
        }

        //gg修改器apk修改生成
        if (btn_gg_apk_generate != null) {
            btn_gg_apk_generate.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setClass(getContext(), ModifierActivity.class);
                startActivity(intent);
            });
        }
        //gg修改器函数添加教程
        if (btn_gg_addingfunction != null) {
            btn_gg_addingfunction.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setClass(getContext(), GGFunctionAddingTutorialActivity.class);
                startActivity(intent);
            });
        }


        card2.setOnClickListener(v -> {
            // 在这里处理卡片2的点击事件
        });

        card3.setOnClickListener(v -> {
            // 在这里处理卡片3的点击事件
        });

        card4.setOnClickListener(v -> {
            // 在这里处理卡片4的点击事件
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.home_toolbar_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_more) {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home_donate) {
            // 处理捐赠
        } else if (id == R.id.nav_home_settings) {
            // 处理设置
        } else if (id == R.id.nav_home_update) {
            // 处理更新
        } else if (id == R.id.nav_home_openAddress) {
            // 处理打开地址
        } else if (id == R.id.nav_home_openLicense) {
            // 启动许可证界面
            Intent intent = new Intent(getContext(), OpenSourceActivity.class);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.END);
        return true;
    }

    public boolean handleBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 初始化文件选择器
     */
    private void initializeFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleSelectedFile(uri);
                    }
                }
        );
    }

    /**
     * 显示Unluac弹窗
     */
    private void showUnluacDialog() {
        currentDialog = new Dialog(getContext());
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentDialog.setContentView(R.layout.dialog_unluac);

        setupDialogWindow(currentDialog);
        initializeUnluacDialogViews();

        currentDialog.show();
        addDialogAnimation(currentDialog);
    }

    /**
     * 显示TDECOMPILE弹窗
     */
    private void showTdecompileDialog() {
        currentDialog = new Dialog(getContext());
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentDialog.setContentView(R.layout.dialog_tdecompile);

        setupDialogWindow(currentDialog);
        initializeTdecompileDialogViews();

        currentDialog.show();
        addDialogAnimation(currentDialog);
    }

    /**
     * 设置弹窗窗口属性
     */
    private void setupDialogWindow(Dialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * 初始化Unluac弹窗视图
     */
    private void initializeUnluacDialogViews() {
        LinearLayout llIntroHeader = currentDialog.findViewById(R.id.ll_intro_header);
        CardView cvIntroContent = currentDialog.findViewById(R.id.cv_intro_content);
        ImageView ivExpandIcon = currentDialog.findViewById(R.id.iv_expand_icon);
        MaterialButton btnSelectFromProject = currentDialog.findViewById(R.id.btn_select_from_project);
        MaterialButton btnSelectCustomScript = currentDialog.findViewById(R.id.btn_select_custom_script);
        TextView tvCancel = currentDialog.findViewById(R.id.tv_cancel);

        // 折叠/展开功能
        llIntroHeader.setOnClickListener(v -> {
            toggleExpandableView(cvIntroContent, ivExpandIcon);
        });

        // 从项目列表选择
        btnSelectFromProject.setOnClickListener(v -> {
            currentDialog.dismiss();
        });

        // 选择自定义脚本
        btnSelectCustomScript.setOnClickListener(v -> {
            openFilePicker();
        });

        // 取消按钮
        tvCancel.setOnClickListener(v -> {
            currentDialog.dismiss();
        });
    }

    /**
     * 初始化TDECOMPILE弹窗视图
     */
    private void initializeTdecompileDialogViews() {
        LinearLayout llIntroHeader = currentDialog.findViewById(R.id.ll_tdecompile_intro_header);
        CardView cvIntroContent = currentDialog.findViewById(R.id.cv_tdecompile_intro_content);
        ImageView ivExpandIcon = currentDialog.findViewById(R.id.iv_tdecompile_expand_icon);
        MaterialButton btnSelectFromProject = currentDialog.findViewById(R.id.btn_tdecompile_select_from_project);
        MaterialButton btnSelectCustom = currentDialog.findViewById(R.id.btn_tdecompile_select_custom);
        MaterialButton btnBatch = currentDialog.findViewById(R.id.btn_tdecompile_auto);
        TextView tvCancel = currentDialog.findViewById(R.id.tv_tdecompile_cancel);

        // 折叠/展开功能
        llIntroHeader.setOnClickListener(v -> {
            toggleExpandableView(cvIntroContent, ivExpandIcon);
        });

        // 下载TD
        btnSelectFromProject.setOnClickListener(v -> {
            currentDialog.dismiss();
            redirectToDownload(tdecompileDownloadUrl);
        });

        // 选择打开
        btnSelectCustom.setOnClickListener(v -> {
            if (isAppInstalled("com.tc")) {
                launchApp("com.tc");
            } else {
                redirectToDownload(tdecompileDownloadUrl);
            }
        });

        // 自动处理
        btnBatch.setOnClickListener(v -> {
            currentDialog.dismiss();
            showAutoProcessDialog();
        });

        // 取消按钮
        tvCancel.setOnClickListener(v -> {
            currentDialog.dismiss();
        });
    }

    /**
     * 切换可展开视图
     */
    private void toggleExpandableView(View contentView, ImageView expandIcon) {
        if (contentView.getVisibility() == View.GONE) {
            expandView(contentView);
            rotateIcon(expandIcon, 0, 180);
        } else {
            collapseView(contentView);
            rotateIcon(expandIcon, 180, 0);
        }
    }

    /**
     * 展开视图动画
     */
    private void expandView(View view) {
        view.setVisibility(View.VISIBLE);
        Animation expandAnimation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
        expandAnimation.setDuration(300);
        view.startAnimation(expandAnimation);
    }

    /**
     * 折叠视图动画
     */
    private void collapseView(View view) {
        Animation collapseAnimation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
        collapseAnimation.setDuration(300);
        collapseAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(collapseAnimation);
    }

    /**
     * 旋转图标动画
     */
    private void rotateIcon(ImageView icon, float fromDegree, float toDegree) {
        RotateAnimation rotate = new RotateAnimation(
                fromDegree, toDegree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(300);
        rotate.setFillAfter(true);
        icon.startAnimation(rotate);
    }

    /**
     * 添加弹窗显示动画
     */
    private void addDialogAnimation(Dialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }
    }

    /**
     * 打开文件选择器
     */
    private void openFilePicker() {
        Log.d("UnluacDecompile", "Opening file picker for: " + currentDecompilerType);
        filePickerLauncher.launch("*/*");
    }

    /**
     * 处理选中的文件
     */
    private void handleSelectedFile(Uri fileUri) {
        String fileName = getFileName(fileUri);

        // 检查文件扩展名
        if (fileName != null && (fileName.toLowerCase().endsWith(".lua") ||
                fileName.toLowerCase().endsWith(".luac"))) {

            if (currentDialog != null) {
                currentDialog.dismiss();
            }

            // 根据不同的反编译工具处理
            switch (currentDecompilerType) {
                case UNLUAC:
                    processUnluacDecompile(fileUri);
                    break;
                case TDECOMPILE:
                    processTdecompileDecompile(fileUri);
                    break;
                case GG_MODIFIER:
                    processGGModifierDecompile(fileUri);
                    break;
            }
        } else {
            Toast.makeText(getContext(), "请选择.lua或.luac文件", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取反编译工具名称
     */
    private String getDecompilerName(DecompilerType type) {
        switch (type) {
            case UNLUAC:
                return "Unluac";
            case TDECOMPILE:
                return "TDECOMPILE";
            case GG_MODIFIER:
                return "GG修改器";
            default:
                return "未知工具";
        }
    }

    /**
     * 获取文件名
     */
    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                return path.substring(cut + 1);
            }
        }
        return uri.getLastPathSegment();
    }

    /**
     * 显示批量处理弹窗
     */
    private void showAutoProcessDialog() {
        Toast.makeText(getContext(), "TDECOMPILE批量处理功能开发中...", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理Unluac反编译
     */
    private void processUnluacDecompile(Uri fileUri) {
        Log.d("UnluacDecompile", "开始反编译: " + fileUri.toString());

        Dialog loadingDialog = showLoadingDialog();
        long startTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    throw new Exception("无法打开文件");
                }

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[8192];
                int bytesRead;
                long fileSize = 0;

                while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, bytesRead);
                    fileSize += bytesRead;
                }
                inputStream.close();

                byte[] bytes = buffer.toByteArray();
                final long finalFileSize = fileSize;
                final String fileName = getFileName(fileUri);

                // 获取原文件目录路径
                final String originalDir = getFileDirectory(fileUri);

                String decompiled = unluacWrapper.decompileBytesSync(bytes);

                long endTime = System.currentTimeMillis();
                final long elapsedTime = endTime - startTime;

                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    // 传入原目录路径
                    showDecompileResultDialog(decompiled, fileName, finalFileSize, elapsedTime, originalDir);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    showErrorDialog("反编译失败", e.getMessage());
                });
            }
        }).start();
    }
    /**
     * 获取文件所在目录路径
     */
    private String getFileDirectory(Uri uri) {
        String path = null;

        // 尝试从 URI 获取真实路径
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 尝试从 content URI 获取路径
            try {
                String[] projection = {MediaStore.MediaColumns.DATA};
                Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        path = cursor.getString(columnIndex);
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 备用：尝试从 DocumentFile 获取
            if (path == null) {
                try {
                    DocumentFile docFile = DocumentFile.fromSingleUri(requireContext(), uri);
                    if (docFile != null) {
                        // 这种方式可能无法获取真实路径，返回 null
                        path = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 返回目录路径
        if (path != null) {
            File file = new File(path);
            if (file.getParentFile() != null) {
                return file.getParentFile().getAbsolutePath();
            }
        }

        // 无法获取时返回默认下载目录
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }
    // 保存当前结果对话框的引用，便于在保存后关闭
    private Dialog currentUnluacResultDialog;

    /**
     * 显示unluac加载对话框
     */
    private Dialog showLoadingDialog() {
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        View view = LayoutInflater.from(getContext()).inflate(
                android.R.layout.simple_list_item_1, null
        );

        // 创建简单的加载视图
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);
        layout.setGravity(android.view.Gravity.CENTER);

        ProgressBar progressBar = new ProgressBar(getContext());
        layout.addView(progressBar);

        TextView textView = new TextView(getContext());
        textView.setText("正在反编译,请稍候...");
        textView.setTextSize(16);
        textView.setPadding(0, 30, 0, 0);
        layout.addView(textView);

        dialog.setContentView(layout);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();
        return dialog;
    }

    /**
     * 显示反编译结果对话框
     */
    private void showDecompileResultDialog(String decompiled, String fileName,
                                           long fileSize, long elapsedTime, String originalDir) {
        currentUnluacResultDialog = new Dialog(getContext());
        currentUnluacResultDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentUnluacResultDialog.setContentView(R.layout.dialog_unluac_decompile_result);

        if (currentUnluacResultDialog.getWindow() != null) {
            currentUnluacResultDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90);
            currentUnluacResultDialog.getWindow().setLayout(width, height);
        }

        ImageView ivClose = currentUnluacResultDialog.findViewById(R.id.iv_close_dialog);
        TextView tvFileName = currentUnluacResultDialog.findViewById(R.id.tv_file_name);
        TextView tvFileSize = currentUnluacResultDialog.findViewById(R.id.tv_file_size);
        TextView tvDecompileTime = currentUnluacResultDialog.findViewById(R.id.tv_decompile_time);
        TextView tvResult = currentUnluacResultDialog.findViewById(R.id.tv_decompile_result);
        MaterialButton btnCopy = currentUnluacResultDialog.findViewById(R.id.btn_copy_result);
        MaterialButton btnSave = currentUnluacResultDialog.findViewById(R.id.btn_save_result);

        tvFileName.setText("文件名: " + fileName);
        tvFileSize.setText("大小: " + formatFileSize(fileSize));
        tvDecompileTime.setText("耗时: " + (elapsedTime / 1000.0) + "s");
        tvResult.setText(decompiled);

        ivClose.setOnClickListener(v -> currentUnluacResultDialog.dismiss());

        btnCopy.setOnClickListener(v -> {
            ClipboardHelper helper = new ClipboardHelper(requireContext());
            helper.copyText(decompiled);
            Toast.makeText(getContext(), "代码已复制", Toast.LENGTH_SHORT).show();
        });

        // 点击保存，弹出选项对话框，传入原目录
        btnSave.setOnClickListener(v -> {
            showSaveOptionsDialog(decompiled, fileName, originalDir);
        });

        currentUnluacResultDialog.show();
    }

    /**
     * 生成unluac输出文件名
     */
    private String generateOutputFileName(String originalFileName) {
        if (originalFileName.toLowerCase().endsWith(".luac")) {
            return originalFileName.substring(0, originalFileName.length() - 1);
        } else if (!originalFileName.toLowerCase().endsWith(".lua")) {
            return originalFileName + "_decompiled.lua";
        }
        return originalFileName;
    }

    /**
     * 显示unluac结果保存选项对话框
     */
    private void showSaveOptionsDialog(String code, String originalFileName, String originalDir) {
        Dialog saveDialog = new Dialog(getContext());
        saveDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        saveDialog.setContentView(R.layout.dialog_unluac_decompile_save_options);

        if (saveDialog.getWindow() != null) {
            saveDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            saveDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // 获取视图
        ImageView ivClose = saveDialog.findViewById(R.id.iv_close_save);
        TextView tvOutputFilename = saveDialog.findViewById(R.id.tv_output_filename);
        RadioGroup rgLocation = saveDialog.findViewById(R.id.rg_save_location);
        RadioButton rbOriginalDir = saveDialog.findViewById(R.id.rb_original_dir);
        RadioButton rbCustomDir = saveDialog.findViewById(R.id.rb_custom_dir);
        TextInputLayout tilCustomPath = saveDialog.findViewById(R.id.til_custom_path);
        TextInputEditText etCustomPath = saveDialog.findViewById(R.id.et_custom_path);
        LinearLayout llOriginalPathInfo = saveDialog.findViewById(R.id.ll_original_path_info);
        TextView tvOriginalPath = saveDialog.findViewById(R.id.tv_original_path);
        MaterialButton btnCancel = saveDialog.findViewById(R.id.btn_cancel_save);
        MaterialButton btnConfirm = saveDialog.findViewById(R.id.btn_confirm_save);

        // 设置输出文件名预览
        String outputFileName = generateOutputFileName(originalFileName);
        tvOutputFilename.setText(outputFileName);

        // 设置原目录路径显示
        tvOriginalPath.setText(originalDir);

        // 预填自定义路径
        etCustomPath.setText(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

        // 关闭按钮
        ivClose.setOnClickListener(v -> saveDialog.dismiss());

        //正确的切换显示逻辑
        rgLocation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_custom_dir) {
                tilCustomPath.setVisibility(View.VISIBLE);
                llOriginalPathInfo.setVisibility(View.GONE);
            } else if (checkedId == R.id.rb_original_dir) {
                tilCustomPath.setVisibility(View.GONE);
                llOriginalPathInfo.setVisibility(View.VISIBLE);
            }
        });

        // 初始状态确保正确
        tilCustomPath.setVisibility(View.GONE);
        llOriginalPathInfo.setVisibility(View.VISIBLE);

        btnCancel.setOnClickListener(v -> saveDialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String targetDir;

            if (rgLocation.getCheckedRadioButtonId() == R.id.rb_custom_dir) {
                targetDir = etCustomPath.getText().toString().trim();
                if (targetDir.isEmpty()) {
                    tilCustomPath.setError("请输入保存路径");
                    return;
                }
                tilCustomPath.setError(null);
            } else {
                targetDir = originalDir;
            }

            saveDialog.dismiss();
            saveDecompiledCode(code, outputFileName, targetDir, true);
        });

        saveDialog.show();
    }

    /**
     * 保存unluac反编译代码
     * @param closeResultDialog 保存成功后是否关闭结果弹窗
     */
    private void saveDecompiledCode(String code, String outputFileName, String targetDir, boolean closeResultDialog) {
        new Thread(() -> {
            try {
                File outputDir = new File(targetDir);

                if (!outputDir.exists()) {
                    if (!outputDir.mkdirs()) {
                        throw new IOException("无法创建目录: " + outputDir.getAbsolutePath());
                    }
                }

                File outputFile = new File(outputDir, outputFileName);

                // 如果文件已存在，添加序号
                int count = 1;
                String baseName = outputFileName;
                String extension = "";
                int dotIndex = outputFileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseName = outputFileName.substring(0, dotIndex);
                    extension = outputFileName.substring(dotIndex);
                }

                while (outputFile.exists()) {
                    outputFile = new File(outputDir, baseName + "_" + count + extension);
                    count++;
                }

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(code.getBytes("UTF-8"));
                }

                final String savedPath = outputFile.getAbsolutePath();

                requireActivity().runOnUiThread(() -> {
                    // 显示保存成功提示
                    showSaveSuccessToast(savedPath);

                    // 关闭结果弹窗
                    if (closeResultDialog && currentUnluacResultDialog != null && currentUnluacResultDialog.isShowing()) {
                        currentUnluacResultDialog.dismiss();
                        currentUnluacResultDialog = null;
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    showErrorDialog("保存失败", "请检查存储权限或路径是否正确。\n\n错误信息: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示unluac保存成功提示
     */
    private void showSaveSuccessToast(String filePath) {
        // 使用 Snackbar 或自定义 Toast 显示更友好的提示
        View rootView = getView();
        if (rootView != null) {
            Snackbar.make(rootView, "文件保存成功", Snackbar.LENGTH_LONG)
                    .setAction("查看路径", v -> {
                        // 复制路径到剪贴板
                        ClipboardHelper helper = new ClipboardHelper(requireContext());
                        helper.copyText(filePath);
                        Toast.makeText(getContext(), "路径已复制:\n" + filePath, Toast.LENGTH_LONG).show();
                    })
                    .setActionTextColor(Color.parseColor("#BB86FC"))
                    .show();
        } else {
            Toast.makeText(getContext(), "已保存: " + filePath, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * 显示unluac错误对话框 (已美化)
     */
    private void showErrorDialog(String title, String message) {
        Dialog errorDialog = new Dialog(getContext());
        errorDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 使用新的错误布局 XML
        errorDialog.setContentView(R.layout.dialog_unluac_decompile_error);

        if (errorDialog.getWindow() != null) {
            errorDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            errorDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = errorDialog.findViewById(R.id.tv_error_title);
        TextView tvMsg = errorDialog.findViewById(R.id.tv_error_message);
        ImageView ivClose = errorDialog.findViewById(R.id.iv_close_dialog);
        Button btnClose = errorDialog.findViewById(R.id.btn_close_error);

        tvTitle.setText(title);
        tvMsg.setText(message);

        View.OnClickListener closeListener = v -> errorDialog.dismiss();
        ivClose.setOnClickListener(closeListener);
        btnClose.setOnClickListener(closeListener);

        errorDialog.show();
    }

    /**
     * 保存unluac反编译代码到文件
     */
    private void saveDecompiledCode(String code, String originalFileName) {
        new Thread(() -> {
            try {
                // 生成输出文件名
                String outputFileName = originalFileName;
                if (outputFileName.toLowerCase().endsWith(".luac")) {
                    outputFileName = outputFileName.substring(0, outputFileName.length() - 1); // 移除 c
                } else if (!outputFileName.toLowerCase().endsWith(".lua")) {
                    outputFileName = outputFileName + "_decompiled.lua";
                }

                // 保存到应用私有目录
                File outputDir = new File(requireContext().getExternalFilesDir(null), "decompiled");
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                File outputFile = new File(outputDir, outputFileName);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(code.getBytes("UTF-8"));
                }

                requireActivity().runOnUiThread(() -> {
                    String message = "文件已保存到:\n" + outputFile.getAbsolutePath();
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "保存失败: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }


    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    /**
     * 处理TDECOMPILE反编译
     */
    private void processTdecompileDecompile(Uri fileUri) {
        Log.d("TDECompile", "Processing TDECOMPILE decompile for: " + fileUri.toString());
        Toast.makeText(getContext(), "正在使用TDECOMPILE进行高级反编译...", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理GG修改器反编译
     */
    private void processGGModifierDecompile(Uri fileUri) {
        Log.d("GGModifier", "Processing GG Modifier decompile for: " + fileUri.toString());
        Toast.makeText(getContext(), "正在使用GG修改器进行反编译...", Toast.LENGTH_SHORT).show();
    }

    /**
     * 检测应用是否安装
     */
    private boolean isAppInstalled(String packageName) {
        PackageManager pm = requireContext().getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 启动目标应用
     */
    private void launchApp(String packageName) {
        try {
            Intent intent = requireContext().getPackageManager()
                    .getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "无法启动应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "启动失败：" + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 跳转到下载页面
     */
    private void redirectToDownload(String DOWNLOAD_URL) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(DOWNLOAD_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法打开下载链接,已复制到剪贴板", Toast.LENGTH_SHORT).show();
            ClipboardHelper helper = new ClipboardHelper(requireActivity().getApplicationContext());
            helper.copyText(DOWNLOAD_URL);
        }
    }
}
package mituran.gglua.tool;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import mituran.gglua.tool.communityFragment.CommunityFragment;
import mituran.gglua.tool.util.AssetsCopyUtil;


public class MainActivity extends AppCompatActivity {


    private BottomNavigationView bottomNavigationView;//底部导航栏
    private Fragment currentFragment = null;

    private static final String AGREEMENT_FILE = "user_agreement.txt"; // assets文件夹中用户协议
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_FIRST_TIME = "isFirstTime";
    private static final String KEY_AGREEMENT_ACCEPTED = "agreementAccepted";

    // 权限相关常量
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO} :
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE} :
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if (getSupportActionBar() != null) {//隐藏appbar
        //   getSupportActionBar().hide();
        //}

        setContentView(R.layout.activity_main);


        //底部导航栏..............................................................................................
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 设置默认Fragment
        if (savedInstanceState == null) {
            currentFragment = new HomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, currentFragment)
                    .commit();
        }

        // 设置导航监听器
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.navigation_project) {
                    selectedFragment = new ProjectFragment();
                } else if (itemId == R.id.navigation_community) {
                    selectedFragment = new CommunityFragment();
                }

                if (selectedFragment != null && !selectedFragment.equals(currentFragment)) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    currentFragment = selectedFragment;
                    return true;
                }

                return false;
            }
        });
        //.................................................................................................


        checkFirstTimeUser();//检查是否是首次进入软件
    }

    //检查是否是首次进入软件
    private void checkFirstTimeUser() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstTime = preferences.getBoolean(KEY_FIRST_TIME, true);
        boolean agreementAccepted = preferences.getBoolean(KEY_AGREEMENT_ACCEPTED, false);

        if (isFirstTime || !agreementAccepted) {
            showUserAgreementDialog();
        } else {
            // 用户已同意协议，检查权限
            checkAndRequestPermissions();
        }
    }

    private void showUserAgreementDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_user_agreement, null);

        TextView tvAgreement = dialogView.findViewById(R.id.tv_agreement_content);

        // 从assets文件夹读取协议内容
        String agreementText = loadAgreementFromAssets();
        tvAgreement.setText(agreementText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("用户协议与隐私政策")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveAgreementStatus(true);
                        dialog.dismiss();
                        // 同意协议后检查权限
                        checkAndRequestPermissions();
                    }
                })
                .setNegativeButton("不同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //从assets文件夹读取用户协议
    private String loadAgreementFromAssets() {
        StringBuilder text = new StringBuilder();
        try {
            InputStream inputStream = getAssets().open(AGREEMENT_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            //Log.e(TAG, "Error reading agreement file", e);
            return "无法加载用户协议内容";
        }
        return text.toString();
    }

    //保存是否同意用户协议
    private void saveAgreementStatus(boolean accepted) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_FIRST_TIME, false);
        editor.putBoolean(KEY_AGREEMENT_ACCEPTED, accepted);
        editor.apply();
    }

    // 获取需要请求的权限
    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12，使用MANAGE_EXTERNAL_STORAGE
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        } else {
            // Android 10及以下
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    // 检查并请求权限
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11及以上版本使用MANAGE_EXTERNAL_STORAGE权限
            if (!Environment.isExternalStorageManager()) {
                showManageStoragePermissionDialog();
            } else {
                onPermissionsGranted();
            }
        } else {
            // Android 10及以下版本使用传统权限
            if (!hasStoragePermissions()) {
                requestStoragePermissions();
            } else {
                onPermissionsGranted();
            }
        }
    }

    // 检查是否有存储权限
    private boolean hasStoragePermissions() {
        String[] permissions = getRequiredPermissions();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        copyTemplates();
        return true;
    }

    // 请求存储权限
    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQUEST_CODE);
    }

    // 权限获取成功后的操作
    private void onPermissionsGranted() {
        createRequiredDirectories();
        copyTemplates();
    }

    // 处理权限请求结果 - 修正版
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                onPermissionsGranted();
            } else {
                // 检查是否需要显示权限说明
                boolean shouldShowRationale = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }

                if (shouldShowRationale) {
                    showPermissionExplanationDialog();
                } else {
                    // 用户选择了"不再询问"，引导去设置页面
                    showGoToSettingsDialog();
                }
            }
        }
    }

    // 新增：引导用户去设置页面
    private void showGoToSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("需要存储权限");
        builder.setMessage("您已拒绝存储权限，请在设置中手动开启权限以正常使用应用。");
        builder.setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                android.content.Intent intent = new android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "缺少存储权限，部分功能可能无法使用", Toast.LENGTH_LONG).show();
            }
        });
        builder.show();
    }




    // 显示管理存储权限对话框（Android 11及以上）
    private void showManageStoragePermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("存储权限申请");
        builder.setMessage("应用需要存储权限来保存和读取文件，请在设置中允许\"所有文件访问权限\"");
        builder.setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                    } catch (Exception e) {
                        android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                    }
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "缺少存储权限，部分功能可能无法使用", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }



    // 显示权限说明对话框
    private void showPermissionExplanationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("需要存储权限");
        builder.setMessage("应用需要存储权限来保存项目文件、模板和插件。请授予权限以正常使用应用。");
        builder.setPositiveButton("重新申请", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestStoragePermissions();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "缺少存储权限，部分功能可能无法使用", Toast.LENGTH_LONG).show();
            }
        });
        builder.show();
    }

    // Activity结果回调（Android 11及以上）
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    createRequiredDirectories();
                    copyTemplates();
                } else {
                    Toast.makeText(this, "缺少存储权限，部分功能可能无法使用", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // 创建必需的目录
    private void createRequiredDirectories() {
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File baseDir = new File(sdCard, "GGtool");

            // 创建基础目录
            if (!baseDir.exists()) {
                if (!baseDir.mkdirs()) {
                    Toast.makeText(this, "创建工作目录失败", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 创建子目录
            String[] subDirs = {"project", "templates", "plugins","apk"};
            for (String dirName : subDirs) {
                File subDir = new File(baseDir, dirName);
                if (!subDir.exists()) {
                    if (subDir.mkdirs()) {
                        // 可选：创建成功日志
                        // Log.d("MainActivity", "Created directory: " + subDir.getAbsolutePath());
                    } else {
                        Toast.makeText(this, "创建" + dirName + "目录失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }


            //Toast.makeText(this, "目录初始化完成", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "创建目录时出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void copyTemplates() {
        // 后台线程执行，避免阻塞UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 检查是否已经复制过
                if (!AssetsCopyUtil.isTemplatesExist()) {
                    boolean success = AssetsCopyUtil.copyTemplatesFromAssets(MainActivity.this);

                    // 在UI线程显示结果
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                //Toast.makeText(MainActivity.this,"模板文件复制成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "模板文件复制失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(MainActivity.this,"模板文件已存在", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
    @Override
    public void onBackPressed() {
        // 先检查当前Fragment是否处理了返回键
        if (currentFragment instanceof HomeFragment) {
            HomeFragment homeFragment = (HomeFragment) currentFragment;
            if (homeFragment.handleBackPressed()) {
                return;
            }
        }

        if (bottomNavigationView.getSelectedItemId() != R.id.navigation_home) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        } else {
            // 显示退出确认弹窗
            showExitDialog();
            //super.onBackPressed();
        }
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("退出确认");
        builder.setMessage("确定要退出应用吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置圆角背景
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
    }

}
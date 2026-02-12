package mituran.gglua.tool.apktools.apksign;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/**
 * 签名选择界面
 */
public class SignatureSelectionActivity extends Activity {
    private SignatureConfig mSignatureConfig;
    private List<SignatureConfig.Config> mCustomConfigs;
    private ConfigAdapter mAdapter;

    // 回调接口
    public interface SignatureCallback {
        void onSignatureSelected(SignatureConfig.Config config);

        void onCancelled();
    }

    private SignatureCallback mCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSignatureConfig = new SignatureConfig(this);

        // 初始化默认测试签名（如果不存在）
        SignatureConfig.initDefaultKeystore(this);

        showSignatureDialog();
    }

    /**
     * 显示签名选择对话框
     */
    private void showSignatureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择签名方式");

        String[] options = {"使用默认测试签名", "使用自定义签名", "创建新签名"};

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // 使用默认测试签名
                        useDefaultSignature();
                        break;
                    case 1:
                        // 使用自定义签名
                        selectCustomSignature();
                        break;
                    case 2:
                        // 创建新签名
                        createNewSignature();
                        break;
                }
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mCallback != null) {
                    mCallback.onCancelled();
                }
                finish();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mCallback != null) {
                    mCallback.onCancelled();
                }
                finish();
            }
        });

        builder.show();
    }

    /**
     * 使用默认测试签名
     */
    private void useDefaultSignature() {
        SignatureConfig.Config config = mSignatureConfig.getDefaultTestConfig();

        // 验证默认签名文件
        if (!SignatureConfig.validateConfig(config)) {
            Toast.makeText(this, "默认签名文件不存在或无效，请重新初始化", Toast.LENGTH_SHORT).show();

            // 尝试重新生成
            if (SignatureConfig.initDefaultKeystore(this)) {
                if (mCallback != null) {
                    mCallback.onSignatureSelected(config);
                }
                finish();
            } else {
                Toast.makeText(this, "初始化默认签名失败", Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }

        if (mCallback != null) {
            mCallback.onSignatureSelected(config);
        }
        finish();
    }

    /**
     * 选择自定义签名
     */
    private void selectCustomSignature() {
        mCustomConfigs = mSignatureConfig.getAllConfigs();

        if (mCustomConfigs.isEmpty()) {
            Toast.makeText(this, "暂无自定义签名，请先创建", Toast.LENGTH_SHORT).show();
            createNewSignature();
            return;
        }

        // 显示自定义签名列表
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择自定义签名");

        ListView listView = new ListView(this);
        mAdapter = new ConfigAdapter(this, mCustomConfigs);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SignatureConfig.Config config = mCustomConfigs.get(position);

                // 验证签名配置
                if (!SignatureConfig.validateConfig(config)) {
                    Toast.makeText(SignatureSelectionActivity.this,
                            "签名文件无效: " + config.name, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mCallback != null) {
                    mCallback.onSignatureSelected(config);
                }
                finish();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final SignatureConfig.Config config = mCustomConfigs.get(position);
                showConfigOptions(config, position);
                return true;
            }
        });

        builder.setView(listView);
        builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showSignatureDialog();
            }
        });

        builder.show();
    }

    /**
     * 显示配置选项菜单
     */
    private void showConfigOptions(final SignatureConfig.Config config, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(config.name);

        String[] options = {"查看详情", "删除"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        showConfigDetails(config);
                        break;
                    case 1:
                        deleteConfig(config, position);
                        break;
                }
            }
        });

        builder.show();
    }

    /**
     * 显示配置详情
     */
    private void showConfigDetails(SignatureConfig.Config config) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("签名详情");

        String details = "名称: " + config.name + "\n" +
                "密钥库路径: " + config.keystorePath + "\n" +
                "密钥别名: " + config.keyAlias + "\n" +
                "是否默认: " + (config.isDefault ? "是" : "否");

        builder.setMessage(details);
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    /**
     * 删除配置
     */
    private void deleteConfig(final SignatureConfig.Config config, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认删除");
        builder.setMessage("确定要删除签名配置 \"" + config.name + "\" 吗？");

        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mSignatureConfig.deleteConfig(config.name)) {
                    mCustomConfigs.remove(position);
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(SignatureSelectionActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SignatureSelectionActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 创建新签名
     */
    private void createNewSignature() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("创建新签名");

        String[] options = {"选择现有密钥库", "生成新密钥库"};

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        selectExistingKeystore();
                        break;
                    case 1:
                        generateNewKeystore();
                        break;
                }
            }
        });

        builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showSignatureDialog();
            }
        });

        builder.show();
    }

    /**
     * 选择现有密钥库
     */
    private void selectExistingKeystore() {
        // 创建输入对话框
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameEdit = new EditText(this);
        nameEdit.setHint("配置名称");
        layout.addView(nameEdit);

        final EditText pathEdit = new EditText(this);
        pathEdit.setHint("密钥库路径（如 /sdcard/my.keystore）");
        layout.addView(pathEdit);

        final EditText keystorePassEdit = new EditText(this);
        keystorePassEdit.setHint("密钥库密码");
        keystorePassEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(keystorePassEdit);

        final EditText aliasEdit = new EditText(this);
        aliasEdit.setHint("密钥别名");
        layout.addView(aliasEdit);

        final EditText keyPassEdit = new EditText(this);
        keyPassEdit.setHint("密钥密码");
        keyPassEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(keyPassEdit);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入密钥库信息");
        builder.setView(layout);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameEdit.getText().toString().trim();
                String path = pathEdit.getText().toString().trim();
                String keystorePass = keystorePassEdit.getText().toString().trim();
                String alias = aliasEdit.getText().toString().trim();
                String keyPass = keyPassEdit.getText().toString().trim();

                if (name.isEmpty() || path.isEmpty() || keystorePass.isEmpty() ||
                        alias.isEmpty() || keyPass.isEmpty()) {
                    Toast.makeText(SignatureSelectionActivity.this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 验证密钥库
                if (!KeyStoreGenerator.validateKeyStore(path, keystorePass, alias, keyPass)) {
                    Toast.makeText(SignatureSelectionActivity.this, "密钥库验证失败，请检查路径和密码", Toast.LENGTH_LONG).show();
                    return;
                }

                SignatureConfig.Config config = new SignatureConfig.Config(
                        name, path, keystorePass, alias, keyPass, false
                );

                if (mSignatureConfig.saveConfig(config)) {
                    Toast.makeText(SignatureSelectionActivity.this, "签名配置已保存", Toast.LENGTH_SHORT).show();
                    showSignatureDialog();
                } else {
                    Toast.makeText(SignatureSelectionActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createNewSignature();
            }
        });

        builder.show();
    }

    /**
     * 生成新密钥库
     */
    private void generateNewKeystore() {
        // 创建输入对话框
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameEdit = new EditText(this);
        nameEdit.setHint("配置名称");
        layout.addView(nameEdit);

        final EditText filenameEdit = new EditText(this);
        filenameEdit.setHint("密钥库文件名（如 my.keystore）");
        layout.addView(filenameEdit);

        final EditText keystorePassEdit = new EditText(this);
        keystorePassEdit.setHint("密钥库密码");
        keystorePassEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(keystorePassEdit);

        final EditText aliasEdit = new EditText(this);
        aliasEdit.setHint("密钥别名");
        layout.addView(aliasEdit);

        final EditText keyPassEdit = new EditText(this);
        keyPassEdit.setHint("密钥密码");
        keyPassEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(keyPassEdit);

        final EditText dnEdit = new EditText(this);
        dnEdit.setHint("证书DN名称（如 CN=MyApp,O=MyOrg,C=CN）");
        dnEdit.setText("CN=Android,O=MyOrg,C=CN");
        layout.addView(dnEdit);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("生成新密钥库");
        builder.setView(layout);

        builder.setPositiveButton("生成", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameEdit.getText().toString().trim();
                String filename = filenameEdit.getText().toString().trim();
                String keystorePass = keystorePassEdit.getText().toString().trim();
                String alias = aliasEdit.getText().toString().trim();
                String keyPass = keyPassEdit.getText().toString().trim();
                String dn = dnEdit.getText().toString().trim();

                if (name.isEmpty() || filename.isEmpty() || keystorePass.isEmpty() ||
                        alias.isEmpty() || keyPass.isEmpty() || dn.isEmpty()) {
                    Toast.makeText(SignatureSelectionActivity.this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 生成密钥库路径
                String keystorePath = "/sdcard/GGtool/keystore/" + filename;

                // 异步生成密钥库
                new GenerateKeystoreTask(name, keystorePath, keystorePass, alias, keyPass, dn).execute();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createNewSignature();
            }
        });

        builder.show();
    }

    /**
     * 异步生成密钥库任务
     */
    private class GenerateKeystoreTask extends AsyncTask<Void, Void, Boolean> {
        private ProgressDialog mProgressDialog;
        private String mName, mPath, mKeystorePass, mAlias, mKeyPass, mDN;

        GenerateKeystoreTask(String name, String path, String keystorePass,
                             String alias, String keyPass, String dn) {
            this.mName = name;
            this.mPath = path;
            this.mKeystorePass = keystorePass;
            this.mAlias = alias;
            this.mKeyPass = keyPass;
            this.mDN = dn;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(SignatureSelectionActivity.this);
            mProgressDialog.setMessage("正在生成密钥库...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // 确保目录存在
            File keystoreFile = new File(mPath);
            File parentDir = keystoreFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            KeyStoreGenerator generator = new KeyStoreGenerator();
            return generator.generateKeyStore(mPath, mKeystorePass, mAlias, mKeyPass, mDN, 25);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mProgressDialog.dismiss();

            if (success) {
                SignatureConfig.Config config = new SignatureConfig.Config(
                        mName, mPath, mKeystorePass, mAlias, mKeyPass, false
                );

                if (mSignatureConfig.saveConfig(config)) {
                    Toast.makeText(SignatureSelectionActivity.this,
                            "密钥库生成成功并已保存配置", Toast.LENGTH_SHORT).show();
                    showSignatureDialog();
                } else {
                    Toast.makeText(SignatureSelectionActivity.this,
                            "密钥库生成成功但保存配置失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(SignatureSelectionActivity.this,
                        "密钥库生成失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 配置列表适配器
     */
    private class ConfigAdapter extends ArrayAdapter<SignatureConfig.Config> {
        ConfigAdapter(Context context, List<SignatureConfig.Config> configs) {
            super(context, 0, configs);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            SignatureConfig.Config config = getItem(position);

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            text1.setText(config.name);
            text2.setText("别名: " + config.keyAlias);

            return convertView;
        }
    }

    /**
     * 设置回调接口
     */
    public void setCallback(SignatureCallback callback) {
        this.mCallback = callback;
    }
}

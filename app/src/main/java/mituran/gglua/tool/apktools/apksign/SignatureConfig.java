package mituran.gglua.tool.apktools.apksign;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 签名配置管理类
 */
public class SignatureConfig {
    private static final String TAG = "SignatureConfig";
    private static final String PREFS_NAME = "signature_configs";
    private static final String KEY_CONFIGS = "configs";
    private static final String KEY_DEFAULT_CONFIG = "default_config";

    private Context mContext;
    private SharedPreferences mPrefs;

    // 默认测试签名文件路径
    public static final String DEFAULT_KEYSTORE_PATH = "/sdcard/GGtool/keystore/test.keystore";
    public static final String DEFAULT_KEYSTORE_PASSWORD = "android";
    public static final String DEFAULT_KEY_ALIAS = "androiddebugkey";
    public static final String DEFAULT_KEY_PASSWORD = "android";

    public SignatureConfig(Context context) {
        this.mContext = context;
        this.mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 签名配置信息
     */
    public static class Config {
        public String name;              // 配置名称
        public String keystorePath;      // 密钥库路径
        public String keystorePassword;  // 密钥库密码
        public String keyAlias;          // 密钥别名
        public String keyPassword;       // 密钥密码
        public boolean isDefault;        // 是否为默认测试签名

        public Config() {
        }

        public Config(String name, String keystorePath, String keystorePassword,
                      String keyAlias, String keyPassword, boolean isDefault) {
            this.name = name;
            this.keystorePath = keystorePath;
            this.keystorePassword = keystorePassword;
            this.keyAlias = keyAlias;
            this.keyPassword = keyPassword;
            this.isDefault = isDefault;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("keystorePath", keystorePath);
            json.put("keystorePassword", keystorePassword);
            json.put("keyAlias", keyAlias);
            json.put("keyPassword", keyPassword);
            json.put("isDefault", isDefault);
            return json;
        }

        public static Config fromJson(JSONObject json) throws JSONException {
            Config config = new Config();
            config.name = json.getString("name");
            config.keystorePath = json.getString("keystorePath");
            config.keystorePassword = json.getString("keystorePassword");
            config.keyAlias = json.getString("keyAlias");
            config.keyPassword = json.getString("keyPassword");
            config.isDefault = json.optBoolean("isDefault", false);
            return config;
        }
    }

    /**
     * 获取默认测试签名配置
     */
    public Config getDefaultTestConfig() {
        return new Config(
                "默认测试签名",
                DEFAULT_KEYSTORE_PATH,
                DEFAULT_KEYSTORE_PASSWORD,
                DEFAULT_KEY_ALIAS,
                DEFAULT_KEY_PASSWORD,
                true
        );
    }

    /**
     * 保存签名配置
     */
    public boolean saveConfig(Config config) {
        try {
            List<Config> configs = getAllConfigs();

            // 检查是否已存在同名配置
            boolean found = false;
            for (int i = 0; i < configs.size(); i++) {
                if (configs.get(i).name.equals(config.name)) {
                    configs.set(i, config);
                    found = true;
                    break;
                }
            }

            if (!found) {
                configs.add(config);
            }

            // 保存到SharedPreferences
            JSONArray jsonArray = new JSONArray();
            for (Config c : configs) {
                jsonArray.put(c.toJson());
            }

            mPrefs.edit()
                    .putString(KEY_CONFIGS, jsonArray.toString())
                    .apply();

            Log.d(TAG, "签名配置已保存: " + config.name);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "保存签名配置失败", e);
            return false;
        }
    }

    /**
     * 获取所有自定义签名配置
     */
    public List<Config> getAllConfigs() {
        List<Config> configs = new ArrayList<>();

        try {
            String configsJson = mPrefs.getString(KEY_CONFIGS, null);
            if (configsJson != null) {
                JSONArray jsonArray = new JSONArray(configsJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    configs.add(Config.fromJson(json));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "读取签名配置失败", e);
        }

        return configs;
    }

    /**
     * 删除签名配置
     */
    public boolean deleteConfig(String configName) {
        try {
            List<Config> configs = getAllConfigs();
            configs.removeIf(c -> c.name.equals(configName));

            // 保存
            JSONArray jsonArray = new JSONArray();
            for (Config c : configs) {
                jsonArray.put(c.toJson());
            }

            mPrefs.edit()
                    .putString(KEY_CONFIGS, jsonArray.toString())
                    .apply();

            Log.d(TAG, "签名配置已删除: " + configName);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "删除签名配置失败", e);
            return false;
        }
    }

    /**
     * 根据名称获取配置
     */
    public Config getConfigByName(String name) {
        List<Config> configs = getAllConfigs();
        for (Config config : configs) {
            if (config.name.equals(name)) {
                return config;
            }
        }
        return null;
    }

    /**
     * 验证签名配置是否有效
     */
    public static boolean validateConfig(Config config) {
        if (config == null) {
            return false;
        }

        // 检查必填字段
        if (config.keystorePath == null || config.keystorePath.isEmpty() ||
                config.keystorePassword == null || config.keystorePassword.isEmpty() ||
                config.keyAlias == null || config.keyAlias.isEmpty() ||
                config.keyPassword == null || config.keyPassword.isEmpty()) {
            return false;
        }

        // 检查密钥库文件是否存在
        File keystoreFile = new File(config.keystorePath);
        if (!keystoreFile.exists() || !keystoreFile.isFile()) {
            Log.e(TAG, "密钥库文件不存在: " + config.keystorePath);
            return false;
        }

        return true;
    }

    /**
     * 设置/取消默认配置
     */
    public void setDefaultConfig(String configName) {
        mPrefs.edit()
                .putString(KEY_DEFAULT_CONFIG, configName)
                .apply();
    }

    /**
     * 获取默认配置名称
     */
    public String getDefaultConfigName() {
        return mPrefs.getString(KEY_DEFAULT_CONFIG, null);
    }

    /**
     * 初始化默认测试签名文件
     */
    public static boolean initDefaultKeystore(Context context) {
        try {
            File keystoreFile = new File(DEFAULT_KEYSTORE_PATH);

            // 如果已存在则跳过
            if (keystoreFile.exists()) {
                Log.d(TAG, "默认密钥库已存在");
                return true;
            }

            // 创建目录
            File parentDir = keystoreFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 从assets复制测试签名文件
            // 或者使用KeyStoreGenerator生成新的密钥库
            KeyStoreGenerator generator = new KeyStoreGenerator();
            boolean success = generator.generateKeyStore(
                    keystoreFile.getAbsolutePath(),
                    DEFAULT_KEYSTORE_PASSWORD,
                    DEFAULT_KEY_ALIAS,
                    DEFAULT_KEY_PASSWORD,
                    "CN=Android Debug,O=Android,C=US",
                    10  // 10年有效期
            );

            if (success) {
                Log.d(TAG, "默认密钥库已生成: " + keystoreFile.getAbsolutePath());
            } else {
                Log.e(TAG, "生成默认密钥库失败");
            }

            return success;

        } catch (Exception e) {
            Log.e(TAG, "初始化默认密钥库失败", e);
            return false;
        }
    }
}
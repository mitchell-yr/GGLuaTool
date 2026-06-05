package mituran.gglua.tool.communityFragment;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * ============================================================
 * 网络接口配置类 - 所有服务器地址和接口路径集中管理
 * 修改服务器IP或接口只需修改此文件
 * ============================================================
 */
public class ApiConfig {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_SERVER_URL = "community_server_url";

    // 默认服务器地址
    public static final String DEFAULT_BASE_URL = "http://localhost:8080";

    // ==================== 接口路径（常量） ====================
    public static final String BANNER_LIST_PATH = "/api/community/banners";
    public static final String SCRIPT_LIST_PATH = "/api/community/scripts";
    public static final String RESOURCE_LIST_PATH = "/api/community/resources";
    public static final String RESOURCE_DETAIL_PATH = "/api/community/resource/detail";

    // ==================== 超时设置(毫秒) ====================
    public static final int CONNECT_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 15000;

    /**
     * 从 SharedPreferences 读取用户设置的服务器地址
     */
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SERVER_URL, DEFAULT_BASE_URL);
    }

    /**
     * 保存用户设置的服务器地址
     */
    public static void setBaseUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
    }

    // ==================== 完整 URL（使用 getBaseUrl） ====================

    /** 轮播图列表 URL */
    public static String bannerListUrl(Context context) {
        return getBaseUrl(context) + BANNER_LIST_PATH;
    }

    /** 脚本分享列表 URL */
    public static String scriptListUrl(Context context) {
        return getBaseUrl(context) + SCRIPT_LIST_PATH;
    }

    /** 资源列表 URL */
    public static String resourceListUrl(Context context) {
        return getBaseUrl(context) + RESOURCE_LIST_PATH;
    }

    /** 资源详情 URL */
    public static String resourceDetailUrl(Context context) {
        return getBaseUrl(context) + RESOURCE_DETAIL_PATH;
    }
}

package mituran.gglua.tool.communityFragment;

/**
 * ============================================================
 * 网络接口配置类 - 所有服务器地址和接口路径集中管理
 * 修改服务器IP或接口只需修改此文件
 * ============================================================
 */
public class ApiConfig {

    // ==================== 服务器基础地址 ====================
    // 修改此处即可切换服务器
    public static final String BASE_URL = "http://localhost:8080";

    // ==================== 轮播图接口 ====================
    public static final String BANNER_LIST = BASE_URL + "/api/community/banners";

    // ==================== 脚本分享接口 ====================
    public static final String SCRIPT_LIST = BASE_URL + "/api/community/scripts";

    // ==================== 资源获取接口 ====================
    public static final String RESOURCE_LIST = BASE_URL + "/api/community/resources";

    // ==================== 资源详情接口 ====================
    // 用法: RESOURCE_DETAIL + "?id=1" 或 RESOURCE_DETAIL + "/1"
    public static final String RESOURCE_DETAIL = BASE_URL + "/api/community/resource/detail";

    // ==================== 超时设置(毫秒) ====================
    public static final int CONNECT_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 15000;
}
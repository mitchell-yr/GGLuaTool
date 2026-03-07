package mituran.gglua.tool.communityFragment;

import java.io.Serializable;

public class ResourceItem implements Serializable {
    private int id;
    private String title;
    private String desc;
    private String iconUrl;
    private String size;
    private String downloadUrl;

    // ====== 新增字段 ======
    private String version;         // 版本号
    private String author;          // 作者/开发者
    private String updateDate;      // 更新日期
    private String category;        // 分类标签
    private String detailUrl;       // 详情Markdown内容接口URL（网络资源用）
    private String assetMdPath;     // assets中的md文件路径（内置资源用）
    private String assetIconPath;   // assets中的图标路径（内置资源用）
    private boolean isBuiltIn;      // 是否是内置资源
    private int downloadCount;      // 下载量/获取次数
    private float rating;           // 评分 0-5

    // ====== 构造器 ======
    public ResourceItem() {}

    /**
     * 内置资源构造器
     */
    public static ResourceItem createBuiltIn(int id, String title, String desc,
                                             String version, String author,
                                             String size, String category,
                                             String assetMdPath, String assetIconPath,
                                             String downloadUrl) {
        ResourceItem item = new ResourceItem();
        item.id = id;
        item.title = title;
        item.desc = desc;
        item.version = version;
        item.author = author;
        item.size = size;
        item.category = category;
        item.assetMdPath = assetMdPath;
        item.assetIconPath = assetIconPath;
        item.downloadUrl = downloadUrl;
        item.isBuiltIn = true;
        return item;
    }

    // ====== Getter/Setter ======
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }

    public String getAssetMdPath() { return assetMdPath; }
    public void setAssetMdPath(String assetMdPath) { this.assetMdPath = assetMdPath; }

    public String getAssetIconPath() { return assetIconPath; }
    public void setAssetIconPath(String assetIconPath) { this.assetIconPath = assetIconPath; }

    public boolean isBuiltIn() { return isBuiltIn; }
    public void setBuiltIn(boolean builtIn) { isBuiltIn = builtIn; }

    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
}
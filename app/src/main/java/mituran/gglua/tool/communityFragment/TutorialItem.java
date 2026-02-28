package mituran.gglua.tool.communityFragment;

public class TutorialItem {
    private int iconResId;
    private String title;
    private String description;
    private String actionKey; // 用于区分点击事件

    public TutorialItem(int iconResId, String title, String description, String actionKey) {
        this.iconResId = iconResId;
        this.title = title;
        this.description = description;
        this.actionKey = actionKey;
    }

    public int getIconResId() { return iconResId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getActionKey() { return actionKey; }
}
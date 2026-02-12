package mituran.gglua.tool.tutorial;

// 这是一个公共的数据类，可以在项目的任何地方使用
public class TutorialChapter {
    private String title;
    private String content;

    public TutorialChapter(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }
}
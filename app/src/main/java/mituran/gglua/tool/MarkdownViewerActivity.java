package mituran.gglua.tool;

// Markdown文档查看器

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import mituran.gglua.tool.R;

public class MarkdownViewerActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markdown_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");

        if (title != null) {
            getSupportActionBar().setTitle(title);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);

        // 将Markdown转换为HTML显示
        String html = convertMarkdownToHtml(content);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String convertMarkdownToHtml(String markdown) {
        // 简单的Markdown转HTML，实际项目建议使用第三方库如commonmark
        if (markdown == null) {
            markdown = "暂无文档";
        }

        // 基本转换
        markdown = markdown.replaceAll("### (.*)", "<h3>$1</h3>");
        markdown = markdown.replaceAll("## (.*)", "<h2>$1</h2>");
        markdown = markdown.replaceAll("# (.*)", "<h1>$1</h1>");
        markdown = markdown.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        markdown = markdown.replaceAll("\\*(.*?)\\*", "<em>$1</em>");
        markdown = markdown.replaceAll("`(.*?)`", "<code>$1</code>");
        markdown = markdown.replaceAll("\n\n", "</p><p>");
        markdown = markdown.replaceAll("- (.*)", "<li>$1</li>");

        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; padding: 16px; line-height: 1.6; }" +
                "h1 { color: #333; border-bottom: 2px solid #007AFF; padding-bottom: 8px; }" +
                "h2 { color: #555; margin-top: 20px; }" +
                "h3 { color: #777; }" +
                "code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-family: monospace; }" +
                "pre { background: #f4f4f4; padding: 12px; border-radius: 5px; overflow-x: auto; }" +
                "li { margin: 8px 0; }" +
                "p { margin: 12px 0; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<p>" + markdown + "</p>" +
                "</body>" +
                "</html>";

        return html;
    }
}
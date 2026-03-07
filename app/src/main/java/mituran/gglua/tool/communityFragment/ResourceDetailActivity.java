package mituran.gglua.tool.communityFragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.noties.markwon.Markwon;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import mituran.gglua.tool.R;
import mituran.gglua.tool.util.HttpHelper;

public class ResourceDetailActivity extends AppCompatActivity {

    private static final String TAG = "ResourceDetail";

    public static final String EXTRA_RESOURCE_ITEM = "extra_resource_item";
    public static final String EXTRA_RESOURCE_ID = "extra_resource_id";

    // Views
    private CollapsingToolbarLayout collapsingToolbar;
    private Toolbar toolbar;
    private ImageView detailIcon;
    private TextView titleHeader, categoryHeader;
    private TextView detailVersion, detailSize, detailDownloads, detailRating;
    private TextView detailAuthor, detailDate, detailDesc;
    private ProgressBar mdLoading;
    private TextView mdError;
    private TextView markdownView;
    private TextView downloadBtn;
    private TextView priceLabel, sizeBottom;

    private Markwon markwon;
    private ResourceItem resourceItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 状态栏沉浸
        setStatusBarColor();

        setContentView(R.layout.activity_resource_detail);

        initViews();
        initMarkwon();
        parseIntent();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(0xFF6200EE);
    }

    private void initViews() {
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        toolbar = findViewById(R.id.detail_toolbar);
        detailIcon = findViewById(R.id.detail_icon);
        titleHeader = findViewById(R.id.detail_title_header);
        categoryHeader = findViewById(R.id.detail_category_header);

        detailVersion = findViewById(R.id.detail_version);
        detailSize = findViewById(R.id.detail_size);
        detailDownloads = findViewById(R.id.detail_downloads);
        detailRating = findViewById(R.id.detail_rating);

        detailAuthor = findViewById(R.id.detail_author);
        detailDate = findViewById(R.id.detail_date);
        detailDesc = findViewById(R.id.detail_desc);

        mdLoading = findViewById(R.id.detail_md_loading);
        mdError = findViewById(R.id.detail_md_error);
        markdownView = findViewById(R.id.detail_markdown_view);

        downloadBtn = findViewById(R.id.detail_download_btn);
        priceLabel = findViewById(R.id.detail_price_label);
        sizeBottom = findViewById(R.id.detail_size_bottom);

        // Toolbar返回
        toolbar.setNavigationOnClickListener(v -> finish());

        // 重试按钮
        mdError.setOnClickListener(v -> loadMarkdownContent());
    }

    private void initMarkwon() {
        markwon = Markwon.builder(this)
                .usePlugin(ImagesPlugin.create())
                .usePlugin(HtmlPlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .build();
    }

    private void parseIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        // 尝试从Intent获取序列化对象
        resourceItem = (ResourceItem) intent.getSerializableExtra(EXTRA_RESOURCE_ITEM);

        if (resourceItem != null) {
            bindData();
            loadMarkdownContent();
        } else {
            // 只传了ID，需要从网络获取详情
            int resourceId = intent.getIntExtra(EXTRA_RESOURCE_ID, -1);
            if (resourceId > 0) {
                loadDetailFromNetwork(resourceId);
            } else {
                Toast.makeText(this, "资源信息无效", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * 绑定基础数据到UI
     */
    private void bindData() {
        if (resourceItem == null) return;

        // 标题
        collapsingToolbar.setTitle(resourceItem.getTitle());
        titleHeader.setText(resourceItem.getTitle());

        // 分类标签
        if (!TextUtils.isEmpty(resourceItem.getCategory())) {
            categoryHeader.setVisibility(View.VISIBLE);
            categoryHeader.setText(resourceItem.getCategory());
        } else {
            categoryHeader.setVisibility(View.GONE);
        }

        // 指标
        detailVersion.setText(TextUtils.isEmpty(resourceItem.getVersion()) ?
                "--" : resourceItem.getVersion());
        detailSize.setText(TextUtils.isEmpty(resourceItem.getSize()) ?
                "--" : resourceItem.getSize());
        detailDownloads.setText(resourceItem.getDownloadCount() > 0 ?
                formatCount(resourceItem.getDownloadCount()) : "--");
        detailRating.setText(resourceItem.getRating() > 0 ?
                String.format("%.1f", resourceItem.getRating()) : "--");

        // 详细信息
        detailAuthor.setText(TextUtils.isEmpty(resourceItem.getAuthor()) ?
                "未知" : resourceItem.getAuthor());
        detailDate.setText(TextUtils.isEmpty(resourceItem.getUpdateDate()) ?
                "未知" : resourceItem.getUpdateDate());
        detailDesc.setText(TextUtils.isEmpty(resourceItem.getDesc()) ?
                "暂无描述" : resourceItem.getDesc());

        // 底部栏
        sizeBottom.setText(TextUtils.isEmpty(resourceItem.getSize()) ?
                "" : "大小: " + resourceItem.getSize());

        // 图标
        loadIcon();

        // 下载按钮
        downloadBtn.setOnClickListener(v -> onDownloadClick());
    }

    /**
     * 加载图标
     */
    private void loadIcon() {
        if (resourceItem == null) return;

        if (resourceItem.isBuiltIn() && !TextUtils.isEmpty(resourceItem.getAssetIconPath())) {
            // 从assets加载图标
            try {
                InputStream is = getAssets().open(resourceItem.getAssetIconPath());
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                is.close();
                if (bitmap != null) {
                    detailIcon.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, "Load asset icon failed: " + e.getMessage());
            }
        }

        if (!TextUtils.isEmpty(resourceItem.getIconUrl())) {
            // 从网络加载图标
            HttpHelper.downloadBitmap(resourceItem.getIconUrl(), new HttpHelper.BitmapCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    detailIcon.setImageBitmap(bitmap);
                }

                @Override
                public void onError(String error) {
                    detailIcon.setImageResource(R.drawable.ic_tutorial_function);
                }
            });
        } else {
            detailIcon.setImageResource(R.drawable.ic_tutorial_function);
        }
    }

    /**
     * 加载Markdown内容
     */
    private void loadMarkdownContent() {
        mdLoading.setVisibility(View.VISIBLE);
        mdError.setVisibility(View.GONE);
        markdownView.setVisibility(View.GONE);

        if (resourceItem == null) return;

        if (resourceItem.isBuiltIn() && !TextUtils.isEmpty(resourceItem.getAssetMdPath())) {
            // ====== 从assets加载Markdown ======
            loadMarkdownFromAssets(resourceItem.getAssetMdPath());
        } else if (!TextUtils.isEmpty(resourceItem.getDetailUrl())) {
            // ====== 从网络加载Markdown ======
            loadMarkdownFromNetwork(resourceItem.getDetailUrl());
        } else {
            // 尝试用资源ID从网络获取
            loadMarkdownFromNetwork(
                    ApiConfig.RESOURCE_DETAIL + "?id=" + resourceItem.getId());
        }
    }

    /**
     * 从assets加载MD文件
     */
    private void loadMarkdownFromAssets(String assetPath) {
        new Thread(() -> {
            try {
                InputStream is = getAssets().open(assetPath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                is.close();

                String mdContent = sb.toString();
                runOnUiThread(() -> renderMarkdown(mdContent));
            } catch (Exception e) {
                Log.e(TAG, "Load asset MD failed", e);
                runOnUiThread(() -> {
                    mdLoading.setVisibility(View.GONE);
                    mdError.setVisibility(View.VISIBLE);
                    mdError.setText("内容加载失败: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 从网络加载Markdown
     * 服务端返回格式: { "code":0, "data":{ "markdownContent":"...", ...其他字段 } }
     */
    private void loadMarkdownFromNetwork(String url) {
        HttpHelper.get(url, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        String mdContent = data.optString("markdownContent", "");

                        // 如果详情接口也返回了更新的基础信息，刷新显示
                        updateFromDetailResponse(data);

                        if (!TextUtils.isEmpty(mdContent)) {
                            renderMarkdown(mdContent);
                        } else {
                            mdLoading.setVisibility(View.GONE);
                            mdError.setVisibility(View.VISIBLE);
                            mdError.setText("暂无详细介绍");
                        }
                    } else {
                        mdLoading.setVisibility(View.GONE);
                        mdError.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse detail error", e);
                    // 如果返回的不是JSON，可能直接是Markdown文本
                    if (response != null && (response.contains("#") || response.contains("*"))) {
                        renderMarkdown(response);
                    } else {
                        mdLoading.setVisibility(View.GONE);
                        mdError.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(String error) {
                mdLoading.setVisibility(View.GONE);
                mdError.setVisibility(View.VISIBLE);
                mdError.setText("加载失败，点击重试\n" + error);
            }
        });
    }

    /**
     * 从网络获取完整详情（当只传ID时）
     */
    private void loadDetailFromNetwork(int resourceId) {
        String url = ApiConfig.RESOURCE_DETAIL + "?id=" + resourceId;
        HttpHelper.get(url, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        resourceItem = parseResourceFromJson(data);
                        bindData();

                        String mdContent = data.optString("markdownContent", "");
                        if (!TextUtils.isEmpty(mdContent)) {
                            renderMarkdown(mdContent);
                        } else {
                            mdLoading.setVisibility(View.GONE);
                            mdError.setVisibility(View.VISIBLE);
                            mdError.setText("暂无详细介绍");
                        }
                    } else {
                        Toast.makeText(ResourceDetailActivity.this,
                                "获取资源信息失败", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Toast.makeText(ResourceDetailActivity.this,
                            "数据解析失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ResourceDetailActivity.this,
                        "网络请求失败: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * 渲染Markdown到TextView
     */
    private void renderMarkdown(String mdContent) {
        mdLoading.setVisibility(View.GONE);
        mdError.setVisibility(View.GONE);
        markdownView.setVisibility(View.VISIBLE);
        markwon.setMarkdown(markdownView, mdContent);
    }

    /**
     * 从详情响应中更新基础信息
     */
    private void updateFromDetailResponse(JSONObject data) {
        try {
            if (resourceItem == null) return;

            String version = data.optString("version", "");
            if (!TextUtils.isEmpty(version)) {
                resourceItem.setVersion(version);
                detailVersion.setText(version);
            }

            String author = data.optString("author", "");
            if (!TextUtils.isEmpty(author)) {
                resourceItem.setAuthor(author);
                detailAuthor.setText(author);
            }

            String updateDate = data.optString("updateDate", "");
            if (!TextUtils.isEmpty(updateDate)) {
                resourceItem.setUpdateDate(updateDate);
                detailDate.setText(updateDate);
            }

            int downloadCount = data.optInt("downloadCount", 0);
            if (downloadCount > 0) {
                resourceItem.setDownloadCount(downloadCount);
                detailDownloads.setText(formatCount(downloadCount));
            }

            double rating = data.optDouble("rating", 0);
            if (rating > 0) {
                resourceItem.setRating((float) rating);
                detailRating.setText(String.format("%.1f", rating));
            }

            String downloadUrl = data.optString("downloadUrl", "");
            if (!TextUtils.isEmpty(downloadUrl)) {
                resourceItem.setDownloadUrl(downloadUrl);
            }
        } catch (Exception e) {
            Log.w(TAG, "Update detail fields error", e);
        }
    }

    /**
     * 从JSON解析ResourceItem
     */
    private ResourceItem parseResourceFromJson(JSONObject obj) {
        ResourceItem item = new ResourceItem();
        item.setId(obj.optInt("id", 0));
        item.setTitle(obj.optString("title", ""));
        item.setDesc(obj.optString("desc", ""));
        item.setIconUrl(obj.optString("iconUrl", ""));
        item.setSize(obj.optString("size", ""));
        item.setDownloadUrl(obj.optString("downloadUrl", ""));
        item.setVersion(obj.optString("version", ""));
        item.setAuthor(obj.optString("author", ""));
        item.setUpdateDate(obj.optString("updateDate", ""));
        item.setCategory(obj.optString("category", ""));
        item.setDetailUrl(obj.optString("detailUrl", ""));
        item.setDownloadCount(obj.optInt("downloadCount", 0));
        item.setRating((float) obj.optDouble("rating", 0));
        item.setBuiltIn(false);
        return item;
    }

    /**
     * 点击获取资源
     */
    private void onDownloadClick() {
        if (resourceItem == null) return;

        String url = resourceItem.getDownloadUrl();
        if (!TextUtils.isEmpty(url)) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开下载链接", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "暂无下载链接", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 格式化数量
     */
    private String formatCount(int count) {
        if (count >= 10000) {
            return String.format("%.1fw", count / 10000.0);
        } else if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
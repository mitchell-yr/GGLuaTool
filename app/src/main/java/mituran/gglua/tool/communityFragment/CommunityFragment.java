package mituran.gglua.tool.communityFragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import mituran.gglua.tool.R;
import mituran.gglua.tool.util.HttpHelper;
import mituran.gglua.tool.tutorial.EncryptTutorialActivity;
import mituran.gglua.tool.tutorial.GGFunctionAddingTutorialActivity;
import mituran.gglua.tool.tutorial.GGFunctionDocumentViewActivity;
import mituran.gglua.tool.tutorial.GGTutorialActivity;

public class CommunityFragment extends Fragment {

    private static final String TAG = "CommunityFragment";

    // ==================== 轮播图 ====================
    private ViewPager2 bannerViewPager;
    private LinearLayout indicatorContainer;
    private BannerAdapter bannerAdapter;
    private Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;
    private static final long BANNER_DELAY = 3000L; // 轮播间隔3秒

    // 本地默认轮播图
    private final int[] defaultBannerDrawables = {
            R.drawable.banner_default_1,
            R.drawable.banner_default_1,
            R.drawable.banner_default_1
    };

    // ==================== 教程 ====================
    private RecyclerView tutorialRecyclerView;

    // ==================== 脚本分享 ====================
    private RecyclerView scriptRecyclerView;
    private ScriptAdapter scriptAdapter;
    private ProgressBar scriptLoading;
    private TextView scriptEmpty;

    // ==================== 资源获取 ====================
    private RecyclerView resourceRecyclerView;
    private ResourceAdapter resourceAdapter;
    private ProgressBar resourceLoading;
    private TextView resourceEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupBanner();
        setupTutorials();
        setupScripts();
        setupResources();

        // 加载网络数据
        loadBannerData();
        loadScriptData();
        loadResourceData();
    }

    private void initViews(View view) {
        // 轮播图
        bannerViewPager = view.findViewById(R.id.banner_viewpager);
        indicatorContainer = view.findViewById(R.id.banner_indicator_container);

        // 教程
        tutorialRecyclerView = view.findViewById(R.id.tutorial_recyclerview);

        // 脚本分享
        scriptRecyclerView = view.findViewById(R.id.script_recyclerview);
        scriptLoading = view.findViewById(R.id.script_loading);
        scriptEmpty = view.findViewById(R.id.script_empty);
        view.findViewById(R.id.script_more).setOnClickListener(v -> {
            // TODO: 点击"更多"跳转脚本列表页
            Toast.makeText(getContext(), "查看更多脚本", Toast.LENGTH_SHORT).show();
        });

        // 资源获取
        resourceRecyclerView = view.findViewById(R.id.resource_recyclerview);
        resourceLoading = view.findViewById(R.id.resource_loading);
        resourceEmpty = view.findViewById(R.id.resource_empty);
        view.findViewById(R.id.resource_more).setOnClickListener(v -> {
            // TODO: 点击"更多"跳转资源列表页
            Toast.makeText(getContext(), "查看更多资源", Toast.LENGTH_SHORT).show();
        });
    }

    // ================================================================
    //                       轮播图
    // ================================================================

    private void setupBanner() {
        bannerAdapter = new BannerAdapter(defaultBannerDrawables);
        bannerAdapter.setOnBannerClickListener((item, position) -> {
            // ========== 轮播图点击事件预留 ==========
            // TODO: 在这里对接你的点击事件
            if (item.getLinkUrl() != null && !item.getLinkUrl().isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getLinkUrl()));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "轮播图 " + (position + 1) + ": " + item.getTitle(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        bannerViewPager.setAdapter(bannerAdapter);

        // 先设置本地默认轮播图
        List<BannerItem> defaultItems = new ArrayList<>();
        defaultItems.add(new BannerItem("", "", "GGlua工具箱"));
        defaultItems.add(new BannerItem("", "", "Lua脚本开发"));
        defaultItems.add(new BannerItem("", "", "社区交流"));
        bannerAdapter.setItems(defaultItems);

        // 设置到中间位置（实现"无限"轮播）
        int startPos = defaultItems.size() * 500;
        bannerViewPager.setCurrentItem(startPos, false);

        // 指示器
        setupIndicators(defaultItems.size());

        // 页面变化监听
        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(bannerAdapter.getRealPosition(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // 拖拽时暂停自动轮播，空闲时恢复
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    stopAutoScroll();
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    startAutoScroll();
                }
            }
        });

        startAutoScroll();
    }

    private void setupIndicators(int count) {
        indicatorContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(i == 0 ?
                    R.drawable.indicator_dot_selected : R.drawable.indicator_dot_normal);
            indicatorContainer.addView(dot);
        }
    }

    private void updateIndicators(int selectedPosition) {
        for (int i = 0; i < indicatorContainer.getChildCount(); i++) {
            ImageView dot = (ImageView) indicatorContainer.getChildAt(i);
            dot.setImageResource(i == selectedPosition ?
                    R.drawable.indicator_dot_selected : R.drawable.indicator_dot_normal);
        }
    }

    private void startAutoScroll() {
        stopAutoScroll();
        autoScrollRunnable = () -> {
            if (bannerViewPager != null && bannerAdapter.getRealItemCount() > 0) {
                int next = bannerViewPager.getCurrentItem() + 1;
                bannerViewPager.setCurrentItem(next, true);
            }
            autoScrollHandler.postDelayed(autoScrollRunnable, BANNER_DELAY);
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, BANNER_DELAY);
    }

    private void stopAutoScroll() {
        if (autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    /**
     * 从网络加载轮播图数据
     */
    private void loadBannerData() {
        HttpHelper.get(ApiConfig.BANNER_LIST, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.getInt("code") == 0) {
                        JSONArray dataArray = json.getJSONArray("data");
                        List<BannerItem> bannerItems = new ArrayList<>();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);
                            BannerItem item = new BannerItem();
                            item.setImageUrl(obj.optString("imageUrl", ""));
                            item.setLinkUrl(obj.optString("linkUrl", ""));
                            item.setTitle(obj.optString("title", ""));
                            bannerItems.add(item);
                        }

                        if (!bannerItems.isEmpty()) {
                            bannerAdapter.setItems(bannerItems);
                            int startPos = bannerItems.size() * 500;
                            bannerViewPager.setCurrentItem(startPos, false);
                            setupIndicators(bannerItems.size());

                            // 加载网络图片
                            for (int i = 0; i < bannerItems.size(); i++) {
                                final int index = i;
                                BannerItem item = bannerItems.get(i);
                                if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                                    HttpHelper.downloadBitmap(item.getImageUrl(),
                                            new HttpHelper.BitmapCallback() {
                                                @Override
                                                public void onSuccess(Bitmap bitmap) {
                                                    bannerAdapter.setBitmapForPosition(index, bitmap);
                                                }

                                                @Override
                                                public void onError(String error) {
                                                    Log.w(TAG, "Banner image load failed: " + error);
                                                }
                                            });
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Parse banner data error: " + e.getMessage());
                    // 解析失败使用本地默认图片，不做额外处理
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Load banner error: " + error);
                // 网络失败，继续使用本地默认轮播图
            }
        });
    }

    // ================================================================
    //                       教程版块
    // ================================================================

    private void setupTutorials() {
        List<TutorialItem> tutorials = new ArrayList<>();

        tutorials.add(new TutorialItem(
                R.drawable.ic_gg_logo,
                "GameGuardian使用教程",
                "学习GG修改器的基本使用方法",
                "ACTION_TUTORIAL_GG"
        ));

        tutorials.add(new TutorialItem(
                R.drawable.ic_gg_logo,
                "GGlua函数文档",
                "GG修改器Lua API完整参考文档",
                "ACTION_TUTORIAL_LUA_DOC"
        ));

        tutorials.add(new TutorialItem(
                R.drawable.ic_gg_logo,
                "lua脚本加密教程",
                "保护你的Lua脚本代码安全",
                "ACTION_TUTORIAL_ENCRYPT"
        ));

        tutorials.add(new TutorialItem(
                R.drawable.ic_gg_logo,
                "lua脚本解密教程",
                "学习Lua脚本解密分析技术",
                "ACTION_TUTORIAL_DECRYPT"
        ));

        tutorials.add(new TutorialItem(
                R.drawable.ic_gg_logo,
                "修改器布局美化教程",
                "打造专业美观的修改器界面",
                "ACTION_TUTORIAL_LAYOUT"
        ));

        tutorials.add(new TutorialItem(
                R.drawable.ic_gg_logo,
                "修改器函数添加教程",
                "学习如何自定义扩展修改器功能",
                "ACTION_TUTORIAL_FUNCTION"
        ));

        TutorialAdapter adapter = new TutorialAdapter(tutorials, (item, position) -> {
            // ========================================================
            // TODO: 在此对接你的Intent跳转
            // 通过 item.getActionKey() 区分不同教程
            // ========================================================
            switch (item.getActionKey()) {
                case "ACTION_TUTORIAL_GG":
                    Intent intent = new Intent(getContext(), GGTutorialActivity.class);
                    startActivity(intent);
                    break;

                case "ACTION_TUTORIAL_LUA_DOC":
                    Intent intent2 = new Intent(getContext(), GGFunctionDocumentViewActivity.class);
                    startActivity(intent2);
                    break;

                case "ACTION_TUTORIAL_ENCRYPT":
                    Intent intent3 = new Intent(getContext(), EncryptTutorialActivity.class);
                    startActivity(intent3);
                    break;

                case "ACTION_TUTORIAL_DECRYPT":
                    break;

                case "ACTION_TUTORIAL_LAYOUT":
                    break;

                case "ACTION_TUTORIAL_FUNCTION":
                    Intent intent6 = new Intent(getContext(), GGFunctionAddingTutorialActivity.class);
                    startActivity(intent6);
                    break;

                default:
                    Toast.makeText(getContext(), "打开失败："+item.getTitle(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        tutorialRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tutorialRecyclerView.setAdapter(adapter);
        tutorialRecyclerView.setNestedScrollingEnabled(false);
    }

    // ================================================================
    //                       脚本分享版块
    // ================================================================

    private void setupScripts() {
        scriptAdapter = new ScriptAdapter((item, position) -> {
            // TODO: 点击脚本分享项
            if (item.getUrl() != null && !item.getUrl().isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        scriptRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        scriptRecyclerView.setAdapter(scriptAdapter);
        scriptRecyclerView.setNestedScrollingEnabled(false);
    }

    private void loadScriptData() {
        scriptLoading.setVisibility(View.VISIBLE);
        scriptEmpty.setVisibility(View.GONE);
        scriptRecyclerView.setVisibility(View.GONE);

        HttpHelper.get(ApiConfig.SCRIPT_LIST, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                scriptLoading.setVisibility(View.GONE);
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.getInt("code") == 0) {
                        JSONArray dataArray = json.getJSONArray("data");
                        List<ScriptItem> scripts = new ArrayList<>();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);
                            ScriptItem item = new ScriptItem();
                            item.setId(obj.optInt("id", 0));
                            item.setTitle(obj.optString("title", ""));
                            item.setDesc(obj.optString("desc", ""));
                            item.setAuthor(obj.optString("author", ""));
                            item.setTag(obj.optString("tag", ""));
                            item.setDate(obj.optString("date", ""));
                            item.setUrl(obj.optString("url", ""));
                            scripts.add(item);
                        }

                        if (scripts.isEmpty()) {
                            scriptEmpty.setVisibility(View.VISIBLE);
                        } else {
                            scriptRecyclerView.setVisibility(View.VISIBLE);
                            scriptAdapter.setItems(scripts);
                        }
                    } else {
                        scriptEmpty.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse script data error", e);
                    scriptEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                scriptLoading.setVisibility(View.GONE);
                scriptEmpty.setVisibility(View.VISIBLE);
                Log.w(TAG, "Load script error: " + error);
            }
        });
    }

    // ================================================================
    //                       资源获取版块
    // ================================================================

    private void setupResources() {
        resourceAdapter = new ResourceAdapter(new ResourceAdapter.OnResourceClickListener() {
            @Override
            public void onDownloadClick(ResourceItem item, int position) {
                // TODO: 点击"获取"按钮
                if (item.getDownloadUrl() != null && !item.getDownloadUrl().isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(item.getDownloadUrl()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "无法打开下载链接", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "暂无下载链接", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onItemClick(ResourceItem item, int position) {
                // TODO: 点击资源项
                Toast.makeText(getContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        resourceRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        resourceRecyclerView.setAdapter(resourceAdapter);
        resourceRecyclerView.setNestedScrollingEnabled(false);
    }

    private void loadResourceData() {
        resourceLoading.setVisibility(View.VISIBLE);
        resourceEmpty.setVisibility(View.GONE);
        resourceRecyclerView.setVisibility(View.GONE);

        HttpHelper.get(ApiConfig.RESOURCE_LIST, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                resourceLoading.setVisibility(View.GONE);
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.getInt("code") == 0) {
                        JSONArray dataArray = json.getJSONArray("data");
                        List<ResourceItem> resources = new ArrayList<>();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);
                            ResourceItem item = new ResourceItem();
                            item.setId(obj.optInt("id", 0));
                            item.setTitle(obj.optString("title", ""));
                            item.setDesc(obj.optString("desc", ""));
                            item.setIconUrl(obj.optString("iconUrl", ""));
                            item.setSize(obj.optString("size", ""));
                            item.setDownloadUrl(obj.optString("downloadUrl", ""));
                            resources.add(item);
                        }

                        if (resources.isEmpty()) {
                            resourceEmpty.setVisibility(View.VISIBLE);
                        } else {
                            resourceRecyclerView.setVisibility(View.VISIBLE);
                            resourceAdapter.setItems(resources);
                        }
                    } else {
                        resourceEmpty.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse resource data error", e);
                    resourceEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                resourceLoading.setVisibility(View.GONE);
                resourceEmpty.setVisibility(View.VISIBLE);
                Log.w(TAG, "Load resource error: " + error);
            }
        });
    }

    // ================================================================
    //                       生命周期
    // ================================================================

    @Override
    public void onResume() {
        super.onResume();
        startAutoScroll();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoScroll();
        autoScrollHandler.removeCallbacksAndMessages(null);
    }
}
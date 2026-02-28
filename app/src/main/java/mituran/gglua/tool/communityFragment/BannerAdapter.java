package mituran.gglua.tool.communityFragment;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mituran.gglua.tool.R;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {

    private List<BannerItem> items = new ArrayList<>();
    private Map<String, Bitmap> bitmapCache = new HashMap<>();
    private int[] defaultDrawables;
    private OnBannerClickListener listener;

    public interface OnBannerClickListener {
        void onBannerClick(BannerItem item, int position);
    }

    public BannerAdapter(int[] defaultDrawables) {
        this.defaultDrawables = defaultDrawables;
    }

    public void setOnBannerClickListener(OnBannerClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<BannerItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setBitmapForPosition(int position, Bitmap bitmap) {
        if (position >= 0 && position < items.size()) {
            bitmapCache.put(items.get(position).getImageUrl(), bitmap);
            notifyItemChanged(position);
        }
    }

    // 获取真实位置（用于无限轮播）
    public int getRealPosition(int position) {
        if (items.size() == 0) return 0;
        return position % items.size();
    }

    @Override
    public int getItemCount() {
        // 返回一个很大的数实现"无限"轮播
        return items.size() == 0 ? 0 : items.size() * 1000;
    }

    public int getRealItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int realPos = getRealPosition(position);
        BannerItem item = items.get(realPos);

        // 尝试加载网络图片缓存
        Bitmap cached = bitmapCache.get(item.getImageUrl());
        if (cached != null) {
            holder.imageView.setImageBitmap(cached);
        } else if (defaultDrawables != null && realPos < defaultDrawables.length) {
            // 使用本地默认图
            holder.imageView.setImageResource(defaultDrawables[realPos]);
        } else if (defaultDrawables != null && defaultDrawables.length > 0) {
            holder.imageView.setImageResource(defaultDrawables[realPos % defaultDrawables.length]);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(item, realPos);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.banner_image);
        }
    }
}
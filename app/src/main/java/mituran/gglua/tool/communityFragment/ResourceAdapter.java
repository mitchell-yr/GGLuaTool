package mituran.gglua.tool.communityFragment;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mituran.gglua.tool.R;
import mituran.gglua.tool.util.HttpHelper;

public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ViewHolder> {

    private List<ResourceItem> items = new ArrayList<>();
    private Map<String, Bitmap> iconCache = new HashMap<>();
    private OnResourceClickListener listener;

    public interface OnResourceClickListener {
        void onDownloadClick(ResourceItem item, int position);
        void onItemClick(ResourceItem item, int position);
    }

    public ResourceAdapter(OnResourceClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ResourceItem> items) {
        this.items = items;
        notifyDataSetChanged();
        // 加载图标
        for (int i = 0; i < items.size(); i++) {
            loadIcon(items.get(i), i);
        }
    }

    private void loadIcon(ResourceItem item, int position) {
        if (item.getIconUrl() == null || item.getIconUrl().isEmpty()) return;
        HttpHelper.downloadBitmap(item.getIconUrl(), new HttpHelper.BitmapCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                iconCache.put(item.getIconUrl(), bitmap);
                notifyItemChanged(position);
            }

            @Override
            public void onError(String error) {
                // 使用默认图标
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resource, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResourceItem item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.desc.setText(item.getDesc());
        holder.size.setText(item.getSize());

        Bitmap cached = iconCache.get(item.getIconUrl());
        if (cached != null) {
            holder.icon.setImageBitmap(cached);
        } else {
            holder.icon.setImageResource(R.drawable.ic_tutorial_function); // 默认图标
        }

        holder.downloadBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(item, position);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, desc, size, downloadBtn;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.resource_icon);
            title = itemView.findViewById(R.id.resource_title);
            desc = itemView.findViewById(R.id.resource_desc);
            size = itemView.findViewById(R.id.resource_size);
            downloadBtn = itemView.findViewById(R.id.resource_download_btn);
        }
    }
}
package mituran.gglua.tool.communityFragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
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
    }

    /**
     * 追加数据（用于合并内置+网络）
     */
    public void addItems(List<ResourceItem> newItems) {
        int start = this.items.size();
        this.items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
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

        // 大小 + 内置标识
        String sizeText = item.getSize() != null ? item.getSize() : "";
        if (item.isBuiltIn()) {
            sizeText = "内置 · " + sizeText;
        }
        holder.size.setText(sizeText);

        // 加载图标
        loadItemIcon(holder, item, position);

        // 获取按钮
        holder.downloadBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(item, position);
            }
        });

        // 整项点击 → 打开详情页
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item, position);
            }
        });
    }

    private void loadItemIcon(ViewHolder holder, ResourceItem item, int position) {
        // 优先从缓存
        String cacheKey = item.isBuiltIn() ? ("asset:" + item.getAssetIconPath())
                : item.getIconUrl();

        if (cacheKey != null && iconCache.containsKey(cacheKey)) {
            holder.icon.setImageBitmap(iconCache.get(cacheKey));
            return;
        }

        // 内置资源从assets加载
        if (item.isBuiltIn() && item.getAssetIconPath() != null
                && !item.getAssetIconPath().isEmpty()) {
            try {
                InputStream is = holder.itemView.getContext().getAssets()
                        .open(item.getAssetIconPath());
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                is.close();
                if (bitmap != null) {
                    iconCache.put(cacheKey, bitmap);
                    holder.icon.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception e) {
                // fallback
            }
        }

        // 网络图标
        if (item.getIconUrl() != null && !item.getIconUrl().isEmpty()) {
            holder.icon.setImageResource(R.drawable.ic_tutorial_function); // 占位
            HttpHelper.downloadBitmap(item.getIconUrl(), new HttpHelper.BitmapCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    iconCache.put(cacheKey, bitmap);
                    notifyItemChanged(position);
                }

                @Override
                public void onError(String error) { }
            });
        } else {
            holder.icon.setImageResource(R.drawable.ic_tutorial_function);
        }
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
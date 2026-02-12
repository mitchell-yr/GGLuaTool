package mituran.gglua.tool.plugin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

import mituran.gglua.tool.R;
import mituran.gglua.tool.model.Plugin;

public class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.ViewHolder> {
    private Context context;
    private List<Plugin> plugins;
    private OnPluginActionListener listener;

    public interface OnPluginActionListener {
        void onEnableChanged(Plugin plugin, boolean enabled);
        void onItemClick(Plugin plugin);
        void onActionClick(Plugin plugin, View view);
        void onDocumentClick(Plugin plugin);
    }

    public PluginAdapter(Context context, List<Plugin> plugins, OnPluginActionListener listener) {
        this.context = context;
        this.plugins = plugins;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_plugin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Plugin plugin = plugins.get(position);

        //空值判断，防止 getName() 为 null
        String name = plugin.getName();
        holder.tvName.setText(name != null ? name : "未知插件");

        //防止 Version 为 null (尽管字符串拼接通常没问题，但严谨起见)
        String version = plugin.getVersion();
        holder.tvVersion.setText("v" + (version != null ? version : "未知版本"));

        //防止 getDescription() 为 null ---
        String description = plugin.getDescription();
        if (description == null || description.isEmpty()) {
            holder.tvDescription.setText("暂无描述");
            holder.tvExpandHint.setVisibility(View.GONE);
            holder.cardPlugin.setOnClickListener(null);
        } else {
            holder.tvDescription.setText(description);

            // 默认先折叠，重置状态（因为RecyclerView会复用View，必须重置）
            holder.tvDescription.setMaxLines(2);

            holder.tvDescription.post(() -> {
                // 防止视图已经被回收或Layout为空
                if (holder.tvDescription.getLayout() != null) {
                    android.text.Layout layout = holder.tvDescription.getLayout();
                    int lineCount = layout.getLineCount();

                    // 逻辑：
                    // 1. lineCount > 0: 确保有内容
                    // 2. layout.getEllipsisCount(lineCount - 1) > 0: 检查最后一行显示的文字是否有省略号(...)，如果有，说明被截断了
                    if (lineCount > 0 && layout.getEllipsisCount(lineCount - 1) > 0) {

                        holder.tvExpandHint.setVisibility(View.VISIBLE);
                        holder.tvExpandHint.setText("点击展开更多");

                        // 定义展开/收起的点击事件
                        View.OnClickListener toggleListener = v -> {
                            if (holder.tvDescription.getMaxLines() == 2) {
                                // 展开
                                holder.tvDescription.setMaxLines(Integer.MAX_VALUE);
                                holder.tvExpandHint.setText("点击收起");
                            } else {
                                // 收起
                                holder.tvDescription.setMaxLines(2);
                                holder.tvExpandHint.setText("点击展开更多");
                            }
                            // 保持原有的 item 点击回调
                            if (listener != null) {
                                listener.onItemClick(plugin);
                            }
                        };

                        // 给卡片和提示文本都绑定点击事件
                        holder.cardPlugin.setOnClickListener(toggleListener);
                        holder.tvExpandHint.setOnClickListener(toggleListener);

                    } else {
                        // 没有被截断（行数少于等于2），隐藏提示
                        holder.tvExpandHint.setVisibility(View.GONE);

                        // 恢复普通的 item 点击事件
                        holder.cardPlugin.setOnClickListener(v -> {
                            if (listener != null) listener.onItemClick(plugin);
                        });
                    }
                }
            });
        }

        holder.switchEnable.setChecked(plugin.isEnable());

        // 更新状态标签和指示条
        updateStatusUI(holder, plugin.isEnable());

        // 开关监听
        holder.switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                updateStatusUI(holder, isChecked);
                if (listener != null) {
                    listener.onEnableChanged(plugin, isChecked);
                }
            }
        });

        // 操作按钮
        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActionClick(plugin, v);
            }
        });

        // 使用文档按钮
        holder.btnDocument.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentClick(plugin);
            }
        });
    }

    private void updateStatusUI(ViewHolder holder, boolean isEnabled) {
        if (isEnabled) {
            holder.tvStatus.setText("已启用");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_enabled);
            holder.viewIndicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.purple_700));
        } else {
            holder.tvStatus.setText("已禁用");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_disabled);
            holder.viewIndicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.gray_400));
        }
    }

    @Override
    public int getItemCount() {
        return plugins != null ? plugins.size() : 0;
    }

    public void updatePlugins(List<Plugin> newPlugins) {
        this.plugins = newPlugins;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardPlugin;
        View viewIndicator;
        ImageView ivPluginIcon;
        TextView tvName, tvVersion, tvStatus, tvDescription, tvExpandHint;
        MaterialButton btnAction, btnDocument;
        MaterialSwitch switchEnable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPlugin = itemView.findViewById(R.id.card_plugin);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            ivPluginIcon = itemView.findViewById(R.id.iv_plugin_icon);
            tvName = itemView.findViewById(R.id.tv_plugin_name);
            tvVersion = itemView.findViewById(R.id.tv_plugin_version);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDescription = itemView.findViewById(R.id.tv_plugin_description);
            tvExpandHint = itemView.findViewById(R.id.tv_expand_hint);
            btnAction = itemView.findViewById(R.id.btn_action);
            btnDocument = itemView.findViewById(R.id.btn_document);
            switchEnable = itemView.findViewById(R.id.switch_enable);
        }
    }
}
package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mituran.gglua.tool.R;

public class TabAdapter extends RecyclerView.Adapter<TabAdapter.ViewHolder> {
    private Context context;
    private List<CodeTab> tabs;
    private int selectedPosition = 0;
    private OnTabClickListener listener;

    public interface OnTabClickListener {
        void onTabClick(int position);
        void onTabLongClick(int position);
    }

    public TabAdapter(Context context, List<CodeTab> tabs, OnTabClickListener listener) {
        this.context = context;
        this.tabs = tabs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.visual_lua_script_editor_item_tab, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CodeTab tab = tabs.get(position);

        // 设置标签名称
        if (tab.getType() == CodeTab.TabType.MAIN) {
            holder.tabName.setText("📝 " + tab.getName());
        } else {
            holder.tabName.setText("⚙️ " + tab.getName());
        }

        // 设置选中状态
        if (position == selectedPosition) {
            holder.tabContainer.setBackgroundColor(Color.parseColor("#FFFFFF"));
            holder.tabName.setTextColor(Color.parseColor("#2196F3"));
        } else {
            holder.tabContainer.setBackgroundColor(Color.parseColor("#E3F2FD"));
            holder.tabName.setTextColor(Color.parseColor("#666666"));
        }

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onTabClick(position);
            }
        });

        // 长按事件（用于删除或重命名）
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTabLongClick(position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View tabContainer;
        TextView tabName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tabContainer = itemView.findViewById(R.id.tab_container);
            tabName = itemView.findViewById(R.id.tab_name);
        }
    }
}
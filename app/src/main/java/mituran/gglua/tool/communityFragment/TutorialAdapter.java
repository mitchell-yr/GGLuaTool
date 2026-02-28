package mituran.gglua.tool.communityFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mituran.gglua.tool.R;

public class TutorialAdapter extends RecyclerView.Adapter<TutorialAdapter.ViewHolder> {

    private List<TutorialItem> items;
    private OnTutorialClickListener listener;

    public interface OnTutorialClickListener {
        void onTutorialClick(TutorialItem item, int position);
    }

    public TutorialAdapter(List<TutorialItem> items, OnTutorialClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tutorial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TutorialItem item = items.get(position);
        holder.icon.setImageResource(item.getIconResId());
        holder.title.setText(item.getTitle());
        holder.desc.setText(item.getDescription());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTutorialClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, desc;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tutorial_icon);
            title = itemView.findViewById(R.id.tutorial_title);
            desc = itemView.findViewById(R.id.tutorial_desc);
        }
    }
}
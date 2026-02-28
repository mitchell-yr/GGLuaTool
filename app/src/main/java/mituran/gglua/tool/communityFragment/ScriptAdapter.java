package mituran.gglua.tool.communityFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mituran.gglua.tool.R;

public class ScriptAdapter extends RecyclerView.Adapter<ScriptAdapter.ViewHolder> {

    private List<ScriptItem> items = new ArrayList<>();
    private OnScriptClickListener listener;

    public interface OnScriptClickListener {
        void onScriptClick(ScriptItem item, int position);
    }

    public ScriptAdapter(OnScriptClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ScriptItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_script, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScriptItem item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.desc.setText(item.getDesc());
        holder.author.setText("作者: " + item.getAuthor());
        holder.date.setText(item.getDate());

        if (item.getTag() != null && !item.getTag().isEmpty()) {
            holder.tag.setVisibility(View.VISIBLE);
            holder.tag.setText(item.getTag());
        } else {
            holder.tag.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onScriptClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, desc, author, date, tag;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.script_title);
            desc = itemView.findViewById(R.id.script_desc);
            author = itemView.findViewById(R.id.script_author);
            date = itemView.findViewById(R.id.script_date);
            tag = itemView.findViewById(R.id.script_tag);
        }
    }
}
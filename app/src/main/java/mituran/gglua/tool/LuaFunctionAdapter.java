package mituran.gglua.tool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import mituran.gglua.tool.model.LuaFunction;

public class LuaFunctionAdapter extends RecyclerView.Adapter<LuaFunctionAdapter.ViewHolder> {

    private List<LuaFunction> functionList;
    private List<LuaFunction> functionListFull; // 用于搜索的完整列表
    private OnFunctionClickListener listener;

    public interface OnFunctionClickListener {
        void onFunctionClick(LuaFunction function);
    }

    public LuaFunctionAdapter(List<LuaFunction> functionList) {
        this.functionList = functionList;
        this.functionListFull = new ArrayList<>(functionList);
    }

    public void setOnFunctionClickListener(OnFunctionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lua_function, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LuaFunction function = functionList.get(position);
        holder.tvName.setText(function.getName());
        holder.tvDesc.setText(function.getDescription());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFunctionClick(function);
            }
        });
    }

    @Override
    public int getItemCount() {
        return functionList.size();
    }

    // 搜索过滤
    public void filter(String query) {
        functionList.clear();
        if (query.isEmpty()) {
            functionList.addAll(functionListFull);
        } else {
            String lowerQuery = query.toLowerCase();
            for (LuaFunction function : functionListFull) {
                if (function.getName().toLowerCase().contains(lowerQuery) ||
                        function.getDescription().toLowerCase().contains(lowerQuery)) {
                    functionList.add(function);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDesc;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_function_name);
            tvDesc = itemView.findViewById(R.id.tv_function_desc);
        }
    }
}
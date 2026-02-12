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

public class ExpandableBlockTypeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_BLOCK = 1;

    private Context context;
    private List<CodeBlockTypeItem> categories;
    private OnBlockTypeSelectedListener listener;
    private List<ExpandableItem> expandableItems;

    public interface OnBlockTypeSelectedListener {
        void onBlockTypeSelected(DynamicCodeBlockType blockType);
    }

    // 用于展开/折叠的项目类
    private static class ExpandableItem {
        CodeBlockTypeItem category;
        DynamicCodeBlockType blockType;
        boolean isCategory;

        ExpandableItem(CodeBlockTypeItem category) {
            this.category = category;
            this.isCategory = true;
        }

        ExpandableItem(CodeBlockTypeItem category, DynamicCodeBlockType blockType) {
            this.category = category;
            this.blockType = blockType;
            this.isCategory = false;
        }
    }

    public ExpandableBlockTypeAdapter(Context context, List<CodeBlockTypeItem> categories,
                                      OnBlockTypeSelectedListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
        this.expandableItems = new java.util.ArrayList<>();

        // 初始化展开项列表（默认全部折叠）
        for (CodeBlockTypeItem category : categories) {
            expandableItems.add(new ExpandableItem(category));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return expandableItems.get(position).isCategory ? TYPE_CATEGORY : TYPE_BLOCK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CATEGORY) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.visual_lua_script_editor_item_block_category, parent, false);
            return new CategoryViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.visual_lua_script_editor_item_block_type, parent, false);
            return new BlockTypeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ExpandableItem item = expandableItems.get(position);

        if (holder instanceof CategoryViewHolder) {
            CategoryViewHolder categoryHolder = (CategoryViewHolder) holder;
            CodeBlockTypeItem category = item.category;

            categoryHolder.categoryName.setText(category.getCategoryName());
            categoryHolder.expandIcon.setText(category.isExpanded() ? "▼" : "▶");

            categoryHolder.itemView.setOnClickListener(v -> {
                toggleCategory(position);
            });

        } else if (holder instanceof BlockTypeViewHolder) {
            BlockTypeViewHolder blockHolder = (BlockTypeViewHolder) holder;
            DynamicCodeBlockType blockType = item.blockType;

            blockHolder.blockName.setText(blockType.getDisplayName());
            blockHolder.blockContainer.setBackgroundColor(Color.parseColor(blockType.getColor()));

            blockHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBlockTypeSelected(blockType);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return expandableItems.size();
    }

    // 切换分类的展开/折叠状态
    private void toggleCategory(int position) {
        ExpandableItem item = expandableItems.get(position);
        CodeBlockTypeItem category = item.category;

        if (category.isExpanded()) {
            // 折叠：移除所有子项
            category.setExpanded(false);
            int removeCount = 0;
            for (int i = position + 1; i < expandableItems.size(); i++) {
                if (expandableItems.get(i).isCategory) {
                    break;
                }
                removeCount++;
            }

            for (int i = 0; i < removeCount; i++) {
                expandableItems.remove(position + 1);
            }

            notifyItemChanged(position);
            notifyItemRangeRemoved(position + 1, removeCount);

        } else {
            // 展开：添加所有子项
            category.setExpanded(true);
            int insertPosition = position + 1;

            for (DynamicCodeBlockType blockType : category.getBlockTypes()) {
                expandableItems.add(insertPosition, new ExpandableItem(category, blockType));
                insertPosition++;
            }

            notifyItemChanged(position);
            notifyItemRangeInserted(position + 1, category.getBlockTypes().size());
        }
    }

    // 分类ViewHolder
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;
        TextView expandIcon;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.category_name);
            expandIcon = itemView.findViewById(R.id.expand_icon);
        }
    }

    // 代码块类型ViewHolder
    static class BlockTypeViewHolder extends RecyclerView.ViewHolder {
        View blockContainer;
        TextView blockName;

        BlockTypeViewHolder(@NonNull View itemView) {
            super(itemView);
            blockContainer = itemView.findViewById(R.id.block_type_container);
            blockName = itemView.findViewById(R.id.block_type_name);
        }
    }
}
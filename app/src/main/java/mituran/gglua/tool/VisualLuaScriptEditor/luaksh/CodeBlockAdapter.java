package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import mituran.gglua.tool.R;

public class CodeBlockAdapter extends RecyclerView.Adapter<CodeBlockAdapter.ViewHolder> {
    private Context context;
    private List<CodeBlock> codeBlocks;
    private OnCodeBlockClickListener listener;

    public interface OnCodeBlockClickListener {
        void onBlockClick(int position);
        void onBlockLongClick(int position);
    }

    public CodeBlockAdapter(Context context, List<CodeBlock> codeBlocks, OnCodeBlockClickListener listener) {
        this.context = context;
        this.codeBlocks = codeBlocks;
        this.listener = listener;
    }

    public void updateCodeBlocks(List<CodeBlock> newCodeBlocks) {
        this.codeBlocks = newCodeBlocks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.visual_lua_script_editor_item_code_block, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CodeBlock block = codeBlocks.get(position);

        // 清除之前的内容
        holder.contentContainer.removeAllViews();

        // 获取代码块颜色
        String blockColor = getBlockColor(block, position);

        // 设置背景颜色
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor(blockColor));
        drawable.setCornerRadius(8);

        // 特殊起始块添加边框效果
        if (block.getType().isSpecialStartBlock()) {
            drawable.setStroke(2, Color.parseColor("#FFFFFF"));
        }

        holder.blockContainer.setBackground(drawable);

        // 设置缩进
        int indentDp = block.getIndentLevel() * 32;
        int indicatorWidth = 20;

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.blockContainer.getLayoutParams();
        params.setMargins(dpToPx(indentDp + 8), dpToPx(1), dpToPx(8), dpToPx(1));
        holder.blockContainer.setLayoutParams(params);

        // 设置分支指示器
        LinearLayout.LayoutParams indicatorParams = (LinearLayout.LayoutParams) holder.branchIndicator.getLayoutParams();
        indicatorParams.width = dpToPx(indicatorWidth);
        holder.branchIndicator.setLayoutParams(indicatorParams);

        if (block.getType().isSpecialStartBlock()) {
            // 特殊起始块显示特殊图标
            holder.branchIndicator.setVisibility(View.VISIBLE);
            holder.branchIndicator.setText("★");
        } else if (block.getType().isBlockStart()) {
            holder.branchIndicator.setVisibility(View.VISIBLE);
            holder.branchIndicator.setText("▼");
        } else if (block.getType().isBlockMiddle()) {
            holder.branchIndicator.setVisibility(View.VISIBLE);
            holder.branchIndicator.setText("◆");
        } else if (block.getType().isBlockEnd()) {
            holder.branchIndicator.setVisibility(View.VISIBLE);
            holder.branchIndicator.setText("▲");
        } else {
            holder.branchIndicator.setVisibility(View.INVISIBLE);
            holder.branchIndicator.setText("");
        }

        // 构建结构化内容
        List<CodeBlockStructure.Part> parts = block.getParts();
        for (int i = 0; i < parts.size(); i++) {
            CodeBlockStructure.Part part = parts.get(i);

            if (part.type == CodeBlockStructure.PartType.LABEL) {
                TextView label = createLabel(part.text);
                holder.contentContainer.addView(label);
            } else {
                EditText input = createInput(part, i, block, position);
                holder.contentContainer.addView(input);
            }
        }

        // 设置点击监听
        holder.blockContainer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBlockClick(position);
            }
        });

        holder.blockContainer.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onBlockLongClick(position);
            }
            return true;
        });
    }

    private String getBlockColor(CodeBlock block, int position) {
        CodeBlockType type = block.getType();

        if (!type.isBlockEnd()) {
            return type.getColor();
        }

        String matchingColor = findMatchingStartBlockColor(position);
        return matchingColor != null ? matchingColor : type.getColor();
    }

    private String findMatchingStartBlockColor(int endPosition) {
        int depth = 0;

        for (int i = endPosition - 1; i >= 0; i--) {
            CodeBlock block = codeBlocks.get(i);
            CodeBlockType type = block.getType();

            if (type.isBlockEnd()) {
                depth++;
            } else if (type.isBlockStart()) {
                if (depth == 0) {
                    return type.getColor();
                } else {
                    depth--;
                }
            }
        }

        return null;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setTextSize(13);
        label.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
        return label;
    }

    private EditText createInput(CodeBlockStructure.Part part, int partIndex,
                                 CodeBlock block, int blockPosition) {
        EditText input = new EditText(context);
        input.setHint(part.text);
        input.setText(part.value);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#CCFFFFFF"));
        input.setTextSize(13);
        input.setBackgroundColor(Color.parseColor("#33FFFFFF"));
        input.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4));
        input.setMinWidth(dpToPx(50));
        input.setMinimumWidth(dpToPx(50));
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(2), 0, dpToPx(2), 0);
        input.setLayoutParams(params);

        input.setOnClickListener(v -> {
            v.requestFocus();
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                part.value = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return input;
    }

    @Override
    public int getItemCount() {
        return codeBlocks.size();
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout blockContainer;
        LinearLayout contentContainer;
        TextView branchIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            blockContainer = itemView.findViewById(R.id.block_container);
            contentContainer = itemView.findViewById(R.id.content_container);
            branchIndicator = itemView.findViewById(R.id.branch_indicator);
        }
    }
}
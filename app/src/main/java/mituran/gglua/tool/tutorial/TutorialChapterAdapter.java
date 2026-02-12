package mituran.gglua.tool.tutorial;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import mituran.gglua.tool.R;

public class TutorialChapterAdapter extends RecyclerView.Adapter<TutorialChapterAdapter.ViewHolder> {

    private List<TutorialChapter> chapters;
    private OnChapterClickListener listener;
    private int selectedPosition = 0;

    public interface OnChapterClickListener {
        void onChapterClick(int position);
    }

    public TutorialChapterAdapter(List<TutorialChapter> chapters, OnChapterClickListener listener) {
        this.chapters = chapters;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tutorial__chapter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TutorialChapter chapter = chapters.get(position);  // 这里改成 GGTutorialActivity
        holder.titleText.setText(chapter.getTitle());

        // 设置副标题（章节序号）
        holder.subtitleText.setText(String.format("章节 %d/%d", position + 1, chapters.size()));

        // 设置选中状态
        boolean isSelected = position == selectedPosition;

        // 动画效果切换卡片状态
        if (isSelected) {
            // 选中状态
            holder.cardView.setCardElevation(8f);
            holder.cardView.setStrokeWidth(2);
            holder.cardView.setStrokeColor(holder.itemView.getContext().getColor(R.color.purple_500));
            holder.iconContainer.setBackgroundResource(R.drawable.chapter_icon_bg_selected);
            holder.titleText.setTextColor(holder.itemView.getContext().getColor(R.color.purple_500));
            holder.checkIcon.setVisibility(View.VISIBLE);

            // 缩放动画
            holder.cardView.animate()
                    .scaleX(1.02f)
                    .scaleY(1.02f)
                    .setDuration(200)
                    .start();
        } else {
            // 未选中状态
            holder.cardView.setCardElevation(2f);
            holder.cardView.setStrokeWidth(0);
            holder.iconContainer.setBackgroundResource(R.drawable.chapter_icon_bg);
            holder.titleText.setTextColor(holder.itemView.getContext().getColor(R.color.text_primary));
            holder.checkIcon.setVisibility(View.GONE);

            holder.cardView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChapterClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView titleText;
        TextView subtitleText;
        LinearLayout iconContainer;
        ImageView checkIcon;

        ViewHolder(View view) {
            super(view);
            cardView = (MaterialCardView) view;
            titleText = view.findViewById(R.id.chapter_title);
            subtitleText = view.findViewById(R.id.chapter_subtitle);
            iconContainer = view.findViewById(R.id.icon_container);
            checkIcon = view.findViewById(R.id.check_icon);
        }
    }
}
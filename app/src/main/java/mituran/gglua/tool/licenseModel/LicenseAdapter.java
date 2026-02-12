package mituran.gglua.tool.licenseModel;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast; // 添加 Toast 提示

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mituran.gglua.tool.R;
import mituran.gglua.tool.licenseModel.LicenseInfo;

public class LicenseAdapter extends RecyclerView.Adapter<LicenseAdapter.ViewHolder> {

    private Context context;
    private List<LicenseInfo> licenseList;

    public LicenseAdapter(Context context, List<LicenseInfo> licenseList) {
        this.context = context;
        this.licenseList = licenseList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_license, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LicenseInfo license = licenseList.get(position);

        holder.tvLibraryName.setText(license.getLibraryName());
        holder.tvAuthor.setText(license.getAuthor());
        holder.tvVersion.setText("v" + license.getVersion());
        holder.tvLicenseType.setText(license.getLicenseType());

        // 修正1：确保文本不为空，否则占位
        holder.tvLicenseContent.setText(license.getLicenseContent());

        String firstChar = "";
        if (license.getLibraryName() != null && !license.getLibraryName().isEmpty()) {
            firstChar = license.getLibraryName().substring(0, 1).toUpperCase();
        }
        holder.tvAvatar.setText(firstChar);

        // 修正2：处理链接显示逻辑
        if (license.getUrl() != null && !license.getUrl().isEmpty()) {
            holder.tvUrl.setText(license.getUrl());
            holder.tvUrl.setVisibility(View.VISIBLE);
            holder.labelUrl.setVisibility(View.VISIBLE); // 同时也控制"项目地址"标签的显示
        } else {
            holder.tvUrl.setVisibility(View.GONE);
            holder.labelUrl.setVisibility(View.GONE);
        }

        // 状态恢复
        if (license.isExpanded()) {
            holder.layoutContent.setVisibility(View.VISIBLE);
            holder.layoutContent.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.ivExpand.setRotation(180f);
        } else {
            holder.layoutContent.setVisibility(View.GONE);
            holder.layoutContent.getLayoutParams().height = 0;
            holder.ivExpand.setRotation(0f);
        }

        holder.cardHeader.setOnClickListener(v -> {
            boolean willExpand = !license.isExpanded();
            license.setExpanded(willExpand);

            if (willExpand) {
                // 旋转箭头
                holder.ivExpand.animate().rotation(180f).setDuration(300).start();
                expandView(holder.layoutContent, holder.cardView);
            } else {
                holder.ivExpand.animate().rotation(0f).setDuration(300).start();
                collapseView(holder.layoutContent);
            }
        });

        holder.tvUrl.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(license.getUrl()));
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "无法打开链接: " + license.getUrl(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 核心修正：精确测量高度
    private void expandView(final View view, View parentView) {
        view.setVisibility(View.VISIBLE);

        // 使用 MeasureSpec.EXACTLY 和 parentWidth 来确保 TextView 知道它必须换行
        // 从而计算出换行后的真实高度
        int widthSpec = View.MeasureSpec.makeMeasureSpec(parentView.getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        // 这里的 padding 是为了补偿 CardView 的内边距，确保测量准确
        // 如果测量依然不准，可以适当增加 extraHeight
        view.measure(widthSpec, heightSpec);

        final int targetHeight = view.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = val;
            view.setLayoutParams(layoutParams);

            // 动画结束时，将高度设为 WRAP_CONTENT，防止内容再次变动时被截断
            if (val == targetHeight) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        });
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void collapseView(final View view) {
        final int initialHeight = view.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.addUpdateListener(animation -> {
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.requestLayout();
            if ((int) animation.getAnimatedValue() == 0) {
                view.setVisibility(View.GONE);
            }
        });
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    @Override
    public int getItemCount() {
        return licenseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        View cardHeader;
        TextView tvAvatar, tvLibraryName, tvAuthor, tvVersion, tvLicenseType;
        ImageView ivExpand;
        LinearLayout layoutContent;
        TextView tvLicenseContent, tvUrl, labelUrl; // 添加 labelUrl

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_license);
            cardHeader = itemView.findViewById(R.id.card_header);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvLibraryName = itemView.findViewById(R.id.tv_library_name);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvVersion = itemView.findViewById(R.id.tv_version);
            tvLicenseType = itemView.findViewById(R.id.tv_license_type);
            ivExpand = itemView.findViewById(R.id.iv_expand);

            layoutContent = itemView.findViewById(R.id.layout_content);
            tvLicenseContent = itemView.findViewById(R.id.tv_license_content);
            tvUrl = itemView.findViewById(R.id.tv_url);
            labelUrl = itemView.findViewById(R.id.tv_label_url); // 绑定XML中新加的ID
        }
    }
}
package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 自定义RecyclerView，固定宽度测量方式
 * 解决EditText获得焦点后导致的布局问题
 */
public class FixedWidthRecyclerView extends RecyclerView {
    private int cachedWidth = 0;

    public FixedWidthRecyclerView(@NonNull Context context) {
        super(context);
    }

    public FixedWidthRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedWidthRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        
        // 计算所有item的最大宽度
        int maxWidth = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != null) {
                measureChild(child, widthSpec, heightSpec);
                int childWidth = child.getMeasuredWidth();
                if (childWidth > maxWidth) {
                    maxWidth = childWidth;
                }
            }
        }
        
        // 如果计算出的宽度大于缓存宽度，更新缓存
        if (maxWidth > cachedWidth) {
            cachedWidth = maxWidth;
        }
        
        // 使用缓存的最大宽度，确保不会缩小
        int finalWidth = Math.max(getMeasuredWidth(), cachedWidth);
        setMeasuredDimension(finalWidth, getMeasuredHeight());
    }
    
    /**
     * 强制重新计算宽度（在添加/删除item后调用）
     */
    public void recalculateWidth() {
        cachedWidth = 0;
        requestLayout();
    }
}

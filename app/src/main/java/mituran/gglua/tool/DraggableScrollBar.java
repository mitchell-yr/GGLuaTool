package mituran.gglua.tool;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * 可拖动的滚动条组件
 */
public class DraggableScrollBar extends View {

    private Paint trackPaint;
    private Paint thumbPaint;
    private RectF thumbRect;
    private ScrollView scrollView;

    private float thumbHeight = 100f;
    private float thumbY = 0f;
    private boolean isDragging = false;
    private float dragStartY = 0f;
    private float dragStartThumbY = 0f;

    private int thumbColor = Color.parseColor("#6200EE");
    private int trackColor = Color.parseColor("#E0E0E0");
    private float thumbWidth = 8f;
    private float thumbRadius = 4f;

    public DraggableScrollBar(Context context) {
        super(context);
        init();
    }

    public DraggableScrollBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DraggableScrollBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化画笔
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(thumbColor);
        thumbPaint.setStyle(Paint.Style.FILL);

        thumbRect = new RectF();

        // 转换dp到px
        float density = getResources().getDisplayMetrics().density;
        thumbWidth = thumbWidth * density;
        thumbRadius = thumbRadius * density;

        // 确保视图可见
        setVisibility(VISIBLE);
        setWillNotDraw(false);
    }

    /**
     * 绑定ScrollView
     */
    public void attachToScrollView(ScrollView scrollView) {
        this.scrollView = scrollView;

        // 监听ScrollView的滚动事件
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (!isDragging) {
                updateThumbPosition();
                invalidate();
            }
        });

        // 监听布局变化
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            updateThumbPosition();
            invalidate();
        });

        // 初始更新
        post(() -> {
            updateThumbPosition();
            invalidate();
        });
    }

    /**
     * 更新滑块位置
     */
    private void updateThumbPosition() {
        if (scrollView == null || getHeight() == 0) return;

        View child = scrollView.getChildAt(0);
        if (child == null || child.getHeight() == 0) return;

        int scrollViewHeight = scrollView.getHeight();
        int contentHeight = child.getHeight();

        if (contentHeight <= scrollViewHeight) {
            // 内容不需要滚动
            setVisibility(INVISIBLE);
            return;
        } else {
            setVisibility(VISIBLE);
        }

        // 计算滑块高度（根据可见区域比例）
        float visibleRatio = (float) scrollViewHeight / contentHeight;
        thumbHeight = Math.max(50f, getHeight() * visibleRatio);

        // 计算滑块位置
        int scrollY = scrollView.getScrollY();
        int maxScroll = contentHeight - scrollViewHeight;

        if (maxScroll > 0) {
            float scrollRatio = (float) scrollY / maxScroll;
            float maxThumbY = getHeight() - thumbHeight;
            thumbY = scrollRatio * maxThumbY;
        } else {
            thumbY = 0f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        if (width == 0 || height == 0) return;

        // 绘制轨道
        float trackLeft = (width - thumbWidth) / 2;
        RectF trackRect = new RectF(trackLeft, 0, trackLeft + thumbWidth, height);
        canvas.drawRoundRect(trackRect, thumbRadius, thumbRadius, trackPaint);

        // 如果没有绑定ScrollView或内容不够长，绘制一个最小滑块
        if (scrollView == null) {
            thumbHeight = Math.min(100f, height * 0.3f);
            thumbY = 0;
        }

        // 绘制滑块
        thumbRect.set(trackLeft, thumbY, trackLeft + thumbWidth, thumbY + thumbHeight);
        canvas.drawRoundRect(thumbRect, thumbRadius, thumbRadius, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scrollView == null) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 检查是否点击在滑块上
                if (thumbRect.contains(event.getX(), event.getY())) {
                    isDragging = true;
                    dragStartY = event.getY();
                    dragStartThumbY = thumbY;

                    // 增大滑块（视觉反馈）
                    thumbPaint.setColor(Color.parseColor("#3700B3"));
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float deltaY = event.getY() - dragStartY;
                    thumbY = Math.max(0, Math.min(getHeight() - thumbHeight, dragStartThumbY + deltaY));

                    // 根据滑块位置更新ScrollView
                    updateScrollViewPosition();
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;

                    // 恢复滑块颜色
                    thumbPaint.setColor(thumbColor);
                    invalidate();
                    return true;
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * 根据滑块位置更新ScrollView的滚动位置
     */
    private void updateScrollViewPosition() {
        if (scrollView == null) return;

        View child = scrollView.getChildAt(0);
        if (child == null) return;

        int scrollViewHeight = scrollView.getHeight();
        int contentHeight = child.getHeight();
        int maxScroll = contentHeight - scrollViewHeight;

        float maxThumbY = getHeight() - thumbHeight;
        float scrollRatio = thumbY / maxThumbY;

        int targetScrollY = (int) (scrollRatio * maxScroll);
        scrollView.scrollTo(0, targetScrollY);
    }

    /**
     * 设置滑块颜色
     */
    public void setThumbColor(int color) {
        this.thumbColor = color;
        thumbPaint.setColor(color);
        invalidate();
    }

    /**
     * 设置轨道颜色
     */
    public void setTrackColor(int color) {
        this.trackColor = color;
        trackPaint.setColor(color);
        invalidate();
    }

    /**
     * 强制刷新滚动条
     */
    public void refresh() {
        post(() -> {
            updateThumbPosition();
            invalidate();
        });
    }
}
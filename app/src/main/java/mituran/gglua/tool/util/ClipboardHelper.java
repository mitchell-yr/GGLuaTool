package mituran.gglua.tool.util;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;

public class ClipboardHelper {

    private final Context context;
    private final ClipboardManager clipboardManager;

    public ClipboardHelper(Context context) {
        this.context = context.getApplicationContext();
        this.clipboardManager = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    /**
     * 复制文本
     */
    public boolean copyText(String text) {
        return copyText("text", text);
    }

    /**
     * 复制带标签的文本
     */
    public boolean copyText(String label, String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        try {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboardManager.setPrimaryClip(clip);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查剪贴板是否有文本
     */
    public boolean hasText() {
        if (!clipboardManager.hasPrimaryClip()) {
            return false;
        }

        ClipDescription description = clipboardManager.getPrimaryClipDescription();
        if (description == null) {
            return false;
        }

        return description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML);
    }

    /**
     * 获取剪贴板文本
     */
    public String getText() {
        if (!hasText()) {
            return null;
        }

        ClipData clip = clipboardManager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return null;
        }

        CharSequence text = clip.getItemAt(0).getText();
        return text != null ? text.toString() : null;
    }

    /**
     * 清空剪贴板
     */
    public void clear() {
        ClipData clip = ClipData.newPlainText("", "");
        clipboardManager.setPrimaryClip(clip);
    }

    /**
     * 添加剪贴板监听器
     */
    public void addPrimaryClipChangedListener(ClipboardManager.OnPrimaryClipChangedListener listener) {
        clipboardManager.addPrimaryClipChangedListener(listener);
    }

    /**
     * 移除剪贴板监听器
     */
    public void removePrimaryClipChangedListener(ClipboardManager.OnPrimaryClipChangedListener listener) {
        clipboardManager.removePrimaryClipChangedListener(listener);
    }
}
package mituran.gglua.tool;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BuildOutputLogManager {

    public enum LogLevel {
        INFO(Color.BLACK),
        SUCCESS(Color.parseColor("#4CAF50")),
        WARNING(Color.parseColor("#FF9800")),
        ERROR(Color.parseColor("#F44336")),
        DEBUG(Color.parseColor("#2196F3"));

        private final int color;

        LogLevel(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    private SpannableStringBuilder logBuffer;
    private SimpleDateFormat timeFormat;
    private LogCallback callback;

    public interface LogCallback {
        void onLogUpdated(SpannableStringBuilder log);
    }

    public BuildOutputLogManager() {
        this.logBuffer = new SpannableStringBuilder();
        this.timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    }

    public void setCallback(LogCallback callback) {
        this.callback = callback;
    }

    public void log(String message) {
        log(message, LogLevel.INFO);
    }

    public void log(String message, LogLevel level) {
        String timestamp = "[" + timeFormat.format(new Date()) + "] ";
        String fullMessage = timestamp + message + "\n";

        SpannableString spannable = new SpannableString(fullMessage);
        spannable.setSpan(
                new ForegroundColorSpan(level.getColor()),
                0,
                fullMessage.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        logBuffer.append(spannable);

        if (callback != null) {
            callback.onLogUpdated(logBuffer);
        }
    }

    public void logInfo(String message) {
        log("INFO: " + message, LogLevel.INFO);
    }

    public void logSuccess(String message) {
        log("SUCCESS: " + message, LogLevel.SUCCESS);
    }

    public void logWarning(String message) {
        log("WARNING: " + message, LogLevel.WARNING);
    }

    public void logError(String message) {
        log("ERROR: " + message, LogLevel.ERROR);
    }

    public void logDebug(String message) {
        log("DEBUG: " + message, LogLevel.DEBUG);
    }

    public void clear() {
        logBuffer.clear();
        if (callback != null) {
            callback.onLogUpdated(logBuffer);
        }
    }

    public String getLogText() {
        return logBuffer.toString();
    }

    public SpannableStringBuilder getLogBuffer() {
        return logBuffer;
    }
}
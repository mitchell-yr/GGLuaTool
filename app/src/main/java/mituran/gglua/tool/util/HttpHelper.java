package mituran.gglua.tool.util;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mituran.gglua.tool.communityFragment.ApiConfig;

/**
 * 轻量级网络请求工具 - 无需额外依赖
 */
public class HttpHelper {

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(String response);
        void onError(String error);
    }

    /**
     * GET请求
     */
    public static void get(String urlStr, Callback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(ApiConfig.CONNECT_TIMEOUT);
                connection.setReadTimeout(ApiConfig.READ_TIMEOUT);
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    is.close();

                    String result = sb.toString();
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP Error: " + responseCode));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * 下载图片为Bitmap（用于轮播图）
     */
    public static void downloadBitmap(String urlStr, BitmapCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(ApiConfig.CONNECT_TIMEOUT);
                connection.setReadTimeout(ApiConfig.READ_TIMEOUT);

                InputStream is = connection.getInputStream();
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                is.close();

                if (bitmap != null) {
                    mainHandler.post(() -> callback.onSuccess(bitmap));
                } else {
                    mainHandler.post(() -> callback.onError("Decode failed"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public interface BitmapCallback {
        void onSuccess(android.graphics.Bitmap bitmap);
        void onError(String error);
    }
}
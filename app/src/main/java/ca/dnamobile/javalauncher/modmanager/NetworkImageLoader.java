package ca.dnamobile.javalauncher.modmanager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class NetworkImageLoader {
    private NetworkImageLoader() {
    }

    public static void load(final ImageView imageView, final String str, final int i) {
        imageView.setTag(str == null ? "" : str);
        imageView.setImageResource(i);
        if (str == null || str.trim().isEmpty()) {
            return;
        }
        Thread thread = new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.modmanager.NetworkImageLoader$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                NetworkImageLoader.lambda$load$1(str, imageView, i);
            }
        }, "ModrinthIconLoader");
        thread.setDaemon(true);
        thread.start();
    }

    static /* synthetic */ void lambda$load$1(final String str, final ImageView imageView, final int i) {
        final Bitmap bitmapDecodeStream = null;
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setReadTimeout(15000);
            httpURLConnection.setRequestProperty("User-Agent", "JavaLauncher/1.0 (Android Minecraft Launcher)");
            try {
                InputStream inputStream = httpURLConnection.getInputStream();
                try {
                    bitmapDecodeStream = BitmapFactory.decodeStream(inputStream);
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } finally {
                }
            } finally {
                httpURLConnection.disconnect();
            }
        } catch (Throwable unused) {
        }
        imageView.post(new Runnable() { // from class: ca.dnamobile.javalauncher.modmanager.NetworkImageLoader$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                NetworkImageLoader.lambda$load$0(imageView, str, bitmapDecodeStream, i);
            }
        });
    }

    static /* synthetic */ void lambda$load$0(ImageView imageView, String str, Bitmap bitmap, int i) {
        Object tag = imageView.getTag();
        if ((tag instanceof String) && tag.equals(str)) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(i);
            }
        }
    }
}

package ca.dnamobile.javalauncher.skin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.ImageView;
import ca.dnamobile.javalauncher.R;
import ca.dnamobile.javalauncher.data.AccountStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class PlayerHeadLoader {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private PlayerHeadLoader() {
    }

    public static void loadInto(Context context, final ImageView imageView, AccountStore.Account account, CustomSkinStore customSkinStore) {
        imageView.setImageResource(R.drawable.ic_player_head_placeholder);
        final Context applicationContext = context.getApplicationContext();
        final File fileResolveActiveOfflineSkin = resolveActiveOfflineSkin(account);
        final File skinFile = (fileResolveActiveOfflineSkin == null && customSkinStore != null && customSkinStore.isEnabled()) ? customSkinStore.getSkinFile() : null;
        final String strNormalizeSkinUrl = account != null ? AccountSkinCache.normalizeSkinUrl(account.skinUrl) : "";
        final File cachedSkinFileIfPresent = AccountSkinCache.getCachedSkinFileIfPresent(applicationContext, account);
        EXECUTOR.execute(new Runnable() { // from class: ca.dnamobile.javalauncher.skin.PlayerHeadLoader$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PlayerHeadLoader.lambda$loadInto$1(fileResolveActiveOfflineSkin, skinFile, cachedSkinFileIfPresent, strNormalizeSkinUrl, applicationContext, imageView);
            }
        });
    }

    static /* synthetic */ void lambda$loadInto$1(File file, File file2, File file3, String str, Context context, final ImageView imageView) {
        final Bitmap bitmapLoadHeadFromSkinFile = (file == null || !file.isFile()) ? null : loadHeadFromSkinFile(file);
        if (bitmapLoadHeadFromSkinFile == null && file2 != null && file2.isFile()) {
            bitmapLoadHeadFromSkinFile = loadHeadFromSkinFile(file2);
        }
        if (bitmapLoadHeadFromSkinFile == null && file3 != null && file3.isFile()) {
            bitmapLoadHeadFromSkinFile = loadHeadFromSkinFile(file3);
        }
        if (bitmapLoadHeadFromSkinFile == null && str.length() > 0) {
            File cachedSkinFile = getCachedSkinFile(context, str);
            if (!cachedSkinFile.exists()) {
                downloadSkinQuietly(str, cachedSkinFile);
            }
            if (cachedSkinFile.exists()) {
                bitmapLoadHeadFromSkinFile = loadHeadFromSkinFile(cachedSkinFile);
            }
        }
        imageView.post(new Runnable() { // from class: ca.dnamobile.javalauncher.skin.PlayerHeadLoader$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                PlayerHeadLoader.lambda$loadInto$0(bitmapLoadHeadFromSkinFile, imageView);
            }
        });
    }

    static /* synthetic */ void lambda$loadInto$0(Bitmap bitmap, ImageView imageView) {
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.ic_player_head_placeholder);
        }
    }

    private static File resolveActiveOfflineSkin(AccountStore.Account account) {
        if (account == null || !account.isOfflineAccount() || account.offlineSkinPath == null || account.offlineSkinPath.trim().isEmpty()) {
            return null;
        }
        File file = new File(account.offlineSkinPath);
        if (file.isFile()) {
            return file;
        }
        return null;
    }

    public static Bitmap loadHeadFromSkinFile(File file) {
        Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmapDecodeFile == null || bitmapDecodeFile.getWidth() < 64 || bitmapDecodeFile.getHeight() < 32) {
            return null;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(7);
        Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap(bitmapDecodeFile, 8, 8, 8, 8);
        canvas.drawBitmap(bitmapCreateBitmap2, 0.0f, 0.0f, paint);
        if (bitmapDecodeFile.getWidth() >= 48 && bitmapDecodeFile.getHeight() >= 16) {
            Bitmap bitmapCreateBitmap3 = Bitmap.createBitmap(bitmapDecodeFile, 40, 8, 8, 8);
            canvas.drawBitmap(bitmapCreateBitmap3, 0.0f, 0.0f, paint);
            bitmapCreateBitmap3.recycle();
        }
        bitmapCreateBitmap2.recycle();
        Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmapCreateBitmap, 128, 128, false);
        bitmapCreateBitmap.recycle();
        return bitmapCreateScaledBitmap;
    }

    private static File getCachedSkinFile(Context context, String str) {
        File file = new File(context.getCacheDir(), "player_skins");
        if (!file.exists()) {
            file.mkdirs();
        }
        return new File(file, sha1(str) + ".png");
    }

    private static void downloadSkinQuietly(String str, File file) {
        HttpURLConnection httpURLConnection = null;
        try {
            HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(AccountSkinCache.normalizeSkinUrl(str)).openConnection();
            try {
                httpURLConnection2.setConnectTimeout(10000);
                httpURLConnection2.setReadTimeout(10000);
                httpURLConnection2.setUseCaches(true);
                httpURLConnection2.setRequestProperty("User-Agent", "JavaLauncher");
                int responseCode = httpURLConnection2.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    File file2 = new File(file.getParentFile(), file.getName() + ".tmp");
                    InputStream inputStream = httpURLConnection2.getInputStream();
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(file2);
                        try {
                            byte[] bArr = new byte[8192];
                            while (true) {
                                int i = inputStream.read(bArr);
                                if (i == -1) {
                                    break;
                                } else {
                                    fileOutputStream.write(bArr, 0, i);
                                }
                            }
                            fileOutputStream.close();
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            if (CustomSkinStore.isSkinValid(file2)) {
                                if (file.exists()) {
                                    file.delete();
                                }
                                file2.renameTo(file);
                            } else {
                                file2.delete();
                            }
                            if (httpURLConnection2 != null) {
                                httpURLConnection2.disconnect();
                                return;
                            }
                            return;
                        } finally {
                        }
                    } finally {
                    }
                }
                if (httpURLConnection2 != null) {
                    httpURLConnection2.disconnect();
                }
            } catch (Throwable unused) {
                httpURLConnection = httpURLConnection2;
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
        } catch (Throwable unused2) {
        }
    }

    private static String sha1(String str) {
        try {
            byte[] bArrDigest = MessageDigest.getInstance("SHA-1").digest(str.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bArrDigest) {
                sb.append(String.format("%02x", Byte.valueOf(b)));
            }
            return sb.toString();
        } catch (Throwable unused) {
            return String.valueOf(str.hashCode());
        }
    }
}

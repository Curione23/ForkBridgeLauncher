package ca.dnamobile.javalauncher.skin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class CustomSkinStore {
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_MODEL = "model";
    private static final String PREFS = "java_launcher_custom_skin";
    private static final String SKIN_DIR = "custom_skins";
    private static final String SKIN_FILE = "selected_skin.png";
    private final Context context;
    private final SharedPreferences preferences;

    public CustomSkinStore(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext;
        this.preferences = applicationContext.getSharedPreferences(PREFS, 0);
    }

    public SkinModelType importSkin(Uri uri) throws IOException {
        File skinDirectory = getSkinDirectory();
        if (!skinDirectory.exists() && !skinDirectory.mkdirs()) {
            throw new IOException("Could not create skin folder: " + skinDirectory.getAbsolutePath());
        }
        File file = new File(skinDirectory, "selected_skin.png.tmp");
        File file2 = new File(skinDirectory, SKIN_FILE);
        copyUriToFile(uri, file);
        if (!isSkinValid(file)) {
            file.delete();
            throw new IOException("Invalid skin. Use a 64x64 or 64x32 PNG skin.");
        }
        SkinModelType skinModel = getSkinModel(file);
        if (file2.exists() && !file2.delete()) {
            throw new IOException("Could not replace old skin.");
        }
        if (!file.renameTo(file2)) {
            copyFile(file, file2);
            file.delete();
        }
        this.preferences.edit().putBoolean(KEY_ENABLED, true).putString(KEY_MODEL, skinModel.id).apply();
        return skinModel;
    }

    public void clear() {
        this.preferences.edit().clear().apply();
        File skinFile = getSkinFile();
        if (skinFile.exists()) {
            skinFile.delete();
        }
    }

    public boolean isEnabled() {
        return this.preferences.getBoolean(KEY_ENABLED, false) && getSkinFile().exists();
    }

    public File getSkinFile() {
        return new File(getSkinDirectory(), SKIN_FILE);
    }

    public SkinModelType getSkinModel() {
        return SkinModelType.fromId(this.preferences.getString(KEY_MODEL, SkinModelType.CLASSIC.id));
    }

    public OfflineSkinProfile buildOfflineProfile(String str) {
        boolean zIsEnabled = isEnabled();
        SkinModelType skinModel = zIsEnabled ? getSkinModel() : SkinModelType.NONE;
        return new OfflineSkinProfile(getOfflineUuidWithSkinModel(str, skinModel), zIsEnabled ? getSkinFile() : null, skinModel, zIsEnabled);
    }

    private File getSkinDirectory() {
        return new File(this.context.getFilesDir(), SKIN_DIR);
    }

    private void copyUriToFile(Uri uri, File file) throws IOException {
        InputStream inputStreamOpenInputStream = this.context.getContentResolver().openInputStream(uri);
        try {
            if (inputStreamOpenInputStream == null) {
                throw new IOException("Could not open selected skin.");
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                byte[] bArr = new byte[8192];
                while (true) {
                    int i = inputStreamOpenInputStream.read(bArr);
                    if (i == -1) {
                        break;
                    } else {
                        fileOutputStream.write(bArr, 0, i);
                    }
                }
                fileOutputStream.close();
                if (inputStreamOpenInputStream != null) {
                    inputStreamOpenInputStream.close();
                }
            } finally {
            }
        } catch (Throwable th) {
            if (inputStreamOpenInputStream != null) {
                try {
                    inputStreamOpenInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static void copyFile(File file, File file2) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                byte[] bArr = new byte[8192];
                while (true) {
                    int i = fileInputStream.read(bArr);
                    if (i != -1) {
                        fileOutputStream.write(bArr, 0, i);
                    } else {
                        fileOutputStream.close();
                        fileInputStream.close();
                        return;
                    }
                }
            } finally {
            }
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static boolean isSkinValid(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth == 64 && options.outHeight == 64) {
            return true;
        }
        return options.outWidth == 64 && options.outHeight == 32;
    }

    public static SkinModelType getSkinModel(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmapDecodeFile == null) {
            return SkinModelType.CLASSIC;
        }
        return bitmapDecodeFile.getHeight() == 32 ? SkinModelType.CLASSIC : detectSkinModel(bitmapDecodeFile);
    }

    private static SkinModelType detectSkinModel(Bitmap bitmap) {
        if (bitmap.getWidth() < 64 || bitmap.getHeight() < 64) {
            return SkinModelType.CLASSIC;
        }
        return isTransparent(bitmap, 54, 20, 2, 12) ? SkinModelType.SLIM : SkinModelType.CLASSIC;
    }

    private static boolean isTransparent(Bitmap bitmap, int i, int i2, int i3, int i4) {
        for (int i5 = i; i5 < i + i3; i5++) {
            for (int i6 = i2; i6 < i2 + i4; i6++) {
                if (((bitmap.getPixel(i5, i6) >>> 24) & 255) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String getOfflineUuidWithSkinModel(String str, SkinModelType skinModelType) {
        UUID uuidNameUUIDFromBytes = UUID.nameUUIDFromBytes(("OfflinePlayer:" + str).getBytes(StandardCharsets.UTF_8));
        if (skinModelType == SkinModelType.NONE) {
            return uuidNameUUIDFromBytes.toString();
        }
        if (((uuidNameUUIDFromBytes.hashCode() & 1) == 1) == (skinModelType == SkinModelType.SLIM)) {
            return uuidNameUUIDFromBytes.toString();
        }
        String string = uuidNameUUIDFromBytes.toString();
        int iDigit = Character.digit(string.charAt(string.length() - 1), 16);
        if (iDigit == -1) {
            return uuidNameUUIDFromBytes.toString();
        }
        return string.substring(0, string.length() - 1) + Integer.toHexString(iDigit ^ 1).charAt(0);
    }
}

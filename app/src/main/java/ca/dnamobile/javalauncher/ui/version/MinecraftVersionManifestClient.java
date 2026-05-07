package ca.dnamobile.javalauncher.ui.version;

import android.content.Context;
import androidx.browser.trusted.sharing.ShareTarget;
import ca.dnamobile.javalauncher.data.model.MinecraftVersion;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class MinecraftVersionManifestClient {
    private static final String VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private MinecraftVersionManifestClient() {
    }

    public static List<MinecraftVersion> loadVersions(Context context) throws Exception {
        String string;
        PathManager.initContextConstants(context);
        File file = new File(PathManager.FILE_VERSION_LIST);
        try {
            string = downloadText(VERSION_MANIFEST_URL);
            writeString(file, string);
        } catch (Throwable th) {
            if (!file.exists()) {
                throw th;
            }
            string = readString(file);
        }
        return parseVersions(string);
    }

    public static String downloadText(String str) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(30000);
        httpURLConnection.setRequestProperty("User-Agent", "JavaLauncher/1.0");
        httpURLConnection.setRequestMethod(ShareTarget.METHOD_GET);
        int responseCode = httpURLConnection.getResponseCode();
        String stream = readStream((responseCode < 200 || responseCode >= 300) ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
        httpURLConnection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("HTTP " + responseCode + " while downloading version manifest");
        }
        return stream;
    }

    private static List<MinecraftVersion> parseVersions(String str) throws Exception {
        JSONArray jSONArray = new JSONObject(str).getJSONArray("versions");
        ArrayList arrayList = new ArrayList(jSONArray.length());
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObject = jSONArray.getJSONObject(i);
            arrayList.add(new MinecraftVersion(jSONObject.optString("id"), jSONObject.optString("type"), jSONObject.optString("releaseTime"), jSONObject.optString("url")));
        }
        return arrayList;
    }

    private static String readStream(InputStream inputStream) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    sb.append(line).append('\n');
                } else {
                    String string = sb.toString();
                    bufferedReader.close();
                    return string;
                }
            }
        } catch (Throwable th) {
            try {
                bufferedReader.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static String readString(File file) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream;
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
        try {
            byte[] bArr = new byte[65536];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i != -1) {
                    byteArrayOutputStream.write(bArr, 0, i);
                } else {
                    String string = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                    byteArrayOutputStream.close();
                    fileInputStream.close();
                    return string;
                }
                fileInputStream.close();
                throw th;
            }
        } finally {
        }
    }

    private static void writeString(File file, String str) throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            fileOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();
        } catch (Throwable th) {
            try {
                fileOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }
}

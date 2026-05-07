package ca.dnamobile.javalauncher.skin;

import androidx.browser.trusted.sharing.ShareTarget;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class MicrosoftSkinUploader {
    private static final String SKIN_UPLOAD_URL = "https://api.minecraftservices.com/minecraft/profile/skins";

    private MicrosoftSkinUploader() {
    }

    public static void uploadSkin(String str, File file, SkinModelType skinModelType) throws IOException {
        InputStream errorStream;
        if (str.trim().isEmpty()) {
            throw new IOException("Missing Minecraft access token. Refresh the Microsoft account and try again.");
        }
        if (!file.isFile()) {
            throw new IOException("Selected skin file was not found.");
        }
        if (!CustomSkinStore.isSkinValid(file)) {
            throw new IOException("Invalid skin. Use a 64x64 or 64x32 PNG skin.");
        }
        String str2 = skinModelType == SkinModelType.SLIM ? "slim" : "classic";
        String str3 = "JavaLauncherSkinBoundary" + UUID.randomUUID().toString().replace("-", "");
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(SKIN_UPLOAD_URL).openConnection();
        httpURLConnection.setRequestMethod(ShareTarget.METHOD_POST);
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setRequestProperty("Authorization", "Bearer " + str);
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + str3);
        OutputStream outputStream = httpURLConnection.getOutputStream();
        try {
            writeTextPart(outputStream, str3, "variant", str2);
            writeFilePart(outputStream, str3, "file", "skin.png", "image/png", file);
            outputStream.write(("--" + str3 + "--\r\n").getBytes(StandardCharsets.UTF_8));
            if (outputStream != null) {
                outputStream.close();
            }
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                errorStream = httpURLConnection.getInputStream();
            } else {
                errorStream = httpURLConnection.getErrorStream();
            }
            String fully = readFully(errorStream);
            httpURLConnection.disconnect();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Minecraft skin upload failed: HTTP " + responseCode + (fully.isEmpty() ? "" : " " + fully));
            }
        } catch (Throwable th) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static void writeTextPart(OutputStream outputStream, String str, String str2, String str3) throws IOException {
        outputStream.write(("--" + str + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + str2 + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(str3.getBytes(StandardCharsets.UTF_8));
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFilePart(OutputStream outputStream, String str, String str2, String str3, String str4, File file) throws IOException {
        outputStream.write(("--" + str + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + str2 + "\"; filename=\"" + str3 + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: " + str4 + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] bArr = new byte[8192];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i != -1) {
                    outputStream.write(bArr, 0, i);
                } else {
                    fileInputStream.close();
                    outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    return;
                }
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

    private static String readFully(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line != null) {
                    sb.append(line);
                } else {
                    bufferedReader.close();
                    return sb.toString();
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
    }
}

package ca.dnamobile.javalauncher.skin;

import android.util.Base64;
import androidx.browser.trusted.sharing.ShareTarget;
import ca.dnamobile.javalauncher.feature.log.Logging;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class OfflineYggdrasilServer {
    private static final String TAG = "OfflineYggdrasilServer";
    private final Map<String, CharacterProfile> charactersByName;
    private final Map<String, CharacterProfile> charactersByUuid;
    private final String implementationName;
    private final String implementationVersion;
    private final KeyPair keyPair;
    private final int requestedPort;
    private volatile boolean running;
    private final String serverName;
    private ServerSocket serverSocket;
    private Thread serverThread;

    /* JADX INFO: Access modifiers changed from: private */
    interface Signer {
        String sign(String str) throws Exception;
    }

    public OfflineYggdrasilServer() throws Exception {
        this("JavaLauncher_Offline", "JavaLauncher", "1.0");
    }

    public OfflineYggdrasilServer(String str, String str2, String str3) throws Exception {
        this(0, str, str2, str3);
    }

    public OfflineYggdrasilServer(int i, String str, String str2, String str3) throws Exception {
        this.charactersByUuid = new ConcurrentHashMap();
        this.charactersByName = new ConcurrentHashMap();
        this.requestedPort = Math.max(0, i);
        this.serverName = str;
        this.implementationName = str2;
        this.implementationVersion = str3;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        this.keyPair = keyPairGenerator.generateKeyPair();
    }

    public synchronized void start() throws IOException {
        if (this.running) {
            return;
        }
        this.serverSocket = new ServerSocket(this.requestedPort, 50, InetAddress.getByName("127.0.0.1"));
        this.running = true;
        Thread thread = new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.skin.OfflineYggdrasilServer$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                OfflineYggdrasilServer.this.acceptLoop();
            }
        }, "JavaLauncherOfflineYggdrasilServer");
        this.serverThread = thread;
        thread.setDaemon(true);
        this.serverThread.start();
    }

    public synchronized void stop() {
        this.running = false;
        try {
            ServerSocket serverSocket = this.serverSocket;
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Throwable unused) {
        }
        this.serverSocket = null;
    }

    public int getPort() {
        ServerSocket serverSocket = this.serverSocket;
        if (!this.running || serverSocket == null || serverSocket.isClosed()) {
            return -1;
        }
        return serverSocket.getLocalPort();
    }

    public void addCharacter(String str, String str2, File file, SkinModelType skinModelType) throws Exception {
        byte[] skinBytes = readSkinBytes(file);
        CharacterProfile characterProfile = new CharacterProfile(str.replace("-", "").toLowerCase(Locale.ROOT), str2, skinBytes != null ? sha256(skinBytes) : null, skinBytes, skinModelType);
        this.charactersByUuid.put(characterProfile.uuid.toLowerCase(Locale.ROOT), characterProfile);
        this.charactersByName.put(characterProfile.name.toLowerCase(Locale.ROOT), characterProfile);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void acceptLoop() {
        while (this.running) {
            try {
                final Socket socketAccept = this.serverSocket.accept();
                Thread thread = new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.skin.OfflineYggdrasilServer$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        OfflineYggdrasilServer.this.lambda$acceptLoop$0(socketAccept);
                    }
                }, "JavaLauncherOfflineYggdrasilRequest");
                thread.setDaemon(true);
                thread.start();
            } catch (Throwable th) {
                if (this.running) {
                    Logging.e(TAG, "Accept failed", th);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: handleSocket, reason: merged with bridge method [inline-methods] */
    public void lambda$acceptLoop$0(Socket socket) {
        try {
            try {
                InputStream inputStream = socket.getInputStream();
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    try {
                        HttpRequest request = readRequest(inputStream);
                        if (request == null) {
                            writeResponse(outputStream, 400, "text/plain; charset=utf-8", "Bad Request".getBytes(StandardCharsets.UTF_8));
                            if (outputStream != null) {
                                outputStream.close();
                            }
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            if (socket != null) {
                                socket.close();
                                return;
                            }
                            return;
                        }
                        route(request, outputStream);
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } finally {
                    }
                } finally {
                }
            } catch (Throwable th) {
                Logging.e(TAG, "Request failed", th);
            }
        } finally {
        }
    }

    private void route(HttpRequest httpRequest, OutputStream outputStream) throws Exception {
        String str = httpRequest.path;
        if ("/".equals(str)) {
            writeJson(outputStream, root());
            return;
        }
        if ("/status".equals(str)) {
            writeJson(outputStream, status());
            return;
        }
        if (ShareTarget.METHOD_POST.equals(httpRequest.method) && "/api/profiles/minecraft".equals(str)) {
            writeJson(outputStream, profiles(httpRequest.body));
            return;
        }
        if ("/sessionserver/session/minecraft/hasJoined".equals(str)) {
            writeJson(outputStream, hasJoined(httpRequest.query.get("username")));
            return;
        }
        if (ShareTarget.METHOD_POST.equals(httpRequest.method) && "/sessionserver/session/minecraft/join".equals(str)) {
            writeResponse(outputStream, 204, "text/plain; charset=utf-8", new byte[0]);
            return;
        }
        if (str.startsWith("/sessionserver/session/minecraft/profile/")) {
            writeJson(outputStream, profile(str.substring("/sessionserver/session/minecraft/profile/".length())));
        } else if (str.startsWith("/textures/")) {
            writeTexture(outputStream, str.substring("/textures/".length()));
        } else {
            writeResponse(outputStream, 404, "application/json; charset=utf-8", "{}".getBytes(StandardCharsets.UTF_8));
        }
    }

    private String root() throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("serverName", this.serverName);
        jSONObject.put("implementationName", this.implementationName);
        jSONObject.put("implementationVersion", this.implementationVersion);
        jSONObject.put("feature.non_email_login", true);
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("skinDomains", new JSONArray().put("127.0.0.1").put("localhost"));
        jSONObject2.put("meta", jSONObject);
        jSONObject2.put("signaturePublickey", toPemPublicKey(this.keyPair.getPublic()));
        return jSONObject2.toString();
    }

    private String status() throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("user.count", this.charactersByUuid.size());
        jSONObject.put("token.count", 0);
        return jSONObject.toString();
    }

    private String profiles(byte[] bArr) throws Exception {
        JSONArray jSONArray = new JSONArray(new String(bArr, StandardCharsets.UTF_8));
        JSONArray jSONArray2 = new JSONArray();
        for (int i = 0; i < jSONArray.length(); i++) {
            CharacterProfile characterProfile = this.charactersByName.get(jSONArray.optString(i, "").toLowerCase(Locale.ROOT));
            if (characterProfile != null) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("id", characterProfile.uuid);
                jSONObject.put("name", characterProfile.name);
                jSONArray2.put(jSONObject);
            }
        }
        return jSONArray2.toString();
    }

    private String hasJoined(String str) throws Exception {
        if (str == null || str.trim().isEmpty()) {
            return new JSONObject().toString();
        }
        CharacterProfile characterProfile = this.charactersByName.get(str.toLowerCase(Locale.ROOT));
        return characterProfile != null ? characterProfile.toCompleteResponse(rootUrl(), new OfflineYggdrasilServer$$ExternalSyntheticLambda0(this)) : new JSONObject().toString();
    }

    private String profile(String str) throws Exception {
        CharacterProfile characterProfile = this.charactersByUuid.get(str.replace("-", "").toLowerCase(Locale.ROOT));
        return characterProfile != null ? characterProfile.toCompleteResponse(rootUrl(), new OfflineYggdrasilServer$$ExternalSyntheticLambda0(this)) : new JSONObject().toString();
    }

    private void writeTexture(OutputStream outputStream, String str) throws Exception {
        for (CharacterProfile characterProfile : this.charactersByUuid.values()) {
            if (characterProfile.skinHash != null && characterProfile.skinHash.equalsIgnoreCase(str) && characterProfile.skinBytes != null) {
                writeResponse(outputStream, 200, "image/png", characterProfile.skinBytes, "Cache-Control: max-age=2592000, public\r\nEtag: \"" + str + "\"\r\n");
                return;
            }
        }
        writeResponse(outputStream, 404, "text/plain; charset=utf-8", "Not Found".getBytes(StandardCharsets.UTF_8));
    }

    private String rootUrl() {
        return "http://127.0.0.1:" + getPort();
    }

    private void writeJson(OutputStream outputStream, String str) throws IOException {
        writeResponse(outputStream, 200, "application/json; charset=utf-8", str.getBytes(StandardCharsets.UTF_8));
    }

    private void writeResponse(OutputStream outputStream, int i, String str, byte[] bArr) throws IOException {
        writeResponse(outputStream, i, str, bArr, "");
    }

    private void writeResponse(OutputStream outputStream, int i, String str, byte[] bArr, String str2) throws IOException {
        String str3 = "OK";
        if (i != 200) {
            if (i == 204) {
                str3 = "No Content";
            } else if (i == 400) {
                str3 = "Bad Request";
            } else if (i == 404) {
                str3 = "Not Found";
            }
        }
        outputStream.write(("HTTP/1.1 " + i + " " + str3 + "\r\nConnection: close\r\nContent-Type: " + str + "\r\n" + str2 + "Content-Length: " + bArr.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(bArr);
        outputStream.flush();
    }

    private HttpRequest readRequest(InputStream inputStream) throws IOException {
        String strSubstring;
        int i;
        String line = readLine(inputStream);
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String[] strArrSplit = line.split(" ");
        if (strArrSplit.length < 2) {
            return null;
        }
        int i2 = 0;
        String upperCase = strArrSplit[0].trim().toUpperCase(Locale.ROOT);
        String strTrim = strArrSplit[1].trim();
        int iIndexOf = strTrim.indexOf(63);
        if (iIndexOf < 0) {
            strSubstring = "";
        } else {
            String strSubstring2 = strTrim.substring(0, iIndexOf);
            strSubstring = strTrim.substring(iIndexOf + 1);
            strTrim = strSubstring2;
        }
        HashMap map = new HashMap();
        while (true) {
            String line2 = readLine(inputStream);
            if (line2 != null && !line2.isEmpty()) {
                int iIndexOf2 = line2.indexOf(58);
                if (iIndexOf2 > 0) {
                    map.put(line2.substring(0, iIndexOf2).trim().toLowerCase(Locale.ROOT), line2.substring(iIndexOf2 + 1).trim());
                }
            } else {
                try {
                    i = Integer.parseInt((String) map.getOrDefault("content-length", "0"));
                } catch (Throwable unused) {
                    i = 0;
                }
                int iMax = Math.max(0, i);
                byte[] bArr = new byte[iMax];
                while (i2 < iMax) {
                    int i3 = inputStream.read(bArr, i2, iMax - i2);
                    if (i3 < 0) {
                        break;
                    }
                    i2 += i3;
                }
                return new HttpRequest(upperCase, strTrim, parseQuery(strSubstring), bArr);
            }
        }
    }

    private String readLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        boolean z = false;
        while (true) {
            int i = inputStream.read();
            if (i == -1) {
                break;
            }
            if (i == 10) {
                z = true;
                break;
            }
            if (i != 13) {
                byteArrayOutputStream.write(i);
            }
            z = true;
        }
        if (z || byteArrayOutputStream.size() != 0) {
            return byteArrayOutputStream.toString("UTF-8");
        }
        return null;
    }

    private Map<String, String> parseQuery(String str) throws IOException {
        HashMap map = new HashMap();
        if (str.trim().isEmpty()) {
            return map;
        }
        for (String str2 : str.split("&")) {
            int iIndexOf = str2.indexOf(61);
            map.put(urlDecode(iIndexOf >= 0 ? str2.substring(0, iIndexOf) : str2), urlDecode(iIndexOf >= 0 ? str2.substring(iIndexOf + 1) : ""));
        }
        return map;
    }

    private String urlDecode(String str) throws IOException {
        return URLDecoder.decode(str, "UTF-8");
    }

    private byte[] readSkinBytes(File file) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream;
        if (file == null || !file.isFile()) {
            return null;
        }
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
            byte[] bArr = new byte[8192];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i == -1) {
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    byteArrayOutputStream.close();
                    fileInputStream.close();
                    return byteArray;
                }
                byteArrayOutputStream.write(bArr, 0, i);
                fileInputStream.close();
                throw th;
            }
        } finally {
        }
    }

    private String toPemPublicKey(PublicKey publicKey) {
        return "-----BEGIN PUBLIC KEY-----\n" + Base64.encodeToString(publicKey.getEncoded(), 2) + "\n-----END PUBLIC KEY-----";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String sign(String str) throws Exception {
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(this.keyPair.getPrivate());
        signature.update(str.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(signature.sign(), 2);
    }

    private String sha256(byte[] bArr) throws Exception {
        byte[] bArrDigest = MessageDigest.getInstance("SHA-256").digest(bArr);
        StringBuilder sb = new StringBuilder();
        for (byte b : bArrDigest) {
            sb.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 255)));
        }
        return sb.toString();
    }

    private static final class HttpRequest {
        final byte[] body;
        final String method;
        final String path;
        final Map<String, String> query;

        HttpRequest(String str, String str2, Map<String, String> map, byte[] bArr) {
            this.method = str;
            this.path = str2;
            this.query = map;
            this.body = bArr;
        }
    }

    private static final class CharacterProfile {
        final SkinModelType model;
        final String name;
        final byte[] skinBytes;
        final String skinHash;
        final String uuid;

        CharacterProfile(String str, String str2, String str3, byte[] bArr, SkinModelType skinModelType) {
            this.uuid = str;
            this.name = str2;
            this.skinHash = str3;
            this.skinBytes = bArr;
            this.model = skinModelType;
        }

        String toCompleteResponse(String str, Signer signer) throws Exception {
            JSONObject jSONObject = new JSONObject();
            String str2 = this.skinHash;
            if (str2 != null && str2.length() > 0) {
                JSONObject jSONObject2 = new JSONObject();
                jSONObject2.put("url", str + "/textures/" + this.skinHash);
                if (this.model == SkinModelType.SLIM) {
                    jSONObject2.put("metadata", new JSONObject().put("model", "slim"));
                }
                jSONObject.put("SKIN", jSONObject2);
            }
            JSONObject jSONObject3 = new JSONObject();
            jSONObject3.put("timestamp", System.currentTimeMillis());
            jSONObject3.put("profileId", this.uuid);
            jSONObject3.put("profileName", this.name);
            jSONObject3.put("textures", jSONObject);
            String strEncodeToString = Base64.encodeToString(jSONObject3.toString().getBytes(StandardCharsets.UTF_8), 2);
            JSONObject jSONObject4 = new JSONObject();
            jSONObject4.put("name", "textures");
            jSONObject4.put("value", strEncodeToString);
            jSONObject4.put("signature", signer.sign(strEncodeToString));
            JSONObject jSONObject5 = new JSONObject();
            jSONObject5.put("id", this.uuid);
            jSONObject5.put("name", this.name);
            jSONObject5.put("properties", new JSONArray().put(jSONObject4));
            return jSONObject5.toString();
        }
    }
}

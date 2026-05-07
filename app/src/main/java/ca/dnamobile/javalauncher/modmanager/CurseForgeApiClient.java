package ca.dnamobile.javalauncher.modmanager;

import android.content.Context;
import ca.dnamobile.javalauncher.BuildConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class CurseForgeApiClient {
    private static final String BASE_URL = "https://api.curseforge.com/v1";
    private static final int MINECRAFT_GAME_ID = 432;
    private static final String USER_AGENT = "JavaLauncher/1.0 (Android Minecraft Launcher)";
    private final String apiKey = CurseForgeApiKeyProvider.resolve();

    public static final class SearchResult {
        public final ArrayList<ModrinthProject> hits;
        public final int limit;
        public final int offset;
        public final int totalHits;

        SearchResult(ArrayList<ModrinthProject> arrayList, int i, int i2, int i3) {
            this.hits = arrayList;
            this.offset = i;
            this.limit = i2;
            this.totalHits = i3;
        }
    }

    public CurseForgeApiClient(Context context) {
    }

    public SearchResult searchProjects(String str, ModManagerContentType modManagerContentType, String str2, String str3, int i, int i2, String str4) throws Exception {
        ensureApiKey();
        StringBuilder sbAppend = new StringBuilder(BASE_URL).append("/mods/search?");
        appendQuery(sbAppend, "gameId", String.valueOf(MINECRAFT_GAME_ID));
        appendQuery(sbAppend, "classId", String.valueOf(getClassId(modManagerContentType)));
        appendQuery(sbAppend, "index", String.valueOf(Math.max(0, i2)));
        appendQuery(sbAppend, "pageSize", String.valueOf(Math.max(1, Math.min(50, i))));
        appendQuery(sbAppend, "sortField", String.valueOf(getSortField(str4)));
        appendQuery(sbAppend, "sortOrder", "desc");
        if (!isBlank(str)) {
            appendQuery(sbAppend, "searchFilter", str.trim());
        }
        if (!isBlank(str2)) {
            appendQuery(sbAppend, "gameVersion", str2.trim());
        }
        int modLoaderType = getModLoaderType(str3);
        if (modManagerContentType.isLoaderSpecific() && modLoaderType > 0) {
            appendQuery(sbAppend, "modLoaderType", String.valueOf(modLoaderType));
        }
        JSONObject jSONObject = new JSONObject(get(sbAppend.toString()));
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("data");
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("pagination");
        ArrayList arrayList = new ArrayList();
        if (jSONArrayOptJSONArray != null) {
            for (int i3 = 0; i3 < jSONArrayOptJSONArray.length(); i3++) {
                JSONObject jSONObjectOptJSONObject2 = jSONArrayOptJSONArray.optJSONObject(i3);
                if (jSONObjectOptJSONObject2 != null) {
                    arrayList.add(parseProject(jSONObjectOptJSONObject2));
                }
            }
        }
        int iOptInt = jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optInt("totalCount", arrayList.size()) : arrayList.size();
        if (jSONObjectOptJSONObject != null) {
            i2 = jSONObjectOptJSONObject.optInt("index", i2);
        }
        if (jSONObjectOptJSONObject != null) {
            i = jSONObjectOptJSONObject.optInt("pageSize", i);
        }
        return new SearchResult(arrayList, i2, i, iOptInt);
    }

    public ModrinthProject getProject(String str) throws Exception {
        ensureApiKey();
        JSONObject jSONObjectOptJSONObject = new JSONObject(get("https://api.curseforge.com/v1/mods/" + encodePath(str))).optJSONObject("data");
        if (jSONObjectOptJSONObject == null) {
            throw new IllegalStateException("CurseForge project response is empty.");
        }
        return parseProject(jSONObjectOptJSONObject);
    }

    public ArrayList<ModrinthVersion> getProjectVersions(String str, ModManagerContentType modManagerContentType, String str2, String str3) throws Exception {
        ensureApiKey();
        StringBuilder sbAppend = new StringBuilder(BASE_URL).append("/mods/").append(encodePath(str)).append("/files?");
        appendQuery(sbAppend, "pageSize", "50");
        appendQuery(sbAppend, "index", "0");
        if (!isBlank(str2)) {
            appendQuery(sbAppend, "gameVersion", str2.trim());
        }
        int modLoaderType = getModLoaderType(str3);
        if (modManagerContentType.isLoaderSpecific() && modLoaderType > 0) {
            appendQuery(sbAppend, "modLoaderType", String.valueOf(modLoaderType));
        }
        JSONArray jSONArrayOptJSONArray = new JSONObject(get(sbAppend.toString())).optJSONArray("data");
        ArrayList<ModrinthVersion> arrayList = new ArrayList<>();
        if (jSONArrayOptJSONArray != null) {
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    arrayList.add(parseVersion(str, jSONObjectOptJSONObject));
                }
            }
        }
        return arrayList;
    }

    public String getDownloadUrl(String str, String str2) throws Exception {
        ensureApiKey();
        String strOptString = new JSONObject(get("https://api.curseforge.com/v1/mods/" + encodePath(str) + "/files/" + encodePath(str2) + "/download-url")).optString("data", "");
        if (isBlank(strOptString)) {
            throw new IllegalStateException("CurseForge did not provide a download URL.");
        }
        return strOptString.trim();
    }

    public void downloadToFile(String str, File file) throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IllegalStateException("Unable to create folder: " + parentFile.getAbsolutePath());
        }
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(30000);
        httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("Download failed with HTTP " + responseCode + ": " + str);
        }
        try {
            InputStream inputStream = httpURLConnection.getInputStream();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    byte[] bArr = new byte[65536];
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
                } finally {
                }
            } finally {
            }
        } finally {
            httpURLConnection.disconnect();
        }
    }

    private ModrinthProject parseProject(JSONObject jSONObject) {
        JSONArray jSONArray;
        String strValueOf = String.valueOf(jSONObject.optLong("id", 0L));
        String strOptString = jSONObject.optString("slug", strValueOf);
        String strOptString2 = jSONObject.optString("name", strOptString);
        String strOptString3 = jSONObject.optString("summary", "");
        long jOptLong = jSONObject.optLong("downloadCount", 0L);
        String strOptString4 = jSONObject.optString("dateModified", null);
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("links");
        String strOptString5 = jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optString("websiteUrl", null) : null;
        JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("logo");
        String strOptString6 = jSONObjectOptJSONObject2 != null ? jSONObjectOptJSONObject2.optString("thumbnailUrl", jSONObjectOptJSONObject2.optString("url", null)) : null;
        ArrayList arrayList = new ArrayList();
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("authors");
        if (jSONArrayOptJSONArray != null) {
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject3 = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject3 != null) {
                    String strOptString7 = jSONObjectOptJSONObject3.optString("name", "");
                    if (!isBlank(strOptString7)) {
                        arrayList.add(strOptString7);
                    }
                }
            }
        }
        ArrayList arrayList2 = new ArrayList();
        JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("categories");
        if (jSONArrayOptJSONArray2 != null) {
            int i2 = 0;
            while (i2 < jSONArrayOptJSONArray2.length()) {
                JSONObject jSONObjectOptJSONObject4 = jSONArrayOptJSONArray2.optJSONObject(i2);
                if (jSONObjectOptJSONObject4 == null) {
                    jSONArray = jSONArrayOptJSONArray2;
                } else {
                    jSONArray = jSONArrayOptJSONArray2;
                    String strOptString8 = jSONObjectOptJSONObject4.optString("name", jSONObjectOptJSONObject4.optString("slug", ""));
                    if (!isBlank(strOptString8)) {
                        arrayList2.add(strOptString8);
                    }
                }
                i2++;
                jSONArrayOptJSONArray2 = jSONArray;
            }
        }
        ArrayList arrayList3 = new ArrayList();
        JSONArray jSONArrayOptJSONArray3 = jSONObject.optJSONArray("screenshots");
        if (jSONArrayOptJSONArray3 != null) {
            for (int i3 = 0; i3 < jSONArrayOptJSONArray3.length(); i3++) {
                JSONObject jSONObjectOptJSONObject5 = jSONArrayOptJSONArray3.optJSONObject(i3);
                if (jSONObjectOptJSONObject5 != null) {
                    String strOptString9 = jSONObjectOptJSONObject5.optString("url", jSONObjectOptJSONObject5.optString("thumbnailUrl", ""));
                    if (!isBlank(strOptString9)) {
                        arrayList3.add(strOptString9);
                    }
                }
            }
        }
        return new ModrinthProject(strValueOf, strOptString, strOptString2, join(arrayList, ", "), strOptString3, strOptString3, strOptString6, getProjectType(jSONObject.optInt("classId", 0)), jOptLong, 0L, strOptString4, arrayList2, arrayList3, ModManagerSource.CURSEFORGE, strOptString5);
    }

    private ModrinthVersion parseVersion(String str, JSONObject jSONObject) throws Exception {
        String str2;
        String strOptString;
        ArrayList arrayList;
        String strValueOf = String.valueOf(jSONObject.optLong("id", 0L));
        String strOptString2 = jSONObject.optString("displayName", jSONObject.optString("fileName", strValueOf));
        String strOptString3 = jSONObject.optString("fileName", strOptString2);
        String strOptString4 = jSONObject.optString("downloadUrl", "");
        if (isBlank(strOptString4)) {
            try {
                strOptString4 = getDownloadUrl(str, strValueOf);
            } catch (Throwable unused) {
                str2 = "";
            }
        }
        str2 = strOptString4;
        ArrayList<String> stringArray = readStringArray(jSONObject.optJSONArray("gameVersions"));
        ArrayList arrayList2 = new ArrayList();
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("sortableGameVersions");
        int i = 0;
        if (jSONArrayOptJSONArray != null) {
            for (int i2 = 0; i2 < jSONArrayOptJSONArray.length(); i2++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i2);
                if (jSONObjectOptJSONObject != null) {
                    String strModLoaderToString = modLoaderToString(jSONObjectOptJSONObject.optInt("modLoader", 0));
                    if (!isBlank(strModLoaderToString) && !arrayList2.contains(strModLoaderToString)) {
                        arrayList2.add(strModLoaderToString);
                    }
                }
            }
        }
        ArrayList arrayList3 = new ArrayList();
        JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("dependencies");
        if (jSONArrayOptJSONArray2 != null) {
            int i3 = 0;
            while (i3 < jSONArrayOptJSONArray2.length()) {
                JSONObject jSONObjectOptJSONObject2 = jSONArrayOptJSONArray2.optJSONObject(i3);
                if (jSONObjectOptJSONObject2 == null) {
                    arrayList = arrayList2;
                } else {
                    arrayList = arrayList2;
                    arrayList3.add(new ModrinthDependency(null, String.valueOf(jSONObjectOptJSONObject2.optLong("modId", 0L)), null, jSONObjectOptJSONObject2.optInt("relationType", i) == 3 ? "required" : "optional"));
                }
                i3++;
                arrayList2 = arrayList;
                i = 0;
            }
        }
        ArrayList arrayList4 = arrayList2;
        JSONArray jSONArrayOptJSONArray3 = jSONObject.optJSONArray("hashes");
        if (jSONArrayOptJSONArray3 != null) {
            for (int i4 = 0; i4 < jSONArrayOptJSONArray3.length(); i4++) {
                JSONObject jSONObjectOptJSONObject3 = jSONArrayOptJSONArray3.optJSONObject(i4);
                if (jSONObjectOptJSONObject3 != null && jSONObjectOptJSONObject3.optInt("algo", 0) == 1) {
                    strOptString = jSONObjectOptJSONObject3.optString("value", null);
                    break;
                }
            }
            strOptString = null;
        } else {
            strOptString = null;
        }
        ArrayList arrayList5 = new ArrayList();
        arrayList5.add(new ModrinthFile(str2, strOptString3, strOptString, true, jSONObject.optLong("fileLength", jSONObject.optLong("fileSizeOnDisk", 0L))));
        return new ModrinthVersion(strValueOf, str, strOptString2, strOptString2, releaseTypeToString(jSONObject.optInt("releaseType", 1)), jSONObject.optString("fileDate", null), null, jSONObject.optLong("downloadCount", 0L), stringArray, arrayList4, arrayList3, arrayList5);
    }

    private String get(String str) throws Exception {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        String text = readText((responseCode < 200 || responseCode >= 300) ? httpURLConnectionOpenConnection.getErrorStream() : httpURLConnectionOpenConnection.getInputStream());
        httpURLConnectionOpenConnection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("CurseForge API HTTP " + responseCode + ": " + text);
        }
        return text;
    }

    private HttpURLConnection openConnection(String str) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(30000);
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestProperty("x-api-key", this.apiKey);
        httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
        return httpURLConnection;
    }

    private void ensureApiKey() {
        if (isBlank(this.apiKey)) {
            throw new IllegalStateException("Missing CurseForge API key. Add res/values/curseforge_api_key.xml or BuildConfig.CURSEFORGE_API_KEY.");
        }
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.modmanager.CurseForgeApiClient$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType;

        static {
            int[] iArr = new int[ModManagerContentType.values().length];
            $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType = iArr;
            try {
                iArr[ModManagerContentType.RESOURCEPACKS.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[ModManagerContentType.SHADERPACKS.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[ModManagerContentType.MODS.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
        }
    }

    private static int getClassId(ModManagerContentType modManagerContentType) {
        int i = AnonymousClass1.$SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[modManagerContentType.ordinal()];
        if (i != 1) {
            return i != 2 ? 6 : 6552;
        }
        return 12;
    }

    private static String getProjectType(int i) {
        if (i == 12) {
            return "resourcepack";
        }
        if (i == 6552) {
            return "shader";
        }
        return "mod";
    }

    private static int getSortField(String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        if (lowerCase.contains("updated") || lowerCase.contains("date")) {
            return 3;
        }
        if (lowerCase.contains("name")) {
            return 4;
        }
        return lowerCase.contains("download") ? 6 : 2;
    }

    public static int getModLoaderType(String str) {
        if (str == null) {
            return 0;
        }
        String lowerCase = str.toLowerCase(Locale.US);
        if (lowerCase.contains("neoforge") || lowerCase.contains("neo forge")) {
            return 6;
        }
        if (lowerCase.contains("quilt")) {
            return 5;
        }
        if (lowerCase.contains("fabric")) {
            return 4;
        }
        return lowerCase.contains("forge") ? 1 : 0;
    }

    private static String modLoaderToString(int i) {
        if (i == 1) {
            return "forge";
        }
        if (i == 4) {
            return "fabric";
        }
        if (i == 5) {
            return "quilt";
        }
        if (i == 6) {
            return "neoforge";
        }
        return "";
    }

    private static String releaseTypeToString(int i) {
        if (i == 2) {
            return "beta";
        }
        if (i == 3) {
            return "alpha";
        }
        return BuildConfig.BUILD_TYPE;
    }

    private static ArrayList<String> readStringArray(JSONArray jSONArray) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (jSONArray == null) {
            return arrayList;
        }
        for (int i = 0; i < jSONArray.length(); i++) {
            String strOptString = jSONArray.optString(i, "");
            if (!isBlank(strOptString)) {
                arrayList.add(strOptString);
            }
        }
        return arrayList;
    }

    private static String join(ArrayList<String> arrayList, String str) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : arrayList) {
            if (!isBlank(str2)) {
                if (sb.length() > 0) {
                    sb.append(str);
                }
                sb.append(str2);
            }
        }
        return sb.toString();
    }

    private static void appendQuery(StringBuilder sb, String str, String str2) throws Exception {
        if (sb.charAt(sb.length() - 1) != '?' && sb.charAt(sb.length() - 1) != '&') {
            sb.append('&');
        }
        sb.append(URLEncoder.encode(str, "UTF-8"));
        sb.append('=');
        sb.append(URLEncoder.encode(str2, "UTF-8"));
    }

    private static String encodePath(String str) throws Exception {
        return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
    }

    /* JADX WARN: Removed duplicated region for block: B:34:0x003a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static java.lang.String readText(java.io.InputStream r4) throws java.lang.Exception {
        /*
            if (r4 != 0) goto L5
            java.lang.String r4 = ""
            return r4
        L5:
            java.io.ByteArrayOutputStream r0 = new java.io.ByteArrayOutputStream     // Catch: java.lang.Throwable -> L37
            r0.<init>()     // Catch: java.lang.Throwable -> L37
            r1 = 16384(0x4000, float:2.2959E-41)
            byte[] r1 = new byte[r1]     // Catch: java.lang.Throwable -> L2d
        Le:
            int r2 = r4.read(r1)     // Catch: java.lang.Throwable -> L2d
            r3 = -1
            if (r2 == r3) goto L1a
            r3 = 0
            r0.write(r1, r3, r2)     // Catch: java.lang.Throwable -> L2d
            goto Le
        L1a:
            java.nio.charset.Charset r1 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> L2d
            java.lang.String r1 = r1.name()     // Catch: java.lang.Throwable -> L2d
            java.lang.String r1 = r0.toString(r1)     // Catch: java.lang.Throwable -> L2d
            r0.close()     // Catch: java.lang.Throwable -> L37
            if (r4 == 0) goto L2c
            r4.close()
        L2c:
            return r1
        L2d:
            r1 = move-exception
            r0.close()     // Catch: java.lang.Throwable -> L32
            goto L36
        L32:
            r0 = move-exception
            r1.addSuppressed(r0)     // Catch: java.lang.Throwable -> L37
        L36:
            throw r1     // Catch: java.lang.Throwable -> L37
        L37:
            r0 = move-exception
            if (r4 == 0) goto L42
            r4.close()     // Catch: java.lang.Throwable -> L3e
            goto L42
        L3e:
            r4 = move-exception
            r0.addSuppressed(r4)
        L42:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.modmanager.CurseForgeApiClient.readText(java.io.InputStream):java.lang.String");
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty() || "null".equalsIgnoreCase(str.trim());
    }
}

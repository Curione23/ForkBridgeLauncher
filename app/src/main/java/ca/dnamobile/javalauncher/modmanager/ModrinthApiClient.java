package ca.dnamobile.javalauncher.modmanager;

import androidx.constraintlayout.core.motion.utils.TypedValues;
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
public final class ModrinthApiClient {
    private static final String BASE_URL = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "JavaLauncher/1.0 (Android Minecraft Launcher)";

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

    public SearchResult searchProjects(String str, ModManagerContentType modManagerContentType, String str2, String str3, int i, int i2, String str4) throws Exception {
        StringBuilder sbAppend = new StringBuilder(BASE_URL).append("/search?");
        appendQuery(sbAppend, "query", str);
        appendQuery(sbAppend, "limit", String.valueOf(Math.max(1, Math.min(100, i))));
        appendQuery(sbAppend, "offset", String.valueOf(Math.max(0, i2)));
        appendQuery(sbAppend, "index", str4);
        appendQuery(sbAppend, "facets", buildSearchFacets(modManagerContentType, str2, str3).toString());
        JSONObject jSONObject = new JSONObject(get(sbAppend.toString()));
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("hits");
        ArrayList arrayList = new ArrayList();
        if (jSONArrayOptJSONArray != null) {
            for (int i3 = 0; i3 < jSONArrayOptJSONArray.length(); i3++) {
                arrayList.add(parseProject(jSONArrayOptJSONArray.getJSONObject(i3), false));
            }
        }
        return new SearchResult(arrayList, jSONObject.optInt("offset", i2), jSONObject.optInt("limit", i), jSONObject.optInt("total_hits", arrayList.size()));
    }

    public ModrinthProject getProject(String str) throws Exception {
        return parseProject(new JSONObject(get("https://api.modrinth.com/v2/project/" + encodePath(str))), true);
    }

    public ModrinthProject getProjectWithFallback(String str, String str2) throws Exception {
        try {
            return getProject(str);
        } catch (Throwable th) {
            if (isBlank(str2) || str.equals(str2.trim())) {
                throw th;
            }
            return getProject(str2.trim());
        }
    }

    public ArrayList<ModrinthVersion> getProjectVersions(String str, ModManagerContentType modManagerContentType, String str2, String str3, boolean z) throws Exception {
        StringBuilder sbAppend = new StringBuilder(BASE_URL).append("/project/").append(encodePath(str)).append("/version?");
        appendQuery(sbAppend, "include_changelog", z ? "true" : "false");
        JSONArray jSONArray = new JSONArray();
        if (!isBlank(str2)) {
            jSONArray.put(str2.trim());
        }
        if (jSONArray.length() > 0) {
            appendQuery(sbAppend, "game_versions", jSONArray.toString());
        }
        JSONArray jSONArrayBuildVersionLoaders = buildVersionLoaders(modManagerContentType, str3);
        if (jSONArrayBuildVersionLoaders.length() > 0) {
            appendQuery(sbAppend, "loaders", jSONArrayBuildVersionLoaders.toString());
        }
        JSONArray jSONArray2 = new JSONArray(get(sbAppend.toString()));
        ArrayList<ModrinthVersion> arrayList = new ArrayList<>();
        for (int i = 0; i < jSONArray2.length(); i++) {
            arrayList.add(parseVersion(jSONArray2.getJSONObject(i)));
        }
        return arrayList;
    }

    public ArrayList<ModrinthVersion> getProjectVersionsWithFallback(ModrinthProject modrinthProject, ModManagerContentType modManagerContentType, String str, String str2, boolean z) throws Exception {
        try {
            return getProjectVersions(modrinthProject.projectId, modManagerContentType, str, str2, z);
        } catch (Throwable th) {
            if (isBlank(modrinthProject.slug) || modrinthProject.projectId.equals(modrinthProject.slug.trim())) {
                throw th;
            }
            return getProjectVersions(modrinthProject.slug.trim(), modManagerContentType, str, str2, z);
        }
    }

    public ModrinthVersion getVersion(String str) throws Exception {
        return parseVersion(new JSONObject(get("https://api.modrinth.com/v2/version/" + encodePath(str))));
    }

    public void downloadToFile(String str, File file) throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IllegalStateException("Unable to create folder: " + parentFile.getAbsolutePath());
        }
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("Download failed with HTTP " + responseCode + ": " + str);
        }
        try {
            InputStream inputStream = httpURLConnectionOpenConnection.getInputStream();
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
            httpURLConnectionOpenConnection.disconnect();
        }
    }

    private String get(String str) throws Exception {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        String text = readText((responseCode < 200 || responseCode >= 300) ? httpURLConnectionOpenConnection.getErrorStream() : httpURLConnectionOpenConnection.getInputStream());
        httpURLConnectionOpenConnection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("Modrinth API HTTP " + responseCode + ": " + text);
        }
        return text;
    }

    private HttpURLConnection openConnection(String str) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(30000);
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
        return httpURLConnection;
    }

    private JSONArray buildSearchFacets(ModManagerContentType modManagerContentType, String str, String str2) {
        JSONArray jSONArray = new JSONArray();
        jSONArray.put(new JSONArray().put("project_type:" + modManagerContentType.getModrinthProjectType()));
        if (!isBlank(str)) {
            jSONArray.put(new JSONArray().put("versions:" + str.trim()));
        }
        String strNormalizeLoader = normalizeLoader(str2);
        if (modManagerContentType.isLoaderSpecific() && !isBlank(strNormalizeLoader) && !"vanilla".equals(strNormalizeLoader)) {
            jSONArray.put(new JSONArray().put("categories:" + strNormalizeLoader));
        }
        return jSONArray;
    }

    private JSONArray buildVersionLoaders(ModManagerContentType modManagerContentType, String str) {
        JSONArray jSONArray = new JSONArray();
        String strNormalizeLoader = normalizeLoader(str);
        if (modManagerContentType.isLoaderSpecific() && !isBlank(strNormalizeLoader) && !"vanilla".equals(strNormalizeLoader)) {
            jSONArray.put(strNormalizeLoader);
        }
        return jSONArray;
    }

    public static String normalizeLoader(String str) {
        if (str == null) {
            return "";
        }
        String lowerCase = str.trim().toLowerCase(Locale.US);
        if (lowerCase.contains("neoforge") || lowerCase.contains("neo forge")) {
            return "neoforge";
        }
        if (lowerCase.contains("forge")) {
            return "forge";
        }
        if (lowerCase.contains("fabric")) {
            return "fabric";
        }
        if (lowerCase.contains("quilt")) {
            return "quilt";
        }
        if (lowerCase.contains("vanilla")) {
            return "vanilla";
        }
        return lowerCase.replace(' ', '-');
    }

    private static ModrinthProject parseProject(JSONObject jSONObject, boolean z) {
        JSONArray jSONArray;
        String strOptString = jSONObject.optString("project_id", jSONObject.optString("id", ""));
        String strOptString2 = jSONObject.optString("slug", strOptString);
        String strOptString3 = jSONObject.optString("title", strOptString2);
        String strOptString4 = jSONObject.optString("author", jSONObject.optString("team", ""));
        String strOptString5 = jSONObject.optString("description", jSONObject.optString("summary", ""));
        String strOptString6 = jSONObject.optString("body", null);
        String strOptString7 = jSONObject.optString("icon_url", null);
        String strOptString8 = jSONObject.optString("project_type", "mod");
        long jOptLong = jSONObject.optLong("downloads", 0L);
        long jOptLong2 = jSONObject.optLong("follows", jSONObject.optLong("followers", 0L));
        String strOptString9 = jSONObject.optString("date_modified", jSONObject.optString("updated", null));
        ArrayList<String> stringArray = readStringArray(jSONObject.optJSONArray("categories"));
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("additional_categories");
        if (jSONArrayOptJSONArray != null) {
            stringArray.addAll(readStringArray(jSONArrayOptJSONArray));
        }
        ArrayList arrayList = new ArrayList();
        JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("gallery");
        if (jSONArrayOptJSONArray2 != null) {
            int i = 0;
            while (i < jSONArrayOptJSONArray2.length()) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray2.optJSONObject(i);
                if (jSONObjectOptJSONObject == null) {
                    jSONArray = jSONArrayOptJSONArray2;
                } else {
                    jSONArray = jSONArrayOptJSONArray2;
                    String strOptString10 = jSONObjectOptJSONObject.optString("url", "");
                    if (!strOptString10.trim().isEmpty()) {
                        arrayList.add(strOptString10);
                    }
                }
                i++;
                jSONArrayOptJSONArray2 = jSONArray;
            }
        }
        if (!z && stringArray.isEmpty()) {
            stringArray.add(strOptString8);
        }
        return new ModrinthProject(strOptString, strOptString2, strOptString3, strOptString4, strOptString5, strOptString6, strOptString7, strOptString8, jOptLong, jOptLong2, strOptString9, stringArray, arrayList);
    }

    private static ModrinthVersion parseVersion(JSONObject jSONObject) {
        ArrayList arrayList = new ArrayList();
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("dependencies");
        boolean z = false;
        String str = null;
        if (jSONArrayOptJSONArray != null) {
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    arrayList.add(new ModrinthDependency(jSONObjectOptJSONObject.optString("version_id", null), jSONObjectOptJSONObject.optString("project_id", null), jSONObjectOptJSONObject.optString("file_name", null), jSONObjectOptJSONObject.optString("dependency_type", "required")));
                }
            }
        }
        ArrayList arrayList2 = new ArrayList();
        JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("files");
        if (jSONArrayOptJSONArray2 != null) {
            int i2 = 0;
            while (i2 < jSONArrayOptJSONArray2.length()) {
                JSONObject jSONObjectOptJSONObject2 = jSONArrayOptJSONArray2.optJSONObject(i2);
                if (jSONObjectOptJSONObject2 != null) {
                    JSONObject jSONObjectOptJSONObject3 = jSONObjectOptJSONObject2.optJSONObject("hashes");
                    arrayList2.add(new ModrinthFile(jSONObjectOptJSONObject2.optString("url", ""), jSONObjectOptJSONObject2.optString("filename", "download.jar"), jSONObjectOptJSONObject3 != null ? jSONObjectOptJSONObject3.optString("sha1", str) : str, jSONObjectOptJSONObject2.optBoolean("primary", z), jSONObjectOptJSONObject2.optLong("size", 0L)));
                }
                i2++;
                z = false;
                str = null;
            }
        }
        return new ModrinthVersion(jSONObject.optString("id", ""), jSONObject.optString("project_id", ""), jSONObject.optString("name", jSONObject.optString("version_number", "")), jSONObject.optString("version_number", ""), jSONObject.optString("version_type", BuildConfig.BUILD_TYPE), jSONObject.optString("date_published", null), jSONObject.optString("changelog", null), jSONObject.optLong("downloads", 0L), readStringArray(jSONObject.optJSONArray("game_versions")), readStringArray(jSONObject.optJSONArray("loaders")), arrayList, arrayList2);
    }

    private static ArrayList<String> readStringArray(JSONArray jSONArray) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (jSONArray == null) {
            return arrayList;
        }
        for (int i = 0; i < jSONArray.length(); i++) {
            String strOptString = jSONArray.optString(i, "");
            if (!strOptString.trim().isEmpty()) {
                arrayList.add(strOptString);
            }
        }
        return arrayList;
    }

    /* JADX WARN: Removed duplicated region for block: B:34:0x003b A[EXC_TOP_SPLITTER, SYNTHETIC] */
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
            java.io.ByteArrayOutputStream r0 = new java.io.ByteArrayOutputStream     // Catch: java.lang.Throwable -> L38
            r0.<init>()     // Catch: java.lang.Throwable -> L38
            r1 = 32768(0x8000, float:4.5918E-41)
            byte[] r1 = new byte[r1]     // Catch: java.lang.Throwable -> L2e
        Lf:
            int r2 = r4.read(r1)     // Catch: java.lang.Throwable -> L2e
            r3 = -1
            if (r2 == r3) goto L1b
            r3 = 0
            r0.write(r1, r3, r2)     // Catch: java.lang.Throwable -> L2e
            goto Lf
        L1b:
            java.nio.charset.Charset r1 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> L2e
            java.lang.String r1 = r1.name()     // Catch: java.lang.Throwable -> L2e
            java.lang.String r1 = r0.toString(r1)     // Catch: java.lang.Throwable -> L2e
            r0.close()     // Catch: java.lang.Throwable -> L38
            if (r4 == 0) goto L2d
            r4.close()
        L2d:
            return r1
        L2e:
            r1 = move-exception
            r0.close()     // Catch: java.lang.Throwable -> L33
            goto L37
        L33:
            r0 = move-exception
            r1.addSuppressed(r0)     // Catch: java.lang.Throwable -> L38
        L37:
            throw r1     // Catch: java.lang.Throwable -> L38
        L38:
            r0 = move-exception
            if (r4 == 0) goto L43
            r4.close()     // Catch: java.lang.Throwable -> L3f
            goto L43
        L3f:
            r4 = move-exception
            r0.addSuppressed(r4)
        L43:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.modmanager.ModrinthApiClient.readText(java.io.InputStream):java.lang.String");
    }

    private static void appendQuery(StringBuilder sb, String str, String str2) throws Exception {
        if (sb.charAt(sb.length() - 1) != '?' && sb.charAt(sb.length() - 1) != '&') {
            sb.append('&');
        }
        sb.append(URLEncoder.encode(str, "UTF-8")).append('=').append(URLEncoder.encode(str2, "UTF-8"));
        sb.append('&');
    }

    private static String encodePath(String str) throws Exception {
        return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}

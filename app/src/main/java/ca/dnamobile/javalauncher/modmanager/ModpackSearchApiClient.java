package ca.dnamobile.javalauncher.modmanager;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModpackSearchApiClient {
    private static final int CURSEFORGE_MINECRAFT_GAME_ID = 432;
    private static final int CURSEFORGE_MODPACK_CLASS_ID = 4471;

    private ModpackSearchApiClient() {
    }

    public static final class SearchResult {
        public final ArrayList<ModrinthProject> hits;
        public final int totalHits;

        public SearchResult(ArrayList<ModrinthProject> arrayList, int i) {
            this.hits = arrayList;
            this.totalHits = i;
        }
    }

    public static SearchResult search(Context context, ModManagerSource modManagerSource, String str, String str2, String str3, int i, int i2) throws Exception {
        if (modManagerSource == ModManagerSource.CURSEFORGE) {
            return searchCurseForge(context, str, str2, str3, i, i2);
        }
        return searchModrinth(str, str2, str3, i, i2);
    }

    private static SearchResult searchModrinth(String str, String str2, String str3, int i, int i2) throws Exception {
        String str4;
        JSONArray jSONArray = new JSONArray();
        jSONArray.put(new JSONArray().put("project_type:modpack"));
        if (!isBlank(str2)) {
            jSONArray.put(new JSONArray().put("versions:" + str2.trim()));
        }
        String strNormalizeLoader = normalizeLoader(str3);
        if (!isBlank(strNormalizeLoader)) {
            jSONArray.put(new JSONArray().put("categories:" + strNormalizeLoader));
        }
        String str5 = "https://api.modrinth.com/v2/search?limit=" + i + "&offset=" + i2 + "&index=" + (str.trim().isEmpty() ? "downloads" : "relevance") + "&facets=" + urlEncode(jSONArray.toString());
        if (!str.trim().isEmpty()) {
            str5 = str5 + "&query=" + urlEncode(str.trim());
        }
        String str6 = null;
        JSONObject jSONObject = new JSONObject(httpGetString(str5, null));
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("hits");
        ArrayList arrayList = new ArrayList();
        int i3 = 0;
        while (jSONArrayOptJSONArray != null && i3 < jSONArrayOptJSONArray.length()) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i3);
            if (jSONObjectOptJSONObject == null) {
                str4 = str6;
            } else {
                ArrayList<String> arrayListStringArray = stringArray(jSONObjectOptJSONObject.optJSONArray("categories"));
                arrayListStringArray.add("modpack");
                str4 = null;
                arrayList.add(new ModrinthProject(jSONObjectOptJSONObject.optString("project_id", ""), jSONObjectOptJSONObject.optString("slug", ""), jSONObjectOptJSONObject.optString("title", "Modpack"), jSONObjectOptJSONObject.optString("description", ""), jSONObjectOptJSONObject.optString("author", str6), jSONObjectOptJSONObject.optString("icon_url", str6), arrayListStringArray, jSONObjectOptJSONObject.optLong("downloads", 0L), jSONObjectOptJSONObject.optLong("follows", 0L), jSONObjectOptJSONObject.optString("date_modified", null), ModManagerSource.MODRINTH));
            }
            i3++;
            str6 = str4;
        }
        return new SearchResult(arrayList, jSONObject.optInt("total_hits", arrayList.size()));
    }

    private static SearchResult searchCurseForge(Context context, String str, String str2, String str3, int i, int i2) throws Exception {
        String strResolve = CurseForgeApiKeyProvider.resolve();
        if (isBlank(strResolve)) {
            throw new IOException("Missing CurseForge API key.");
        }
        StringBuilder sbAppend = new StringBuilder("https://api.curseforge.com/v1/mods/search?gameId=432&classId=4471&pageSize=").append(i).append("&index=").append(i2).append("&sortField=6&sortOrder=desc");
        if (!str.trim().isEmpty()) {
            sbAppend.append("&searchFilter=").append(urlEncode(str.trim()));
        }
        if (!isBlank(str2)) {
            sbAppend.append("&gameVersion=").append(urlEncode(str2.trim()));
        }
        int iCurseForgeLoaderType = curseForgeLoaderType(str3);
        if (iCurseForgeLoaderType > 0) {
            sbAppend.append("&modLoaderType=").append(iCurseForgeLoaderType);
        }
        JSONObject jSONObject = new JSONObject(httpGetString(sbAppend.toString(), strResolve));
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("data");
        ArrayList arrayList = new ArrayList();
        int i3 = 0;
        int i4 = 0;
        while (jSONArrayOptJSONArray != null && i4 < jSONArrayOptJSONArray.length()) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i4);
            if (jSONObjectOptJSONObject != null) {
                JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("logo");
                JSONObject jSONObjectOptJSONObject3 = jSONObjectOptJSONObject.optJSONObject("links");
                ArrayList<String> arrayListCurseForgeCategories = curseForgeCategories(jSONObjectOptJSONObject.optJSONArray("categories"));
                arrayListCurseForgeCategories.add("modpack");
                arrayList.add(new ModrinthProject(String.valueOf(jSONObjectOptJSONObject.optInt("id", i3)), jSONObjectOptJSONObject3 == null ? jSONObjectOptJSONObject.optString("slug", "") : jSONObjectOptJSONObject3.optString("websiteUrl", jSONObjectOptJSONObject.optString("slug", "")), jSONObjectOptJSONObject.optString("name", "Modpack"), jSONObjectOptJSONObject.optString("summary", ""), readFirstAuthor(jSONObjectOptJSONObject.optJSONArray("authors")), jSONObjectOptJSONObject2 == null ? null : jSONObjectOptJSONObject2.optString("thumbnailUrl", jSONObjectOptJSONObject2.optString("url", null)), arrayListCurseForgeCategories, jSONObjectOptJSONObject.optLong("downloadCount", 0L), 0L, jSONObjectOptJSONObject.optString("dateModified", null), ModManagerSource.CURSEFORGE));
            }
            i4++;
            i3 = 0;
        }
        JSONObject jSONObjectOptJSONObject4 = jSONObject.optJSONObject("pagination");
        return new SearchResult(arrayList, jSONObjectOptJSONObject4 == null ? arrayList.size() : jSONObjectOptJSONObject4.optInt("totalCount", arrayList.size()));
    }

    private static ArrayList<String> curseForgeCategories(JSONArray jSONArray) {
        ArrayList<String> arrayList = new ArrayList<>();
        for (int i = 0; jSONArray != null && i < jSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                String strTrim = jSONObjectOptJSONObject.optString("slug", "").trim();
                String strTrim2 = jSONObjectOptJSONObject.optString("name", "").trim();
                if (!strTrim.isEmpty()) {
                    arrayList.add(strTrim);
                } else if (!strTrim2.isEmpty()) {
                    arrayList.add(strTrim2.toLowerCase(Locale.US).replace(' ', '-'));
                }
            }
        }
        return arrayList;
    }

    private static String readFirstAuthor(JSONArray jSONArray) {
        JSONObject jSONObjectOptJSONObject;
        if (jSONArray == null || jSONArray.length() == 0 || (jSONObjectOptJSONObject = jSONArray.optJSONObject(0)) == null) {
            return null;
        }
        return jSONObjectOptJSONObject.optString("name", null);
    }

    private static ArrayList<String> stringArray(JSONArray jSONArray) {
        ArrayList<String> arrayList = new ArrayList<>();
        for (int i = 0; jSONArray != null && i < jSONArray.length(); i++) {
            String strTrim = jSONArray.optString(i, "").trim();
            if (!strTrim.isEmpty()) {
                arrayList.add(strTrim);
            }
        }
        return arrayList;
    }

    private static String httpGetString(String str, String str2) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(30000);
        httpURLConnection.setReadTimeout(60000);
        httpURLConnection.setRequestProperty("User-Agent", "JavaLauncher/ModpackSearch");
        if (!isBlank(str2)) {
            httpURLConnection.setRequestProperty("x-api-key", str2.trim());
        }
        int responseCode = httpURLConnection.getResponseCode();
        int i = responseCode / 100;
        InputStream inputStream = i == 2 ? httpURLConnection.getInputStream() : httpURLConnection.getErrorStream();
        String toString = inputStream == null ? "" : readToString(inputStream);
        httpURLConnection.disconnect();
        if (i == 2) {
            return toString;
        }
        throw new IOException("HTTP " + responseCode + ": " + toString);
    }

    private static String readToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[32768];
        while (true) {
            int i = inputStream.read(bArr);
            if (i == -1) {
                return byteArrayOutputStream.toString("UTF-8");
            }
            byteArrayOutputStream.write(bArr, 0, i);
        }
    }

    private static String normalizeLoader(String str) {
        String lowerCase = str == null ? "" : str.trim().toLowerCase(Locale.US);
        if (lowerCase.equals("vanilla") || lowerCase.equals("minecraft")) {
            return "";
        }
        return lowerCase.equals("fabric-loader") ? "fabric" : lowerCase.equals("neoforge") ? "neoforge" : lowerCase;
    }

    private static int curseForgeLoaderType(String str) {
        String strNormalizeLoader = normalizeLoader(str);
        if ("forge".equals(strNormalizeLoader)) {
            return 1;
        }
        if ("fabric".equals(strNormalizeLoader)) {
            return 4;
        }
        if ("quilt".equals(strNormalizeLoader)) {
            return 5;
        }
        return "neoforge".equals(strNormalizeLoader) ? 6 : 0;
    }

    private static String urlEncode(String str) throws Exception {
        return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}

package ca.dnamobile.javalauncher.ui.version;

import androidx.browser.trusted.sharing.ShareTarget;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class LoaderVersionResolver {
    private static final int BUFFER_SIZE = 65536;
    private static final String FABRIC_META = "https://meta.fabricmc.net/v2";
    private static final String FORGE_METADATA_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml";
    private static final String NEOFORGE_LEGACY_METADATA_URL = "https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml";
    private static final String NEOFORGE_METADATA_URL = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";

    private LoaderVersionResolver() {
    }

    public static final class LoaderVersionOption {
        public final String displayName;
        public final String loaderVersion;

        public LoaderVersionOption(String str, String str2) {
            this.displayName = str;
            this.loaderVersion = str2;
        }
    }

    public static ArrayList<LoaderVersionOption> resolveVersions(String str, String str2) throws Exception {
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        if ("fabric".equals(lowerCase)) {
            return resolveFabricVersions(str2);
        }
        if ("forge".equals(lowerCase)) {
            return resolveForgeVersions(str2);
        }
        if ("neoforge".equals(lowerCase)) {
            return resolveNeoForgeVersions(str2);
        }
        ArrayList<LoaderVersionOption> arrayList = new ArrayList<>();
        arrayList.add(new LoaderVersionOption("Vanilla", ""));
        return arrayList;
    }

    private static ArrayList<LoaderVersionOption> resolveFabricVersions(String str) throws Exception {
        JSONArray jSONArray = new JSONArray(downloadText("https://meta.fabricmc.net/v2/versions/loader/" + encode(str)));
        ArrayList<LoaderVersionOption> arrayList = new ArrayList<>();
        ArrayList arrayList2 = new ArrayList();
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("loader");
                if (jSONObjectOptJSONObject2 != null) {
                    jSONObjectOptJSONObject = jSONObjectOptJSONObject2;
                }
                String strOptString = jSONObjectOptJSONObject.optString("version", "");
                if (!strOptString.trim().isEmpty()) {
                    boolean zOptBoolean = jSONObjectOptJSONObject.optBoolean("stable", false);
                    LoaderVersionOption loaderVersionOption = new LoaderVersionOption(zOptBoolean ? strOptString + "  • stable" : strOptString, strOptString);
                    if (zOptBoolean) {
                        arrayList.add(loaderVersionOption);
                    } else {
                        arrayList2.add(loaderVersionOption);
                    }
                }
            }
        }
        arrayList.addAll(arrayList2);
        return arrayList;
    }

    private static ArrayList<LoaderVersionOption> resolveForgeVersions(String str) throws Exception {
        ArrayList<String> mavenVersions = parseMavenVersions(downloadText(FORGE_METADATA_URL));
        final String str2 = str + "-";
        ArrayList arrayList = new ArrayList();
        for (String str3 : mavenVersions) {
            if (str3.startsWith(str2)) {
                arrayList.add(str3);
            }
        }
        arrayList.sort(new Comparator() { // from class: ca.dnamobile.javalauncher.ui.version.LoaderVersionResolver$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                String str4 = str2;
                return LoaderVersionResolver.compareBuildVersions(((String) obj2).substring(str4.length()), ((String) obj).substring(str4.length()));
            }
        });
        ArrayList<LoaderVersionOption> arrayList2 = new ArrayList<>();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            String strSubstring = ((String) it.next()).substring(str2.length());
            arrayList2.add(new LoaderVersionOption(strSubstring, strSubstring));
        }
        return arrayList2;
    }

    private static ArrayList<LoaderVersionOption> resolveNeoForgeVersions(String str) throws Exception {
        ArrayList<LoaderVersionOption> arrayList = new ArrayList<>();
        try {
            ArrayList<String> mavenVersions = parseMavenVersions(downloadText(NEOFORGE_LEGACY_METADATA_URL));
            final String str2 = str + "-";
            ArrayList arrayList2 = new ArrayList();
            for (String str3 : mavenVersions) {
                if (str3.startsWith(str2)) {
                    arrayList2.add(str3);
                }
            }
            arrayList2.sort(new Comparator() { // from class: ca.dnamobile.javalauncher.ui.version.LoaderVersionResolver$$ExternalSyntheticLambda1
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    String str4 = str2;
                    return LoaderVersionResolver.compareBuildVersions(((String) obj2).substring(str4.length()), ((String) obj).substring(str4.length()));
                }
            });
            Iterator it = arrayList2.iterator();
            while (it.hasNext()) {
                String strSubstring = ((String) it.next()).substring(str2.length());
                arrayList.add(new LoaderVersionOption(strSubstring + "  • NeoForged Forge", strSubstring));
            }
        } catch (Throwable unused) {
        }
        try {
            ArrayList<String> mavenVersions2 = parseMavenVersions(downloadText(NEOFORGE_METADATA_URL));
            ArrayList<String> arrayList3 = new ArrayList();
            for (String str4 : mavenVersions2) {
                if (str.equals(formatNeoForgeGameVersion(str4))) {
                    arrayList3.add(str4);
                }
            }
            arrayList3.sort(new Comparator() { // from class: ca.dnamobile.javalauncher.ui.version.LoaderVersionResolver$$ExternalSyntheticLambda2
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return LoaderVersionResolver.compareBuildVersions((String) obj2, (String) obj);
                }
            });
            for (String str5 : arrayList3) {
                arrayList.add(new LoaderVersionOption(str5, str5));
            }
        } catch (Throwable th) {
            if (arrayList.isEmpty()) {
                if (th instanceof Exception) {
                    throw th;
                }
                throw new Exception(th);
            }
        }
        return arrayList;
    }

    private static String formatNeoForgeGameVersion(String str) {
        StringBuilder sbAppend;
        StringBuilder sb;
        StringBuilder sbAppend2;
        if (str.contains("1.20.1")) {
            return "1.20.1";
        }
        if (str.startsWith("0.")) {
            String str2 = str.substring("0.".length()).split("-", 2)[0];
            int iLastIndexOf = str2.lastIndexOf(46);
            return iLastIndexOf > 0 ? str2.substring(0, iLastIndexOf) : str2;
        }
        String[] strArrSplit = str.split("-", 2)[0].split("\\.");
        ArrayList arrayList = new ArrayList();
        for (String str3 : strArrSplit) {
            if (str3 != null && !str3.trim().isEmpty()) {
                try {
                    arrayList.add(Integer.valueOf(Integer.parseInt(str3.trim())));
                } catch (NumberFormatException unused) {
                    return str;
                }
            }
        }
        if (arrayList.isEmpty()) {
            return str;
        }
        int iIntValue = ((Integer) arrayList.get(0)).intValue();
        int iIntValue2 = arrayList.size() > 1 ? ((Integer) arrayList.get(1)).intValue() : 0;
        int iIntValue3 = arrayList.size() > 2 ? ((Integer) arrayList.get(2)).intValue() : 0;
        if (iIntValue < 25) {
            if (iIntValue2 != 0) {
                sbAppend = new StringBuilder("1.").append(iIntValue).append(".").append(iIntValue2);
            } else {
                sbAppend = new StringBuilder("1.").append(iIntValue);
            }
            return sbAppend.toString();
        }
        if (iIntValue3 != 0) {
            sb = new StringBuilder();
            sbAppend2 = sb.append(iIntValue).append(".").append(iIntValue2).append(".").append(iIntValue3);
        } else {
            sb = new StringBuilder();
            sbAppend2 = sb.append(iIntValue).append(".").append(iIntValue2);
        }
        return sbAppend2.toString();
    }

    private static ArrayList<String> parseMavenVersions(String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(str);
        while (matcher.find()) {
            String strGroup = matcher.group(1);
            if (strGroup != null && !strGroup.trim().isEmpty()) {
                arrayList.add(strGroup.trim());
            }
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int compareBuildVersions(String str, String str2) {
        int[] buildParts = parseBuildParts(str);
        int[] buildParts2 = parseBuildParts(str2);
        int iMax = Math.max(buildParts.length, buildParts2.length);
        int i = 0;
        while (i < iMax) {
            int i2 = i < buildParts.length ? buildParts[i] : 0;
            int i3 = i < buildParts2.length ? buildParts2[i] : 0;
            if (i2 != i3) {
                return Integer.compare(i2, i3);
            }
            i++;
        }
        return str.compareToIgnoreCase(str2);
    }

    private static int[] parseBuildParts(String str) {
        String[] strArrSplit = str.trim().split("[.+\\-]");
        ArrayList arrayList = new ArrayList();
        for (String str2 : strArrSplit) {
            try {
                arrayList.add(Integer.valueOf(Integer.parseInt(str2)));
            } catch (NumberFormatException unused) {
            }
        }
        int[] iArr = new int[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            iArr[i] = ((Integer) arrayList.get(i)).intValue();
        }
        return iArr;
    }

    private static String encode(String str) throws Exception {
        return URLEncoder.encode(str, "UTF-8");
    }

    private static String downloadText(String str) throws Exception {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        String stream = readStream((responseCode < 200 || responseCode >= 300) ? httpURLConnectionOpenConnection.getErrorStream() : httpURLConnectionOpenConnection.getInputStream());
        httpURLConnectionOpenConnection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("HTTP " + responseCode + " " + stream);
        }
        return stream;
    }

    private static HttpURLConnection openConnection(String str) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(45000);
        httpURLConnection.setRequestProperty("User-Agent", "JavaLauncher/1.0");
        httpURLConnection.setRequestMethod(ShareTarget.METHOD_GET);
        return httpURLConnection;
    }

    /* JADX WARN: Removed duplicated region for block: B:34:0x003a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static java.lang.String readStream(java.io.InputStream r4) throws java.lang.Exception {
        /*
            if (r4 != 0) goto L5
            java.lang.String r4 = ""
            return r4
        L5:
            java.io.ByteArrayOutputStream r0 = new java.io.ByteArrayOutputStream     // Catch: java.lang.Throwable -> L37
            r0.<init>()     // Catch: java.lang.Throwable -> L37
            r1 = 65536(0x10000, float:9.1835E-41)
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
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.ui.version.LoaderVersionResolver.readStream(java.io.InputStream):java.lang.String");
    }
}

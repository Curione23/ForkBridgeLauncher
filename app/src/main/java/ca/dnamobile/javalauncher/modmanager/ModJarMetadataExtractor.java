package ca.dnamobile.javalauncher.modmanager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModJarMetadataExtractor {
    private static final int MAX_IMAGE_BYTES = 2097152;
    private static final int MAX_NESTED_JARS = 48;
    private static final int MAX_NESTED_JAR_BYTES = 12582912;
    private static final int MAX_RECURSION_DEPTH = 2;
    private static final int MAX_TEXT_BYTES = 1048576;

    private static Bitmap firstNonNull(Bitmap bitmap, Bitmap bitmap2) {
        return bitmap != null ? bitmap : bitmap2;
    }

    private ModJarMetadataExtractor() {
    }

    public static Result read(File file) {
        Metadata metadata = readMetadata(file);
        if (metadata == null || !metadata.hasAny()) {
            return null;
        }
        return new Result(metadata.displayName, metadata.icon);
    }

    public static String readDisplayName(File file) {
        Result result = read(file);
        if (result == null) {
            return null;
        }
        return result.getDisplayName();
    }

    public static Bitmap readIcon(File file) {
        Result result = read(file);
        if (result == null) {
            return null;
        }
        return result.getIcon();
    }

    private static Metadata readMetadata(File file) {
        if (!file.isFile()) {
            return null;
        }
        Metadata metadata = new Metadata();
        try {
            ZipFile zipFile = new ZipFile(file);
            try {
                metadata.merge(readZipFileMetadata(zipFile));
                if (!metadata.isComplete()) {
                    metadata.merge(readNestedJarMetadata(zipFile));
                }
                zipFile.close();
                if (metadata.hasAny()) {
                    return metadata;
                }
                return null;
            } finally {
            }
        } catch (Throwable unused) {
            return null;
        }
    }

    private static Metadata readZipFileMetadata(ZipFile zipFile) throws IOException {
        Metadata metadata = new Metadata();
        String zipEntryText = readZipEntryText(zipFile, "fabric.mod.json");
        metadata.displayName = firstNonBlank(metadata.displayName, extractJsonString(zipEntryText, "name"));
        Bitmap bitmapDecodeZipBitmap = decodeZipBitmap(zipFile, extractJsonIconString(zipEntryText));
        if (metadata.icon == null) {
            metadata.icon = bitmapDecodeZipBitmap;
        }
        String zipEntryText2 = readZipEntryText(zipFile, "quilt.mod.json");
        metadata.displayName = firstNonBlank(metadata.displayName, extractJsonString(zipEntryText2, "name"));
        Bitmap bitmapDecodeZipBitmap2 = decodeZipBitmap(zipFile, extractJsonIconString(zipEntryText2));
        if (metadata.icon == null) {
            metadata.icon = bitmapDecodeZipBitmap2;
        }
        String zipEntryText3 = readZipEntryText(zipFile, "META-INF/mods.toml");
        metadata.displayName = firstNonBlank(metadata.displayName, extractTomlString(zipEntryText3, "displayName"));
        Bitmap bitmapDecodeZipBitmap3 = decodeZipBitmap(zipFile, firstNonBlank(extractTomlString(zipEntryText3, "logoFile"), extractTomlString(zipEntryText3, "catalogueImageIcon")));
        if (metadata.icon == null) {
            metadata.icon = bitmapDecodeZipBitmap3;
        }
        String zipEntryText4 = readZipEntryText(zipFile, "META-INF/neoforge.mods.toml");
        metadata.displayName = firstNonBlank(metadata.displayName, extractTomlString(zipEntryText4, "displayName"));
        Bitmap bitmapDecodeZipBitmap4 = decodeZipBitmap(zipFile, firstNonBlank(extractTomlString(zipEntryText4, "logoFile"), extractTomlString(zipEntryText4, "catalogueImageIcon")));
        if (metadata.icon == null) {
            metadata.icon = bitmapDecodeZipBitmap4;
        }
        metadata.displayName = firstNonBlank(metadata.displayName, extractJsonString(readZipEntryText(zipFile, "mcmod.info"), "name"));
        if (metadata.icon == null) {
            metadata.icon = decodeFirstLikelyIcon(zipFile);
        }
        return metadata;
    }

    private static Metadata readNestedJarMetadata(ZipFile zipFile) throws IOException {
        byte[] zipEntryBytes;
        Metadata metadata = new Metadata();
        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
        int i = 0;
        while (enumerationEntries.hasMoreElements() && i < 48 && !metadata.isComplete()) {
            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
            if (!zipEntryNextElement.isDirectory() && normalizeZipPath(zipEntryNextElement.getName()).toLowerCase(Locale.US).endsWith(".jar") && zipEntryNextElement.getSize() <= 12582912 && (zipEntryBytes = readZipEntryBytes(zipFile, zipEntryNextElement, MAX_NESTED_JAR_BYTES)) != null && zipEntryBytes.length != 0) {
                i++;
                metadata.merge(readMemoryZipMetadata(zipEntryBytes, 1));
            }
        }
        return metadata;
    }

    private static Metadata readMemoryZipMetadata(byte[] bArr, int i) throws IOException {
        byte[] streamBytes;
        Metadata metadata = new Metadata();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        ArrayList<byte[]> arrayList = new ArrayList();
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bArr));
        while (true) {
            try {
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry == null) {
                    break;
                }
                if (!nextEntry.isDirectory()) {
                    String lowerCase = normalizeZipPath(nextEntry.getName()).toLowerCase(Locale.US);
                    if (isMetadataEntry(lowerCase)) {
                        byte[] streamBytes2 = readStreamBytes(zipInputStream, 1048576);
                        if (streamBytes2 != null) {
                            linkedHashMap.put(lowerCase, new String(streamBytes2, "UTF-8"));
                        }
                    } else if (isImageEntry(lowerCase)) {
                        byte[] streamBytes3 = readStreamBytes(zipInputStream, 2097152);
                        if (streamBytes3 != null) {
                            linkedHashMap2.put(lowerCase, streamBytes3);
                        }
                    } else if (i < 2 && lowerCase.endsWith(".jar") && (streamBytes = readStreamBytes(zipInputStream, MAX_NESTED_JAR_BYTES)) != null && arrayList.size() < 48) {
                        arrayList.add(streamBytes);
                    }
                }
            } catch (Throwable th) {
                try {
                    zipInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        zipInputStream.close();
        String str = (String) linkedHashMap.get("fabric.mod.json");
        metadata.displayName = firstNonBlank(metadata.displayName, extractJsonString(str, "name"));
        metadata.icon = firstNonNull(metadata.icon, decodeMemoryBitmap(linkedHashMap2, extractJsonIconString(str)));
        String str2 = (String) linkedHashMap.get("quilt.mod.json");
        metadata.displayName = firstNonBlank(metadata.displayName, extractJsonString(str2, "name"));
        metadata.icon = firstNonNull(metadata.icon, decodeMemoryBitmap(linkedHashMap2, extractJsonIconString(str2)));
        String str3 = (String) linkedHashMap.get("meta-inf/mods.toml");
        metadata.displayName = firstNonBlank(metadata.displayName, extractTomlString(str3, "displayName"));
        metadata.icon = firstNonNull(metadata.icon, decodeMemoryBitmap(linkedHashMap2, firstNonBlank(extractTomlString(str3, "logoFile"), extractTomlString(str3, "catalogueImageIcon"))));
        String str4 = (String) linkedHashMap.get("meta-inf/neoforge.mods.toml");
        metadata.displayName = firstNonBlank(metadata.displayName, extractTomlString(str4, "displayName"));
        metadata.icon = firstNonNull(metadata.icon, decodeMemoryBitmap(linkedHashMap2, firstNonBlank(extractTomlString(str4, "logoFile"), extractTomlString(str4, "catalogueImageIcon"))));
        metadata.displayName = firstNonBlank(metadata.displayName, extractJsonString((String) linkedHashMap.get("mcmod.info"), "name"));
        if (metadata.icon == null) {
            metadata.icon = decodeFirstLikelyMemoryIcon(linkedHashMap2);
        }
        if (!metadata.isComplete()) {
            for (byte[] bArr2 : arrayList) {
                if (metadata.isComplete()) {
                    break;
                }
                metadata.merge(readMemoryZipMetadata(bArr2, i + 1));
            }
        }
        return metadata;
    }

    private static String readZipEntryText(ZipFile zipFile, String str) throws IOException {
        byte[] zipEntryBytes;
        ZipEntry entry = zipFile.getEntry(str);
        if (entry == null || entry.isDirectory() || (zipEntryBytes = readZipEntryBytes(zipFile, entry, 1048576)) == null) {
            return null;
        }
        return new String(zipEntryBytes, "UTF-8");
    }

    private static byte[] readZipEntryBytes(ZipFile zipFile, ZipEntry zipEntry, int i) throws IOException {
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        try {
            byte[] streamBytes = readStreamBytes(inputStream, i);
            if (inputStream != null) {
                inputStream.close();
            }
            return streamBytes;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static byte[] readStreamBytes(InputStream inputStream, int i) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[16384];
        int i2 = 0;
        while (true) {
            int i3 = inputStream.read(bArr);
            if (i3 == -1) {
                return byteArrayOutputStream.toByteArray();
            }
            i2 += i3;
            if (i2 > i) {
                return null;
            }
            byteArrayOutputStream.write(bArr, 0, i3);
        }
    }

    private static Bitmap decodeZipBitmap(ZipFile zipFile, String str) throws IOException {
        if (isBlank(str)) {
            return null;
        }
        String strNormalizeZipPath = normalizeZipPath(str.trim());
        while (strNormalizeZipPath.startsWith("/")) {
            strNormalizeZipPath = strNormalizeZipPath.substring(1);
        }
        ZipEntry entry = zipFile.getEntry(strNormalizeZipPath);
        if (entry == null && strNormalizeZipPath.startsWith("./")) {
            entry = zipFile.getEntry(strNormalizeZipPath.substring(2));
        }
        if (entry == null) {
            entry = findZipEntryBySuffix(zipFile, strNormalizeZipPath);
        }
        if (entry == null || entry.isDirectory()) {
            return null;
        }
        InputStream inputStream = zipFile.getInputStream(entry);
        try {
            Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            return bitmapDecodeStream;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static Bitmap decodeMemoryBitmap(Map<String, byte[]> map, String str) {
        if (isBlank(str)) {
            return null;
        }
        String lowerCase = normalizeZipPath(str.trim()).toLowerCase(Locale.US);
        while (lowerCase.startsWith("/")) {
            lowerCase = lowerCase.substring(1);
        }
        if (lowerCase.startsWith("./")) {
            lowerCase = lowerCase.substring(2);
        }
        byte[] value = map.get(lowerCase);
        if (value == null) {
            for (Map.Entry<String, byte[]> entry : map.entrySet()) {
                String key = entry.getKey();
                if (key.equals(lowerCase) || key.endsWith("/" + lowerCase)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        if (value == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(value, 0, value.length);
    }

    private static Bitmap decodeFirstLikelyIcon(ZipFile zipFile) throws IOException {
        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
            if (!zipEntryNextElement.isDirectory()) {
                String lowerCase = normalizeZipPath(zipEntryNextElement.getName()).toLowerCase(Locale.US);
                if (isImageEntry(lowerCase) && isLikelyIconName(lowerCase)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntryNextElement);
                    try {
                        Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStream);
                        if (bitmapDecodeStream != null) {
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            return bitmapDecodeStream;
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (Throwable th) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                }
            }
        }
        return null;
    }

    private static Bitmap decodeFirstLikelyMemoryIcon(Map<String, byte[]> map) {
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            if (isLikelyIconName(entry.getKey())) {
                byte[] value = entry.getValue();
                Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(value, 0, value.length);
                if (bitmapDecodeByteArray != null) {
                    return bitmapDecodeByteArray;
                }
            }
        }
        return null;
    }

    private static ZipEntry findZipEntryBySuffix(ZipFile zipFile, String str) {
        String lowerCase = normalizeZipPath(str).toLowerCase(Locale.US);
        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
            if (!zipEntryNextElement.isDirectory()) {
                String lowerCase2 = normalizeZipPath(zipEntryNextElement.getName()).toLowerCase(Locale.US);
                if (lowerCase2.equals(lowerCase) || lowerCase2.endsWith("/" + lowerCase)) {
                    return zipEntryNextElement;
                }
            }
        }
        return null;
    }

    private static String extractJsonString(String str, String str2) {
        if (str == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(str2) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractJsonIconString(String str) {
        String strExtractJsonString = extractJsonString(str, "icon");
        if (!isBlank(strExtractJsonString)) {
            return strExtractJsonString;
        }
        if (str == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\\"icon\\\"\\s*:\\s*\\{([^}]+)\\}", 32).matcher(str);
        if (!matcher.find()) {
            return null;
        }
        Matcher matcher2 = Pattern.compile("\\\"[^\\\"]+\\\"\\s*:\\s*\\\"([^\\\"]+\\.(?:png|jpg|jpeg|webp))\\\"").matcher(matcher.group(1));
        if (matcher2.find()) {
            return matcher2.group(1);
        }
        return null;
    }

    private static String extractTomlString(String str, String str2) {
        if (str == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(str2) + "\\s*=\\s*(?:\\\"([^\\\"]+)\\\"|'([^']+)'|([^#\\r\\n]+))").matcher(str);
        if (!matcher.find()) {
            return null;
        }
        String strGroup = matcher.group(1);
        String strGroup2 = matcher.group(2);
        String strGroup3 = matcher.group(3);
        if (isBlank(strGroup)) {
            strGroup = !isBlank(strGroup2) ? strGroup2 : strGroup3;
        }
        if (strGroup == null) {
            return null;
        }
        int iIndexOf = strGroup.indexOf(35);
        if (iIndexOf >= 0) {
            strGroup = strGroup.substring(0, iIndexOf);
        }
        return strGroup.trim();
    }

    private static boolean isMetadataEntry(String str) {
        return str.equals("fabric.mod.json") || str.equals("quilt.mod.json") || str.equals("mcmod.info") || str.equals("meta-inf/mods.toml") || str.equals("meta-inf/neoforge.mods.toml");
    }

    private static boolean isImageEntry(String str) {
        return str.endsWith(".png") || str.endsWith(".jpg") || str.endsWith(".jpeg") || str.endsWith(".webp");
    }

    private static boolean isLikelyIconName(String str) {
        int i;
        int iLastIndexOf = str.lastIndexOf(47);
        if (iLastIndexOf >= 0 && (i = iLastIndexOf + 1) < str.length()) {
            str = str.substring(i);
        }
        return str.equals("icon.png") || str.equals("logo.png") || str.equals("pack.png") || str.equals("mod_icon.png") || str.equals("modicon.png") || str.endsWith("_icon.png") || str.endsWith("-icon.png") || str.contains("icon") || str.contains("logo");
    }

    private static String normalizeZipPath(String str) {
        return str.replace('\\', '/');
    }

    private static String firstNonBlank(String str, String str2) {
        if (!isBlank(str)) {
            return str;
        }
        if (isBlank(str2)) {
            return null;
        }
        return str2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static final class Result {
        private final String displayName;
        private final Bitmap icon;

        private Result(String str, Bitmap bitmap) {
            this.displayName = str;
            this.icon = bitmap;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public Bitmap getIcon() {
            return this.icon;
        }
    }

    private static final class Metadata {
        String displayName;
        Bitmap icon;

        private Metadata() {
        }

        boolean hasAny() {
            return (ModJarMetadataExtractor.isBlank(this.displayName) && this.icon == null) ? false : true;
        }

        boolean isComplete() {
            return (ModJarMetadataExtractor.isBlank(this.displayName) || this.icon == null) ? false : true;
        }

        void merge(Metadata metadata) {
            Bitmap bitmap;
            if (metadata == null) {
                return;
            }
            if (ModJarMetadataExtractor.isBlank(this.displayName) && !ModJarMetadataExtractor.isBlank(metadata.displayName)) {
                this.displayName = metadata.displayName;
            }
            if (this.icon != null || (bitmap = metadata.icon) == null) {
                return;
            }
            this.icon = bitmap;
        }
    }
}

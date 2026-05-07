package ca.dnamobile.javalauncher.ui.version;

import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.kdt.pojavlaunch.Architecture;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class NativesExtractor {
    private static final ArrayList<String> LIBRARY_BLACKLIST = createLibraryBlacklist();
    private final File destinationDir;
    private final String libraryLocation = "jni/" + getAarArchitectureName() + "/";

    public NativesExtractor(File file) {
        this.destinationDir = file;
    }

    private static ArrayList<String> createLibraryBlacklist() {
        String[] list;
        ArrayList<String> arrayList = new ArrayList<>();
        if (PathManager.DIR_NATIVE_LIB == null || PathManager.DIR_NATIVE_LIB.trim().isEmpty() || (list = new File(PathManager.DIR_NATIVE_LIB).list()) == null) {
            return arrayList;
        }
        for (String str : list) {
            if (!"libjnidispatch.so".equals(str)) {
                arrayList.add(str);
            }
        }
        arrayList.trimToSize();
        return arrayList;
    }

    private static String getAarArchitectureName() {
        int deviceArchitecture = Architecture.getDeviceArchitecture();
        if (deviceArchitecture == 0) {
            return "armeabi-v7a";
        }
        if (deviceArchitecture == 1) {
            return "arm64-v8a";
        }
        if (deviceArchitecture == 2) {
            return "x86";
        }
        if (deviceArchitecture == 3) {
            return "x86_64";
        }
        throw new RuntimeException("Unknown CPU architecture: " + deviceArchitecture);
    }

    public void extractFromAar(File file) throws IOException {
        String fileName;
        if (!file.isFile()) {
            throw new IOException("Missing native AAR: " + file.getAbsolutePath());
        }
        if (!this.destinationDir.exists() && !this.destinationDir.mkdirs() && !this.destinationDir.isDirectory()) {
            throw new IOException("Unable to create native directory: " + this.destinationDir.getAbsolutePath());
        }
        byte[] bArr = new byte[8192];
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            try {
                NonCloseableInputStream nonCloseableInputStream = new NonCloseableInputStream(zipInputStream);
                while (true) {
                    ZipEntry nextEntry = zipInputStream.getNextEntry();
                    if (nextEntry != null) {
                        String name = nextEntry.getName();
                        if (name.startsWith(this.libraryLocation) && !nextEntry.isDirectory() && (fileName = getFileName(name)) != null && !LIBRARY_BLACKLIST.contains(fileName)) {
                            processEntry(nonCloseableInputStream, nextEntry, new File(this.destinationDir, fileName), bArr);
                        }
                    } else {
                        zipInputStream.close();
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

    private static String getFileName(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        int iMax = Math.max(str.lastIndexOf(47), str.lastIndexOf(92));
        if (iMax >= 0) {
            str = str.substring(iMax + 1);
        }
        if (str.trim().isEmpty()) {
            return null;
        }
        return str;
    }

    private static long fileCrc32(File file, byte[] bArr) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            CRC32 crc32 = new CRC32();
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i != -1) {
                    crc32.update(bArr, 0, i);
                } else {
                    long value = crc32.getValue();
                    fileInputStream.close();
                    return value;
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

    private void processEntry(InputStream inputStream, ZipEntry zipEntry, File file, byte[] bArr) throws IOException {
        if (file.exists()) {
            long size = zipEntry.getSize();
            long crc = zipEntry.getCrc();
            long length = file.length();
            long jFileCrc32 = fileCrc32(file, bArr);
            if (length == size && jFileCrc32 == crc) {
                return;
            }
        }
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs() && !parentFile.isDirectory()) {
            throw new IOException("Unable to create directory: " + parentFile);
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        while (true) {
            try {
                int i = inputStream.read(bArr);
                if (i != -1) {
                    fileOutputStream.write(bArr, 0, i);
                } else {
                    fileOutputStream.close();
                    file.setReadable(true, false);
                    file.setExecutable(true, false);
                    return;
                }
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

    private static final class NonCloseableInputStream extends FilterInputStream {
        @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
        public void close() {
        }

        private NonCloseableInputStream(InputStream inputStream) {
            super(inputStream);
        }
    }
}

package ca.dnamobile.javalauncher;

/* JADX INFO: compiled from: D8$$SyntheticClass */
/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final /* synthetic */ class MainActivity$$ExternalSyntheticBackport0 {
    public static /* synthetic */ boolean m(String str) {
        int length = str.length();
        int iCharCount = 0;
        while (iCharCount < length) {
            int iCodePointAt = str.codePointAt(iCharCount);
            if (!Character.isWhitespace(iCodePointAt)) {
                return false;
            }
            iCharCount += Character.charCount(iCodePointAt);
        }
        return true;
    }
}

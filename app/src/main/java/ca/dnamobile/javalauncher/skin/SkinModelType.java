package ca.dnamobile.javalauncher.skin;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public enum SkinModelType {
    NONE("none"),
    CLASSIC("classic"),
    SLIM("slim");

    public final String id;

    SkinModelType(String str) {
        this.id = str;
    }

    public static SkinModelType fromId(String str) {
        if (str == null) {
            return CLASSIC;
        }
        for (SkinModelType skinModelType : values()) {
            if (skinModelType.id.equalsIgnoreCase(str)) {
                return skinModelType;
            }
        }
        return CLASSIC;
    }
}

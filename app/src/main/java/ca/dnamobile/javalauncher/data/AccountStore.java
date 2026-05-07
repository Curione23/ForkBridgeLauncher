package ca.dnamobile.javalauncher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import ca.dnamobile.javalauncher.skin.CustomSkinStore;
import ca.dnamobile.javalauncher.skin.SkinModelType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class AccountStore {
    private static final String KEY_ACCOUNT_JSON = "active_account_json";
    private static final String KEY_LAST_MICROSOFT_ACCOUNT_JSON = "last_microsoft_account_json";
    private static final String KEY_MICROSOFT_LOGIN_COMPLETED_ONCE = "microsoft_login_completed_once";
    private static final String KEY_OFFLINE_ACCOUNTS_JSON = "offline_accounts_json";
    private static final String OFFLINE_DIR = "offline_accounts";
    private static final String PREFS = "java_launcher_accounts";
    private final Context context;
    private final SharedPreferences preferences;

    public AccountStore(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext;
        this.preferences = applicationContext.getSharedPreferences(PREFS, 0);
    }

    public void save(Account account) {
        if (account.isMicrosoftAccount()) {
            saveMicrosoftAccount(account);
        } else {
            saveOfflineAccount(account);
        }
    }

    public void saveMicrosoftAccount(Account account) {
        Account accountAsMicrosoftAccount = account.asMicrosoftAccount();
        this.preferences.edit().putString(KEY_ACCOUNT_JSON, accountAsMicrosoftAccount.toJson().toString()).putString(KEY_LAST_MICROSOFT_ACCOUNT_JSON, accountAsMicrosoftAccount.toJson().toString()).putBoolean(KEY_MICROSOFT_LOGIN_COMPLETED_ONCE, true).apply();
    }

    public void useLastMicrosoftAccount() {
        Account accountLoadLastMicrosoftAccount = loadLastMicrosoftAccount();
        if (accountLoadLastMicrosoftAccount == null) {
            throw new IllegalStateException("No remembered Microsoft account is available.");
        }
        saveActiveOnly(accountLoadLastMicrosoftAccount.asMicrosoftAccount());
    }

    public void saveOfflineAccount(String str) {
        saveOrUpdateOfflineAccount(null, str, null, false);
    }

    public Account saveOrUpdateOfflineAccount(String str, String str2, Uri uri, boolean z) {
        Account next;
        if (!hasMicrosoftLoginCompletedOnce()) {
            throw new IllegalStateException("Microsoft sign-in must be completed before using offline accounts.");
        }
        String strSanitizePlayerName = Account.sanitizePlayerName(str2);
        if (strSanitizePlayerName.length() < 3 || strSanitizePlayerName.length() > 16) {
            throw new IllegalStateException("Offline username must be 3-16 letters, numbers, or underscores.");
        }
        ArrayList<Account> arrayListListOfflineAccounts = listOfflineAccounts();
        if (str == null || str.trim().length() <= 0) {
            next = null;
        } else {
            Iterator<Account> it = arrayListListOfflineAccounts.iterator();
            while (it.hasNext()) {
                next = it.next();
                if (str.equals(next.accountId)) {
                    break;
                }
            }
            next = null;
        }
        String string = next != null ? next.accountId : UUID.randomUUID().toString();
        File offlineSkinFile = getOfflineSkinFile(string);
        String absolutePath = "";
        String str3 = next != null ? next.offlineSkinPath : "";
        SkinModelType skinModelTypeFromId = next != null ? SkinModelType.fromId(next.offlineSkinModel) : SkinModelType.NONE;
        try {
            if (uri != null) {
                ensureParent(offlineSkinFile);
                File file = new File(offlineSkinFile.getParentFile(), offlineSkinFile.getName() + ".tmp");
                copyUriToFile(uri, file);
                if (!CustomSkinStore.isSkinValid(file)) {
                    file.delete();
                    throw new IllegalStateException("Invalid skin. Use a 64x64 or 64x32 PNG skin.");
                }
                skinModelTypeFromId = CustomSkinStore.getSkinModel(file);
                if (offlineSkinFile.exists()) {
                    offlineSkinFile.delete();
                }
                if (!file.renameTo(offlineSkinFile)) {
                    copyFile(file, offlineSkinFile);
                    file.delete();
                }
                absolutePath = offlineSkinFile.getAbsolutePath();
            } else if (z) {
                if (offlineSkinFile.exists()) {
                    offlineSkinFile.delete();
                }
                skinModelTypeFromId = SkinModelType.NONE;
            } else if (str3.length() <= 0 || new File(str3).isFile()) {
                absolutePath = str3;
            } else {
                skinModelTypeFromId = SkinModelType.NONE;
            }
            Account accountOffline = Account.offline(string, strSanitizePlayerName, CustomSkinStore.getOfflineUuidWithSkinModel(strSanitizePlayerName, skinModelTypeFromId), absolutePath, skinModelTypeFromId.id);
            int i = 0;
            while (true) {
                if (i < arrayListListOfflineAccounts.size()) {
                    if (accountOffline.accountId.equals(arrayListListOfflineAccounts.get(i).accountId)) {
                        arrayListListOfflineAccounts.set(i, accountOffline);
                        break;
                    }
                    i++;
                } else {
                    arrayListListOfflineAccounts.add(accountOffline);
                    break;
                }
            }
            saveOfflineAccounts(arrayListListOfflineAccounts);
            saveActiveOnly(accountOffline);
            return accountOffline;
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage() != null ? e.getMessage() : e.toString(), e);
        }
    }

    public void saveOfflineAccount(Account account) {
        if (!account.isOfflineAccount()) {
            throw new IllegalArgumentException("Expected offline account");
        }
        if (!hasMicrosoftLoginCompletedOnce()) {
            throw new IllegalStateException("Microsoft sign-in must be completed before using offline accounts.");
        }
        ArrayList<Account> arrayListListOfflineAccounts = listOfflineAccounts();
        Account accountAsOfflineAccount = account.asOfflineAccount();
        int i = 0;
        while (true) {
            if (i < arrayListListOfflineAccounts.size()) {
                if (accountAsOfflineAccount.accountId.equals(arrayListListOfflineAccounts.get(i).accountId)) {
                    arrayListListOfflineAccounts.set(i, accountAsOfflineAccount);
                    break;
                }
                i++;
            } else {
                arrayListListOfflineAccounts.add(accountAsOfflineAccount);
                break;
            }
        }
        saveOfflineAccounts(arrayListListOfflineAccounts);
        saveActiveOnly(accountAsOfflineAccount);
    }

    public void activateOfflineAccount(String str) {
        for (Account account : listOfflineAccounts()) {
            if (str.equals(account.accountId)) {
                saveActiveOnly(account);
                return;
            }
        }
        throw new IllegalStateException("Offline account was not found.");
    }

    public void deleteOfflineAccount(String str) {
        Account accountRemove;
        ArrayList<Account> arrayListListOfflineAccounts = listOfflineAccounts();
        int i = 0;
        while (true) {
            if (i >= arrayListListOfflineAccounts.size()) {
                accountRemove = null;
                break;
            } else {
                if (str.equals(arrayListListOfflineAccounts.get(i).accountId)) {
                    accountRemove = arrayListListOfflineAccounts.remove(i);
                    break;
                }
                i++;
            }
        }
        if (accountRemove != null) {
            File offlineSkinFile = getOfflineSkinFile(str);
            if (offlineSkinFile.exists()) {
                offlineSkinFile.delete();
            }
        }
        saveOfflineAccounts(arrayListListOfflineAccounts);
        Account accountLoad = load();
        if (accountLoad != null && accountLoad.isOfflineAccount() && str.equals(accountLoad.accountId)) {
            Account accountLoadLastMicrosoftAccount = loadLastMicrosoftAccount();
            if (accountLoadLastMicrosoftAccount != null) {
                saveActiveOnly(accountLoadLastMicrosoftAccount.asMicrosoftAccount());
            } else {
                clear();
            }
        }
    }

    public ArrayList<Account> listOfflineAccounts() {
        ArrayList<Account> arrayList = new ArrayList<>();
        try {
            JSONArray jSONArray = new JSONArray(this.preferences.getString(KEY_OFFLINE_ACCOUNTS_JSON, "[]"));
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    Account accountFromJson = Account.fromJson(jSONObjectOptJSONObject);
                    if (accountFromJson.isOfflineAccount()) {
                        arrayList.add(accountFromJson);
                    }
                }
            }
        } catch (Throwable unused) {
        }
        return arrayList;
    }

    private void saveOfflineAccounts(ArrayList<Account> arrayList) {
        JSONArray jSONArray = new JSONArray();
        Iterator<Account> it = arrayList.iterator();
        while (it.hasNext()) {
            jSONArray.put(it.next().toJson());
        }
        this.preferences.edit().putString(KEY_OFFLINE_ACCOUNTS_JSON, jSONArray.toString()).apply();
    }

    private void saveActiveOnly(Account account) {
        this.preferences.edit().putString(KEY_ACCOUNT_JSON, account.toJson().toString()).apply();
    }

    public Account load() {
        return readAccount(KEY_ACCOUNT_JSON);
    }

    public Account loadLastMicrosoftAccount() {
        Account account = readAccount(KEY_LAST_MICROSOFT_ACCOUNT_JSON);
        if (account != null && account.isMicrosoftAccount()) {
            return account;
        }
        Account accountLoad = load();
        if (accountLoad == null || !accountLoad.isMicrosoftAccount()) {
            return null;
        }
        return accountLoad;
    }

    private Account readAccount(String str) {
        String string = this.preferences.getString(str, null);
        if (string != null && string.length() != 0) {
            try {
                return Account.fromJson(new JSONObject(string));
            } catch (JSONException unused) {
            }
        }
        return null;
    }

    public boolean hasActiveAccount() {
        return load() != null;
    }

    public boolean hasActiveMicrosoftAccount() {
        Account accountLoad = load();
        return accountLoad != null && accountLoad.isMicrosoftAccount() && accountLoad.hasMinecraftSession();
    }

    public boolean hasStoredMicrosoftAccount() {
        return loadLastMicrosoftAccount() != null;
    }

    public boolean hasMicrosoftLoginCompletedOnce() {
        if (this.preferences.getBoolean(KEY_MICROSOFT_LOGIN_COMPLETED_ONCE, false)) {
            return true;
        }
        Account accountLoadLastMicrosoftAccount = loadLastMicrosoftAccount();
        return accountLoadLastMicrosoftAccount != null && accountLoadLastMicrosoftAccount.isMicrosoftAccount();
    }

    public boolean canUseOfflineMode() {
        return hasMicrosoftLoginCompletedOnce();
    }

    public void markMicrosoftLoginCompletedOnce() {
        this.preferences.edit().putBoolean(KEY_MICROSOFT_LOGIN_COMPLETED_ONCE, true).apply();
    }

    public void signOutMicrosoftAccount() {
        this.preferences.edit().remove(KEY_ACCOUNT_JSON).remove(KEY_LAST_MICROSOFT_ACCOUNT_JSON).remove(KEY_MICROSOFT_LOGIN_COMPLETED_ONCE).commit();
    }

    public void clear() {
        this.preferences.edit().remove(KEY_ACCOUNT_JSON).apply();
    }

    public void clearMicrosoftLoginHistoryForFullResetOnly() {
        this.preferences.edit().remove(KEY_ACCOUNT_JSON).remove(KEY_LAST_MICROSOFT_ACCOUNT_JSON).remove(KEY_MICROSOFT_LOGIN_COMPLETED_ONCE).remove(KEY_OFFLINE_ACCOUNTS_JSON).apply();
    }

    private File getOfflineSkinFile(String str) {
        return new File(new File(this.context.getFilesDir(), "offline_accounts/" + str), "skin.png");
    }

    private void copyUriToFile(Uri uri, File file) throws IOException {
        InputStream inputStreamOpenInputStream = this.context.getContentResolver().openInputStream(uri);
        try {
            if (inputStreamOpenInputStream == null) {
                throw new IOException("Could not open selected skin.");
            }
            ensureParent(file);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                byte[] bArr = new byte[8192];
                while (true) {
                    int i = inputStreamOpenInputStream.read(bArr);
                    if (i == -1) {
                        break;
                    } else {
                        fileOutputStream.write(bArr, 0, i);
                    }
                }
                fileOutputStream.close();
                if (inputStreamOpenInputStream != null) {
                    inputStreamOpenInputStream.close();
                }
            } finally {
            }
        } catch (Throwable th) {
            if (inputStreamOpenInputStream != null) {
                try {
                    inputStreamOpenInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static void copyFile(File file, File file2) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                byte[] bArr = new byte[8192];
                while (true) {
                    int i = fileInputStream.read(bArr);
                    if (i == -1) {
                        fileOutputStream.close();
                        fileInputStream.close();
                        return;
                    }
                    fileOutputStream.write(bArr, 0, i);
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

    private static void ensureParent(File file) throws IOException {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Could not create folder: " + parentFile.getAbsolutePath());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean notEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    public static final class Account {
        public static final String TYPE_MICROSOFT = "microsoft";
        public static final String TYPE_OFFLINE = "offline";
        public final String accessToken;
        public final String accountId;
        public final String accountType;
        public final String displayName;
        public final String email;
        public final String idToken;
        public final String minecraftAccessToken;
        public final String minecraftName;
        public final String minecraftUuid;
        public final String offlineSkinModel;
        public final String offlineSkinPath;
        public final String refreshToken;
        public final String skinUrl;
        public final String skinVariant;
        public final String xuid;

        public Account(String str, String str2, String str3, String str4) {
            this(TYPE_MICROSOFT, "", str, str2, str3, str4, "", "", "", "", "", "", "classic", "", "none");
        }

        public Account(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9) {
            this(TYPE_MICROSOFT, "", str, str2, str3, str4, str5, str6, str7, str8, str9, "", "classic", "", "none");
        }

        public Account(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, String str11) {
            this(TYPE_MICROSOFT, "", str, str2, str3, str4, str5, str6, str7, str8, str9, str10, str11, "", "none");
        }

        public Account(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, String str11, String str12, String str13, String str14, String str15) {
            this.accountType = str;
            this.accountId = str2;
            this.email = str3;
            this.displayName = str4;
            this.idToken = str5;
            this.accessToken = str6;
            this.refreshToken = str7;
            this.minecraftAccessToken = str8;
            this.minecraftName = str9;
            this.minecraftUuid = str10;
            this.xuid = str11;
            this.skinUrl = str12;
            this.skinVariant = str13;
            this.offlineSkinPath = str14;
            this.offlineSkinModel = str15;
        }

        public static Account offline(String str) {
            String strSanitizePlayerName = sanitizePlayerName(str);
            return offline(UUID.randomUUID().toString(), strSanitizePlayerName, CustomSkinStore.getOfflineUuidWithSkinModel(strSanitizePlayerName, SkinModelType.NONE), "", SkinModelType.NONE.id);
        }

        public static Account offline(String str, String str2, String str3, String str4, String str5) {
            String strSanitizePlayerName = sanitizePlayerName(str2);
            return new Account(TYPE_OFFLINE, str, "", strSanitizePlayerName, "", "0", "", "", strSanitizePlayerName, str3, "", "", "classic", str4, str5);
        }

        Account asMicrosoftAccount() {
            return TYPE_MICROSOFT.equals(this.accountType) ? this : new Account(TYPE_MICROSOFT, this.accountId, this.email, this.displayName, this.idToken, this.accessToken, this.refreshToken, this.minecraftAccessToken, this.minecraftName, this.minecraftUuid, this.xuid, this.skinUrl, this.skinVariant, "", "none");
        }

        Account asOfflineAccount() {
            if (TYPE_OFFLINE.equals(this.accountType)) {
                return this;
            }
            return offline(this.accountId.length() > 0 ? this.accountId : UUID.randomUUID().toString(), getBestDisplayName(), CustomSkinStore.getOfflineUuidWithSkinModel(getBestDisplayName(), SkinModelType.fromId(this.offlineSkinModel)), this.offlineSkinPath, this.offlineSkinModel);
        }

        public boolean isOfflineAccount() {
            return TYPE_OFFLINE.equals(this.accountType);
        }

        public boolean isMicrosoftAccount() {
            return TYPE_MICROSOFT.equals(this.accountType) || (!TYPE_OFFLINE.equals(this.accountType) && hasMinecraftSession());
        }

        public boolean hasMinecraftSession() {
            return AccountStore.notEmpty(this.minecraftAccessToken) && AccountStore.notEmpty(this.minecraftName) && AccountStore.notEmpty(this.minecraftUuid);
        }

        public boolean hasOfflineSkin() {
            return AccountStore.notEmpty(this.offlineSkinPath) && new File(this.offlineSkinPath).isFile();
        }

        public String getBestDisplayName() {
            return AccountStore.notEmpty(this.displayName) ? this.displayName : AccountStore.notEmpty(this.minecraftName) ? this.minecraftName : AccountStore.notEmpty(this.email) ? this.email : isOfflineAccount() ? "Offline Player" : "Microsoft Player";
        }

        JSONObject toJson() {
            JSONObject jSONObject = new JSONObject();
            try {
                jSONObject.put("accountType", this.accountType);
                jSONObject.put("accountId", this.accountId);
                jSONObject.put(NotificationCompat.CATEGORY_EMAIL, this.email);
                jSONObject.put("displayName", this.displayName);
                jSONObject.put("idToken", this.idToken);
                jSONObject.put("accessToken", this.accessToken);
                jSONObject.put("refreshToken", this.refreshToken);
                jSONObject.put("minecraftAccessToken", this.minecraftAccessToken);
                jSONObject.put("minecraftName", this.minecraftName);
                jSONObject.put("minecraftUuid", this.minecraftUuid);
                jSONObject.put("xuid", this.xuid);
                jSONObject.put("skinUrl", this.skinUrl);
                jSONObject.put("skinVariant", this.skinVariant);
                jSONObject.put("offlineSkinPath", this.offlineSkinPath);
                jSONObject.put("offlineSkinModel", this.offlineSkinModel);
            } catch (JSONException unused) {
            }
            return jSONObject;
        }

        static Account fromJson(JSONObject jSONObject) {
            String strOptString = jSONObject.optString("accountType", "");
            if (strOptString.length() == 0) {
                strOptString = AccountStore.notEmpty(jSONObject.optString("minecraftAccessToken", "")) ? TYPE_MICROSOFT : TYPE_OFFLINE;
            }
            String str = strOptString;
            String strOptString2 = jSONObject.optString("displayName", "");
            if (strOptString2.length() == 0 && TYPE_MICROSOFT.equals(str)) {
                strOptString2 = "Microsoft Player";
            }
            String str2 = strOptString2;
            String strOptString3 = jSONObject.optString("accountId", "");
            if (TYPE_OFFLINE.equals(str) && strOptString3.length() == 0) {
                strOptString3 = UUID.randomUUID().toString();
            }
            return new Account(str, strOptString3, jSONObject.optString(NotificationCompat.CATEGORY_EMAIL, ""), str2, jSONObject.optString("idToken", ""), jSONObject.optString("accessToken", ""), jSONObject.optString("refreshToken", ""), jSONObject.optString("minecraftAccessToken", ""), jSONObject.optString("minecraftName", str2), jSONObject.optString("minecraftUuid", ""), jSONObject.optString("xuid", ""), jSONObject.optString("skinUrl", ""), jSONObject.optString("skinVariant", "classic"), jSONObject.optString("offlineSkinPath", ""), jSONObject.optString("offlineSkinModel", "none"));
        }

        public static String sanitizePlayerName(String str) {
            String strReplaceAll = str.trim().replaceAll("[^A-Za-z0-9_]", "");
            return strReplaceAll.length() < 3 ? "Player" : strReplaceAll.length() > 16 ? strReplaceAll.substring(0, 16) : strReplaceAll;
        }
    }
}

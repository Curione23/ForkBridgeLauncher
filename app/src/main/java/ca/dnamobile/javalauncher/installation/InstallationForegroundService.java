package ca.dnamobile.javalauncher.installation;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import ca.dnamobile.javalauncher.MainActivity;
import ca.dnamobile.javalauncher.feature.log.Logging;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class InstallationForegroundService extends Service {
    private static final String ACTION_START = "ca.dnamobile.javalauncher.installation.START";
    private static final String ACTION_STOP = "ca.dnamobile.javalauncher.installation.STOP";
    private static final String ACTION_UPDATE = "ca.dnamobile.javalauncher.installation.UPDATE";
    private static final String CHANNEL_ID = "launcher_installation";
    private static final String EXTRA_INDETERMINATE = "indeterminate";
    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_PROGRESS = "progress";
    private static final String EXTRA_TITLE = "title";
    private static final int NOTIFICATION_ID = 4317;
    private static final String TAG = "InstallForeground";
    private String title = "Installing Minecraft";
    private String message = "Preparing installation...";
    private int progress = 0;
    private boolean indeterminate = true;
    private boolean foregroundStarted = false;

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void start(Context context, String str, String str2, int i, boolean z) {
        Intent intent = new Intent(context, (Class<?>) InstallationForegroundService.class);
        intent.setAction(ACTION_START);
        putProgress(intent, str, str2, i, z);
        safeStart(context, intent);
    }

    public static void update(Context context, String str, String str2, int i, boolean z) {
        Intent intent = new Intent(context, (Class<?>) InstallationForegroundService.class);
        intent.setAction(ACTION_UPDATE);
        putProgress(intent, str, str2, i, z);
        safeStart(context, intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, (Class<?>) InstallationForegroundService.class);
        intent.setAction(ACTION_STOP);
        try {
            context.startService(intent);
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to stop install foreground service: " + th.getMessage());
        }
    }

    private static void safeStart(Context context, Intent intent) {
        try {
            context.startForegroundService(intent);
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to start install foreground service", th);
        }
    }

    private static void putProgress(Intent intent, String str, String str2, int i, boolean z) {
        intent.putExtra(EXTRA_TITLE, str);
        intent.putExtra("message", str2);
        intent.putExtra("progress", Math.max(0, Math.min(100, i)));
        intent.putExtra(EXTRA_INDETERMINATE, z);
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopForegroundCompat();
            stopSelf();
            return 2;
        }
        readIntent(intent);
        Notification notificationBuildNotification = buildNotification();
        if (!this.foregroundStarted) {
            startForeground(NOTIFICATION_ID, notificationBuildNotification);
            this.foregroundStarted = true;
        } else {
            NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, notificationBuildNotification);
            }
        }
        return 1;
    }

    private void readIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        this.title = intent.getStringExtra(EXTRA_TITLE) != null ? intent.getStringExtra(EXTRA_TITLE) : this.title;
        this.message = intent.getStringExtra("message") != null ? intent.getStringExtra("message") : this.message;
        this.progress = Math.max(0, Math.min(100, intent.getIntExtra("progress", this.progress)));
        this.indeterminate = intent.getBooleanExtra(EXTRA_INDETERMINATE, this.indeterminate);
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, (Class<?>) MainActivity.class);
        intent.addFlags(603979776);
        PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 201326592);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.stat_sys_download).setContentTitle(this.title).setContentText(this.message).setContentIntent(activity).setOngoing(true).setOnlyAlertOnce(true).setProgress(100, this.progress, this.indeterminate);
        builder.setCategory("progress");
        return builder.build();
    }

    private void createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
        if (notificationManager == null) {
            return;
        }
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Installations", 2);
        notificationChannel.setDescription("Shows Minecraft, Fabric, Forge, and NeoForge installation progress.");
        notificationManager.createNotificationChannel(notificationChannel);
    }

    private void stopForegroundCompat() {
        stopForeground(1);
        this.foregroundStarted = false;
    }
}

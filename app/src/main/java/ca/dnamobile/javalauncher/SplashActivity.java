package ca.dnamobile.javalauncher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.feature.unpack.AbstractUnpackTask;
import ca.dnamobile.javalauncher.feature.unpack.Components;
import ca.dnamobile.javalauncher.feature.unpack.Jre;
import ca.dnamobile.javalauncher.feature.unpack.UnpackComponentsTask;
import ca.dnamobile.javalauncher.feature.unpack.UnpackJreTask;
import ca.dnamobile.javalauncher.feature.unpack.UnpackSingleFilesTask;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.util.ArrayList;
import java.util.List;
import net.kdt.pojavlaunch.Tools;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public class SplashActivity extends AppCompatActivity {
    private volatile boolean finished;
    private TextView statusText;
    private TextView titleText;

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_splash);
        this.titleText = (TextView) findViewById(R.id.textTitle);
        this.statusText = (TextView) findViewById(R.id.textStatus);
        this.titleText.setText(R.string.app_name);
        PathManager.initContextConstants(this);
        if (!Tools.checkStorageRoot()) {
            setStatus(getString(R.string.splash_screen_storage_unavailable));
        } else {
            startInstallThread();
        }
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        this.finished = true;
        super.onDestroy();
    }

    private void startInstallThread() {
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.SplashActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                SplashActivity.this.lambda$startInstallThread$0();
            }
        }, "JavaLauncher Unpack").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startInstallThread$0() {
        try {
            runInstallFlow();
            openMainActivity();
        } catch (Throwable th) {
            Logging.e("SplashActivity", "Launcher preparation failed", th);
            setStatus(getString(R.string.splash_screen_failed, new Object[]{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}));
        }
    }

    private void runInstallFlow() {
        setStatus(getString(R.string.splash_screen_checking));
        List<TaskEntry> listBuildTasks = buildTasks();
        int size = listBuildTasks.size();
        int i = 0;
        for (TaskEntry taskEntry : listBuildTasks) {
            i++;
            setStatus(getString(R.string.splash_screen_checking_item, new Object[]{Integer.valueOf(i), Integer.valueOf(size), taskEntry.name}));
            if (taskEntry.task.isNeedUnpack()) {
                setStatus(getString(R.string.splash_screen_installing_item, new Object[]{Integer.valueOf(i), Integer.valueOf(size), taskEntry.name}));
                taskEntry.task.run();
            }
        }
        setStatus(getString(R.string.splash_screen_finalizing));
        new UnpackSingleFilesTask(this).run();
    }

    private List<TaskEntry> buildTasks() {
        ArrayList arrayList = new ArrayList();
        for (Components components : Components.values()) {
            UnpackComponentsTask unpackComponentsTask = new UnpackComponentsTask(this, components);
            if (!unpackComponentsTask.isCheckFailed()) {
                arrayList.add(new TaskEntry(components.displayName, unpackComponentsTask));
            }
        }
        for (Jre jre : Jre.values()) {
            UnpackJreTask unpackJreTask = new UnpackJreTask(this, jre);
            if (!unpackJreTask.isCheckFailed()) {
                arrayList.add(new TaskEntry(jre.jreName, unpackJreTask));
            }
        }
        return arrayList;
    }

    private void openMainActivity() {
        if (this.finished) {
            return;
        }
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.SplashActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SplashActivity.this.lambda$openMainActivity$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openMainActivity$1() {
        if (this.finished) {
            return;
        }
        setStatus(getString(R.string.splash_screen_done));
        startActivity(new Intent(this, (Class<?>) MainActivity.class));
        finish();
    }

    private void setStatus(final String str) {
        if (this.finished) {
            return;
        }
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.SplashActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SplashActivity.this.lambda$setStatus$2(str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setStatus$2(String str) {
        TextView textView;
        if (this.finished || (textView = this.statusText) == null) {
            return;
        }
        textView.setText(str);
    }

    private static final class TaskEntry {
        final String name;
        final AbstractUnpackTask task;

        TaskEntry(String str, AbstractUnpackTask abstractUnpackTask) {
            this.name = str;
            this.task = abstractUnpackTask;
        }
    }
}

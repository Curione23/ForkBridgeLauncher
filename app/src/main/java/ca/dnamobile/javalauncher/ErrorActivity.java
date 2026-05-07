package ca.dnamobile.javalauncher;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import ca.dnamobile.javalauncher.logs.LauncherLogManager;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public class ErrorActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "message";

    public static void showExitMessage(Context context, int i, boolean z) {
        String str;
        if (z) {
            str = "Minecraft stopped from signal " + i;
        } else {
            str = "Minecraft exited with code " + i;
        }
        Intent intent = new Intent(context, (Class<?>) ErrorActivity.class);
        intent.addFlags(268435456);
        intent.putExtra(EXTRA_MESSAGE, str);
        context.startActivity(intent);
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String stringExtra = getIntent().getStringExtra(EXTRA_MESSAGE);
        if (stringExtra == null || stringExtra.trim().isEmpty()) {
            stringExtra = "Minecraft exited.";
        }
        TextView textView = new TextView(this);
        int i = (int) (getResources().getDisplayMetrics().density * 24.0f);
        textView.setPadding(i, i, i, i);
        textView.setText(stringExtra + "\n\nUse Share latestlog.txt so the crash can be checked.");
        textView.setTextSize(16.0f);
        setContentView(textView);
        new AlertDialog.Builder(this).setTitle("Game exited").setMessage(stringExtra + "\n\nShare latestlog.txt so the crash can be checked?").setPositiveButton(R.string.button_share_latest_log, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.ErrorActivity$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                ErrorActivity.this.lambda$onCreate$0(dialogInterface, i2);
            }
        }).setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.ErrorActivity$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                ErrorActivity.this.lambda$onCreate$1(dialogInterface, i2);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(DialogInterface dialogInterface, int i) {
        LauncherLogManager.shareLatestLog(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(DialogInterface dialogInterface, int i) {
        finish();
    }
}

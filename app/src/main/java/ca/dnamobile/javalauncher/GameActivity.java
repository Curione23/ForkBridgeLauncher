package ca.dnamobile.javalauncher;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.system.Os;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.core.view.InputDeviceCompat;
import ca.dnamobile.javalauncher.controls.ControlsActivity;
import ca.dnamobile.javalauncher.controls.ControlsEditorActivity;
import ca.dnamobile.javalauncher.controls.ControlsPreferences;
import ca.dnamobile.javalauncher.controls.TouchControlsOverlay;
import ca.dnamobile.javalauncher.data.AccountStore;
import ca.dnamobile.javalauncher.databinding.ActivityGameBinding;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.game.FloatingGameSettingsOverlayController;
import ca.dnamobile.javalauncher.input.GameCursorOverlay;
import ca.dnamobile.javalauncher.input.GamepadInputController;
import ca.dnamobile.javalauncher.input.GamepadMappingDialog;
import ca.dnamobile.javalauncher.launcher.LaunchGame;
import ca.dnamobile.javalauncher.logs.LauncherLogManager;
import ca.dnamobile.javalauncher.modcompat.ControlifySDL;
import ca.dnamobile.javalauncher.modcompat.ControllerModCompat;
import ca.dnamobile.javalauncher.renderer.DriverPluginManager;
import ca.dnamobile.javalauncher.renderer.RendererInterface;
import ca.dnamobile.javalauncher.renderer.Renderers;
import ca.dnamobile.javalauncher.settings.LauncherPreferences;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.MinecraftGLSurface;
import org.libsdl.app.SDLControllerManager;
import org.lwjgl.glfw.CallbackBridge;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public class GameActivity extends AppCompatActivity {
    public static final String EXTRA_QUICK_PLAY_WORLD = "ca.dnamobile.javalauncher.extra.QUICK_PLAY_WORLD";
    public static final String EXTRA_VERSION_ID = "ca.dnamobile.javalauncher.extra.VERSION_ID";
    private ActivityGameBinding binding;
    private boolean exiting;
    private FloatingGameSettingsOverlayController floatingGameSettingsOverlayController;
    private GameCursorOverlay gameCursorOverlay;
    private GamepadInputController gamepadInputController;
    private AlertDialog inGameControlsDialog;
    private long lastLegacy4jFallbackProbeMs;
    private boolean launchStarted;
    private boolean legacy4jFallbackLogged;
    private boolean legacy4jGlfwFallbackAllowed;
    private Handler logOverlayHandler;
    private Runnable logOverlayRunnable;
    private String quickPlayWorld;
    private boolean quitWatchdogForceScheduled;
    private Handler quitWatchdogHandler;
    private Runnable quitWatchdogRunnable;
    private TouchControlsOverlay touchControlsOverlay;
    private String versionId;
    private static final int COLOR_DIALOG_BG = Color.rgb(30, 34, 42);
    private static final int COLOR_CARD_BG = Color.rgb(38, 43, 53);
    private static final int COLOR_CARD_BG_PRESSED = Color.rgb(45, 51, 63);
    private static final int COLOR_CARD_STROKE = Color.rgb(54, 61, 74);
    private static final int COLOR_TEXT_PRIMARY = Color.rgb(238, 241, 248);
    private static final int COLOR_TEXT_SECONDARY = Color.rgb(198, 204, 216);
    private static final int COLOR_TEXT_MUTED = Color.rgb(150, 159, 176);
    private static final int COLOR_ACCENT = Color.rgb(37, 211, 128);
    private static final int COLOR_DANGER = Color.rgb(255, AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR, AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR);
    private long lastLogOverlayLength = -1;
    private long lastLogOverlayModified = -1;
    private final Handler surfaceRefreshHandler = new Handler(Looper.getMainLooper());

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PathManager.initContextConstants(this);
        prepareRendererEnvironmentBeforeBridgeLoad();
        CallbackBridge.init(this);
        String stringExtra = getIntent().getStringExtra(EXTRA_VERSION_ID);
        this.versionId = stringExtra;
        if (stringExtra == null || stringExtra.trim().isEmpty()) {
            throw new IllegalStateException("No version id was provided to GameActivity.");
        }
        this.quickPlayWorld = getIntent().getStringExtra(EXTRA_QUICK_PLAY_WORLD);
        configureInputBridgeForVersion(this.versionId);
        CallbackBridge.setInputReady(true);
        ActivityGameBinding activityGameBindingInflate = ActivityGameBinding.inflate(getLayoutInflater());
        this.binding = activityGameBindingInflate;
        setContentView(activityGameBindingInflate.getRoot());
        applyGameDisplaySurfaceOptions();
        installCursorOverlay();
        installTouchControlsOverlay();
        configureLogOverlay();
        configureInGameSettingsButton();
        startQuitWatchdog();
        this.gamepadInputController = new GamepadInputController(this.binding.getRoot(), new GamepadInputController.MappingRequestListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda22
            @Override // ca.dnamobile.javalauncher.input.GamepadInputController.MappingRequestListener
            public final void onRequestControllerMapping() {
                GameActivity.this.lambda$onCreate$0();
            }
        });
        configureWindow();
        lambda$startLaunchOnce$27(getString(R.string.game_surface_waiting));
        this.binding.minecraftSurface.setOnRenderingStartedListener(new MinecraftGLSurface.OnRenderingStartedListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda28
            public final void isStarted() {
                GameActivity.this.lambda$onCreate$2();
            }
        });
        this.binding.minecraftSurface.setSurfaceReadyListener(new MinecraftGLSurface.SurfaceReadyListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda29
            public final void isReady() {
                GameActivity.this.startLaunchOnce();
            }
        });
        this.binding.minecraftSurface.start(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0() {
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda30
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.openInGameButtonOverlay();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2() {
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda25
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$onCreate$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1() {
        CallbackBridge.setInputReady(true);
        CallbackBridge.ensureInputFocus();
        lambda$startLaunchOnce$27(getString(R.string.game_status_rendering));
        this.binding.textStatus.setVisibility(8);
    }

    private void scheduleMinecraftSurfaceRefresh() {
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding == null || activityGameBinding.minecraftSurface == null) {
            return;
        }
        this.surfaceRefreshHandler.removeCallbacksAndMessages(null);
        Runnable runnable = new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$scheduleMinecraftSurfaceRefresh$3();
            }
        };
        this.binding.minecraftSurface.post(runnable);
        this.surfaceRefreshHandler.postDelayed(runnable, 120L);
        this.surfaceRefreshHandler.postDelayed(runnable, 350L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scheduleMinecraftSurfaceRefresh$3() {
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding == null || activityGameBinding.minecraftSurface == null || this.exiting) {
            return;
        }
        applyGameDisplaySurfaceOptions();
        this.binding.minecraftSurface.refreshSize();
        this.binding.minecraftSurface.requestLayout();
        this.binding.minecraftSurface.invalidate();
    }

    private void prepareRendererEnvironmentBeforeBridgeLoad() {
        try {
            Renderers.reload(this);
            RendererInterface selectedRenderer = Renderers.getSelectedRenderer(this);
            clearEarlyRendererEnvAliases();
            setEarlyEnv("POJAV_RENDERER", resolveBridgeRendererId(selectedRenderer));
            for (Map.Entry<String, String> entry : selectedRenderer.getRendererEnv().entrySet()) {
                setEarlyEnv(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry2 : DriverPluginManager.buildEnvironment(this, selectedRenderer).entrySet()) {
                setEarlyEnv(entry2.getKey(), entry2.getValue());
            }
            String strSanitizeLibraryName = sanitizeLibraryName(selectedRenderer.getRendererEGL());
            if (isMobileGluesRenderer(selectedRenderer)) {
                strSanitizeLibraryName = "libmobileglues.so";
            } else if (strSanitizeLibraryName.isEmpty()) {
                strSanitizeLibraryName = inferPojavExecEgl(selectedRenderer);
            }
            if (!strSanitizeLibraryName.isEmpty()) {
                setEarlyEnv("POJAVEXEC_EGL", strSanitizeLibraryName);
            }
            applyEarlyRendererBridgeAliases(selectedRenderer, strSanitizeLibraryName);
            Logging.i("GameActivity", "Early renderer env prepared for " + selectedRenderer.getRendererName() + " rendererId=" + resolveBridgeRendererId(selectedRenderer) + " POJAVEXEC_EGL=" + strSanitizeLibraryName);
        } catch (Throwable th) {
            Logging.e("GameActivity", "Failed to prepare early renderer env", th);
        }
    }

    private void applyEarlyRendererBridgeAliases(RendererInterface rendererInterface, String str) {
        String strSanitizeLibraryName = sanitizeLibraryName(rendererInterface.getRendererLibrary());
        String strRendererIdentity = rendererIdentity(rendererInterface);
        if (isLtwRenderer(rendererInterface)) {
            setEarlyEnv("POJAV_RENDERER", "opengles3_ltw");
            setEarlyEnv("POJAVEXEC_EGL", "libltw.so");
            setEarlyEnv("POJAV_EGL_LIBRARY", "libltw.so");
            setEarlyEnv("POJAVEXEC_EGL_LIBRARY", "libltw.so");
            setEarlyEnv("POJAV_RENDERER_LIBRARY", "libltw.so");
            setEarlyEnv("POJAVEXEC_RENDERER", "libltw.so");
            setEarlyEnv("LIBGL_ES", "3");
            setEarlyEnv("POJAV_USE_SYSTEM_VULKAN", "1");
            setEarlyEnv("DRIVER_PATH", "");
            setEarlyEnv("VK_ICD_FILENAMES", "");
            setEarlyEnv("VK_DRIVER_FILES", "");
            setEarlyEnv("LIBGL_DRIVERS_PATH", "");
            setEarlyEnv("EGL_DRIVERS_PATH", "");
            setEarlyEnv("OSMESA_LIB", "");
            setEarlyEnv("GALLIUM_DRIVER", "");
            setEarlyEnv("MESA_LOADER_DRIVER_OVERRIDE", "");
            setEarlyEnv("LTW_NEVER_FLUSH_BUFFERS", "0");
            setEarlyEnv("LTW_COHERENT_DYNAMIC_STORAGE", "0");
            return;
        }
        if (isMobileGluesRenderer(rendererInterface)) {
            setEarlyEnv("POJAV_RENDERER", "mobileglues");
            setEarlyEnv("POJAV_RENDERER_LIBRARY", "libmobileglues.so");
            setEarlyEnv("POJAVEXEC_RENDERER", "libmobileglues.so");
            setEarlyEnv("POJAVEXEC_EGL", "libmobileglues.so");
            setEarlyEnv("POJAV_EGL_LIBRARY", "libmobileglues.so");
            setEarlyEnv("POJAVEXEC_EGL_LIBRARY", "libmobileglues.so");
            setEarlyEnv("OSMESA_LIB", "");
            setEarlyEnv("GALLIUM_DRIVER", "");
            setEarlyEnv("MESA_LOADER_DRIVER_OVERRIDE", "");
            setEarlyEnv("LIBGL_ES", "");
            return;
        }
        if (!strSanitizeLibraryName.isEmpty()) {
            setEarlyEnv("POJAV_RENDERER_LIBRARY", strSanitizeLibraryName);
            setEarlyEnv("POJAVEXEC_RENDERER", strSanitizeLibraryName);
            setEarlyEnv("OSMESA_LIB", strSanitizeLibraryName);
        }
        if (!str.isEmpty()) {
            setEarlyEnv("POJAV_EGL_LIBRARY", str);
            setEarlyEnv("POJAVEXEC_EGL_LIBRARY", str);
        }
        if (strRendererIdentity.contains("zink") || strRendererIdentity.contains("osmesa")) {
            setEarlyEnv("POJAV_RENDERER", "vulkan_zink");
            if (strSanitizeLibraryName.isEmpty()) {
                strSanitizeLibraryName = "libOSMesa_8.so";
            }
            setEarlyEnv("POJAVEXEC_EGL", strSanitizeLibraryName);
            setEarlyEnv("LIBGL_ES", "3");
            setEarlyEnv("MESA_LOADER_DRIVER_OVERRIDE", "zink");
            setEarlyEnv("GALLIUM_DRIVER", "zink");
        }
    }

    private void clearEarlyRendererEnvAliases() {
        setEarlyEnv("POJAV_RENDERER", "");
        setEarlyEnv("POJAV_RENDERER_LIBRARY", "");
        setEarlyEnv("POJAVEXEC_RENDERER", "");
        setEarlyEnv("POJAVEXEC_EGL", "");
        setEarlyEnv("POJAV_EGL_LIBRARY", "");
        setEarlyEnv("POJAVEXEC_EGL_LIBRARY", "");
        setEarlyEnv("OSMESA_LIB", "");
        setEarlyEnv("GALLIUM_DRIVER", "");
        setEarlyEnv("MESA_LOADER_DRIVER_OVERRIDE", "");
        setEarlyEnv("LIBGL_ES", "");
        setEarlyEnv("POJAV_USE_SYSTEM_VULKAN", "");
        setEarlyEnv("DRIVER_PATH", "");
        setEarlyEnv("VK_ICD_FILENAMES", "");
        setEarlyEnv("VK_DRIVER_FILES", "");
        setEarlyEnv("LIBGL_DRIVERS_PATH", "");
        setEarlyEnv("EGL_DRIVERS_PATH", "");
        setEarlyEnv("LTW_NEVER_FLUSH_BUFFERS", "");
        setEarlyEnv("LTW_COHERENT_DYNAMIC_STORAGE", "");
    }

    private void setEarlyEnv(String str, String str2) {
        if (str2 == null) {
            return;
        }
        try {
            if (str2.isEmpty()) {
                Os.unsetenv(str);
            } else {
                Os.setenv(str, str2, true);
            }
        } catch (Throwable th) {
            Logging.e("GameActivity", "Failed to update early env " + str, th);
        }
    }

    private String inferPojavExecEgl(RendererInterface rendererInterface) {
        String strRendererIdentity = rendererIdentity(rendererInterface);
        if (isLtwRenderer(rendererInterface)) {
            return "libltw.so";
        }
        if (isMobileGluesRenderer(rendererInterface)) {
            return "libmobileglues.so";
        }
        if (strRendererIdentity.contains("gl4es") || strRendererIdentity.contains("opengles") || strRendererIdentity.contains("krypton") || strRendererIdentity.contains("ng_gl4es")) {
            return "libEGL.so";
        }
        if (strRendererIdentity.contains("osmesa") || strRendererIdentity.contains("zink") || strRendererIdentity.contains("mesa") || strRendererIdentity.contains("virgl") || strRendererIdentity.contains("freedreno") || strRendererIdentity.contains("panfrost")) {
            return sanitizeLibraryName(rendererInterface.getRendererLibrary());
        }
        return "";
    }

    private String resolveBridgeRendererId(RendererInterface rendererInterface) {
        return isLtwRenderer(rendererInterface) ? "opengles3_ltw" : isMobileGluesRenderer(rendererInterface) ? "mobileglues" : rendererInterface.getRendererId();
    }

    private boolean isLtwRenderer(RendererInterface rendererInterface) {
        String strRendererIdentity = rendererIdentity(rendererInterface);
        return strRendererIdentity.contains("ltw") || strRendererIdentity.contains("libltw.so");
    }

    private boolean isMobileGluesRenderer(RendererInterface rendererInterface) {
        String strRendererIdentity = rendererIdentity(rendererInterface);
        return strRendererIdentity.contains("mobileglues") || strRendererIdentity.contains("mobile glues") || strRendererIdentity.contains("com.fcl.plugin.mobileglues");
    }

    private String rendererIdentity(RendererInterface rendererInterface) {
        return safeLower(rendererInterface.getRendererId()) + " " + safeLower(rendererInterface.getRendererName()) + " " + safeLower(rendererInterface.getRendererLibrary()) + " " + safeLower(rendererInterface.getRendererEGL()) + " " + safeLower(rendererInterface.getUniqueIdentifier());
    }

    private String safeLower(String str) {
        return str == null ? "" : str.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeLibraryName(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        if (strTrim.isEmpty() || "null".equalsIgnoreCase(strTrim) || "(null)".equalsIgnoreCase(strTrim)) {
            return "";
        }
        return new File(strTrim).getName();
    }

    private void configureInputBridgeForVersion(String str) {
        boolean zShouldUseInputStackQueue = shouldUseInputStackQueue(str);
        CallbackBridge.setUseInputStackQueue(zShouldUseInputStackQueue);
        Logging.i("GameActivity", "Input stack queue for " + str + " = " + zShouldUseInputStackQueue);
    }

    private static boolean shouldUseInputStackQueue(String str) {
        String lowerCase = str.toLowerCase();
        if (lowerCase.startsWith("26") || lowerCase.contains("snapshot") || lowerCase.contains("pre") || lowerCase.contains("rc") || !lowerCase.startsWith("1.")) {
            return true;
        }
        String[] strArrSplit = lowerCase.split("[^0-9]+");
        if (strArrSplit.length >= 2) {
            try {
                int i = Integer.parseInt(strArrSplit[0]);
                int i2 = Integer.parseInt(strArrSplit[1]);
                if (i <= 1) {
                    return i == 1 && i2 >= 13;
                }
                return true;
            } catch (NumberFormatException unused) {
            }
        }
        return false;
    }

    private void installTouchControlsOverlay() {
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding == null) {
            return;
        }
        FrameLayout root = activityGameBinding.getRoot();
        if (root instanceof ViewGroup) {
            TouchControlsOverlay touchControlsOverlay = new TouchControlsOverlay(this);
            this.touchControlsOverlay = touchControlsOverlay;
            touchControlsOverlay.setAppMenuListener(new TouchControlsOverlay.AppMenuListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda6
                @Override // ca.dnamobile.javalauncher.controls.TouchControlsOverlay.AppMenuListener
                public final void onTouchControlsMenuRequested() {
                    GameActivity.this.openInGameButtonOverlay();
                }
            });
            this.touchControlsOverlay.loadSelectedLayout();
            this.touchControlsOverlay.setControlsVisible(ControlsPreferences.isTouchControlsEnabled(this));
            root.addView(this.touchControlsOverlay, new ViewGroup.LayoutParams(-1, -1));
        }
    }

    private void refreshTouchControlsOverlay() {
        TouchControlsOverlay touchControlsOverlay = this.touchControlsOverlay;
        if (touchControlsOverlay == null) {
            return;
        }
        touchControlsOverlay.loadSelectedLayout();
        this.touchControlsOverlay.setControlsVisible(ControlsPreferences.isTouchControlsEnabled(this));
        this.touchControlsOverlay.bringToFront();
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding != null && activityGameBinding.layoutLogOverlay.getVisibility() == 0) {
            this.binding.layoutLogOverlay.bringToFront();
        }
        FloatingGameSettingsOverlayController floatingGameSettingsOverlayController = this.floatingGameSettingsOverlayController;
        if (floatingGameSettingsOverlayController != null) {
            floatingGameSettingsOverlayController.bringToFront();
            return;
        }
        ActivityGameBinding activityGameBinding2 = this.binding;
        if (activityGameBinding2 == null || activityGameBinding2.buttonGameSettings.getVisibility() != 0) {
            return;
        }
        this.binding.buttonGameSettings.bringToFront();
    }

    private void installCursorOverlay() {
        final FrameLayout root = this.binding.getRoot();
        if (root instanceof ViewGroup) {
            GameCursorOverlay gameCursorOverlay = new GameCursorOverlay(this);
            this.gameCursorOverlay = gameCursorOverlay;
            root.addView(gameCursorOverlay, new ViewGroup.LayoutParams(-1, -1));
            root.post(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda17
                @Override // java.lang.Runnable
                public final void run() {
                    GameActivity.lambda$installCursorOverlay$4(root);
                }
            });
        }
    }

    static /* synthetic */ void lambda$installCursorOverlay$4(View view) {
        if (CallbackBridge.windowWidth <= 1) {
            CallbackBridge.windowWidth = Math.max(1, view.getWidth());
        }
        if (CallbackBridge.windowHeight <= 1) {
            CallbackBridge.windowHeight = Math.max(1, view.getHeight());
        }
        if (CallbackBridge.physicalWidth <= 1) {
            CallbackBridge.physicalWidth = Math.max(1, view.getWidth());
        }
        if (CallbackBridge.physicalHeight <= 1) {
            CallbackBridge.physicalHeight = Math.max(1, view.getHeight());
        }
        CallbackBridge.setInputReady(true);
        CallbackBridge.ensureInputFocus();
        if (CallbackBridge.mouseX == 0.0f && CallbackBridge.mouseY == 0.0f) {
            CallbackBridge.sendCursorPos(Math.max(1, CallbackBridge.windowWidth) / 2.0f, Math.max(1, CallbackBridge.windowHeight) / 2.0f);
        }
    }

    private void configureLogOverlay() {
        if (this.binding == null) {
            return;
        }
        stopLogOverlayTicker();
        boolean zIsShowGameLogOverlay = LauncherPreferences.isShowGameLogOverlay(this);
        this.binding.layoutLogOverlay.setVisibility(zIsShowGameLogOverlay ? 0 : 8);
        this.binding.layoutLogOverlay.setEnabled(false);
        this.binding.scrollLogOverlay.setEnabled(false);
        this.binding.textLogOverlay.setEnabled(false);
        if (zIsShowGameLogOverlay) {
            this.binding.layoutLogOverlay.bringToFront();
            this.binding.layoutLogOverlay.setElevation(9000.0f);
            this.binding.layoutLogOverlay.setTranslationZ(9000.0f);
            this.binding.layoutLogOverlay.setAlpha(1.0f);
            final FrameLayout root = this.binding.getRoot();
            root.post(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda27
                @Override // java.lang.Runnable
                public final void run() {
                    GameActivity.this.lambda$configureLogOverlay$5(root);
                }
            });
            if (this.binding.textLogOverlay.length() == 0) {
                this.binding.textLogOverlay.setText("Waiting for log output...");
            }
            this.logOverlayHandler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity.1
                @Override // java.lang.Runnable
                public void run() {
                    GameActivity.this.refreshLogOverlay();
                    if (GameActivity.this.exiting || GameActivity.this.binding == null || GameActivity.this.logOverlayHandler == null) {
                        return;
                    }
                    GameActivity.this.logOverlayHandler.postDelayed(this, 500L);
                }
            };
            this.logOverlayRunnable = runnable;
            this.logOverlayHandler.post(runnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$configureLogOverlay$5(View view) {
        if (this.binding == null) {
            return;
        }
        int iDpToPx = dpToPx(TypedValues.CycleType.TYPE_EASING);
        int iMax = Math.max(dpToPx(260), (int) (view.getWidth() * 0.38f));
        ViewGroup.LayoutParams layoutParams = this.binding.layoutLogOverlay.getLayoutParams();
        layoutParams.width = Math.min(iDpToPx, iMax);
        this.binding.layoutLogOverlay.setLayoutParams(layoutParams);
        this.binding.layoutLogOverlay.bringToFront();
        if (this.binding.buttonGameSettings.getVisibility() == 0) {
            this.binding.buttonGameSettings.bringToFront();
        }
    }

    private void stopLogOverlayTicker() {
        Runnable runnable;
        Handler handler = this.logOverlayHandler;
        if (handler != null && (runnable = this.logOverlayRunnable) != null) {
            handler.removeCallbacks(runnable);
        }
        this.logOverlayRunnable = null;
        this.logOverlayHandler = null;
    }

    private void configureInGameSettingsButton() {
        if (this.binding == null) {
            return;
        }
        if (this.floatingGameSettingsOverlayController == null) {
            FloatingGameSettingsOverlayController floatingGameSettingsOverlayController = new FloatingGameSettingsOverlayController(this, this.binding.buttonGameSettings);
            this.floatingGameSettingsOverlayController = floatingGameSettingsOverlayController;
            floatingGameSettingsOverlayController.attach();
        }
        this.binding.buttonGameSettings.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                GameActivity.this.lambda$configureInGameSettingsButton$6(view);
            }
        });
        this.floatingGameSettingsOverlayController.refreshFromPreferences();
        this.floatingGameSettingsOverlayController.bringToFront();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$configureInGameSettingsButton$6(View view) {
        openInGameButtonOverlay();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openInGameButtonOverlay() {
        String str;
        if (this.binding == null || this.exiting) {
            return;
        }
        AlertDialog alertDialog = this.inGameControlsDialog;
        if (alertDialog != null && alertDialog.isShowing()) {
            configureWindow();
            return;
        }
        boolean zIsTouchControlsEnabled = ControlsPreferences.isTouchControlsEnabled(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setBackgroundColor(COLOR_DIALOG_BG);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dpToPx(20), dpToPx(18), dpToPx(20), dpToPx(10));
        scrollView.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2));
        TextView textView = new TextView(this);
        textView.setText("In-game controls");
        textView.setTextColor(COLOR_TEXT_PRIMARY);
        textView.setTextSize(24.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setPadding(0, 0, 0, dpToPx(4));
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText("Quick controls, overlay settings, and recovery actions while Minecraft is running.");
        textView2.setTextColor(COLOR_TEXT_SECONDARY);
        textView2.setTextSize(13.0f);
        textView2.setPadding(0, 0, 0, dpToPx(12));
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(-1, -2));
        final AlertDialog[] alertDialogArr = new AlertDialog[1];
        linearLayout.addView(buildInGameDialogAction("Controller / gamepad overlay", "Edit controller mappings, cursor behavior, FPS, log overlay, and floating button placement.", false, new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$openInGameButtonOverlay$9(alertDialogArr);
            }
        }));
        String str2 = zIsTouchControlsEnabled ? "Hide touch controls" : "Show touch controls";
        if (zIsTouchControlsEnabled) {
            str = "Temporarily hides the active touch layout without leaving the game.";
        } else {
            str = "Shows the selected touch layout again.";
        }
        linearLayout.addView(buildInGameDialogAction(str2, str, false, new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$openInGameButtonOverlay$10(alertDialogArr);
            }
        }));
        linearLayout.addView(buildInGameDialogAction("Edit touch controls", "Open the touch layout editor for the current control layout.", false, new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$openInGameButtonOverlay$11(alertDialogArr);
            }
        }));
        linearLayout.addView(buildInGameDialogAction("Manage / import touch layouts", "Choose, import, or manage touch control layouts.", false, new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$openInGameButtonOverlay$12(alertDialogArr);
            }
        }));
        TextView textView3 = new TextView(this);
        textView3.setText("Recovery");
        textView3.setTextColor(COLOR_TEXT_MUTED);
        textView3.setTextSize(12.0f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setPadding(dpToPx(2), dpToPx(8), 0, dpToPx(6));
        linearLayout.addView(textView3, new LinearLayout.LayoutParams(-1, -2));
        linearLayout.addView(buildInGameDialogAction("Force close game", "Use this if Minecraft is frozen, crashed, or will not return to the launcher normally.", true, new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$openInGameButtonOverlay$13(alertDialogArr);
            }
        }));
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setView(scrollView).create();
        alertDialogArr[0] = alertDialogCreate;
        this.inGameControlsDialog = alertDialogCreate;
        alertDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda16
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                GameActivity.this.lambda$openInGameButtonOverlay$14(alertDialogCreate, dialogInterface);
            }
        });
        alertDialogCreate.show();
        styleDarkDialog(alertDialogCreate);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openInGameButtonOverlay$9(AlertDialog[] alertDialogArr) {
        dismissDialog(alertDialogArr[0]);
        GamepadMappingDialog.show(this, new GamepadMappingDialog.OnSettingsSavedListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda34
            @Override // ca.dnamobile.javalauncher.input.GamepadMappingDialog.OnSettingsSavedListener
            public final void onSettingsSaved() {
                GameActivity.this.lambda$openInGameButtonOverlay$8();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openInGameButtonOverlay$8() {
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda26
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$openInGameButtonOverlay$7();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openInGameButtonOverlay$7() {
        applyInGameOverlayPreferences();
        reloadTouchControlsLayout();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openInGameButtonOverlay$10(AlertDialog[] alertDialogArr) {
        dismissDialog(alertDialogArr[0]);
        boolean z = !ControlsPreferences.isTouchControlsEnabled(this);
        ControlsPreferences.setTouchControlsEnabled(this, z);
        TouchControlsOverlay touchControlsOverlay = this.touchControlsOverlay;
        if (touchControlsOverlay != null) {
            touchControlsOverlay.setControlsVisible(z);
            this.touchControlsOverlay.bringToFront();
        }
        applyInGameOverlayPreferences();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openInGameButtonOverlay$11(AlertDialog[] alertDialogArr) {
        dismissDialog(alertDialogArr[0]);
        startActivity(new Intent(this, (Class<?>) ControlsEditorActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openInGameButtonOverlay$12(AlertDialog[] alertDialogArr) {
        dismissDialog(alertDialogArr[0]);
        startActivity(new Intent(this, (Class<?>) ControlsActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openInGameButtonOverlay$13(AlertDialog[] alertDialogArr) {
        dismissDialog(alertDialogArr[0]);
        showForceCloseGameDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openInGameButtonOverlay$14(AlertDialog alertDialog, DialogInterface dialogInterface) {
        if (this.inGameControlsDialog == alertDialog) {
            this.inGameControlsDialog = null;
        }
        configureWindow();
    }

    private View buildInGameDialogAction(String str, String str2, boolean z, final Runnable runnable) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        linearLayout.setBackground(roundedDrawable(COLOR_CARD_BG, COLOR_CARD_STROKE, 18));
        linearLayout.setClickable(true);
        linearLayout.setFocusable(true);
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                runnable.run();
            }
        });
        linearLayout.setOnTouchListener(new View.OnTouchListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return GameActivity.this.lambda$buildInGameDialogAction$16(view, motionEvent);
            }
        });
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(z ? COLOR_DANGER : COLOR_TEXT_PRIMARY);
        textView.setTextSize(16.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText(str2);
        textView2.setTextColor(COLOR_TEXT_SECONDARY);
        textView2.setTextSize(12.5f);
        textView2.setPadding(0, dpToPx(3), 0, 0);
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(0, 0, 0, dpToPx(10));
        linearLayout.setLayoutParams(layoutParams);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$buildInGameDialogAction$16(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            view.setBackground(roundedDrawable(COLOR_CARD_BG_PRESSED, COLOR_CARD_STROKE, 18));
            return false;
        }
        if (motionEvent.getAction() != 1 && motionEvent.getAction() != 3) {
            return false;
        }
        view.setBackground(roundedDrawable(COLOR_CARD_BG, COLOR_CARD_STROKE, 18));
        return false;
    }

    private void showForceCloseGameDialog() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dpToPx(22), dpToPx(18), dpToPx(22), 0);
        linearLayout.setBackgroundColor(COLOR_DIALOG_BG);
        TextView textView = new TextView(this);
        textView.setText("Force close game?");
        textView.setTextColor(COLOR_TEXT_PRIMARY);
        textView.setTextSize(22.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText("This should only be used when Minecraft is frozen, crashed, or will not close normally. The launcher will save the latest log if log history is enabled, reset launcher-side game state, and close the game process.");
        textView2.setTextColor(COLOR_TEXT_SECONDARY);
        textView2.setTextSize(14.0f);
        textView2.setPadding(0, dpToPx(10), 0, dpToPx(2));
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(-1, -2));
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setView(linearLayout).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Force close", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda32
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                GameActivity.this.lambda$showForceCloseGameDialog$18(alertDialogCreate, dialogInterface);
            }
        });
        alertDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda33
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                GameActivity.this.lambda$showForceCloseGameDialog$19(dialogInterface);
            }
        });
        alertDialogCreate.show();
        styleDarkDialog(alertDialogCreate);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showForceCloseGameDialog$18(final AlertDialog alertDialog, DialogInterface dialogInterface) {
        alertDialog.getButton(-2).setTextColor(COLOR_ACCENT);
        alertDialog.getButton(-1).setTextColor(COLOR_DANGER);
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                GameActivity.this.lambda$showForceCloseGameDialog$17(alertDialog, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showForceCloseGameDialog$17(AlertDialog alertDialog, View view) {
        alertDialog.dismiss();
        forceCloseGameAndReturnToLauncher("User requested force close from in-game controls");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showForceCloseGameDialog$19(DialogInterface dialogInterface) {
        configureWindow();
    }

    private void forceCloseGameAndReturnToLauncher(String str) {
        Runnable runnable;
        if (this.exiting) {
            return;
        }
        this.exiting = true;
        try {
            LauncherLogManager.append("ForceClose: " + str);
            LauncherLogManager.preserveLatestLogIfEnabled(this, this.versionId);
        } catch (Throwable th) {
            Logging.e("GameActivity", "Failed to preserve latest log during force close", th);
        }
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$forceCloseGameAndReturnToLauncher$20();
            }
        });
        stopLogOverlayTicker();
        Handler handler = this.quitWatchdogHandler;
        if (handler != null && (runnable = this.quitWatchdogRunnable) != null) {
            handler.removeCallbacks(runnable);
        }
        this.quitWatchdogRunnable = null;
        this.quitWatchdogHandler = null;
        try {
            CallbackBridge.clearInputFocus();
            CallbackBridge.setInputReady(false);
        } catch (Throwable unused) {
        }
        FloatingGameSettingsOverlayController floatingGameSettingsOverlayController = this.floatingGameSettingsOverlayController;
        if (floatingGameSettingsOverlayController != null) {
            floatingGameSettingsOverlayController.pause();
        }
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding != null) {
            try {
                MinecraftGLSurface minecraftGLSurface = activityGameBinding.minecraftSurface;
                minecraftGLSurface.setSurfaceReadyListener((MinecraftGLSurface.SurfaceReadyListener) null);
                minecraftGLSurface.setOnRenderingStartedListener((MinecraftGLSurface.OnRenderingStartedListener) null);
            } catch (Throwable unused2) {
            }
        }
        ControlifySDL.reset();
        ControllerModCompat.reset();
        LaunchGame.resetLaunchState();
        try {
            Intent intent = new Intent(this, (Class<?>) MainActivity.class);
            intent.addFlags(872415232);
            startActivity(intent);
        } catch (Throwable th2) {
            Logging.e("GameActivity", "Failed to reopen launcher after force close", th2);
        }
        finishAndRemoveTask();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.lambda$forceCloseGameAndReturnToLauncher$21();
            }
        }, 250L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$forceCloseGameAndReturnToLauncher$20() {
        lambda$startLaunchOnce$27("Force closing Minecraft...");
    }

    static /* synthetic */ void lambda$forceCloseGameAndReturnToLauncher$21() {
        LauncherLogManager.append("ForceClose: killing game process.");
        Process.killProcess(Process.myPid());
    }

    private void dismissDialog(AlertDialog alertDialog) {
        if (alertDialog == null) {
            return;
        }
        try {
            alertDialog.dismiss();
        } catch (Throwable unused) {
        }
    }

    private void styleDarkDialog(AlertDialog alertDialog) {
        if (alertDialog.getWindow() == null) {
            return;
        }
        Window window = alertDialog.getWindow();
        int i = COLOR_DIALOG_BG;
        window.setBackgroundDrawable(roundedDrawable(i, i, 24));
        alertDialog.getWindow().setDimAmount(0.58f);
    }

    private GradientDrawable roundedDrawable(int i, int i2, int i3) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dpToPx(i3));
        gradientDrawable.setStroke(Math.max(1, dpToPx(1)), i2);
        return gradientDrawable;
    }

    private void applyInGameOverlayPreferences() {
        applyGameDisplaySurfaceOptions();
        configureLogOverlay();
        configureInGameSettingsButton();
        configureWindow();
        scheduleMinecraftSurfaceRefresh();
    }

    private void reloadTouchControlsLayout() {
        TouchControlsOverlay touchControlsOverlay = this.touchControlsOverlay;
        if (touchControlsOverlay == null) {
            return;
        }
        touchControlsOverlay.loadSelectedLayout();
        this.touchControlsOverlay.setControlsVisible(ControlsPreferences.isTouchControlsEnabled(this));
        this.touchControlsOverlay.requestLayout();
        this.touchControlsOverlay.invalidate();
        this.touchControlsOverlay.bringToFront();
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding != null && activityGameBinding.layoutLogOverlay.getVisibility() == 0) {
            this.binding.layoutLogOverlay.bringToFront();
        }
        FloatingGameSettingsOverlayController floatingGameSettingsOverlayController = this.floatingGameSettingsOverlayController;
        if (floatingGameSettingsOverlayController != null) {
            floatingGameSettingsOverlayController.bringToFront();
            return;
        }
        ActivityGameBinding activityGameBinding2 = this.binding;
        if (activityGameBinding2 == null || activityGameBinding2.buttonGameSettings.getVisibility() != 0) {
            return;
        }
        this.binding.buttonGameSettings.bringToFront();
    }

    private int dpToPx(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshLogOverlay() {
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding == null || activityGameBinding.layoutLogOverlay.getVisibility() != 0) {
            return;
        }
        File readableLatestLogFile = getReadableLatestLogFile();
        if (readableLatestLogFile == null || !readableLatestLogFile.isFile()) {
            this.binding.textLogOverlay.setText("Waiting for latestlog.txt...");
            return;
        }
        long length = readableLatestLogFile.length();
        long jLastModified = readableLatestLogFile.lastModified();
        if (length == this.lastLogOverlayLength && jLastModified == this.lastLogOverlayModified) {
            return;
        }
        this.lastLogOverlayLength = length;
        this.lastLogOverlayModified = jLastModified;
        try {
            String logTail = readLogTail(readableLatestLogFile, 98304);
            if (logTail.trim().isEmpty()) {
                return;
            }
            this.binding.textLogOverlay.setText(logTail);
            this.binding.scrollLogOverlay.post(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda31
                @Override // java.lang.Runnable
                public final void run() {
                    GameActivity.this.lambda$refreshLogOverlay$22();
                }
            });
        } catch (Throwable th) {
            Logging.e("GameActivity", "Failed to refresh game log overlay", th);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshLogOverlay$22() {
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding != null) {
            activityGameBinding.scrollLogOverlay.fullScroll(130);
        }
    }

    private File getReadableLatestLogFile() {
        File currentLogFile = Logger.getCurrentLogFile();
        if (currentLogFile != null && currentLogFile.isFile()) {
            return currentLogFile;
        }
        try {
            File latestLogFile = LauncherLogManager.getLatestLogFile(this);
            if (latestLogFile.isFile()) {
                return latestLogFile;
            }
            return null;
        } catch (Throwable th) {
            Logging.e("GameActivity", "Failed to resolve latestlog.txt", th);
            return null;
        }
    }

    private String readLogTail(File file, int i) throws Exception {
        int iIndexOf;
        int i2;
        long length = file.length();
        long jMax = Math.max(0L, length - ((long) i));
        byte[] bArr = new byte[(int) Math.max(0L, length - jMax)];
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        try {
            randomAccessFile.seek(jMax);
            randomAccessFile.readFully(bArr);
            randomAccessFile.close();
            String strReplace = new String(bArr, StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n');
            if (jMax > 0 && (iIndexOf = strReplace.indexOf(10)) >= 0 && (i2 = iIndexOf + 1) < strReplace.length()) {
                strReplace = "...\n" + strReplace.substring(i2);
            }
            return trimToLastLines(strReplace, 280);
        } catch (Throwable th) {
            try {
                randomAccessFile.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private String trimToLastLines(String str, int i) {
        String[] strArrSplit = str.split("\n", -1);
        if (strArrSplit.length <= i) {
            return str;
        }
        StringBuilder sb = new StringBuilder("...\n");
        int length = strArrSplit.length - i;
        while (length < strArrSplit.length) {
            sb.append(strArrSplit[length]);
            length++;
            if (length < strArrSplit.length) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private void startQuitWatchdog() {
        if (isMinecraft26_2OrNewer(this.versionId)) {
            this.quitWatchdogHandler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity.2
                @Override // java.lang.Runnable
                public void run() {
                    GameActivity.this.pollQuitWatchdog();
                    if (GameActivity.this.exiting || GameActivity.this.binding == null || GameActivity.this.quitWatchdogHandler == null) {
                        return;
                    }
                    GameActivity.this.quitWatchdogHandler.postDelayed(this, 1000L);
                }
            };
            this.quitWatchdogRunnable = runnable;
            this.quitWatchdogHandler.postDelayed(runnable, 3000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pollQuitWatchdog() {
        if (this.exiting || this.quitWatchdogForceScheduled) {
            return;
        }
        try {
            File readableLatestLogFile = getReadableLatestLogFile();
            if (readableLatestLogFile != null && readableLatestLogFile.isFile()) {
                String logTail = readLogTail(readableLatestLogFile, 65536);
                boolean zContains = logTail.contains("Stopping!");
                boolean zContains2 = logTail.contains("EGLBridge: Terminating");
                if (zContains && zContains2) {
                    scheduleForcedGameProcessExit("Minecraft stop detected but JVM did not return");
                }
            }
        } catch (Throwable th) {
            Logging.e("GameActivity", "Quit watchdog failed", th);
        }
    }

    private void scheduleForcedGameProcessExit(String str) {
        if (this.quitWatchdogForceScheduled || this.exiting) {
            return;
        }
        this.quitWatchdogForceScheduled = true;
        LauncherLogManager.append("QuitWatchdog: " + str + "; closing game process soon.");
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$scheduleForcedGameProcessExit$23();
            }
        });
        if (this.quitWatchdogHandler == null) {
            this.quitWatchdogHandler = new Handler(Looper.getMainLooper());
        }
        this.quitWatchdogHandler.postDelayed(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$scheduleForcedGameProcessExit$24();
            }
        }, 2500L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scheduleForcedGameProcessExit$23() {
        lambda$startLaunchOnce$27("Minecraft quit detected; closing game process...");
        configureWindow();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scheduleForcedGameProcessExit$24() {
        if (this.exiting) {
            return;
        }
        LauncherLogManager.append("QuitWatchdog: forcing GameActivity/game process exit.");
        LauncherLogManager.preserveLatestLogIfEnabled(this, this.versionId);
        this.exiting = true;
        finishAndRemoveTask();
        Process.killProcess(Process.myPid());
    }

    private boolean isMinecraft26_2OrNewer(String str) {
        return str.toLowerCase(Locale.ROOT).trim().matches("^26\\.(2|[3-9]|[0-9]{2,}).*");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startLaunchOnce() {
        AccountStore.Account accountLoad;
        synchronized (this) {
            if (!this.launchStarted && !this.exiting) {
                this.launchStarted = true;
                configureInputBridgeForVersion(this.versionId);
                CallbackBridge.setInputReady(true);
                runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda18
                    @Override // java.lang.Runnable
                    public final void run() {
                        GameActivity.this.lambda$startLaunchOnce$25();
                    }
                });
                try {
                    try {
                        accountLoad = new AccountStore(this).load();
                    } catch (Throwable th) {
                        Logging.e("GameActivity", "Could not load saved account; using offline session", th);
                        accountLoad = null;
                    }
                    int iMax = Math.max(1, CallbackBridge.windowWidth);
                    int iMax2 = Math.max(1, CallbackBridge.windowHeight);
                    runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda19
                        @Override // java.lang.Runnable
                        public final void run() {
                            GameActivity.this.lambda$startLaunchOnce$26();
                        }
                    });
                    final int iRunGame = LaunchGame.runGame(this, this.versionId, accountLoad, iMax, iMax2, this.quickPlayWorld, new LaunchGame.StatusListener() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda20
                        @Override // ca.dnamobile.javalauncher.launcher.LaunchGame.StatusListener
                        public final void onStatus(String str) {
                            GameActivity.this.lambda$startLaunchOnce$28(str);
                        }
                    });
                    runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda21
                        @Override // java.lang.Runnable
                        public final void run() {
                            GameActivity.this.lambda$startLaunchOnce$29(iRunGame);
                        }
                    });
                } catch (Throwable th2) {
                    Logging.e("GameActivity", "Launch failed", th2);
                    runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda23
                        @Override // java.lang.Runnable
                        public final void run() {
                            GameActivity.this.lambda$startLaunchOnce$30(th2);
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startLaunchOnce$25() {
        lambda$startLaunchOnce$27(getString(R.string.game_status_surface_ready));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startLaunchOnce$26() {
        lambda$startLaunchOnce$27(getString(R.string.game_status_starting_minecraft));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startLaunchOnce$28(final String str) {
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.lambda$startLaunchOnce$27(str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startLaunchOnce$29(int i) {
        lambda$startLaunchOnce$27(getString(R.string.msg_launch_finished, new Object[]{Integer.valueOf(i)}));
        finishAfterExit();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startLaunchOnce$30(Throwable th) {
        this.binding.textStatus.setVisibility(0);
        lambda$startLaunchOnce$27(getString(R.string.msg_launch_failed, new Object[]{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: appendStatus, reason: merged with bridge method [inline-methods] */
    public void lambda$startLaunchOnce$27(String str) {
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding == null) {
            return;
        }
        CharSequence text = activityGameBinding.textStatus.getText();
        this.binding.textStatus.setText(((text == null || text.length() == 0) ? "" : ((Object) text) + "\n") + str);
    }

    private void finishAfterExit() {
        this.exiting = true;
        getWindow().getDecorView().postDelayed(new Runnable() { // from class: ca.dnamobile.javalauncher.GameActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                GameActivity.this.finish();
            }
        }, 1000L);
    }

    private void configureWindow() {
        getWindow().addFlags(128);
        View decorView = getWindow().getDecorView();
        if (LauncherPreferences.isForceFullscreenMode(this)) {
            decorView.setSystemUiVisibility(5894);
        } else {
            decorView.setSystemUiVisibility(256);
        }
    }

    private void applyGameDisplaySurfaceOptions() {
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding == null) {
            return;
        }
        FrameLayout root = activityGameBinding.getRoot();
        root.setClipToOutline(false);
        this.binding.minecraftSurface.setClipToOutline(false);
        if (root instanceof ViewGroup) {
            FrameLayout frameLayout = root;
            frameLayout.setClipChildren(false);
            frameLayout.setClipToPadding(false);
        }
        int iDpToPx = LauncherPreferences.isAvoidRoundedDisplayCorners(this) ? dpToPx(10) : 0;
        root.setPadding(iDpToPx, iDpToPx, iDpToPx, iDpToPx);
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        configureWindow();
        configureInputBridgeForVersion(this.versionId);
        CallbackBridge.setInputReady(true);
        CallbackBridge.ensureInputFocus();
        refreshTouchControlsOverlay();
        FloatingGameSettingsOverlayController floatingGameSettingsOverlayController = this.floatingGameSettingsOverlayController;
        if (floatingGameSettingsOverlayController != null) {
            floatingGameSettingsOverlayController.resume();
        }
        scheduleMinecraftSurfaceRefresh();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            configureWindow();
            scheduleMinecraftSurfaceRefresh();
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onPause() {
        FloatingGameSettingsOverlayController floatingGameSettingsOverlayController = this.floatingGameSettingsOverlayController;
        if (floatingGameSettingsOverlayController != null) {
            floatingGameSettingsOverlayController.pause();
        }
        CallbackBridge.clearInputFocus();
        super.onPause();
    }

    private boolean shouldRouteGamepadToMinecraftFallback() {
        boolean z = false;
        if (ControllerModCompat.shouldSuppressLauncherGamepadInput() && !MinecraftGLSurface.sdlEnabled) {
            if (!isLegacy4JGlfwFallbackLikelyActive()) {
                return false;
            }
            z = true;
            if (!this.legacy4jFallbackLogged) {
                this.legacy4jFallbackLogged = true;
                Logger.appendToLog("ControllerModCompat: allowing Legacy4J/GLFW fallback to receive gamepad input");
                Logging.i("GameActivity", "Allowing Legacy4J/GLFW fallback to receive gamepad input");
            }
        }
        return z;
    }

    private boolean shouldConsumeLauncherGamepadInput() {
        return ControllerModCompat.shouldSuppressLauncherGamepadInput() && !MinecraftGLSurface.sdlEnabled;
    }

    private boolean isLegacy4JGlfwFallbackLikelyActive() {
        File readableLatestLogFile;
        if (this.legacy4jGlfwFallbackAllowed) {
            return true;
        }
        String str = this.versionId;
        String lowerCase = str == null ? "" : str.toLowerCase(Locale.ROOT);
        if (lowerCase.contains("legacy4j") || lowerCase.equals("legacy") || lowerCase.contains("legacy")) {
            this.legacy4jGlfwFallbackAllowed = true;
            return true;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (jUptimeMillis - this.lastLegacy4jFallbackProbeMs < 1000) {
            return false;
        }
        this.lastLegacy4jFallbackProbeMs = jUptimeMillis;
        try {
            readableLatestLogFile = getReadableLatestLogFile();
        } catch (Throwable th) {
            Logging.e("GameActivity", "Unable to inspect Legacy4J controller fallback status", th);
        }
        if (readableLatestLogFile != null && readableLatestLogFile.isFile()) {
            String lowerCase2 = readLogTail(readableLatestLogFile, 98304).toLowerCase(Locale.ROOT);
            if (lowerCase2.contains("controllermodcompat: legacy4j detected") || lowerCase2.contains("sdl3 (isxander's libsdl4j)") || lowerCase2.contains("glfw will be used instead") || lowerCase2.contains("\n\t- legacy ") || lowerCase2.contains("\n- legacy ") || lowerCase2.contains(" legacy 1.")) {
                this.legacy4jGlfwFallbackAllowed = true;
                return true;
            }
            return false;
        }
        return false;
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        if (isGamepadMotionEvent(motionEvent)) {
            if (shouldRouteGamepadToMinecraftFallback()) {
                CallbackBridge.setInputReady(true);
                return super.dispatchGenericMotionEvent(motionEvent);
            }
            if (shouldConsumeLauncherGamepadInput()) {
                return true;
            }
            CallbackBridge.setInputReady(true);
            if (MinecraftGLSurface.sdlEnabled && (motionEvent.getSource() & InputDeviceCompat.SOURCE_JOYSTICK) == 16777232) {
                try {
                    if (SDLControllerManager.handleJoystickMotionEvent(motionEvent)) {
                        return true;
                    }
                } catch (UnsatisfiedLinkError e) {
                    Logger.appendToLog("SDL controller routing disabled: SDLControllerManager native glue is missing: " + e.getMessage());
                    MinecraftGLSurface.sdlEnabled = false;
                    return true;
                } catch (Throwable th) {
                    Logger.appendToLog("SDL controller routing disabled after controller motion failure: " + th.getClass().getName() + ": " + th.getMessage());
                    MinecraftGLSurface.sdlEnabled = false;
                    return true;
                }
            }
            GamepadInputController gamepadInputController = this.gamepadInputController;
            if (gamepadInputController != null && gamepadInputController.handleMotionEvent(motionEvent)) {
                return true;
            }
        }
        CallbackBridge.setInputReady(true);
        return super.dispatchGenericMotionEvent(motionEvent);
    }

    private boolean routePhysicalKeyboardEscapeToMinecraft(KeyEvent keyEvent) {
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding == null || activityGameBinding.minecraftSurface == null) {
            return false;
        }
        AlertDialog alertDialog = this.inGameControlsDialog;
        if ((alertDialog == null || !alertDialog.isShowing()) && MinecraftGLSurface.shouldRouteBackKeyToMinecraft(keyEvent)) {
            return this.binding.minecraftSurface.handleKeyEventFromActivity(keyEvent);
        }
        return false;
    }

    private boolean handleInGameControlsBackShortcut(KeyEvent keyEvent) {
        if (!isInGameControlsBackShortcut(keyEvent)) {
            return false;
        }
        if (keyEvent.getAction() == 1 && !keyEvent.isCanceled()) {
            openInGameButtonOverlay();
        }
        return true;
    }

    private boolean isInGameControlsBackShortcut(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == 4) {
            return !MinecraftGLSurface.shouldRouteBackKeyToMinecraft(keyEvent);
        }
        if (ControlsPreferences.isTouchControlsEnabled(this) || LauncherPreferences.isShowInGameSettingsButton(this) || !isGamepadKeyEvent(keyEvent)) {
            return false;
        }
        return keyCode == 109 || keyCode == 82;
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.core.app.ComponentActivity, android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (routePhysicalKeyboardEscapeToMinecraft(keyEvent) || handleInGameControlsBackShortcut(keyEvent)) {
            return true;
        }
        if (isGamepadKeyEvent(keyEvent)) {
            if (shouldRouteGamepadToMinecraftFallback()) {
                CallbackBridge.setInputReady(true);
                return super.dispatchKeyEvent(keyEvent);
            }
            if (shouldConsumeLauncherGamepadInput()) {
                return true;
            }
            CallbackBridge.setInputReady(true);
            if (MinecraftGLSurface.sdlEnabled) {
                try {
                    int deviceId = keyEvent.getDeviceId();
                    int keyCode = keyEvent.getKeyCode();
                    if (keyEvent.getAction() == 0) {
                        if (SDLControllerManager.onNativePadDown(deviceId, keyCode)) {
                            return true;
                        }
                    } else if (keyEvent.getAction() == 1 && SDLControllerManager.onNativePadUp(deviceId, keyCode)) {
                        return true;
                    }
                    return super.dispatchKeyEvent(keyEvent);
                } catch (UnsatisfiedLinkError e) {
                    Logger.appendToLog("SDL controller routing disabled: SDLControllerManager key native glue is missing: " + e.getMessage());
                    MinecraftGLSurface.sdlEnabled = false;
                    return true;
                } catch (Throwable th) {
                    Logger.appendToLog("SDL controller routing disabled after key failure: " + th.getClass().getName() + ": " + th.getMessage());
                    MinecraftGLSurface.sdlEnabled = false;
                    return true;
                }
            }
            GamepadInputController gamepadInputController = this.gamepadInputController;
            if (gamepadInputController != null && gamepadInputController.handleKeyEvent(keyEvent)) {
                return true;
            }
        }
        CallbackBridge.setInputReady(true);
        return super.dispatchKeyEvent(keyEvent);
    }

    private static boolean isGamepadMotionEvent(MotionEvent motionEvent) {
        int source = motionEvent.getSource();
        return (source & InputDeviceCompat.SOURCE_JOYSTICK) == 16777232 || (source & InputDeviceCompat.SOURCE_GAMEPAD) == 1025;
    }

    private static boolean isGamepadKeyEvent(KeyEvent keyEvent) {
        int deviceId = keyEvent.getDeviceId();
        if (deviceId < 0) {
            return false;
        }
        int source = keyEvent.getSource();
        return (source & InputDeviceCompat.SOURCE_GAMEPAD) == 1025 || (source & InputDeviceCompat.SOURCE_JOYSTICK) == 16777232 || SDLControllerManager.isDeviceSDLJoystick(deviceId);
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        openInGameButtonOverlay();
        configureWindow();
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        Runnable runnable;
        this.exiting = true;
        dismissDialog(this.inGameControlsDialog);
        this.inGameControlsDialog = null;
        stopLogOverlayTicker();
        Handler handler = this.quitWatchdogHandler;
        if (handler != null && (runnable = this.quitWatchdogRunnable) != null) {
            handler.removeCallbacks(runnable);
        }
        this.quitWatchdogRunnable = null;
        this.quitWatchdogHandler = null;
        CallbackBridge.clearInputFocus();
        CallbackBridge.setInputReady(false);
        GamepadInputController gamepadInputController = this.gamepadInputController;
        if (gamepadInputController != null) {
            gamepadInputController.removeSelf();
            this.gamepadInputController = null;
        }
        GameCursorOverlay gameCursorOverlay = this.gameCursorOverlay;
        if (gameCursorOverlay != null) {
            gameCursorOverlay.removeSelf();
            this.gameCursorOverlay = null;
        }
        TouchControlsOverlay touchControlsOverlay = this.touchControlsOverlay;
        if (touchControlsOverlay != null) {
            ViewGroup viewGroup = (ViewGroup) touchControlsOverlay.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(this.touchControlsOverlay);
            }
            this.touchControlsOverlay = null;
        }
        FloatingGameSettingsOverlayController floatingGameSettingsOverlayController = this.floatingGameSettingsOverlayController;
        if (floatingGameSettingsOverlayController != null) {
            floatingGameSettingsOverlayController.detach();
            this.floatingGameSettingsOverlayController = null;
        }
        ActivityGameBinding activityGameBinding = this.binding;
        if (activityGameBinding != null) {
            MinecraftGLSurface minecraftGLSurface = activityGameBinding.minecraftSurface;
            minecraftGLSurface.setSurfaceReadyListener((MinecraftGLSurface.SurfaceReadyListener) null);
            minecraftGLSurface.setOnRenderingStartedListener((MinecraftGLSurface.OnRenderingStartedListener) null);
        }
        this.binding = null;
        ControlifySDL.reset();
        ControllerModCompat.reset();
        LaunchGame.resetLaunchState();
        super.onDestroy();
    }
}

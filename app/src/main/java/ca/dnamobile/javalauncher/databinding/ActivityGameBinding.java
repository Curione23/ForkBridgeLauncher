package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import net.kdt.pojavlaunch.MinecraftGLSurface;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ActivityGameBinding implements ViewBinding {
    public final ImageButton buttonGameSettings;
    public final LinearLayout layoutLogOverlay;
    public final MinecraftGLSurface minecraftSurface;
    private final FrameLayout rootView;
    public final ScrollView scrollLogOverlay;
    public final TextView textLogOverlay;
    public final TextView textStatus;

    private ActivityGameBinding(FrameLayout frameLayout, ImageButton imageButton, LinearLayout linearLayout, MinecraftGLSurface minecraftGLSurface, ScrollView scrollView, TextView textView, TextView textView2) {
        this.rootView = frameLayout;
        this.buttonGameSettings = imageButton;
        this.layoutLogOverlay = linearLayout;
        this.minecraftSurface = minecraftGLSurface;
        this.scrollLogOverlay = scrollView;
        this.textLogOverlay = textView;
        this.textStatus = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static ActivityGameBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ActivityGameBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.activity_game, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ActivityGameBinding bind(View view) {
        MinecraftGLSurface minecraftGLSurfaceFindChildViewById;
        int i = R.id.buttonGameSettings;
        ImageButton imageButton = (ImageButton) ViewBindings.findChildViewById(view, i);
        if (imageButton != null) {
            i = R.id.layoutLogOverlay;
            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, i);
            if (linearLayout != null && (minecraftGLSurfaceFindChildViewById = ViewBindings.findChildViewById(view, (i = R.id.minecraft_surface))) != null) {
                i = R.id.scrollLogOverlay;
                ScrollView scrollView = (ScrollView) ViewBindings.findChildViewById(view, i);
                if (scrollView != null) {
                    i = R.id.textLogOverlay;
                    TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
                    if (textView != null) {
                        i = R.id.textStatus;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
                        if (textView2 != null) {
                            return new ActivityGameBinding((FrameLayout) view, imageButton, linearLayout, minecraftGLSurfaceFindChildViewById, scrollView, textView, textView2);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

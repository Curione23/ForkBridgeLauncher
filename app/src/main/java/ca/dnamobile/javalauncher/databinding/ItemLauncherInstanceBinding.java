package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import com.google.android.material.card.MaterialCardView;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ItemLauncherInstanceBinding implements ViewBinding {
    public final ImageButton buttonDeleteInstance;
    public final ImageView imageInstanceIcon;
    public final MaterialCardView instanceCard;
    private final MaterialCardView rootView;
    public final TextView textInstanceMeta;
    public final TextView textInstanceName;
    public final TextView textInstanceState;

    private ItemLauncherInstanceBinding(MaterialCardView materialCardView, ImageButton imageButton, ImageView imageView, MaterialCardView materialCardView2, TextView textView, TextView textView2, TextView textView3) {
        this.rootView = materialCardView;
        this.buttonDeleteInstance = imageButton;
        this.imageInstanceIcon = imageView;
        this.instanceCard = materialCardView2;
        this.textInstanceMeta = textView;
        this.textInstanceName = textView2;
        this.textInstanceState = textView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public MaterialCardView getRoot() {
        return this.rootView;
    }

    public static ItemLauncherInstanceBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ItemLauncherInstanceBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.item_launcher_instance, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ItemLauncherInstanceBinding bind(View view) {
        int i = R.id.buttonDeleteInstance;
        ImageButton imageButton = (ImageButton) ViewBindings.findChildViewById(view, i);
        if (imageButton != null) {
            i = R.id.imageInstanceIcon;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, i);
            if (imageView != null) {
                MaterialCardView materialCardView = (MaterialCardView) view;
                i = R.id.textInstanceMeta;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
                if (textView != null) {
                    i = R.id.textInstanceName;
                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
                    if (textView2 != null) {
                        i = R.id.textInstanceState;
                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, i);
                        if (textView3 != null) {
                            return new ItemLauncherInstanceBinding(materialCardView, imageButton, imageView, materialCardView, textView, textView2, textView3);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

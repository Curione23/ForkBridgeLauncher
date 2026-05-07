package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ItemInstanceResourceBinding implements ViewBinding {
    public final MaterialButton buttonDeleteResource;
    public final MaterialButton buttonPlayWorld;
    public final MaterialButton buttonUpdateResource;
    public final ShapeableImageView imageResourceIcon;
    public final ImageView imageResourceInstalledSource;
    private final MaterialCardView rootView;
    public final SwitchMaterial switchResourceEnabled;
    public final TextView textResourceName;
    public final TextView textResourceSubtitle;

    private ItemInstanceResourceBinding(MaterialCardView materialCardView, MaterialButton materialButton, MaterialButton materialButton2, MaterialButton materialButton3, ShapeableImageView shapeableImageView, ImageView imageView, SwitchMaterial switchMaterial, TextView textView, TextView textView2) {
        this.rootView = materialCardView;
        this.buttonDeleteResource = materialButton;
        this.buttonPlayWorld = materialButton2;
        this.buttonUpdateResource = materialButton3;
        this.imageResourceIcon = shapeableImageView;
        this.imageResourceInstalledSource = imageView;
        this.switchResourceEnabled = switchMaterial;
        this.textResourceName = textView;
        this.textResourceSubtitle = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public MaterialCardView getRoot() {
        return this.rootView;
    }

    public static ItemInstanceResourceBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ItemInstanceResourceBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.item_instance_resource, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ItemInstanceResourceBinding bind(View view) {
        int i = R.id.buttonDeleteResource;
        MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(view, i);
        if (materialButton != null) {
            i = R.id.buttonPlayWorld;
            MaterialButton materialButton2 = (MaterialButton) ViewBindings.findChildViewById(view, i);
            if (materialButton2 != null) {
                i = R.id.buttonUpdateResource;
                MaterialButton materialButton3 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                if (materialButton3 != null) {
                    i = R.id.imageResourceIcon;
                    ShapeableImageView shapeableImageView = (ShapeableImageView) ViewBindings.findChildViewById(view, i);
                    if (shapeableImageView != null) {
                        i = R.id.imageResourceInstalledSource;
                        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, i);
                        if (imageView != null) {
                            i = R.id.switchResourceEnabled;
                            SwitchMaterial switchMaterial = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                            if (switchMaterial != null) {
                                i = R.id.textResourceName;
                                TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
                                if (textView != null) {
                                    i = R.id.textResourceSubtitle;
                                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
                                    if (textView2 != null) {
                                        return new ItemInstanceResourceBinding((MaterialCardView) view, materialButton, materialButton2, materialButton3, shapeableImageView, imageView, switchMaterial, textView, textView2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

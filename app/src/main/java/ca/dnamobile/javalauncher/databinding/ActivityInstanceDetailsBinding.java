package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import ca.dnamobile.javalauncher.ui.view.RoundedClipFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ActivityInstanceDetailsBinding implements ViewBinding {
    public final MaterialButton buttonAddMods;
    public final MaterialButton buttonBackFromInstanceDetails;
    public final MaterialButton buttonBrowseContent;
    public final MaterialButton buttonCheckContentUpdates;
    public final MaterialButton buttonInstanceSettings;
    public final MaterialButton buttonPlay;
    public final MaterialButton buttonUpdateAllContent;
    public final MaterialCardView cardInstanceHeader;
    public final MaterialCardView cardResourceItems;
    public final TextInputEditText editTextContentSearch;
    public final ImageView imageInstanceIcon;
    public final LinearLayout layoutContentControls;
    public final TextInputLayout layoutContentSearch;
    public final RoundedClipFrameLayout layoutResourceCategoryTabs;
    public final RecyclerView recyclerResourceItems;
    private final LinearLayout rootView;
    public final TabLayout tabResourceCategories;
    public final TextView textInstanceMeta;
    public final TextView textInstanceName;
    public final TextView textModsHint;

    private ActivityInstanceDetailsBinding(LinearLayout linearLayout, MaterialButton materialButton, MaterialButton materialButton2, MaterialButton materialButton3, MaterialButton materialButton4, MaterialButton materialButton5, MaterialButton materialButton6, MaterialButton materialButton7, MaterialCardView materialCardView, MaterialCardView materialCardView2, TextInputEditText textInputEditText, ImageView imageView, LinearLayout linearLayout2, TextInputLayout textInputLayout, RoundedClipFrameLayout roundedClipFrameLayout, RecyclerView recyclerView, TabLayout tabLayout, TextView textView, TextView textView2, TextView textView3) {
        this.rootView = linearLayout;
        this.buttonAddMods = materialButton;
        this.buttonBackFromInstanceDetails = materialButton2;
        this.buttonBrowseContent = materialButton3;
        this.buttonCheckContentUpdates = materialButton4;
        this.buttonInstanceSettings = materialButton5;
        this.buttonPlay = materialButton6;
        this.buttonUpdateAllContent = materialButton7;
        this.cardInstanceHeader = materialCardView;
        this.cardResourceItems = materialCardView2;
        this.editTextContentSearch = textInputEditText;
        this.imageInstanceIcon = imageView;
        this.layoutContentControls = linearLayout2;
        this.layoutContentSearch = textInputLayout;
        this.layoutResourceCategoryTabs = roundedClipFrameLayout;
        this.recyclerResourceItems = recyclerView;
        this.tabResourceCategories = tabLayout;
        this.textInstanceMeta = textView;
        this.textInstanceName = textView2;
        this.textModsHint = textView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ActivityInstanceDetailsBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ActivityInstanceDetailsBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.activity_instance_details, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ActivityInstanceDetailsBinding bind(View view) {
        int i = R.id.buttonAddMods;
        MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(view, i);
        if (materialButton != null) {
            i = R.id.buttonBackFromInstanceDetails;
            MaterialButton materialButton2 = (MaterialButton) ViewBindings.findChildViewById(view, i);
            if (materialButton2 != null) {
                i = R.id.buttonBrowseContent;
                MaterialButton materialButton3 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                if (materialButton3 != null) {
                    i = R.id.buttonCheckContentUpdates;
                    MaterialButton materialButton4 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                    if (materialButton4 != null) {
                        i = R.id.buttonInstanceSettings;
                        MaterialButton materialButton5 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                        if (materialButton5 != null) {
                            i = R.id.buttonPlay;
                            MaterialButton materialButton6 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                            if (materialButton6 != null) {
                                i = R.id.buttonUpdateAllContent;
                                MaterialButton materialButton7 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                if (materialButton7 != null) {
                                    i = R.id.cardInstanceHeader;
                                    MaterialCardView materialCardView = (MaterialCardView) ViewBindings.findChildViewById(view, i);
                                    if (materialCardView != null) {
                                        i = R.id.cardResourceItems;
                                        MaterialCardView materialCardView2 = (MaterialCardView) ViewBindings.findChildViewById(view, i);
                                        if (materialCardView2 != null) {
                                            i = R.id.editTextContentSearch;
                                            TextInputEditText textInputEditText = (TextInputEditText) ViewBindings.findChildViewById(view, i);
                                            if (textInputEditText != null) {
                                                i = R.id.imageInstanceIcon;
                                                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, i);
                                                if (imageView != null) {
                                                    LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.layoutContentControls);
                                                    i = R.id.layoutContentSearch;
                                                    TextInputLayout textInputLayout = (TextInputLayout) ViewBindings.findChildViewById(view, i);
                                                    if (textInputLayout != null) {
                                                        i = R.id.layoutResourceCategoryTabs;
                                                        RoundedClipFrameLayout roundedClipFrameLayout = (RoundedClipFrameLayout) ViewBindings.findChildViewById(view, i);
                                                        if (roundedClipFrameLayout != null) {
                                                            i = R.id.recyclerResourceItems;
                                                            RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, i);
                                                            if (recyclerView != null) {
                                                                i = R.id.tabResourceCategories;
                                                                TabLayout tabLayout = (TabLayout) ViewBindings.findChildViewById(view, i);
                                                                if (tabLayout != null) {
                                                                    i = R.id.textInstanceMeta;
                                                                    TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
                                                                    if (textView != null) {
                                                                        i = R.id.textInstanceName;
                                                                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                        if (textView2 != null) {
                                                                            i = R.id.textModsHint;
                                                                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                            if (textView3 != null) {
                                                                                return new ActivityInstanceDetailsBinding((LinearLayout) view, materialButton, materialButton2, materialButton3, materialButton4, materialButton5, materialButton6, materialButton7, materialCardView, materialCardView2, textInputEditText, imageView, linearLayout, textInputLayout, roundedClipFrameLayout, recyclerView, tabLayout, textView, textView2, textView3);
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
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

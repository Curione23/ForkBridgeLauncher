package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ActivityContentBrowserBinding implements ViewBinding {
    public final MaterialButton buttonBackToInstance;
    public final MaterialButton buttonPageNext;
    public final MaterialButton buttonPagePrevious;
    public final MaterialButton buttonSortContent;
    public final MaterialButton buttonSourceCurseForge;
    public final MaterialButton buttonSourceModrinth;
    public final MaterialButton buttonViewCount;
    public final TextInputEditText editContentSearch;
    public final ImageView imageContentBrowserInstanceIcon;
    public final LinearLayout layoutContentBrowserHeader;
    public final LinearLayout layoutContentPagination;
    public final TextInputLayout layoutContentSearch;
    public final RecyclerView recyclerContentProjects;
    private final NestedScrollView rootView;
    public final NestedScrollView scrollContentBrowserRoot;
    public final TabLayout tabContentTypes;
    public final TextView textContentBrowserInstanceMeta;
    public final TextView textContentBrowserInstanceName;
    public final TextView textContentBrowserLoaderChip;
    public final TextView textContentBrowserResultSummary;
    public final TextView textContentBrowserTitle;
    public final TextView textContentBrowserVersionChip;
    public final TextView textPageIndicator;
    public final MaterialButtonToggleGroup toggleContentSource;

    private ActivityContentBrowserBinding(NestedScrollView nestedScrollView, MaterialButton materialButton, MaterialButton materialButton2, MaterialButton materialButton3, MaterialButton materialButton4, MaterialButton materialButton5, MaterialButton materialButton6, MaterialButton materialButton7, TextInputEditText textInputEditText, ImageView imageView, LinearLayout linearLayout, LinearLayout linearLayout2, TextInputLayout textInputLayout, RecyclerView recyclerView, NestedScrollView nestedScrollView2, TabLayout tabLayout, TextView textView, TextView textView2, TextView textView3, TextView textView4, TextView textView5, TextView textView6, TextView textView7, MaterialButtonToggleGroup materialButtonToggleGroup) {
        this.rootView = nestedScrollView;
        this.buttonBackToInstance = materialButton;
        this.buttonPageNext = materialButton2;
        this.buttonPagePrevious = materialButton3;
        this.buttonSortContent = materialButton4;
        this.buttonSourceCurseForge = materialButton5;
        this.buttonSourceModrinth = materialButton6;
        this.buttonViewCount = materialButton7;
        this.editContentSearch = textInputEditText;
        this.imageContentBrowserInstanceIcon = imageView;
        this.layoutContentBrowserHeader = linearLayout;
        this.layoutContentPagination = linearLayout2;
        this.layoutContentSearch = textInputLayout;
        this.recyclerContentProjects = recyclerView;
        this.scrollContentBrowserRoot = nestedScrollView2;
        this.tabContentTypes = tabLayout;
        this.textContentBrowserInstanceMeta = textView;
        this.textContentBrowserInstanceName = textView2;
        this.textContentBrowserLoaderChip = textView3;
        this.textContentBrowserResultSummary = textView4;
        this.textContentBrowserTitle = textView5;
        this.textContentBrowserVersionChip = textView6;
        this.textPageIndicator = textView7;
        this.toggleContentSource = materialButtonToggleGroup;
    }

    @Override // androidx.viewbinding.ViewBinding
    public NestedScrollView getRoot() {
        return this.rootView;
    }

    public static ActivityContentBrowserBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ActivityContentBrowserBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.activity_content_browser, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ActivityContentBrowserBinding bind(View view) {
        int i = R.id.buttonBackToInstance;
        MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(view, i);
        if (materialButton != null) {
            i = R.id.buttonPageNext;
            MaterialButton materialButton2 = (MaterialButton) ViewBindings.findChildViewById(view, i);
            if (materialButton2 != null) {
                i = R.id.buttonPagePrevious;
                MaterialButton materialButton3 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                if (materialButton3 != null) {
                    i = R.id.buttonSortContent;
                    MaterialButton materialButton4 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                    if (materialButton4 != null) {
                        i = R.id.buttonSourceCurseForge;
                        MaterialButton materialButton5 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                        if (materialButton5 != null) {
                            i = R.id.buttonSourceModrinth;
                            MaterialButton materialButton6 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                            if (materialButton6 != null) {
                                i = R.id.buttonViewCount;
                                MaterialButton materialButton7 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                if (materialButton7 != null) {
                                    i = R.id.editContentSearch;
                                    TextInputEditText textInputEditText = (TextInputEditText) ViewBindings.findChildViewById(view, i);
                                    if (textInputEditText != null) {
                                        i = R.id.imageContentBrowserInstanceIcon;
                                        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, i);
                                        if (imageView != null) {
                                            i = R.id.layoutContentBrowserHeader;
                                            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, i);
                                            if (linearLayout != null) {
                                                i = R.id.layoutContentPagination;
                                                LinearLayout linearLayout2 = (LinearLayout) ViewBindings.findChildViewById(view, i);
                                                if (linearLayout2 != null) {
                                                    i = R.id.layoutContentSearch;
                                                    TextInputLayout textInputLayout = (TextInputLayout) ViewBindings.findChildViewById(view, i);
                                                    if (textInputLayout != null) {
                                                        i = R.id.recyclerContentProjects;
                                                        RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, i);
                                                        if (recyclerView != null) {
                                                            NestedScrollView nestedScrollView = (NestedScrollView) view;
                                                            i = R.id.tabContentTypes;
                                                            TabLayout tabLayout = (TabLayout) ViewBindings.findChildViewById(view, i);
                                                            if (tabLayout != null) {
                                                                i = R.id.textContentBrowserInstanceMeta;
                                                                TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
                                                                if (textView != null) {
                                                                    i = R.id.textContentBrowserInstanceName;
                                                                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                    if (textView2 != null) {
                                                                        i = R.id.textContentBrowserLoaderChip;
                                                                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                        if (textView3 != null) {
                                                                            i = R.id.textContentBrowserResultSummary;
                                                                            TextView textView4 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                            if (textView4 != null) {
                                                                                i = R.id.textContentBrowserTitle;
                                                                                TextView textView5 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                if (textView5 != null) {
                                                                                    i = R.id.textContentBrowserVersionChip;
                                                                                    TextView textView6 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                    if (textView6 != null) {
                                                                                        i = R.id.textPageIndicator;
                                                                                        TextView textView7 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                        if (textView7 != null) {
                                                                                            i = R.id.toggleContentSource;
                                                                                            MaterialButtonToggleGroup materialButtonToggleGroup = (MaterialButtonToggleGroup) ViewBindings.findChildViewById(view, i);
                                                                                            if (materialButtonToggleGroup != null) {
                                                                                                return new ActivityContentBrowserBinding(nestedScrollView, materialButton, materialButton2, materialButton3, materialButton4, materialButton5, materialButton6, materialButton7, textInputEditText, imageView, linearLayout, linearLayout2, textInputLayout, recyclerView, nestedScrollView, tabLayout, textView, textView2, textView3, textView4, textView5, textView6, textView7, materialButtonToggleGroup);
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
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

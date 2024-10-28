package com.example.projecta;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

public class MoreFragment extends Fragment {

    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private Toolbar toolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        // Initialize views
        appBarLayout = view.findViewById(R.id.appBarLayout);
        collapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar);
        toolbar = view.findViewById(R.id.toolbar);

        setupAppBarListener();
        return view;
    }

    private void setupAppBarListener() {
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                // Calculate the scroll percentage
                int totalScrollRange = appBarLayout.getTotalScrollRange();
                float scrollRatio = (float) Math.abs(verticalOffset) / (float) totalScrollRange;

                // Hide title when fully expanded, show title when collapsed
                if (scrollRatio > 0.8) {
                    toolbar.setTitle("Username"); // Set title when collapsed
                    toolbar.setBackgroundColor(Color.WHITE); // Change background color when expanded
                    toolbar.setTitleTextColor(Color.BLACK); // Change text color when expanded
                } else {
                    toolbar.setTitle(""); // Clear title when expanded
                    toolbar.setBackgroundColor(Color.TRANSPARENT); // Change background color when collapsed
                    toolbar.setTitleTextColor(Color.WHITE); // Change text color when collapsed
                }
            }
        });
    }
}

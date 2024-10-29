package com.example.projecta;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MoreFragment extends Fragment {

    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private Toolbar toolbar;
    private TextView userGreeting;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String username = "User";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        appBarLayout = view.findViewById(R.id.appBarLayout);
        collapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar);
        toolbar = view.findViewById(R.id.toolbar);
        userGreeting = view.findViewById(R.id.user_greeting);

        // Fetch user data from Firebase
        fetchUsername();

        // Set up AppBar scroll listener
        setupAppBarListener();

        return view;
    }

    private void fetchUsername() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            username = documentSnapshot.getString("username");
                            if (username != null) {
                                toolbar.setTitle(username); // Set the initial title in the toolbar
                                userGreeting.setText("Hi, " + username); // Update the user greeting
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Log or handle failure to retrieve user data
                        toolbar.setTitle("User"); // Default title in case of error
                        userGreeting.setText("Hi, User"); // Default greeting in case of error
                    });
        }
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
                    toolbar.setTitle(username); // Set title when collapsed
                    toolbar.setBackgroundColor(Color.WHITE); // Change background color when collapsed
                    toolbar.setTitleTextColor(Color.BLACK); // Change text color when collapsed
                } else {
                    toolbar.setTitle(""); // Clear title when expanded
                    toolbar.setBackgroundColor(Color.TRANSPARENT); // Change background color when expanded
                    toolbar.setTitleTextColor(Color.WHITE); // Change text color when expanded
                }
            }
        });
    }
}

package com.example.projecta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReviewBottomSheet extends BottomSheetDialogFragment {

    private String orderId;
    private String restaurantName;
    private String userId; // Will dynamically get this from Firebase Authentication
    private int selectedStarRating = 0; // Variable to store the selected star rating

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_review, container, false);

        // Retrieve arguments passed from parent
        Bundle args = getArguments();
        if (args != null) {
            orderId = args.getString("order_id");
            restaurantName = args.getString("restaurant_name");
        }

        // Get user ID dynamically from Firebase Authentication
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            // If no user is logged in, handle it gracefully (e.g., show an error or exit the bottom sheet)
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            dismiss();
            return view;
        }

        // Initialize views
        EditText reviewTextBox = view.findViewById(R.id.review_text_box);
        Button submitButton = view.findViewById(R.id.btn_submit);
        Button greatServiceButton = view.findViewById(R.id.button_great_service);
        Button quickPickupButton = view.findViewById(R.id.button_quick_pickup);
        Button allergyFriendlyButton = view.findViewById(R.id.button_allergy_friendly);
        Button greatValueButton = view.findViewById(R.id.button_great_value);
        ImageView closeButton = view.findViewById(R.id.close_button);
        LinearLayout starContainer = view.findViewById(R.id.star_container);

        greatServiceButton.setOnClickListener(v -> {
            boolean isSelected = greatServiceButton.isSelected();
            greatServiceButton.setSelected(!isSelected);
        });

        quickPickupButton.setOnClickListener(v -> {
            boolean isSelected = quickPickupButton.isSelected();
            quickPickupButton.setSelected(!isSelected);
        });

        allergyFriendlyButton.setOnClickListener(v -> {
            boolean isSelected = allergyFriendlyButton.isSelected();
            allergyFriendlyButton.setSelected(!isSelected);
        });

        greatValueButton.setOnClickListener(v -> {
            boolean isSelected = greatValueButton.isSelected();
            greatValueButton.setSelected(!isSelected);
        });

        // Set up close button action
        closeButton.setOnClickListener(v -> dismiss());

        // Set up star rating
        setupStarRating(starContainer);

        // Handle submit button click
        submitButton.setOnClickListener(v -> {
            String reviewText = reviewTextBox.getText().toString().trim();
            if (reviewText.isEmpty()) {
                Toast.makeText(requireContext(), "Please write a review before submitting.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedStarRating == 0) {
                Toast.makeText(requireContext(), "Please select a star rating before submitting.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save review logic (Firestore)
            saveReview(view, reviewText);
        });

        return view;
    }

    private void setupStarRating(LinearLayout starContainer) {
        for (int i = 0; i < starContainer.getChildCount(); i++) {
            final int starIndex = i + 1; // Star index (1-based)
            starContainer.getChildAt(i).setOnClickListener(v -> {
                // Highlight the clicked stars
                for (int j = 0; j < starContainer.getChildCount(); j++) {
                    ImageView star = (ImageView) starContainer.getChildAt(j);
                    if (j < starIndex) {
                        star.setImageResource(R.drawable.ic_star_filled); // Highlighted star
                    } else {
                        star.setImageResource(R.drawable.ic_star_empty); // Non-highlighted star
                    }
                }
                selectedStarRating = starIndex; // Update the selected star rating
            });
        }
    }

    private void saveReview(View view, String reviewText) {
        // Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if the review for this order already exists
        db.collection("users")
                .document(userId)
                .collection("reviews")
                .whereEqualTo("orderId", orderId) // Query for the specific orderId
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // If a review already exists, show an error message
                        Toast.makeText(requireContext(), "A review for this order already exists!", Toast.LENGTH_SHORT).show();
                    } else {
                        // If no review exists, proceed to save the review
                        saveNewReview(view, reviewText);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error checking existing reviews: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveNewReview(View view, String reviewText) {
        // Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get button states
        boolean isGreatService = view.findViewById(R.id.button_great_service).isSelected();
        boolean isQuickPickup = view.findViewById(R.id.button_quick_pickup).isSelected();
        boolean isAllergyFriendly = view.findViewById(R.id.button_allergy_friendly).isSelected();
        boolean isGreatValue = view.findViewById(R.id.button_great_value).isSelected();

        // Create review object
        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("orderId", orderId);
        reviewData.put("restaurantName", restaurantName);
        reviewData.put("reviewText", reviewText);
        reviewData.put("greatService", isGreatService);
        reviewData.put("quickPickup", isQuickPickup);
        reviewData.put("allergyFriendly", isAllergyFriendly);
        reviewData.put("greatValue", isGreatValue);
        reviewData.put("starRating", selectedStarRating); // Save the star rating
        reviewData.put("timestamp", System.currentTimeMillis());

        // Save data to the 'reviews' subcollection of the current user
        db.collection("users")
                .document(userId)
                .collection("reviews")
                .add(reviewData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                    dismiss(); // Close the bottom sheet after submission
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error submitting review: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    // Add animations to buttons if necessary
    private void addScaleAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }
}

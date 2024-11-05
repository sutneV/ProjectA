package com.example.projecta;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;


public class MoreFragment extends Fragment {

    private static final String TAG = "MoreFragment";
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private TextView userGreeting;
    private ImageView profileIcon;
    private ImageButton uploadButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private String username = "User";
    private ActivityResultLauncher<Intent> photoPickerLauncher;

    private RecyclerView recyclerViewOrders;
    private TextView noOrdersText;
    private OrderAdapter orderAdapter;
    private List<Order> orderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference("profile_pictures");

        // Initialize views
        appBarLayout = view.findViewById(R.id.appBarLayout);
        collapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar);
        userGreeting = view.findViewById(R.id.user_greeting);
        profileIcon = view.findViewById(R.id.profile_icon);
        uploadButton = view.findViewById(R.id.upload_button);
        recyclerViewOrders = view.findViewById(R.id.recycler_view_orders);
        noOrdersText = view.findViewById(R.id.no_orders_text);

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        orderAdapter = new OrderAdapter(getContext(), orderList);
        recyclerViewOrders.setAdapter(orderAdapter);

        // Debugging check
        if (uploadButton != null) {
            uploadButton.bringToFront();
            Log.d(TAG, "uploadButton brought to front successfully");
        } else {
            Log.e(TAG, "uploadButton is null. Check if it is defined in fragment_more.xml");
        }

        // Set up image picker launcher
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        Log.d(TAG, "Image selected: " + imageUri);
                        uploadImageToFirebase(imageUri);
                    } else {
                        Log.d(TAG, "Image selection failed or was canceled");
                    }
                }
        );

        // Set click listener for uploading profile picture
        uploadButton.setOnClickListener(v -> {
            Log.d(TAG, "Upload button clicked");
            Toast.makeText(getContext(), "Upload button clicked", Toast.LENGTH_SHORT).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestMediaPermission();
            } else {
                openImagePicker();
            }
        });

        // Fetch user data from Firebase and load profile image
        fetchUsername();
        loadProfileImage();
        loadOrdersFromFirestore();

        return view;
    }

    private void loadOrdersFromFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId).collection("orders")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            orderList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Order order = document.toObject(Order.class);
                                Log.d("OrderData", "Order loaded: " + order.getOrderId() + ", " + order.getStatus() + ", " + order.getRestaurantName());
                                orderList.add(order);
                            }
                            updateUI();
                        } else {
                            Log.e(TAG, "Error loading orders", task.getException());
                            Toast.makeText(getContext(), "Failed to load orders", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    private void updateUI() {
        if (orderList.isEmpty()) {
            noOrdersText.setVisibility(View.VISIBLE);
            recyclerViewOrders.setVisibility(View.GONE);
        } else {
            noOrdersText.setVisibility(View.GONE);
            recyclerViewOrders.setVisibility(View.VISIBLE);
            orderAdapter.notifyDataSetChanged();
        }
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
                                userGreeting.setText("Hi, " + username); // Update the user greeting
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        userGreeting.setText("Hi, User");
                        Log.e(TAG, "Error fetching username", e);
                    });
        }
    }

    private void loadProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profileImageUrl)
                                        .transform(new CircleCrop())  // Make the image rounded
                                        .into(profileIcon);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to load profile image", e));
        }
    }

    private void requestMediaPermission() {
        Log.d(TAG, "Checking media permissions...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                Log.d(TAG, "Media permission already granted, opening image picker...");
                openImagePicker();
            }
        } else { // For Android versions below 13
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                Log.d(TAG, "Media permission already granted, opening image picker...");
                openImagePicker();
            }
        }
    }

    private void openImagePicker() {
        Log.d(TAG, "Launching image picker...");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        photoPickerLauncher.launch(intent);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && imageUri != null) {
            String userId = user.getUid();
            StorageReference fileReference = storageRef.child(userId + ".jpg");

            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // Update the ImageView immediately with the new URL
                        Glide.with(this)
                                .load(downloadUrl)
                                .circleCrop()  // Apply circle crop transformation
                                .into(profileIcon);

                        // Save the image URL to Firestore
                        saveImageUrlToFirestore(downloadUrl);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Upload failed", e);
                    });
        }
    }

    private void saveImageUrlToFirestore(String downloadUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId)
                    .update("profileImageUrl", downloadUrl)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Profile URL saved");

                        // Refresh the profile picture by reloading it from Firestore
                        loadProfileImage();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save URL", e));
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult called with requestCode: " + requestCode);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission granted");
                openImagePicker();
            } else {
                Log.d(TAG, "Storage permission denied");
                Toast.makeText(getContext(), "Permission required to pick images", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

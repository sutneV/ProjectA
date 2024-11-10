package com.example.projecta;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private ImageView profileImageView;
    private EditText etEditUsername;
    private EditText etEditEmail;
    private EditText etEditPhoneNumber;
    private Button btnChangePhoto;
    private Button btnSaveProfile;
    private Button btnCancel;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference("profile_pictures");

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        etEditUsername = findViewById(R.id.etEditUsername);
        etEditEmail = findViewById(R.id.etEditEmail);
        etEditPhoneNumber = findViewById(R.id.etEditPhoneNumber);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancel = findViewById(R.id.btnCancel);

        // Pre-fill user data
        loadUserProfile();

        // Change profile picture
        btnChangePhoto.setOnClickListener(v -> openImagePicker());

        // Save profile changes
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        etEditUsername.setText(username);
                        etEditEmail.setText(email);
                        etEditPhoneNumber.setText(phoneNumber);

                        // Load profile image using Glide
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(profileImageView);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load user profile", e));
    }

    private void saveProfileChanges() {
        String username = etEditUsername.getText().toString().trim();
        String email = etEditEmail.getText().toString().trim();
        String phoneNumber = etEditPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("email", email);
        updates.put("phoneNumber", phoneNumber);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Upload image if a new one is selected
                    if (selectedImageUri != null) {
                        uploadProfileImage(userId);
                    } else {
                        finish(); // Close activity if no new image was selected
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating profile", e);
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1000); // Request code for image picker
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImageView.setImageURI(selectedImageUri); // Show the selected image immediately
        }
    }

    private void uploadProfileImage(String userId) {
        if (selectedImageUri != null) {
            StorageReference fileReference = storageRef.child(userId + ".jpg");

            fileReference.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // Save image URL to Firestore
                        db.collection("users").document(userId)
                                .update("profileImageUrl", downloadUrl)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show();
                                    finish(); // Close activity after saving
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to save image URL", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error saving image URL", e);
                                });
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error uploading image", e);
                    });
        }
    }
}

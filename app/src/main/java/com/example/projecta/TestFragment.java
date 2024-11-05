package com.example.projecta;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TestFragment extends Fragment {

    private static final String TAG = "TestFragment";
    private ImageButton uploadButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test, container, false); // Use the simple layout

        // Initialize the upload button
        uploadButton = view.findViewById(R.id.upload_button);

        if (uploadButton == null) {
            Log.e(TAG, "uploadButton is null. Check if it is defined in fragment_test.xml");
        } else {
            Log.d(TAG, "uploadButton initialized successfully");

            // Set a simplified click listener to test button functionality
            uploadButton.setOnClickListener(v -> {
                Log.d(TAG, "Simple click detected in TestFragment");
                Toast.makeText(getContext(), "Simple click detected in TestFragment", Toast.LENGTH_SHORT).show();
            });
        }

        return view;
    }
}

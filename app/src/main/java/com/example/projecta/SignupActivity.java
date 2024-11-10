package com.example.projecta;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SignupActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPhoneNumber, etPassword;
    private Button btnSignup;
    private ProgressBar passwordStrengthBar;
    private TextView tvLogin, passwordStrengthText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        passwordStrengthBar = findViewById(R.id.passwordStrengthBar);
        passwordStrengthText = findViewById(R.id.passwordStrengthText);

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            }
        });
    }

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);

        // Set progress bar color and text based on strength
        if (strength == 0) {
            passwordStrengthBar.setProgress(0);
            passwordStrengthBar.getProgressDrawable().setColorFilter(Color.parseColor("#E0E0E0"), android.graphics.PorterDuff.Mode.SRC_IN);
            passwordStrengthText.setText("Password strength: Weak");
            passwordStrengthText.setTextColor(Color.parseColor("#FF4C4C"));
        }else if (strength <= 25) {
            passwordStrengthBar.setProgress(strength);
            passwordStrengthBar.getProgressDrawable().setColorFilter(Color.parseColor("#FF4C4C"), android.graphics.PorterDuff.Mode.SRC_IN);
            passwordStrengthText.setText("Password strength: Weak");
            passwordStrengthText.setTextColor(Color.parseColor("#FF4C4C"));
        } else if (strength <= 50) {
            passwordStrengthBar.setProgress(strength);
            passwordStrengthBar.getProgressDrawable().setColorFilter(Color.parseColor("#FFC107"), android.graphics.PorterDuff.Mode.SRC_IN);
            passwordStrengthText.setText("Password strength: Medium");
            passwordStrengthText.setTextColor(Color.parseColor("#FFC107"));
        } else if (strength <= 75) {
            passwordStrengthBar.setProgress(strength);
            passwordStrengthBar.getProgressDrawable().setColorFilter(Color.parseColor("#4CAF50"), android.graphics.PorterDuff.Mode.SRC_IN);
            passwordStrengthText.setText("Password strength: Strong");
            passwordStrengthText.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            passwordStrengthBar.setProgress(strength);
            passwordStrengthBar.getProgressDrawable().setColorFilter(Color.parseColor("#0D47A1"), android.graphics.PorterDuff.Mode.SRC_IN);
            passwordStrengthText.setText("Password strength: Very Strong");
            passwordStrengthText.setTextColor(Color.parseColor("#0D47A1"));
        }
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;

        // Check for length
        if (password.length() >= 1 && password.length() < 8) strength += 25;
        if (password.length() >= 8) strength += 25; // Base score for sufficient length
        // Check for uppercase letter
        if (password.matches(".*[A-Z].*")) strength += 25;
        // Check for number
        if (password.matches(".*\\d.*")) strength += 25;
        // Check for special character
        if (password.matches(".*[@#$%^&+=!].*")) strength += 25;

        return strength;
    }

    private void signUp() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        // Check if any field is empty
        if (email.isEmpty() || password.isEmpty() || username.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password strength
        if (!isPasswordStrong(password)) {
            Toast.makeText(this, "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character", Toast.LENGTH_LONG).show();
            return;
        }

        // Proceed with signup if validations pass
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToFirestore(user.getUid(), username, phoneNumber, email);
                    } else {
                        Toast.makeText(SignupActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper method to check password strength
    private boolean isPasswordStrong(String password) {
        // Password must have at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character
        final Pattern PASSWORD_PATTERN = Pattern.compile(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$"
        );
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    private void saveUserToFirestore(String userId, String username, String phoneNumber, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Create a user map with extra fields
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("phoneNumber", phoneNumber);
        user.put("email", email);
        user.put("profileImageUrl", "");

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish(); // Close SignupActivity
                })
                .addOnFailureListener(e -> Toast.makeText(SignupActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show());
    }
}
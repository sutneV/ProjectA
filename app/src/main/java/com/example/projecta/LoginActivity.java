package com.example.projecta;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsernameEmail, etPassword;
    private Button btnLogin, btnLoginWithOtp;
    private TextView tvForgotPassword, tvSignup;
    private FirebaseAuth mAuth;
    private ImageView ivShowHidePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etUsernameEmail = findViewById(R.id.etUsernameEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignup = findViewById(R.id.tvSignup);
        btnLoginWithOtp = findViewById(R.id.btnLoginWithOtp);
        ivShowHidePassword = findViewById(R.id.ivShowHidePassword);

        ivShowHidePassword.setOnClickListener(v -> togglePasswordVisibility());

        btnLoginWithOtp.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, EmailOtpLoginActivity.class))
        );

        // Set up button actions
        btnLogin.setOnClickListener(v -> login());
        tvSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void login() {
        String email = etUsernameEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showForgotPasswordDialog() {
        // Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);

        // Create and configure the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Get the views from the dialog layout
        EditText etResetEmail = dialogView.findViewById(R.id.etResetEmail);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSendResetLink = dialogView.findViewById(R.id.btnSendResetLink);

        // Set click listener for "Cancel" button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Set click listener for "Send" button
        btnSendResetLink.setOnClickListener(v -> {
            String email = etResetEmail.getText().toString().trim();
            if (!email.isEmpty()) {
                resetPassword(email);
                dialog.dismiss();
            } else {
                Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivShowHidePassword.setImageResource(R.drawable.ic_visibility_off); // Eye-Off Icon
        } else {
            // Show password
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivShowHidePassword.setImageResource(R.drawable.ic_visibility); // Eye Icon
        }
        isPasswordVisible = !isPasswordVisible;

        // Move cursor to the end of the text
        etPassword.setSelection(etPassword.getText().length());
    }

    private void resetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

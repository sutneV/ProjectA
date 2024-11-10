package com.example.projecta;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.MessagingException;

public class EmailOtpLoginActivity extends AppCompatActivity {

    private static final String TAG = "EmailOtpLoginActivity";

    private EditText etEmailAddress, etOtp;
    private Button btnSendOtp, btnLoginWithOtp;
    private TextView tvSignup;

    // In-memory OTP store (for demonstration purposes)
    private String generatedOtp = null;
    private String enteredEmail = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_login);

        // Initialize views
        etEmailAddress = findViewById(R.id.etEmailAddress);
        etOtp = findViewById(R.id.etOtp);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnLoginWithOtp = findViewById(R.id.btnLoginWithOtp);
        tvSignup = findViewById(R.id.tvSignup);

        // Button: Send OTP
        btnSendOtp.setOnClickListener(v -> sendOtpEmail());

        // Button: Login with OTP
        btnLoginWithOtp.setOnClickListener(v -> verifyOtp());

        // TextView: Signup Link
        tvSignup.setOnClickListener(v -> {
            // Navigate to SignupActivity (replace with your actual SignupActivity)
            Intent intent = new Intent(EmailOtpLoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Sends OTP to the entered email address.
     */
    private void sendOtpEmail() {
        enteredEmail = etEmailAddress.getText().toString().trim();

        // Validate email address
        if (TextUtils.isEmpty(enteredEmail)) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(enteredEmail).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate OTP
        generatedOtp = generateOtp();
        Log.d(TAG, "Generated OTP: " + generatedOtp);

        // Email content
        String subject = "Your OTP Code";
        String messageBody = "Your OTP code is " + generatedOtp + ". It will expire in 5 minutes.";

        // Send OTP via email
        String fromEmail = "limwg021018@gmail.com"; // Replace with your email
        String password = "hluf wutm flpv clxg";    // Replace with your app password

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                EmailSender.sendEmail(fromEmail, password, enteredEmail, subject, messageBody);
                runOnUiThread(() -> {
                    Toast.makeText(this, "OTP sent successfully to " + enteredEmail, Toast.LENGTH_SHORT).show();
                    etOtp.setVisibility(View.VISIBLE);       // Show OTP field
                    btnLoginWithOtp.setVisibility(View.VISIBLE); // Show "Login with OTP" button
                });
            } catch (MessagingException e) {
                Log.e(TAG, "Error sending OTP email", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to send OTP. Please try again.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Verifies the entered OTP.
     */
    private void verifyOtp() {
        String enteredOtp = etOtp.getText().toString().trim();

        // Validate OTP
        if (TextUtils.isEmpty(enteredOtp)) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (generatedOtp == null) {
            Toast.makeText(this, "No OTP generated. Please request a new OTP.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!enteredOtp.equals(generatedOtp)) {
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // OTP is valid
        Toast.makeText(this, "OTP verified successfully! Logging in...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "User logged in with email: " + enteredEmail);

        // Navigate to MainActivity (or other logic)
        Intent intent = new Intent(EmailOtpLoginActivity.this, MainActivity.class);
        intent.putExtra("email", enteredEmail);
        startActivity(intent);
        finish();
    }

    /**
     * Generates a 6-digit OTP.
     */
    private String generateOtp() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000)); // Generate random 6-digit number
    }
}

package com.example.livescoringui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class signup_page extends AppCompatActivity {

    private TextInputEditText usernameInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private Button signupButton;
    private TextView loginText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        usernameInput = findViewById(R.id.edit_text_username_signup);
        phoneInput = findViewById(R.id.edit_text_phone_signup);
        passwordInput = findViewById(R.id.edit_text_password_signup);
        signupButton = findViewById(R.id.button_signup);
        loginText = findViewById(R.id.text_login);

        // Navigate to login
        loginText.setOnClickListener(v -> finish());

        // Signup button click
        signupButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String username = usernameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("Username is required");
            usernameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            phoneInput.requestFocus();
            return;
        }

        if (phone.length() < 10) {
            phoneInput.setError("Enter a valid phone number");
            phoneInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        // Disable button to prevent multiple clicks
        signupButton.setEnabled(false);
        signupButton.setText("Creating account...");

        // Convert phone to email format for Firebase Auth
        String email = phone + "@hockify.app";

        // Create user with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign up success
                        Log.d("SignupPage", "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Update user profile with username
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d("SignupPage", "User profile updated.");
                                        }
                                    });

                            // Save user data to Firestore
                            saveUserToFirestore(user.getUid(), username, phone);
                        }

                        Toast.makeText(signup_page.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to MainActivity
                        Intent intent = new Intent(signup_page.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        // Sign up failed
                        Log.w("SignupPage", "createUserWithEmail:failure", task.getException());
                        signupButton.setEnabled(true);
                        signupButton.setText("Sign up");
                        
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(signup_page.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String phone) {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("username", username);
        user.put("phone", phone);
        user.put("createdAt", System.currentTimeMillis());

        db.collection("Users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> 
                    Log.d("SignupPage", "User data saved to Firestore"))
                .addOnFailureListener(e -> 
                    Log.w("SignupPage", "Error saving user data", e));
    }
}
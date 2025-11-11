package com.example.livescoringui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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

public class login_page extends AppCompatActivity {

    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private TextView signUpText;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView tr;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        phoneInput = findViewById(R.id.edit_text_phone);
        passwordInput = findViewById(R.id.edit_text_password);
        loginButton = findViewById(R.id.button_login);
        signUpText = findViewById(R.id.text_sign_up);

        // Navigate to signup
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(this, signup_page.class);
            startActivity(intent);
        });

        // Login button click
        loginButton.setOnClickListener(v -> loginUser());


        tr = findViewById(R.id.text_forgot_password);
        tr.setOnClickListener(view -> {
            Intent intent = new Intent(login_page.this, Forgot_pass.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, go to MainActivity
            navigateToMainActivity();
        }
    }

    private void loginUser() {
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
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
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // Convert phone to email format for Firebase Auth
        String email = phone + "@hockify.app";

        // Sign in with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");

                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d("LoginPage", "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(login_page.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        // Sign in failed
                        Log.w("LoginPage", "signInWithEmail:failure", task.getException());
                        Toast.makeText(login_page.this, 
                            "Login failed: " + task.getException().getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                });




    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(login_page.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
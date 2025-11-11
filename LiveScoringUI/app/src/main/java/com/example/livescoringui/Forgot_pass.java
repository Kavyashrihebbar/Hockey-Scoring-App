package com.example.livescoringui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Forgot_pass extends AppCompatActivity {
    Button forg;
    TextView ba;
    Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_pass);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        forg = findViewById(R.id.btn_reset_password);

        forg.setOnClickListener(view -> {
            Toast.makeText(this, "Reset Password link has been send.", Toast.LENGTH_SHORT).show();
        });

        ba = findViewById(R.id.link_back_to_login);

        ba.setOnClickListener(view -> {
            finish();
        });

        btn = findViewById(R.id.btn_reset_password);

        btn.setOnClickListener(view -> {
            Toast.makeText(this, "Forgot password link has been send", Toast.LENGTH_SHORT).show();
        });


    }
}
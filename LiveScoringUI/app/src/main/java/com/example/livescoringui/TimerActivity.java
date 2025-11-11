package com.example.livescoringui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class TimerActivity extends AppCompatActivity {

    private TextView timerMinutes, timerSeconds;
    private ImageView backArrowTimer, playPauseButton, stopTimerButton;
    private ProgressBar circularProgress;
    
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private boolean isPaused = false;
    private long timeLeftInMillis = 1200000; // 20 minutes in milliseconds (standard hockey period)
    private long totalTimeInMillis = 1200000; // Total time for progress calculation
    
    private String matchId;
    private FirebaseFirestore db;
    private int tickCount = 0; // Counter to update Firebase every 5 seconds
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        
        // Get match ID from intent
        Intent intent = getIntent();
        matchId = intent.getStringExtra("matchId");
        
        initializeViews();
        setupClickListeners();
        loadTimerState();
        updateTimerDisplay();
        updateProgressBar();
    }
    
    private void initializeViews() {
        timerMinutes = findViewById(R.id.timerMinutes);
        timerSeconds = findViewById(R.id.timerSeconds);
        backArrowTimer = findViewById(R.id.backArrowTimer);
        playPauseButton = findViewById(R.id.playPauseButton);
        stopTimerButton = findViewById(R.id.stopTimerButton);
        circularProgress = findViewById(R.id.circularProgress);
    }
    
    private void setupClickListeners() {
        backArrowTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                finish();
            }
        });
        
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });
        
        stopTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
            }
        });
    }
    
    private void startTimer() {
        tickCount = 0;
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerDisplay();
                updateProgressBar();
                
                // Update Firebase every 2 seconds to reduce writes
                tickCount++;
                if (tickCount % 2 == 0) {
                    saveTimerState();
                }
            }
            
            @Override
            public void onFinish() {
                isTimerRunning = false;
                isPaused = false;
                playPauseButton.setImageResource(R.drawable.ic_play);
                
                // Timer finished
                timeLeftInMillis = 0;
                updateTimerDisplay();
                updateProgressBar();
                saveTimerState();
                Toast.makeText(TimerActivity.this, "⏰ Period ended!", Toast.LENGTH_LONG).show();
            }
        }.start();
        
        isTimerRunning = true;
        isPaused = false;
        playPauseButton.setImageResource(R.drawable.ic_pause);
        saveTimerState();
    }
    
    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        isPaused = true;
        playPauseButton.setImageResource(R.drawable.ic_play);
        saveTimerState();
    }
    
    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        isPaused = false;
        timeLeftInMillis = totalTimeInMillis; // Reset to 20 minutes
        playPauseButton.setImageResource(R.drawable.ic_play);
        updateTimerDisplay();
        updateProgressBar();
        saveTimerState();
    }
    
    private void saveTimerState() {
        if (matchId != null && db != null) {
            db.collection("Matches").document(matchId)
                    .update(
                            "timerRunning", isTimerRunning,
                            "timeLeftInMillis", timeLeftInMillis
                    )
                    .addOnFailureListener(e -> {
                        android.util.Log.e("TimerActivity", "Error saving timer state", e);
                    });
        }
    }
    
    private void loadTimerState() {
        if (matchId != null && db != null) {
            db.collection("Matches").document(matchId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean running = documentSnapshot.getBoolean("timerRunning");
                            Long timeLeft = documentSnapshot.getLong("timeLeftInMillis");
                            
                            if (timeLeft != null && timeLeft > 0) {
                                timeLeftInMillis = timeLeft;
                                updateTimerDisplay();
                                updateProgressBar();
                            }
                            
                            if (running != null && running) {
                                startTimer();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("TimerActivity", "Error loading timer state", e);
                    });
        }
    }
    
    private void updateTimerDisplay() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        
        timerMinutes.setText(String.format("%02d", minutes));
        timerSeconds.setText(String.format("%02d", seconds));
    }
    
    private void updateProgressBar() {
        // Calculate progress (0-100) based on time elapsed
        long timeElapsed = totalTimeInMillis - timeLeftInMillis;
        int progress = (int) ((timeElapsed * 100) / totalTimeInMillis);
        circularProgress.setProgress(progress);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
    
}
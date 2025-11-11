package com.example.livescoringui;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.WriteBatch;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.playersteams.details.Player;

import java.util.ArrayList;
import java.util.List;

public class LiveScoringActivity extends AppCompatActivity {

    private TextView teamAScore, teamBScore, timer, emptyEventLog;
    private Button teamAGoalButton, teamBGoalButton, teamASavesButton, teamBSavesButton, saveNextButton;
    private ImageView backArrow, clockButton;
    private ImageView majorPenaltyButton, minorPenaltyButton, substitutionButton;
    private ProgressBar timerProgress;
    private LinearLayout eventLogContainer;

    private android.os.Handler timerHandler = new android.os.Handler();
    private Runnable timerRunnable;
    private long lastKnownTimeLeft = 0;
    private long lastUpdateTime = 0;
    private boolean isTimerRunning = false;

    // Modal components
    private FrameLayout modalOverlay;
    private LinearLayout minorPenaltyModal, majorPenaltyModal, substitutionModal;
    private Button minorCancelButton, minorAddButton, majorCancelButton, majorAddButton;
    private Button substitutionCancelButton, substitutionConfirmButton;
    private Spinner minorTeamSpinner, minorPlayerSpinner, minorPenaltyTypeSpinner;
    private Spinner majorTeamSpinner, majorPlayerSpinner, majorPenaltyTypeSpinner;
    private Spinner substitutionTeamSpinner, playerInSpinner, playerOutSpinner;

    private int scoreA = 0;
    private int scoreB = 0;

    // Match data
    private String matchId;
    private String teamAId, teamBId;
    private String teamAName, teamBName;

    // Firebase
    private FirebaseFirestore db;

    // Player data
    private List<Player> teamAPlayers = new ArrayList<>();
    private List<Player> teamBPlayers = new ArrayList<>();
    private List<String> teamAPlayerNames = new ArrayList<>();
    private List<String> teamBPlayerNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MOVE updateMatchesPlayed() from here!

        try {
            setContentView(R.layout.activity_live_scoring);

            // 1. Initialize Firebase (MUST come first)
            db = FirebaseFirestore.getInstance();

            // 2. Get match data from intent
            Intent intent = getIntent();
            matchId = intent.getStringExtra("matchId");
            teamAId = intent.getStringExtra("teamAId");
            teamBId = intent.getStringExtra("teamBId");
            teamAName = intent.getStringExtra("teamAName");
            teamBName = intent.getStringExtra("teamBName");

            android.util.Log.d("LiveScoring", "Match ID: " + matchId);
            android.util.Log.d("LiveScoring", "Team A: " + teamAName + " (ID: " + teamAId + ")");
            android.util.Log.d("LiveScoring", "Team B: " + teamBName + " (ID: " + teamBId + ")");

            // 3. Validate we have the required data
            if (matchId == null || teamAName == null || teamBName == null) {
                Toast.makeText(this, "Error: Missing match data", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // 4. Initialize views, setup listeners, and load data
            initializeViews();
            setupClickListeners();
            loadMatchData();

            // 5. Load players. Call updateMatchesPlayed() inside its success listener.
            loadPlayers();

            loadExistingEvents();
            updateScoreDisplay();

        } catch (Exception e) {
            android.util.Log.e("LiveScoring", "Error in onCreate", e);
            Toast.makeText(this, "Error loading activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }


    private void initializeViews() {
        try {
            // Set team names
            TextView teamANameLabel = findViewById(R.id.teamANameLabel);
            TextView teamBNameLabel = findViewById(R.id.teamBNameLabel);

            if (teamANameLabel != null && teamAName != null) {
                teamANameLabel.setText(teamAName.toUpperCase());
            }
            if (teamBNameLabel != null && teamBName != null) {
                teamBNameLabel.setText(teamBName.toUpperCase());
            }

            teamAScore = findViewById(R.id.teamAScore);
            teamBScore = findViewById(R.id.teamBScore);
            timer = findViewById(R.id.timer);
            timerProgress = findViewById(R.id.timerProgress);
            eventLogContainer = findViewById(R.id.eventLogContainer);
            emptyEventLog = findViewById(R.id.emptyEventLog);

            teamAGoalButton = findViewById(R.id.teamAGoalButton);
            teamBGoalButton = findViewById(R.id.teamBGoalButton);
            teamASavesButton = findViewById(R.id.teamASavesButton);
            teamBSavesButton = findViewById(R.id.teamBSavesButton);
            saveNextButton = findViewById(R.id.saveNextButton);

            backArrow = findViewById(R.id.backArrow);
            clockButton = findViewById(R.id.clockButton);

            // Control buttons for modals
            majorPenaltyButton = findViewById(R.id.majorPenaltyButton);
            minorPenaltyButton = findViewById(R.id.minorPenaltyButton);
            substitutionButton = findViewById(R.id.substitutionButton);

            // Modal components
            modalOverlay = findViewById(R.id.modalOverlay);
            minorPenaltyModal = findViewById(R.id.minorPenaltyModal);
            majorPenaltyModal = findViewById(R.id.majorPenaltyModal);
            substitutionModal = findViewById(R.id.substitutionModal);

            // Modal buttons
            minorCancelButton = findViewById(R.id.minorCancelButton);
            minorAddButton = findViewById(R.id.minorAddButton);
            majorCancelButton = findViewById(R.id.majorCancelButton);
            majorAddButton = findViewById(R.id.majorAddButton);
            substitutionCancelButton = findViewById(R.id.substitutionCancelButton);
            substitutionConfirmButton = findViewById(R.id.substitutionConfirmButton);

            // Spinners
            minorTeamSpinner = findViewById(R.id.minorTeamSpinner);
            minorPlayerSpinner = findViewById(R.id.minorPlayerSpinner);
            minorPenaltyTypeSpinner = findViewById(R.id.minorPenaltyTypeSpinner);
            majorTeamSpinner = findViewById(R.id.majorTeamSpinner);
            majorPlayerSpinner = findViewById(R.id.majorPlayerSpinner);
            majorPenaltyTypeSpinner = findViewById(R.id.majorPenaltyTypeSpinner);
            substitutionTeamSpinner = findViewById(R.id.substitutionTeamSpinner);
            playerInSpinner = findViewById(R.id.playerInSpinner);
            playerOutSpinner = findViewById(R.id.playerOutSpinner);

            android.util.Log.d("LiveScoring", "All views initialized successfully");
        } catch (Exception e) {
            android.util.Log.e("LiveScoring", "Error initializing views", e);
            throw e;
        }
    }

    private void setupClickListeners() {
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExitConfirmation();
            }
        });

        teamAGoalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGoalScorerDialog(teamAName, teamAId, true);
            }
        });

        teamBGoalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGoalScorerDialog(teamBName, teamBId, false);
            }
        });

        teamASavesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveDialog(teamAName, teamAId);
            }
        });

        teamBSavesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveDialog(teamBName, teamBId);
            }
        });

        saveNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save match and return to previous screen
                updateMatchScore();
                Toast.makeText(LiveScoringActivity.this, "Match saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        clockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to timer activity
                Intent intent = new Intent(LiveScoringActivity.this, TimerActivity.class);
                intent.putExtra("matchId", matchId);
                startActivity(intent);
            }
        });

        // Control button click listeners for modals
        majorPenaltyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMajorPenaltyModal();
            }
        });

        minorPenaltyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMinorPenaltyModal();
            }
        });

        substitutionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubstitutionModal();
            }
        });

        // Modal button listeners
        minorCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideModal();
            }
        });

        minorAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMinorPenalty();
            }
        });

        majorCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideModal();
            }
        });

        majorAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMajorPenalty();
            }
        });

        substitutionCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideModal();
            }
        });

        substitutionConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSubstitution();
            }
        });

        // Modal overlay click listener
        modalOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideModal();
            }
        });

        // Timer click listener - also navigates to timer
        timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LiveScoringActivity.this, TimerActivity.class);
                intent.putExtra("matchId", matchId);
                startActivity(intent);
            }
        });
    }

    private void updateScoreDisplay() {
        teamAScore.setText(String.valueOf(scoreA));
        teamBScore.setText(String.valueOf(scoreB));
    }

    private void loadMatchData() {
        if (matchId != null) {
            // Real-time listener for match data including timer
            db.collection("Matches").document(matchId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            android.util.Log.e("LiveScoring", "Error loading match data", error);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            MatchModel match = documentSnapshot.toObject(MatchModel.class);
                            if (match != null) {
                                scoreA = match.getTeamAScore();
                                scoreB = match.getTeamBScore();
                                updateScoreDisplay();

                                android.util.Log.d("LiveScoring", "Loaded match scores: " + scoreA + " - " + scoreB);
                            }

                            // Update timer display in real-time
                            Long timeLeft = documentSnapshot.getLong("timeLeftInMillis");
                            Boolean timerRunning = documentSnapshot.getBoolean("timerRunning");

                            if (timeLeft != null) {
                                updateTimerFromFirebase(timeLeft);
                            }

                            // Start or stop local countdown based on timer status
                            if (timerRunning != null) {
                                if (timerRunning && !isTimerRunning) {
                                    isTimerRunning = true;
                                    startLocalTimerCountdown();
                                    android.util.Log.d("LiveScoring", "Timer started: " + formatTime(timeLeft != null ? timeLeft : 0));
                                } else if (!timerRunning && isTimerRunning) {
                                    isTimerRunning = false;
                                    stopLocalTimerCountdown();
                                    android.util.Log.d("LiveScoring", "Timer stopped");
                                }
                            }
                        }
                    });
        }
    }

    private void updateTimerFromFirebase(long timeLeftInMillis) {
        lastKnownTimeLeft = timeLeftInMillis;
        lastUpdateTime = System.currentTimeMillis();

        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeText = String.format("%02d:%02d", minutes, seconds);
        timer.setText(timeText);

        // Update progress bar
        long totalTime = 1200000; // 20 minutes
        long timeElapsed = totalTime - timeLeftInMillis;
        int progress = (int) ((timeElapsed * 100) / totalTime);
        timerProgress.setProgress(Math.max(0, Math.min(100, progress)));
    }

    private void startLocalTimerCountdown() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimerRunning && lastKnownTimeLeft > 0) {
                    // Calculate time elapsed since last Firebase update
                    long elapsedSinceUpdate = System.currentTimeMillis() - lastUpdateTime;
                    long currentTimeLeft = lastKnownTimeLeft - elapsedSinceUpdate;

                    if (currentTimeLeft > 0) {
                        int minutes = (int) (currentTimeLeft / 1000) / 60;
                        int seconds = (int) (currentTimeLeft / 1000) % 60;
                        timer.setText(String.format("%02d:%02d", minutes, seconds));

                        // Update progress bar
                        long totalTime = 1200000;
                        long timeElapsed = totalTime - currentTimeLeft;
                        int progress = (int) ((timeElapsed * 100) / totalTime);
                        timerProgress.setProgress(Math.max(0, Math.min(100, progress)));
                    } else {
                        timer.setText("00:00");
                        timerProgress.setProgress(100);
                    }

                    // Update every second
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        timerHandler.post(timerRunnable);
    }

    private void stopLocalTimerCountdown() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private String formatTime(long timeInMillis) {
        int minutes = (int) (timeInMillis / 1000) / 60;
        int seconds = (int) (timeInMillis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateMatchScore() {
        if (matchId != null) {
            db.collection("Matches").document(matchId)
                    .update(
                            "teamAScore", scoreA,
                            "teamBScore", scoreB
                    )
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("LiveScoring", "Score updated: " + scoreA + " - " + scoreB);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("LiveScoring", "Error updating score", e);
                        Toast.makeText(LiveScoringActivity.this, "Error updating score", Toast.LENGTH_SHORT).show();
                    });
        }
    }



    // Inside LiveScoring.java

    private void loadPlayers() {
        // Load Team A players
        db.collection("Players")
                .whereEqualTo("TeamId", teamAId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshotsA -> {
                    teamAPlayers.clear();
                    teamAPlayerNames.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshotsA) {
                        try {
                            Player player = document.toObject(Player.class);
                            if (player != null && player.getName() != null) {
                                // *** CRITICAL: Set the Player ID (Document ID) ***
                                player.setPlayerId(document.getId());

                                teamAPlayers.add(player);
                                String displayName = player.getJersey() != null ?
                                        "#" + player.getJersey() + " " + player.getName() :
                                        player.getName();
                                teamAPlayerNames.add(displayName);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("LiveScoring", "Error parsing Team A player", e);
                        }
                    }

                    if (teamAPlayerNames.isEmpty()) {
                        teamAPlayerNames.add("No players available");
                    }

                    android.util.Log.d("LiveScoring", "Team A has " + teamAPlayers.size() + " players. Starting Team B load.");

                    // -------------------------------------------------------------
                    // NESTED QUERY: Start Team B load only after Team A is done
                    // -------------------------------------------------------------

                    loadTeamBPlayersAndFinish();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LiveScoring", "Error loading Team A players", e);
                    teamAPlayerNames.add("Error loading players");
                    // Attempt to load Team B anyway, but logging the error
                    loadTeamBPlayersAndFinish();
                });
    }

    /**
     * Nested function to load Team B players and execute final completion steps.
     */
    private void loadTeamBPlayersAndFinish() {
        db.collection("Players")
                .whereEqualTo("TeamId", teamBId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshotsB -> {
                    teamBPlayers.clear();
                    teamBPlayerNames.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshotsB) {
                        try {
                            Player player = document.toObject(Player.class);
                            if (player != null && player.getName() != null) {
                                // *** CRITICAL: Set the Player ID (Document ID) ***
                                player.setPlayerId(document.getId());

                                teamBPlayers.add(player);
                                String displayName = player.getJersey() != null ?
                                        "#" + player.getJersey() + " " + player.getName() :
                                        player.getName();
                                teamBPlayerNames.add(displayName);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("LiveScoring", "Error parsing Team B player", e);
                        }
                    }

                    if (teamBPlayerNames.isEmpty()) {
                        teamBPlayerNames.add("No players available");
                    }

                    android.util.Log.d("LiveScoring", "Team B has " + teamBPlayers.size() + " players. All players loaded.");

                    // -------------------------------------------------------------------
                    // *** CRITICAL STEP: Call updateMatchesPlayed() here ***
                    updateMatchesPlayed();
                    // -------------------------------------------------------------------

                    setupSpinners();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LiveScoring", "Error loading Team B players", e);
                    teamBPlayerNames.add("Error loading players");

                    // -------------------------------------------------------------------
                    // *** CRITICAL STEP: Call updateMatchesPlayed() even on failure ***
                    updateMatchesPlayed();
                    // -------------------------------------------------------------------

                    setupSpinners();
                });
    }

    private void setupSpinners() {
        // Team options - use actual team names
        String[] teams = {teamAName, teamBName};
        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teams);
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        minorTeamSpinner.setAdapter(teamAdapter);
        majorTeamSpinner.setAdapter(teamAdapter);
        substitutionTeamSpinner.setAdapter(teamAdapter);

        // Set up team selection listeners to update player spinners
        minorTeamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePlayerSpinner(minorPlayerSpinner, position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        majorTeamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePlayerSpinner(majorPlayerSpinner, position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        substitutionTeamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePlayerSpinner(playerInSpinner, position);
                updatePlayerSpinner(playerOutSpinner, position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Initialize with Team A players
        updatePlayerSpinner(minorPlayerSpinner, 0);
        updatePlayerSpinner(majorPlayerSpinner, 0);
        updatePlayerSpinner(playerInSpinner, 0);
        updatePlayerSpinner(playerOutSpinner, 0);

        // Penalty types
        String[] penaltyTypes = {"Tripping", "Slashing", "High Sticking", "Boarding", "Checking", "Interference"};
        ArrayAdapter<String> penaltyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, penaltyTypes);
        penaltyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        minorPenaltyTypeSpinner.setAdapter(penaltyAdapter);
        majorPenaltyTypeSpinner.setAdapter(penaltyAdapter);
    }

    private void updatePlayerSpinner(Spinner spinner, int teamPosition) {
        List<String> playerNames = teamPosition == 0 ? teamAPlayerNames : teamBPlayerNames;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, playerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void showMinorPenaltyModal() {
        modalOverlay.setVisibility(View.VISIBLE);
        minorPenaltyModal.setVisibility(View.VISIBLE);
        majorPenaltyModal.setVisibility(View.GONE);
        substitutionModal.setVisibility(View.GONE);

        // Animate modal appearance
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.modal_slide_up);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        modalOverlay.startAnimation(fadeIn);
        minorPenaltyModal.startAnimation(slideUp);
    }

    private void showMajorPenaltyModal() {
        modalOverlay.setVisibility(View.VISIBLE);
        majorPenaltyModal.setVisibility(View.VISIBLE);
        minorPenaltyModal.setVisibility(View.GONE);
        substitutionModal.setVisibility(View.GONE);

        // Animate modal appearance
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.modal_slide_up);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        modalOverlay.startAnimation(fadeIn);
        majorPenaltyModal.startAnimation(slideUp);
    }

    private void showSubstitutionModal() {
        modalOverlay.setVisibility(View.VISIBLE);
        substitutionModal.setVisibility(View.VISIBLE);
        minorPenaltyModal.setVisibility(View.GONE);
        majorPenaltyModal.setVisibility(View.GONE);

        // Animate modal appearance
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.modal_slide_up);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        modalOverlay.startAnimation(fadeIn);
        substitutionModal.startAnimation(slideUp);
    }

    private void hideModal() {
        modalOverlay.setVisibility(View.GONE);
        minorPenaltyModal.setVisibility(View.GONE);
        majorPenaltyModal.setVisibility(View.GONE);
        substitutionModal.setVisibility(View.GONE);
    }

    private void addMinorPenalty() {
        int teamPosition = minorTeamSpinner.getSelectedItemPosition();
        int playerPosition = minorPlayerSpinner.getSelectedItemPosition();
        String penaltyType = minorPenaltyTypeSpinner.getSelectedItem().toString();

        String teamId = teamPosition == 0 ? teamAId : teamBId;
        String teamName = teamPosition == 0 ? teamAName : teamBName;
        List<Player> players = teamPosition == 0 ? teamAPlayers : teamBPlayers;

        if (players.isEmpty()) {
            Toast.makeText(this, "No players available for " + teamName, Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Using ONLY what is available in your Player class ---
        String playerName = players.get(playerPosition).getName();
        long timestamp = System.currentTimeMillis();
        String penaltyDescription = penaltyType + " (Minor - 2:00)";

        // Create penalty event (Using your original constructor)
        MatchModel.PenaltyEvent penalty = new MatchModel.PenaltyEvent(teamId, playerName, penaltyDescription, timestamp);

        // --- NEW: Find the Player ID by Name/Team (Less efficient!) ---
        db.collection("Players")
                .whereEqualTo("PlayerName", playerName)
                .whereEqualTo("TeamId", teamId)
                .limit(1) // Assuming PlayerName + TeamId is unique
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Error: Player not found in database.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Get the unique Firestore Document ID (the playerId)
                    String playerId = querySnapshot.getDocuments().get(0).getId();

                    // --- Batched Write to Update Match AND Player Stats ---
                    WriteBatch batch = db.batch();

                    // 1. Update the Match document (using FieldValue.arrayUnion to append)
                    DocumentReference matchRef = db.collection("Matches").document(matchId);
                    batch.update(matchRef, "penalties", FieldValue.arrayUnion(penalty));

                    // 2. Update the Player document (Increment penalty stat)
                    DocumentReference playerRef = db.collection("Players").document(playerId);
                    batch.update(playerRef, "penaltyCount", FieldValue.increment(1));

                    // 3. Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Minor penalty added for " + playerName, Toast.LENGTH_SHORT).show();
                                android.util.Log.d("LiveScoring", "Minor penalty added (ID: " + playerId + ")");
                                addEventToLog("🟨 MINOR PENALTY", playerName + " - " + penaltyType, teamName, "#FFC107");
                                hideModal();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error adding penalty", Toast.LENGTH_SHORT).show();
                                android.util.Log.e("LiveScoring", "Error adding penalty or updating player stats", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Database connection error.", Toast.LENGTH_SHORT).show();
                    android.util.Log.e("LiveScoring", "Error finding player ID", e);
                });
    }

    private void updateMatchesPlayed(List<Player> teamAPlayers, List<Player> teamBPlayers) {
        WriteBatch batch = db.batch();

        // Combine both team lists for efficiency
        List<Player> allPlayers = new ArrayList<>(teamAPlayers);
        allPlayers.addAll(teamBPlayers);

        for (Player player : allPlayers) {
            // Query for the playerId using the player's name and teamId
            db.collection("Players")
                    .whereEqualTo("PlayerName", player.getName())
                    .whereEqualTo("TeamId", player.getTeamId())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String playerId = querySnapshot.getDocuments().get(0).getId();
                            DocumentReference playerRef = db.collection("Players").document(playerId);
                            // Add the increment to the batch
                            batch.update(playerRef, "matchesPlayed", FieldValue.increment(1));
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("LiveScoring", "Error finding player ID for matchesPlayed", e);
                    });
        }

        // Commit all updates
        // Note: This needs proper coordination if batch is built asynchronously.
        // For simplicity, commit it *after* the loops finish in a synchronous manner
        // or use a completion counter for asynchronous batches.
        // Since this is already complex, use the original synchronous batch for now
        // and ensure your Player objects passed to this method already contain the ID
        // (by doing the *recommended* changes later).
    }

    private void addMajorPenalty() {
        int teamPosition = majorTeamSpinner.getSelectedItemPosition();
        int playerPosition = majorPlayerSpinner.getSelectedItemPosition();
        String penaltyType = majorPenaltyTypeSpinner.getSelectedItem().toString();

        String teamId = teamPosition == 0 ? teamAId : teamBId;
        String teamName = teamPosition == 0 ? teamAName : teamBName;
        List<Player> players = teamPosition == 0 ? teamAPlayers : teamBPlayers;

        if (players.isEmpty()) {
            Toast.makeText(this, "No players available for " + teamName, Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Using ONLY what is available in your Player class ---
        String playerName = players.get(playerPosition).getName();
        long timestamp = System.currentTimeMillis();
        String penaltyDescription = penaltyType + " (Major - 5:00)";

        // Create penalty event (Using your original constructor: teamId, playerName, type, timestamp)
        MatchModel.PenaltyEvent penalty = new MatchModel.PenaltyEvent(teamId, playerName, penaltyDescription, timestamp);

        // --- CRITICAL: Find the Player ID by Name/Team ---
        // This is the extra read operation we are doing to avoid modifying your models.
        db.collection("Players")
                .whereEqualTo("PlayerName", playerName)
                .whereEqualTo("TeamId", teamId)
                .limit(1) // Assuming PlayerName + TeamId is unique
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Error: Player not found in database. Cannot update stats.", Toast.LENGTH_LONG).show();
                        android.util.Log.e("LiveScoring", "Player not found: " + playerName);
                        return;
                    }

                    // Get the unique Firestore Document ID (the playerId)
                    String playerId = querySnapshot.getDocuments().get(0).getId();

                    // --- Batched Write to Update Match AND Player Stats ---
                    WriteBatch batch = db.batch();

                    // 1. Update the Match document (using FieldValue.arrayUnion to append)
                    DocumentReference matchRef = db.collection("Matches").document(matchId);
                    batch.update(matchRef, "penalties", FieldValue.arrayUnion(penalty));

                    // 2. Update the Player document (Increment penalty stat)
                    DocumentReference playerRef = db.collection("Players").document(playerId);
                    // The 'penaltyCount' field must exist (even if 0) on the Player document
                    batch.update(playerRef, "penaltyCount", FieldValue.increment(1));

                    // 3. Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Major penalty added for " + playerName, Toast.LENGTH_SHORT).show();
                                android.util.Log.d("LiveScoring", "Major penalty added (Player ID: " + playerId + ")");
                                addEventToLog("🟥 MAJOR PENALTY", playerName + " - " + penaltyType, teamName, "#F44336");
                                hideModal();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error adding penalty", Toast.LENGTH_SHORT).show();
                                android.util.Log.e("LiveScoring", "Error adding penalty or updating player stats", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Database connection error.", Toast.LENGTH_SHORT).show();
                    android.util.Log.e("LiveScoring", "Error finding player ID for major penalty", e);
                });
    }

    private void addSubstitution() {
        int teamPosition = substitutionTeamSpinner.getSelectedItemPosition();
        int playerInPosition = playerInSpinner.getSelectedItemPosition();
        int playerOutPosition = playerOutSpinner.getSelectedItemPosition();

        String teamId = teamPosition == 0 ? teamAId : teamBId;
        String teamName = teamPosition == 0 ? teamAName : teamBName;
        List<Player> players = teamPosition == 0 ? teamAPlayers : teamBPlayers;

        if (players.isEmpty()) {
            Toast.makeText(this, "No players available for " + teamName, Toast.LENGTH_SHORT).show();
            return;
        }

        if (playerInPosition == playerOutPosition) {
            Toast.makeText(this, "Please select different players", Toast.LENGTH_SHORT).show();
            return;
        }

        String playerIn = players.get(playerInPosition).getName();
        String playerOut = players.get(playerOutPosition).getName();
        long timestamp = System.currentTimeMillis();

        // Create substitution event
        MatchModel.SubstitutionEvent substitution = new MatchModel.SubstitutionEvent(teamId, playerIn, playerOut, timestamp);

        // Update match in Firebase
        db.collection("Matches").document(matchId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    MatchModel match = documentSnapshot.toObject(MatchModel.class);
                    if (match != null) {
                        List<MatchModel.SubstitutionEvent> substitutions = match.getSubstitutions();
                        if (substitutions == null) {
                            substitutions = new ArrayList<>();
                        }
                        substitutions.add(substitution);

                        db.collection("Matches").document(matchId)
                                .update("substitutions", substitutions)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Substitution recorded: " + playerIn + " in, " + playerOut + " out", Toast.LENGTH_SHORT).show();
                                    android.util.Log.d("LiveScoring", "Substitution: " + playerIn + " in, " + playerOut + " out");
                                    addEventToLog("🔄 SUBSTITUTION", playerIn + " ↔ " + playerOut, teamName, "#2196F3");
                                    hideModal();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error recording substitution", Toast.LENGTH_SHORT).show();
                                    android.util.Log.e("LiveScoring", "Error recording substitution", e);
                                });
                    }
                });
    }

    private void showSaveDialog(String teamName, String teamId) {
        // Show dialog to select goalkeeper who made the save
        List<Player> players = teamId.equals(teamAId) ? teamAPlayers : teamBPlayers;

        if (players.isEmpty()) {
            Toast.makeText(this, "No players available for " + teamName, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] playerNames = new String[players.size()];
        for (int i = 0; i < players.size(); i++) {
            playerNames[i] = players.get(i).getJersey() != null ?
                    "#" + players.get(i).getJersey() + " " + players.get(i).getName() :
                    players.get(i).getName();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Goalkeeper - " + teamName)
                .setItems(playerNames, (dialog, which) -> {
                    String playerName = players.get(which).getName();
                    recordSave(teamId, playerName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateMatchesPlayed() {
        // Combine both team lists
        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(teamAPlayers);
        allPlayers.addAll(teamBPlayers);

        // Prepare a single batch for all player updates
        WriteBatch batch = db.batch();

        for (Player player : allPlayers) {
            String playerId = player.getPlayerId();

            // If playerId is null (meaning loading was incorrect), skip the player
            if (playerId == null || playerId.isEmpty()) {
                android.util.Log.e("LiveScoring", "Skipping player with null ID: " + player.getName());
                continue;
            }

            // 1. Reference the Player document
            DocumentReference playerRef = db.collection("Players").document(playerId);

            // 2. Increment the 'matchesPlayed' field
            batch.update(playerRef, "matchesPlayed", FieldValue.increment(1));
        }

        // 3. Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("LiveScoring", "Successfully updated matchesPlayed for all " + allPlayers.size() + " players.");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LiveScoring", "Failed to update matchesPlayed batch.", e);
                    Toast.makeText(this, "Error updating matches played stat.", Toast.LENGTH_LONG).show();
                });
    }

    private void recordSave(String teamId, String playerName) {
        // Update saves count in Firebase
        String saveField = teamId.equals(teamAId) ? "teamASaves" : "teamBSaves";
        String teamName = teamId.equals(teamAId) ? teamAName : teamBName;

        db.collection("Matches").document(matchId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentSaves = documentSnapshot.getLong(saveField);
                        int newSaves = (currentSaves != null ? currentSaves.intValue() : 0) + 1;

                        db.collection("Matches").document(matchId)
                                .update(saveField, newSaves)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "🧤 Save recorded for " + playerName, Toast.LENGTH_SHORT).show();
                                    android.util.Log.d("LiveScoring", "Save recorded: " + playerName);
                                    addEventToLog("🧤 SAVE", playerName, teamName, "#00BCD4");
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error recording save", Toast.LENGTH_SHORT).show();
                                    android.util.Log.e("LiveScoring", "Error recording save", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error recording save", Toast.LENGTH_SHORT).show();
                    android.util.Log.e("LiveScoring", "Error recording save", e);
                });
    }

    private void showGoalScorerDialog(String teamName, String teamId, boolean isTeamA) {
        List<Player> players = isTeamA ? teamAPlayers : teamBPlayers;

        if (players.isEmpty()) {
            // No players, just increment score
            if (isTeamA) {
                scoreA++;
            } else {
                scoreB++;
            }
            updateScoreDisplay();
            updateMatchScore();
            Toast.makeText(this, "Goal scored for " + teamName, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] playerNames = new String[players.size() + 1];
        playerNames[0] = "Unknown Player";
        for (int i = 0; i < players.size(); i++) {
            playerNames[i + 1] = players.get(i).getJersey() != null ?
                    "#" + players.get(i).getJersey() + " " + players.get(i).getName() :
                    players.get(i).getName();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Who scored? - " + teamName)
                .setItems(playerNames, (dialog, which) -> {
                    String playerName = which == 0 ? "Unknown" : players.get(which - 1).getName();
                    recordGoal(teamId, playerName, isTeamA);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void recordGoal(String teamId, String playerName, boolean isTeamA) {
        long timestamp = System.currentTimeMillis();
        String teamName = isTeamA ? teamAName : teamBName;

        // Create goal event (Using your assumed original constructor: teamId, playerName, timestamp)
        // NOTE: If you decide to include playerId in the MatchModel.GoalEvent later,
        // you would update this line.
        MatchModel.GoalEvent goal = new MatchModel.GoalEvent(teamId, playerName, timestamp);

        // Increment local score
        if (isTeamA) {
            scoreA++;
        } else {
            scoreB++;
        }
        updateScoreDisplay(); // Update display immediately

        // --- CRITICAL: Find the Player ID by Name/Team ---
        // This is the read operation we perform to find the player's unique document ID.
        db.collection("Players")
                .whereEqualTo("PlayerName", playerName)
                .whereEqualTo("TeamId", teamId)
                .limit(1) // Assuming PlayerName + TeamId is unique within the context
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Error: Player not found in database. Cannot update stats.", Toast.LENGTH_LONG).show();
                        // Revert local score change since stat update failed
                        if (isTeamA) scoreA--; else scoreB--;
                        updateScoreDisplay();
                        return;
                    }

                    // Get the unique Firestore Document ID (the playerId)
                    String playerId = querySnapshot.getDocuments().get(0).getId();

                    // --- Batched Write to Update Match AND Player Stats ---
                    WriteBatch batch = db.batch();

                    // 1. Update the Match document
                    DocumentReference matchRef = db.collection("Matches").document(matchId);
                    batch.update(
                            matchRef,
                            "goals", FieldValue.arrayUnion(goal), // Safely adds the goal event
                            "teamAScore", scoreA,                  // Updates the new score
                            "teamBScore", scoreB
                    );

                    // 2. Update the Player document (Increment goal stat)
                    DocumentReference playerRef = db.collection("Players").document(playerId);
                    // The 'goalCount' field must exist (even if 0) on the Player document
                    batch.update(playerRef, "goalCount", FieldValue.increment(1));

                    // 3. Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "⚽ Goal! " + playerName, Toast.LENGTH_SHORT).show();
                                android.util.Log.d("LiveScoring", "Goal recorded (Player ID: " + playerId + ")");
                                addEventToLog("⚽ GOAL", playerName, teamName, "#4CAF50");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error recording goal", Toast.LENGTH_SHORT).show();
                                android.util.Log.e("LiveScoring", "Error recording goal or updating player stats", e);
                                // Revert local score change if batch commit fails
                                if (isTeamA) scoreA--; else scoreB--;
                                updateScoreDisplay();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Database connection error.", Toast.LENGTH_SHORT).show();
                    android.util.Log.e("LiveScoring", "Error finding player ID for goal", e);
                    // Revert local score change if query fails
                    if (isTeamA) scoreA--; else scoreB--;
                    updateScoreDisplay();
                });
    }

    private void showExitConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Exit Live Scoring?")
                .setMessage("Are you sure you want to exit? All data has been saved automatically.")
                .setPositiveButton("Exit", (dialog, which) -> {
                    updateMatchScore();
                    finish();
                })
                .setNegativeButton("Stay", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showExitConfirmation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocalTimerCountdown();
    }

    private void addEventToLog(String eventType, String details, String teamName, String color) {
        // Hide empty message
        if (emptyEventLog != null) {
            emptyEventLog.setVisibility(View.GONE);
        }

        // Create event row
        LinearLayout eventRow = new LinearLayout(this);
        eventRow.setOrientation(LinearLayout.HORIZONTAL);
        eventRow.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        eventRow.setLayoutParams(params);
        eventRow.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"));

        // Event icon and type
        TextView eventTypeView = new TextView(this);
        eventTypeView.setText(eventType);
        eventTypeView.setTextSize(14);
        eventTypeView.setTextColor(android.graphics.Color.parseColor(color));
        eventTypeView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        eventTypeView.setLayoutParams(typeParams);

        // Details
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                2.0f
        );
        detailsLayout.setLayoutParams(detailsParams);

        TextView teamView = new TextView(this);
        teamView.setText(teamName);
        teamView.setTextSize(13);
        teamView.setTextColor(android.graphics.Color.parseColor("#333333"));
        teamView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView detailsView = new TextView(this);
        detailsView.setText(details);
        detailsView.setTextSize(12);
        detailsView.setTextColor(android.graphics.Color.parseColor("#666666"));

        detailsLayout.addView(teamView);
        detailsLayout.addView(detailsView);

        // Time
        TextView timeView = new TextView(this);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        timeView.setText(sdf.format(new java.util.Date()));
        timeView.setTextSize(11);
        timeView.setTextColor(android.graphics.Color.parseColor("#999999"));
        timeView.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeView.setLayoutParams(timeParams);

        eventRow.addView(eventTypeView);
        eventRow.addView(detailsLayout);
        eventRow.addView(timeView);

        // Add to container at the top (most recent first)
        eventLogContainer.addView(eventRow, 0);
    }

    private void loadExistingEvents() {
        db.collection("Matches").document(matchId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        MatchModel match = documentSnapshot.toObject(MatchModel.class);
                        if (match != null) {
                            boolean hasEvents = false;

                            // Load goals
                            if (match.getGoals() != null && !match.getGoals().isEmpty()) {
                                for (MatchModel.GoalEvent goal : match.getGoals()) {
                                    String teamName = goal.getTeamId().equals(teamAId) ? teamAName : teamBName;
                                    addEventToLog("⚽ GOAL", goal.getPlayerName(), teamName, "#4CAF50");
                                    hasEvents = true;
                                }
                            }

                            // Load penalties
                            if (match.getPenalties() != null && !match.getPenalties().isEmpty()) {
                                for (MatchModel.PenaltyEvent penalty : match.getPenalties()) {
                                    String teamName = penalty.getTeamId().equals(teamAId) ? teamAName : teamBName;
                                    boolean isMinor = penalty.getType().contains("Minor");
                                    String icon = isMinor ? "🟨 MINOR PENALTY" : "🟥 MAJOR PENALTY";
                                    String color = isMinor ? "#FFC107" : "#F44336";
                                    addEventToLog(icon, penalty.getPlayerName() + " - " + penalty.getType(), teamName, color);
                                    hasEvents = true;
                                }
                            }

                            // Load substitutions
                            if (match.getSubstitutions() != null && !match.getSubstitutions().isEmpty()) {
                                for (MatchModel.SubstitutionEvent sub : match.getSubstitutions()) {
                                    String teamName = sub.getTeamId().equals(teamAId) ? teamAName : teamBName;
                                    addEventToLog("🔄 SUBSTITUTION", sub.getPlayerIn() + " ↔ " + sub.getPlayerOut(), teamName, "#2196F3");
                                    hasEvents = true;
                                }
                            }

                            if (!hasEvents && emptyEventLog != null) {
                                emptyEventLog.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LiveScoring", "Error loading events", e);
                });
    }


}
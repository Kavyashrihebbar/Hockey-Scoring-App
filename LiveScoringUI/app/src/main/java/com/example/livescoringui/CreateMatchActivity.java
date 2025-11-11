package com.example.livescoringui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.playersteams.details.Team;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateMatchActivity extends AppCompatActivity {

    private Spinner teamASpinner, teamBSpinner;
    private Button createMatchButton, startLiveButton;
    private ImageView backButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private List<Team> teamsList;
    private List<String> teamNames;
    private String selectedDate = "";
    private String selectedTime = "";
    private Calendar calendar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_match);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        calendar = Calendar.getInstance();

        initializeViews();
        loadTeams();
        setupClickListeners();
    }

    private void initializeViews() {
        teamASpinner = findViewById(R.id.teamASpinner);
        teamBSpinner = findViewById(R.id.teamBSpinner);

        createMatchButton = findViewById(R.id.createMatchButton);
        startLiveButton = findViewById(R.id.startLiveButton);
        backButton = findViewById(R.id.backButton);

        teamsList = new ArrayList<>();
        teamNames = new ArrayList<>();
    }

    // Inside CreateMatchActivity.java

    private void setupClickListeners() {
        // Back button still finishes the activity
        backButton.setOnClickListener(v -> finish());

        // *** MODIFIED LOGIC HERE ***
        // The button associated with R.id.createMatchButton now navigates to the team detail page.
        createMatchButton.setOnClickListener(v -> {
            Intent intent = new Intent(CreateMatchActivity.this, teams_detail_page.class);
            startActivity(intent);
            // Do NOT call finish() here if you want the user to be able to return to this screen.
            // If you want to force them back to the main screen after creating a team, you can call finish().
        });
        // *** END MODIFIED LOGIC ***

        // The Start Live Match button keeps its original functionality.
        startLiveButton.setOnClickListener(v -> createMatch(true));
    }

    private void loadTeams() {
        db.collection("Teams")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    teamsList.clear();
                    teamNames.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Team team = document.toObject(Team.class);
                            // Validate team data
                            if (team != null && team.getTeamName() != null && team.getTeamId() != null) {
                                teamsList.add(team);
                                teamNames.add(team.getTeamName());
                                android.util.Log.d("CreateMatch", "Loaded team: " + team.getTeamName() + " (ID: " + team.getTeamId() + ")");
                            } else {
                                android.util.Log.w("CreateMatch", "Skipping team with null data: " + document.getId());
                            }
                        } catch (Exception e) {
                            android.util.Log.e("CreateMatch", "Error parsing team document: " + document.getId(), e);
                        }
                    }

                    if (teamNames.isEmpty()) {
                        // Show dialog to create teams first
                        showNoTeamsDialog();
                        teamNames.add("No teams available - Create teams first");
                        createMatchButton.setEnabled(false);
                        startLiveButton.setEnabled(false);
                    } else {
                        createMatchButton.setEnabled(true);
                        startLiveButton.setEnabled(true);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, teamNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    teamASpinner.setAdapter(adapter);
                    teamBSpinner.setAdapter(adapter);

                    android.util.Log.d("CreateMatch", "Loaded " + teamsList.size() + " teams");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CreateMatch", "Error loading teams", e);
                    Toast.makeText(this, "Error loading teams: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showNoTeamsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("No Teams Available")
                .setMessage("You need to create at least 2 teams before creating a match. Would you like to create teams now?")
                .setPositiveButton("Create Teams", (dialog, which) -> {
                    Intent intent = new Intent(CreateMatchActivity.this, teams_detail_page.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }




    private void createMatch(boolean startLive) {
        android.util.Log.d("CreateMatch", "createMatch called with startLive: " + startLive);

        int teamAPosition = teamASpinner.getSelectedItemPosition();
        int teamBPosition = teamBSpinner.getSelectedItemPosition();

        android.util.Log.d("CreateMatch", "Teams list size: " + teamsList.size());
        android.util.Log.d("CreateMatch", "Team A position: " + teamAPosition);
        android.util.Log.d("CreateMatch", "Team B position: " + teamBPosition);

        // Validation: Check if teams exist
        if (teamsList.isEmpty()) {
            Toast.makeText(this, "Please create teams first", Toast.LENGTH_LONG).show();
            showNoTeamsDialog();
            return;
        }

        if (teamsList.size() < 2) {
            Toast.makeText(this, "You need at least 2 teams to create a match", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(CreateMatchActivity.this, teams_detail_page.class);
            startActivity(intent);
            return;
        }

        // Validation: Check if same team selected
        if (teamAPosition == teamBPosition) {
            Toast.makeText(this, "Please select different teams", Toast.LENGTH_SHORT).show();
            return;
        }



        Team teamA = teamsList.get(teamAPosition);
        Team teamB = teamsList.get(teamBPosition);

        // Check if teams have players
        checkTeamsHavePlayers(teamA, teamB, startLive);
    }

    private void checkTeamsHavePlayers(Team teamA, Team teamB, boolean startLive) {
        // Validate team IDs
        if (teamA.getTeamId() == null || teamB.getTeamId() == null) {
            android.util.Log.e("CreateMatch", "Team ID is null - TeamA: " + teamA.getTeamId() + ", TeamB: " + teamB.getTeamId());
            Toast.makeText(this, "Error: Invalid team data. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }

        // Just proceed with match creation - skip player check for now
        proceedWithMatchCreation(teamA, teamB, startLive);
    }

    private void proceedWithMatchCreation(Team teamA, Team teamB, boolean startLive) {
        try {
            String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

            android.util.Log.d("CreateMatch", "Creating match - TeamA: " + teamA.getTeamName() + " vs TeamB: " + teamB.getTeamName());

            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());


            MatchModel match = new MatchModel(
                    teamA.getTeamId(),
                    teamB.getTeamId(),
                    teamA.getTeamName(),
                    teamB.getTeamName(),
                    currentDate,
                    currentTime,
                    userId
            );

            if (startLive) {
                match.setStatus("live");
                match.setStartTime(System.currentTimeMillis());
            }

            createMatchButton.setEnabled(false);
            startLiveButton.setEnabled(false);
            createMatchButton.setText("Creating...");
            startLiveButton.setText("Starting...");

            db.collection("Matches")
                    .add(match)
                    .addOnSuccessListener(documentReference -> {
                        String matchId = documentReference.getId();
                        documentReference.update("matchId", matchId)
                                .addOnSuccessListener(aVoid -> {
                                    android.util.Log.d("CreateMatch", "Match created with ID: " + matchId);
                                    Toast.makeText(this, "Match created successfully!", Toast.LENGTH_SHORT).show();

                                    if (startLive) {
                                        // Navigate to LiveScoringActivity
                                        Intent intent = new Intent(CreateMatchActivity.this, LiveScoringActivity.class);
                                        intent.putExtra("matchId", matchId);
                                        intent.putExtra("teamAId", teamA.getTeamId());
                                        intent.putExtra("teamBId", teamB.getTeamId());
                                        intent.putExtra("teamAName", teamA.getTeamName());
                                        intent.putExtra("teamBName", teamB.getTeamName());

                                        android.util.Log.d("CreateMatch", "Starting LiveScoringActivity");
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("CreateMatch", "Error updating matchId", e);
                                    Toast.makeText(this, "Match created but error updating ID", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("CreateMatch", "Error creating match", e);
                        createMatchButton.setEnabled(true);
                        startLiveButton.setEnabled(true);
                        createMatchButton.setText("Create Match");
                        startLiveButton.setText("Start Live Match");
                        Toast.makeText(this, "Error creating match: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            android.util.Log.e("CreateMatch", "Exception in proceedWithMatchCreation", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            createMatchButton.setEnabled(true);
            startLiveButton.setEnabled(true);
            createMatchButton.setText("Create Match");
            startLiveButton.setText("Start Live Match");
        }
    }
}
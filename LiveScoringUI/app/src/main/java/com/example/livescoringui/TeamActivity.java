package com.example.livescoringui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TeamActivity extends AppCompatActivity {

    private EditText teamNameInput;
    private ActivityResultLauncher<Intent> addPlayerLauncher;
    private TextView noPlayersText;
    private boolean isBackButtonPressed = false;

    private FirebaseFirestore db;
    private String teamId; // Firestore-generated TeamId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_details);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        noPlayersText = findViewById(R.id.noPlayersText);
        teamNameInput = findViewById(R.id.teamNameInput);
        ImageView backBtn = findViewById(R.id.backBtn);
        Button saveTeamBtn = findViewById(R.id.saveTeamBtn);
        ImageView editTeamBtn = findViewById(R.id.editTeamBtn);
        Button addPlayerBtn = findViewById(R.id.addPlayerBtn);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_teams);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(TeamActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_teams) {
                // Already on teams page
                return true;
            } else if (itemId == R.id.nav_score) {
                Intent intent = new Intent(TeamActivity.this, AllMatchesActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });


        // Launcher for AddPlayer
        addPlayerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent intent = new Intent(TeamActivity.this, TeamActivity1.class);
                        intent.putExtra("teamName", teamNameInput.getText().toString().trim());
                        intent.putExtra("teamId", teamId);
                        intent.putExtras(result.getData().getExtras());
                        startActivity(intent);
                    }
                }
        );
        saveTeamBtn.setOnClickListener(v -> saveOrUpdateTeam());

        // Pre-fill team name if passed
        String predefinedName = getIntent().getStringExtra("teamName");
        teamId = getIntent().getStringExtra("teamId"); // existing TeamId if editing

        if (predefinedName != null) {
            teamNameInput.setText(predefinedName);
        } else {
            teamNameInput.setText("Team A");
        }

        if (teamId == null || teamId.isEmpty()) {
            noPlayersText.setVisibility(View.VISIBLE);
        }
        // Enable editing - user can change team name
        teamNameInput.setEnabled(true);
        teamNameInput.setFocusable(true);
        teamNameInput.setFocusableInTouchMode(true);

        // Back button → save team safely
        backBtn.setOnClickListener(v -> {
            isBackButtonPressed = true; //  Tell system it's the back button
            String teamName = teamNameInput.getText().toString().trim();
            if (teamName.isEmpty()) {
                setResult(Activity.RESULT_CANCELED);
                finish();
                return;
            }

            if (teamId == null) {
                // New team → check for duplicates first
                db.collection("Teams")
                        .whereEqualTo("TeamName", teamName)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                Toast.makeText(this, "Team name already exists!", Toast.LENGTH_SHORT).show();
                            } else {
                                createTeamInFirestore(teamName);
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Error checking team name: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            } else {
                // Existing team → just update
                updateTeamInFirestore(teamName);
            }
        });

        // Edit button - focus on team name for editing
        editTeamBtn.setOnClickListener(v -> {
            teamNameInput.requestFocus();
            teamNameInput.selectAll();
            // Show keyboard
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(teamNameInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
            Toast.makeText(this, "Edit team name and click back to save", Toast.LENGTH_SHORT).show();
        });


        // Add player button
        addPlayerBtn.setOnClickListener(v -> {
            String teamName = teamNameInput.getText().toString().trim();

            android.util.Log.d("TeamActivity", "Add Player clicked");
            android.util.Log.d("TeamActivity", "Current teamId: " + teamId);
            android.util.Log.d("TeamActivity", "Current teamName: " + teamName);

            if (teamName.isEmpty()) {
                Toast.makeText(this, "Please enter a team name first", Toast.LENGTH_SHORT).show();
                teamNameInput.requestFocus();
                return;
            }

            // If team doesn't exist yet, create it first
            if (teamId == null || teamId.isEmpty()) {
                Toast.makeText(this, "Creating team first...", Toast.LENGTH_SHORT).show();
                createTeamThenAddPlayer(teamName);
            } else {
                // Team exists, go to add player
                Toast.makeText(this, "Opening add player for: " + teamName, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(TeamActivity.this, AddPlayer.class);
                intent.putExtra("teamName", teamName);
                intent.putExtra("teamId", teamId);
                android.util.Log.d("TeamActivity", "Launching AddPlayer with teamId: " + teamId);
                addPlayerLauncher.launch(intent);
            }
        });
    }
    private void saveOrUpdateTeam() {
        String teamName = teamNameInput.getText().toString().trim();
        if (teamName.isEmpty()) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        if (teamId == null) {
            // New team → check for duplicates first
            db.collection("Teams")
                    .whereEqualTo("TeamName", teamName)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            Toast.makeText(this, "Team name already exists!", Toast.LENGTH_SHORT).show();
                        } else {
                            createTeamInFirestore(teamName);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error checking team name: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            // Existing team → just update
            updateTeamInFirestore(teamName);
        }
    }

    private void checkForPlayers() {
        if (teamId == null || teamId.isEmpty()) return;

        db.collection("Players")
                .whereEqualTo("TeamId", teamId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        noPlayersText.setVisibility(View.VISIBLE);
                    } else {
                        noPlayersText.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking players: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ----------------------------
    // Create a new team in Firestore
    // ----------------------------
    private void createTeamInFirestore(String teamName) {
        teamId = db.collection("Teams").document().getId();
        Map<String, Object> team = new HashMap<>();
        team.put("TeamId", teamId);
        team.put("TeamName", teamName);

        db.collection("Teams").document(teamId)
                .set(team)
                .addOnSuccessListener(aVoid -> {
                    // Check who called this method (Back or Save)
                    if (isBackButtonPressed) {
                        Toast.makeText(this, "Team saved successfully, going back...", Toast.LENGTH_SHORT).show();
                        returnResult(teamName); // ✅ Go back
                    } else {
                        Toast.makeText(this, "Team saved successfully!", Toast.LENGTH_SHORT).show();
                        // ✅ Stay on same page
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving team: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    // ----------------------------
    // Update an existing team
    // ----------------------------
    private void updateTeamInFirestore(String teamName) {
        Map<String, Object> team = new HashMap<>();
        team.put("TeamId", teamId);
        team.put("TeamName", teamName);

        db.collection("Teams").document(teamId)
                .set(team)
                .addOnSuccessListener(aVoid -> {
                    if (isBackButtonPressed) {
                        Toast.makeText(this, "Team saved successfully, going back...", Toast.LENGTH_SHORT).show();
                        returnResult(teamName); // ✅ Go back
                    } else {
                        Toast.makeText(this, "Team saved successfully!", Toast.LENGTH_SHORT).show();
                        // ✅ Stay on same page
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving team: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    // ----------------------------
    // Return result to MainActivity
    // ----------------------------
    private void returnResult(String teamName) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("teamName", teamName);
        resultIntent.putExtra("teamId", teamId);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    // ----------------------------
    // Create team first, then navigate to add player
    // ----------------------------
    private void createTeamThenAddPlayer(String teamName) {
        // Check for duplicates first
        db.collection("Teams")
                .whereEqualTo("TeamName", teamName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Team name already exists!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Create the team
                        teamId = db.collection("Teams").document().getId();
                        Map<String, Object> team = new HashMap<>();
                        team.put("TeamId", teamId);
                        team.put("TeamName", teamName);

                        db.collection("Teams").document(teamId)
                                .set(team)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Team created! Now add players.", Toast.LENGTH_SHORT).show();
                                    // Now navigate to add player
                                    Intent intent = new Intent(TeamActivity.this, AddPlayer.class);
                                    intent.putExtra("teamName", teamName);
                                    intent.putExtra("teamId", teamId);
                                    addPlayerLauncher.launch(intent);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error creating team: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking team name: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ----------------------------
    // Delete a team by TeamId
    // ----------------------------
    private void deleteTeamFromFirestore(String teamId) {
        db.collection("Teams").document(teamId)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Team deleted from Firestore", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error deleting team: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
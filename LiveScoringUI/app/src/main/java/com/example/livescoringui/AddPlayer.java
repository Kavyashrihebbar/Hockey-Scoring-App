package com.example.livescoringui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddPlayer extends AppCompatActivity {

    private FirebaseFirestore db;
    private String teamId = "";   // Firestore team ID
    private String teamName = ""; // Optional display name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_player);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // Get team info from intent
        teamId = getIntent().getStringExtra("teamId");
        teamName = getIntent().getStringExtra("teamName");

        // Debug: Show what we received
        android.util.Log.d("AddPlayer", "Received teamId: " + teamId);
        android.util.Log.d("AddPlayer", "Received teamName: " + teamName);

        // Validate we have the required data
        if (teamId == null || teamId.isEmpty()) {
            Toast.makeText(this, "Error: No team ID provided. Returning to teams page.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (teamName == null || teamName.isEmpty()) {
            teamName = "Unknown Team";
        }

        // UI elements
        ImageView backBtn = findViewById(R.id.backBtn);
        EditText playerNameInput = findViewById(R.id.playerNameInput);
        EditText jerseyNoInput = findViewById(R.id.jerseyNoInput);
        Spinner positionSpinner = findViewById(R.id.positionSpinner);
        Button savePlayerBtn = findViewById(R.id.savePlayerBtn);

        // Spinner setup
        String[] positions = {"Select", "Goalkeeper", "Defender", "Midfielder", "Forward"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, positions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        positionSpinner.setAdapter(adapter);

        // Back button
        backBtn.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
        // ✅ Bottom Navigation setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_teams);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent homeIntent = new Intent(AddPlayer.this, MainActivity.class);
                startActivity(homeIntent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_teams) {
                Intent teamIntent = new Intent(AddPlayer.this, TeamActivity1.class);
                startActivity(teamIntent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_score) {
                Intent scoreIntent = new Intent(AddPlayer.this, AllMatchesActivity.class);
                startActivity(scoreIntent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // Save player button
        savePlayerBtn.setOnClickListener(v -> {
            // Disable button to prevent multiple clicks
            savePlayerBtn.setEnabled(false);
            savePlayerBtn.setText("Saving...");

            String name = playerNameInput.getText().toString().trim();
            String jersey = jerseyNoInput.getText().toString().trim();
            String position = positionSpinner.getSelectedItem().toString();

            // Validation
            if (name.isEmpty()) {
                playerNameInput.setError("Enter player name");
                playerNameInput.requestFocus();
                savePlayerBtn.setEnabled(true);
                savePlayerBtn.setText("Save Player");
                return;
            }
            if (jersey.isEmpty() || !jersey.matches("\\d+")) {
                jerseyNoInput.setError("Enter valid jersey number");
                jerseyNoInput.requestFocus();
                savePlayerBtn.setEnabled(true);
                savePlayerBtn.setText("Save Player");
                return;
            }
            if (position.equals("Select")) {
                Toast.makeText(AddPlayer.this, "Please select a position", Toast.LENGTH_SHORT).show();
                positionSpinner.requestFocus();
                savePlayerBtn.setEnabled(true);
                savePlayerBtn.setText("Save Player");
                return;
            }

            // Check if teamId exists
            if (teamId == null || teamId.isEmpty()) {
                Toast.makeText(AddPlayer.this, "Error: No team selected. Please try again.", Toast.LENGTH_LONG).show();
                savePlayerBtn.setEnabled(true);
                savePlayerBtn.setText("Save Player");
                finish();
                return;
            }

            // All validation passed, save player
            Toast.makeText(AddPlayer.this, "Saving player...", Toast.LENGTH_SHORT).show();
            addPlayerToFirestore(name, jersey, position, teamId, teamName);
        });
    }
    // Create team first, then save player
    private void createTeamThenSavePlayer(String playerName, String jerseyNo, String position) {
        Map<String, Object> team = new HashMap<>();
        team.put("TeamName", teamName.isEmpty() ? "Default Team" : teamName);

        db.collection("Teams")
                .add(team)
                .addOnSuccessListener(teamRef -> {
                    teamId = teamRef.getId(); // Firestore team ID
                    // Update document with ID inside
                    teamRef.update("TeamId", teamId);
                    addPlayerToFirestore(playerName, jerseyNo, position, teamId, teamName);
                    returnResult(playerName, jerseyNo, position, teamId, teamName);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(AddPlayer.this, "Error creating team: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Save player under given team
    private void addPlayerToFirestore(String playerName, String jerseyNo, String position, String teamId, String teamName) {
        db.collection("Players")
                .whereEqualTo("TeamId", teamId)
                .whereEqualTo("JersyNO", jerseyNo)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Jersey is free, add player
                        Map<String, Object> player = new HashMap<>();
                        player.put("PlayerName", playerName);
                        player.put("JersyNO", jerseyNo);
                        player.put("Position", position);
                        player.put("TeamId", teamId);
                        player.put("TeamName", teamName);

                        db.collection("Players")
                                .add(player)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(AddPlayer.this, "Player added successfully!", Toast.LENGTH_SHORT).show();
                                    // ✅ Only now return result and finish
                                    returnResult(playerName, jerseyNo, position, teamId, teamName);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AddPlayer.this, "Error saving player: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    // Re-enable button on error
                                    Button saveBtn = findViewById(R.id.savePlayerBtn);
                                    saveBtn.setEnabled(true);
                                    saveBtn.setText("Save Player");
                                });
                    } else {
                        // ❌ Jersey exists, show error and stay
                        Toast.makeText(AddPlayer.this, "This jersey number is already taken in this team!", Toast.LENGTH_LONG).show();
                        // Re-enable button
                        Button saveBtn = findViewById(R.id.savePlayerBtn);
                        saveBtn.setEnabled(true);
                        saveBtn.setText("Save Player");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddPlayer.this, "Error checking jersey number: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Re-enable button on error
                    Button saveBtn = findViewById(R.id.savePlayerBtn);
                    saveBtn.setEnabled(true);
                    saveBtn.setText("Save Player");
                });
    }




    // Send results back to calling activity
    private void returnResult(String playerName, String jerseyNo, String position, String teamId, String teamName) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("playerName", playerName);
        resultIntent.putExtra("jerseyNo", jerseyNo);
        resultIntent.putExtra("position", position);
        resultIntent.putExtra("teamId", teamId);
        resultIntent.putExtra("teamName", teamName);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

}
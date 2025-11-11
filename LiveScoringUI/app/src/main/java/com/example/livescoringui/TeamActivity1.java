package com.example.livescoringui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.playersteams.details.Player;


public class TeamActivity1 extends AppCompatActivity {
    private EditText teamNameInput;
    private RecyclerView playerRecyclerView;
    private PlayerAdapter_players playerAdapter;
    private ArrayList<Player> playerList;
    private TextView playerCountText;
    private ActivityResultLauncher<Intent> addPlayerLauncher;

    private FirebaseFirestore db;
    private CollectionReference playersRef;
    private CollectionReference teamsRef;
    private String teamId;
    private int teamCardPosition;
    private String launchFrom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        playersRef = db.collection("Players");
        teamsRef = db.collection("Teams");

        Intent intent = getIntent();
        launchFrom = intent.getStringExtra("launchFrom");
        String currentName = intent.getStringExtra("teamName");
        teamId = intent.getStringExtra("teamId");
        teamCardPosition = intent.getIntExtra("position", -1);

        setContentView(R.layout.activity_team_details1);

        teamNameInput = findViewById(R.id.teamNameInput);
        ImageView backBtn = findViewById(R.id.backBtn);
        Button addPlayerBtn = findViewById(R.id.addPlayerBtn);
        playerCountText = findViewById(R.id.playerCountText);
        playerRecyclerView = findViewById(R.id.playerRecyclerView);

        playerList = new ArrayList<>();
        playerAdapter = new PlayerAdapter_players(playerList);
        playerAdapter.setOnPlayerDeleteListener(() ->
                playerCountText.setText("Players: " + playerList.size())
        );
        playerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playerRecyclerView.setAdapter(playerAdapter);

        if (currentName != null) {
            teamNameInput.setText(currentName);
            if (teamId != null) loadPlayers();
        }

        // ------------------------
        // Add Player Launcher
        // ------------------------
        addPlayerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && teamId != null) {
                        // No need to manually add player. Firestore snapshot listener handles update.
                        loadPlayers(); // Refresh player list

                        Toast.makeText(this, "Player added successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        addPlayerBtn.setOnClickListener(v -> {
            if (teamId == null || teamId.isEmpty()) {
                Toast.makeText(this, "Please save the team first before adding players", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔒 Real-time check from Firestore to ensure accuracy
            playersRef.whereEqualTo("TeamId", teamId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int currentPlayers = querySnapshot.size();

                        if (currentPlayers >= 11) {
                            Toast.makeText(this, "You can only add 11 players to one team", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // ✅ Safe to add player
                        Intent addIntent = new Intent(TeamActivity1.this, AddPlayer.class);
                        addIntent.putExtra("teamId", teamId);
                        addIntent.putExtra("teamName", teamNameInput.getText().toString().trim());
                        addPlayerLauncher.launch(addIntent);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to check player count: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        backBtn.setOnClickListener(v -> handleBackButton());

        // ✅ ------------------------
        // Bottom Navigation setup
        // ✅ ------------------------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_teams);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent homeIntent = new Intent(TeamActivity1.this, MainActivity.class);
                startActivity(homeIntent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_teams) {
                // Already in Teams page
                return true;
            } else if (itemId == R.id.nav_score) {
                Intent scoreIntent = new Intent(TeamActivity1.this, AllMatchesActivity.class);
                startActivity(scoreIntent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
    // ------------------------
    // Handle Back Button
    // ------------------------
    private void handleBackButton() {
        String teamName = teamNameInput.getText().toString().trim();
        if (teamName.isEmpty()) {
            Toast.makeText(this, "Team name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for duplicate team names
        teamsRef.whereEqualTo("TeamName", teamName)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    boolean isDuplicate = false;
                    if (!querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            if (teamId == null || !doc.getId().equals(teamId)) {
                                isDuplicate = true;
                                break;
                            }
                        }
                    }

                    if (isDuplicate) {
                        Toast.makeText(this, "Team name already exists!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Add or update team
                    if (teamId == null || teamId.isEmpty()) {
                        addNewTeam(teamName);
                    } else {
                        updateExistingTeam(teamName);
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking team name: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ------------------------
    // Add New Team
    // ------------------------
    private void addNewTeam(String teamName) {
        Map<String, Object> newTeam = new HashMap<>();
        newTeam.put("TeamName", teamName);

        teamsRef.add(newTeam)
                .addOnSuccessListener(docRef -> {
                    String generatedId = docRef.getId();
                    docRef.update("TeamId", generatedId);
                    teamId = generatedId;

                    // Send result to MainActivity with "teams" tab hint
                    sendResultToMain(teamName, teamId, true, "teams");

                    // Check if we should immediately navigate back (i.e., if launched from main activity)
                    if ("MainActivity".equals(launchFrom)) {
                        Toast.makeText(this, "Team added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // Otherwise, stay here and let the user add players
                        Toast.makeText(this, "Team added! You can now add players.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error adding team: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ------------------------
    // Update Existing Team
    // ------------------------
    private void updateExistingTeam(String teamName) {
        teamsRef.document(teamId)
                .update("TeamName", teamName)
                .addOnSuccessListener(aVoid -> {
                    // Update players
                    updatePlayersTeamName(teamId, teamName);

                    // Send result to MainActivity with "teams" tab hint
                    sendResultToMain(teamName, teamId, false, "teams");

                    // Go back to the previous activity (MainActivity)
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating team: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updatePlayersTeamName(String teamId, String newTeamName) {
        db.collection("Players")
                .whereEqualTo("TeamId", teamId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update("TeamName", newTeamName);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update players", Toast.LENGTH_SHORT).show()
                );
    }


    // ------------------------
    // Send Result to MainActivity
    // ------------------------
    // *** MODIFIED: Added tabToSelect parameter to instruct MainActivity which tab to select ***
    private void sendResultToMain(String name, String id, boolean isNewTeam, String tabToSelect) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("teamName", name);
        resultIntent.putExtra("teamId", id);
        resultIntent.putExtra("position", teamCardPosition);
        resultIntent.putExtra("isNewTeam", isNewTeam);
        resultIntent.putExtra("tabToSelect", tabToSelect); // The key hint for MainActivity
        setResult(Activity.RESULT_OK, resultIntent);
    }

    // ------------------------
    // Open MainActivity (Removed: No longer needed, using finish() and result intent)
    // ------------------------
    /*
    private void openMainActivity() {
        Intent intent = new Intent(TeamActivity1.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    */

    // ------------------------
    // Load Players
    // ------------------------
    private void loadPlayers() {
        playersRef.whereEqualTo("TeamId", teamId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(TeamActivity1.this, "Error loading players", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    playerList.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Player player = doc.toObject(Player.class);
                            playerList.add(player);
                        }
                    }

                    playerAdapter.notifyDataSetChanged();
                    playerCountText.setText("Players: " + playerList.size());
                });
    }
}
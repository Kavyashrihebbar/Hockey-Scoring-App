package com.example.livescoringui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class matchsummary extends AppCompatActivity {

    ImageView back;
    private TableLayout statsTable;
    private MaterialButtonToggleGroup teamToggleGroup;
    private FirebaseFirestore db;
    
    private String matchId;
    private String teamAId, teamBId;
    private String teamAName, teamBName;
    private int teamAScore = 0, teamBScore = 0;
    private int teamAFouls = 0, teamBFouls = 0;
    private int teamASaves = 0, teamBSaves = 0;
    private String matchDate = "", matchTime = "";
    
    private Map<String, Integer> teamAPlayerGoals = new HashMap<>();
    private Map<String, Integer> teamBPlayerGoals = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_matchsummary);

        // --- Action Bar Setup ---
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Match Summary");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get match data from intent
        Intent intent = getIntent();
        matchId = intent.getStringExtra("matchId");
        
        if (matchId == null || matchId.isEmpty()) {
            Toast.makeText(this, "Error: No match ID provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- Initialize Views ---
        statsTable = findViewById(R.id.statsTable);
        teamToggleGroup = findViewById(R.id.teamToggleGroup);

        // Load match data from Firebase
        loadMatchData();

        // --- Toggle Listener ---
        teamToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTeamA) {
                    loadTeamAData();
                } else if (checkedId == R.id.btnTeamB) {
                    loadTeamBData();
                }
            }
        });

        // --- Back Button Listener ---
        back = findViewById(R.id.backBtn);
        back.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void loadMatchData() {
        db.collection("Matches").document(matchId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        MatchModel match = documentSnapshot.toObject(MatchModel.class);
                        if (match != null) {
                            teamAId = match.getTeamAId();
                            teamBId = match.getTeamBId();
                            teamAName = match.getTeamAName();
                            teamBName = match.getTeamBName();
                            teamAScore = match.getTeamAScore();
                            teamBScore = match.getTeamBScore();
                            teamASaves = match.getTeamASaves();
                            teamBSaves = match.getTeamBSaves();
                            matchDate = match.getDate();
                            matchTime = match.getTime();
                            
                            // Count penalties (fouls)
                            if (match.getPenalties() != null) {
                                for (MatchModel.PenaltyEvent penalty : match.getPenalties()) {
                                    if (penalty.getTeamId().equals(teamAId)) {
                                        teamAFouls++;
                                    } else {
                                        teamBFouls++;
                                    }
                                }
                            }
                            
                            // Count goals per player
                            if (match.getGoals() != null) {
                                for (MatchModel.GoalEvent goal : match.getGoals()) {
                                    if (goal.getTeamId().equals(teamAId)) {
                                        teamAPlayerGoals.put(goal.getPlayerName(), 
                                            teamAPlayerGoals.getOrDefault(goal.getPlayerName(), 0) + 1);
                                    } else {
                                        teamBPlayerGoals.put(goal.getPlayerName(), 
                                            teamBPlayerGoals.getOrDefault(goal.getPlayerName(), 0) + 1);
                                    }
                                }
                            }
                            
                            updateUI();
                        }
                    } else {
                        Toast.makeText(this, "Match not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading match: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    android.util.Log.e("MatchSummary", "Error loading match", e);
                });
    }
    
    private void updateUI() {
        try {
            android.util.Log.d("MatchSummary", "Updating UI with data:");
            android.util.Log.d("MatchSummary", "Date: " + matchDate + ", Time: " + matchTime);
            android.util.Log.d("MatchSummary", "Teams: " + teamAName + " vs " + teamBName);
            android.util.Log.d("MatchSummary", "Scores: " + teamAScore + " - " + teamBScore);
            
            // Update match details
            TextView dateView = findViewById(R.id.tvMatchSummaryDate);
            TextView timeView = findViewById(R.id.tvMatchSummaryTime);
            if (dateView != null) {
                dateView.setText("Date: " + (matchDate != null ? matchDate : "N/A"));
            }
            if (timeView != null) {
                timeView.setText("Time: " + (matchTime != null ? matchTime : "N/A"));
            }
            
            // Update team names
            TextView teamANameView = findViewById(R.id.tvTeamAName);
            TextView teamBNameView = findViewById(R.id.tvTeamBName);
            if (teamANameView != null) {
                teamANameView.setText(teamAName != null ? teamAName : "Team A");
            }
            if (teamBNameView != null) {
                teamBNameView.setText(teamBName != null ? teamBName : "Team B");
            }
            
            // Update scores
            TextView teamAGoalsView = findViewById(R.id.tvTeamAGoals);
            TextView teamBGoalsView = findViewById(R.id.tvTeamBGoals);
            if (teamAGoalsView != null) {
                teamAGoalsView.setText(String.valueOf(teamAScore));
            }
            if (teamBGoalsView != null) {
                teamBGoalsView.setText(String.valueOf(teamBScore));
            }
            
            // Update fouls
            TextView teamAFoulsView = findViewById(R.id.tvTeamAFouls);
            TextView teamBFoulsView = findViewById(R.id.tvTeamBFouls);
            if (teamAFoulsView != null) {
                teamAFoulsView.setText(String.valueOf(teamAFouls));
            }
            if (teamBFoulsView != null) {
                teamBFoulsView.setText(String.valueOf(teamBFouls));
            }
            
            // Update saves
            TextView teamASavesView = findViewById(R.id.tvTeamASaves);
            TextView teamBSavesView = findViewById(R.id.tvTeamBSaves);
            if (teamASavesView != null) {
                teamASavesView.setText(String.valueOf(teamASaves));
            }
            if (teamBSavesView != null) {
                teamBSavesView.setText(String.valueOf(teamBSaves));
            }
            
            // Update toggle button labels
            com.google.android.material.button.MaterialButton btnTeamA = findViewById(R.id.btnTeamA);
            com.google.android.material.button.MaterialButton btnTeamB = findViewById(R.id.btnTeamB);
            if (btnTeamA != null && teamAName != null) {
                btnTeamA.setText(teamAName);
            }
            if (btnTeamB != null && teamBName != null) {
                btnTeamB.setText(teamBName);
            }
            
            // Load default team A data
            loadTeamAData();
            
            android.util.Log.d("MatchSummary", "UI updated successfully");
        } catch (Exception e) {
            android.util.Log.e("MatchSummary", "Error updating UI", e);
            Toast.makeText(this, "Error displaying match data", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Team A Data ---
    private void loadTeamAData() {
        statsTable.removeAllViews();
        addHeaderRow();
        
        if (teamAPlayerGoals.isEmpty()) {
            addNoDataRow();
        } else {
            for (Map.Entry<String, Integer> entry : teamAPlayerGoals.entrySet()) {
                addPlayerRow(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
    }

    // --- Team B Data ---
    private void loadTeamBData() {
        statsTable.removeAllViews();
        addHeaderRow();
        
        if (teamBPlayerGoals.isEmpty()) {
            addNoDataRow();
        } else {
            for (Map.Entry<String, Integer> entry : teamBPlayerGoals.entrySet()) {
                addPlayerRow(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
    }
    
    private void addNoDataRow() {
        TableRow row = new TableRow(this);
        TextView noDataText = createTextView("No goals scored yet", false);
        noDataText.setGravity(Gravity.CENTER);
        row.addView(noDataText);
        statsTable.addView(row);
    }

    // --- Table Header ---
    private void addHeaderRow() {
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(0xFFF0F0F0);

        TextView nameHeader = createTextView("Player Name", true);
        TextView goalsHeader = createTextView("Goals", true);
        goalsHeader.setGravity(Gravity.END);

        headerRow.addView(nameHeader);
        headerRow.addView(goalsHeader);
        statsTable.addView(headerRow);
    }

    // --- Player Rows ---
    private void addPlayerRow(String player, String goals) {
        TableRow row = new TableRow(this);
        row.addView(createTextView(player, false));
        TextView goalsText = createTextView(goals, false);
        goalsText.setGravity(Gravity.END);
        row.addView(goalsText);
        statsTable.addView(row);
    }

    // --- Reusable TextView Builder ---
    private TextView createTextView(String text, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(16);
        tv.setGravity(Gravity.START);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    // --- Handle Back Button in Action Bar ---
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

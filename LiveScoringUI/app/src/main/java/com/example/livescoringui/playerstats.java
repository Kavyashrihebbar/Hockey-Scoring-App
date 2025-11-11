package com.example.livescoringui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;
import java.util.Map;


public class playerstats extends AppCompatActivity {

    private static final String TAG = "PlayerStatsActivity";
    private FirebaseFirestore db;
    private String playerId;

    // UI elements for Player Info
    private TextView tvPlayerName;
    private TextView tvPlayerPosition;
    private TextView tvPlayerTeamName;
    private TextView tvPlayerJersey;

    // UI elements for Summary Stats
    private TextView tvMatchesPlayedValue;
    private TextView tvTotalGoalsValue;
    private TextView tvTotalPenaltiesValue;
    private TextView tvRedCardsValue;

    // UI element for Match History
    private TableLayout historyTable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playerstats);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // Retrieve player ID from the Intent
        Intent intent = getIntent();
        playerId = intent.getStringExtra("playerId");
        Log.d(TAG, "Player ID received: " + playerId);

        // Initialize UI elements (MAPPING TO YOUR XML IDs)
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvPlayerPosition = findViewById(R.id.tvPlayerposition);
        tvPlayerTeamName = findViewById(R.id.tvPlayerteamname);
        tvPlayerJersey = findViewById(R.id.tvPlayerjersey);

        // Summary Stats
        tvMatchesPlayedValue = findViewById(R.id.tvMatchesPlayedValue);
        tvTotalGoalsValue = findViewById(R.id.tvTotalGoalsValue);
        tvTotalPenaltiesValue = findViewById(R.id.tvYellowCardsValue);
        tvRedCardsValue = findViewById(R.id.tvRedCardsValue);

        historyTable = findViewById(R.id.historyTable);

        // Handle custom back button in the custom top bar
        ImageView backbtn = findViewById(R.id.backBtn);
        backbtn.setOnClickListener(view -> finish());

        if (playerId != null && !playerId.isEmpty()) {
            // Call the correct function
            loadPlayerStats(playerId);
        } else {
            Toast.makeText(this, "Error: Player ID not provided.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Player ID is null or empty.");
        }
    }

    /**
     * Fetches player stats and triggers match history loading. (The final correct version)
     * @param playerId The ID of the player document.
     */
    private void loadPlayerStats(String playerId) {
        db.collection("Players").document(playerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // --- 1. Load Profile Details ---
                        String name = documentSnapshot.getString("PlayerName");
                        String position = documentSnapshot.getString("Position");
                        String teamName = documentSnapshot.getString("TeamName");
                        String jerseyNo = documentSnapshot.getString("JersyNO");

                        tvPlayerName.setText(name != null ? name : "Unknown Player");
                        tvPlayerPosition.setText(position != null ? position : "N/A");
                        tvPlayerTeamName.setText(teamName != null ? teamName : "N/A");
                        tvPlayerJersey.setText("Jersey No: " + (jerseyNo != null ? jerseyNo : "?"));

                        // --- 2. Load Aggregated Stats ---
                        Long totalGoals = documentSnapshot.getLong("goalCount");
                        Long matchesPlayed = documentSnapshot.getLong("matchesPlayed");
                        Long totalPenalties = documentSnapshot.getLong("penaltyCount");

                        // Display Stats
                        tvMatchesPlayedValue.setText(String.valueOf(matchesPlayed != null ? matchesPlayed : 0));
                        tvTotalGoalsValue.setText(String.valueOf(totalGoals != null ? totalGoals : 0));
                        tvTotalPenaltiesValue.setText(String.valueOf(totalPenalties != null ? totalPenalties : 0));
                        tvRedCardsValue.setText("0");

                        // *** CRITICAL STEP: Call history loader here after name is set ***
                        loadMatchHistory(playerId);

                    } else {
                        Toast.makeText(playerstats.this, "Player document not found for ID: " + playerId, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading player stats: " + e.getMessage());
                    Toast.makeText(playerstats.this, "Failed to load player data.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fetches Match History by querying the 'Matches' collection. (The final correct version)
     */
    private void loadMatchHistory(String playerId) {
        if (historyTable.getChildCount() > 1) {
            historyTable.removeViews(1, historyTable.getChildCount() - 1);
        }

        final String currentPlayersName = tvPlayerName.getText().toString();
        if (currentPlayersName.equals("Player Name") || currentPlayersName.equals("Unknown Player")) {
            addNoHistoryRow();
            return;
        }

        db.collection("Matches")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean historyFound = false;

                    for (QueryDocumentSnapshot matchDocument : queryDocumentSnapshots) {
                        long playerGoals = 0;
                        long playerFouls = 0;

                        String teamAName = matchDocument.getString("teamAName");
                        String teamBName = matchDocument.getString("teamBName");

                        // *** MODIFIED LOGIC HERE ***
                        String shortTeamAName = getShortTeamName(teamAName);
                        String shortTeamBName = getShortTeamName(teamBName);

                        String matchName = shortTeamAName + " vs " + shortTeamBName;

                        // Fallback to document ID if names are missing (if getShortTeamName returned "TBD")
                        if (shortTeamAName.equals("TBD") || shortTeamBName.equals("TBD")) {
                            matchName = "Match " + matchDocument.getId().substring(0, 4);
                        }
                        if (teamAName == null || teamBName == null) {
                            matchName = "Match " + matchDocument.getId().substring(0, 4);
                        }

                        // --- 1. Check Goals Array ---
                        List<Map<String, Object>> goals = (List<Map<String, Object>>) matchDocument.get("goals");
                        if (goals != null) {
                            for (Map<String, Object> goalMap : goals) {
                                String goalPlayerName = (String) goalMap.get("playerName");
                                if (goalPlayerName != null && goalPlayerName.equals(currentPlayersName)) {
                                    playerGoals++;
                                }
                            }
                        }

                        // --- 2. Check Penalties Array ---
                        List<Map<String, Object>> penalties = (List<Map<String, Object>>) matchDocument.get("penalties");
                        if (penalties != null) {
                            for (Map<String, Object> penaltyMap : penalties) {
                                String penaltyPlayerName = (String) penaltyMap.get("playerName");
                                if (penaltyPlayerName != null && penaltyPlayerName.equals(currentPlayersName)) {
                                    playerFouls++;
                                }
                            }
                        }

                        // --- 3. Add Row if player participated in the event ---
                        if (playerGoals > 0 || playerFouls > 0) {
                            addHistoryRow(matchName, playerGoals, playerFouls);
                            historyFound = true;
                        }
                    }

                    if (!historyFound) {
                        addNoHistoryRow();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting player match history: " + e.getMessage());
                    Toast.makeText(playerstats.this, "Failed to load match history.", Toast.LENGTH_SHORT).show();
                    addNoHistoryRow();
                });
    }

    /**
     * Dynamically creates and adds a row to the match history table. (The final correct version)
     */
    private void addHistoryRow(String matchName, long goals, long fouls) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 13, 0, 0);

        TextView matchView = createTableCell(matchName, 16);
        row.addView(matchView);

        TextView goalsView = createTableCell(String.valueOf(goals), 16);
        row.addView(goalsView);

        TextView foulsView = createTableCell(String.valueOf(fouls), 16);
        row.addView(foulsView);

        historyTable.addView(row);
    }

    /**
     * Creates a standardized TextView for a table cell. (The final correct version)
     */
    private TextView createTableCell(String text, int textSize) {
        TextView textView = new TextView(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setGravity(Gravity.CENTER);

        // --- NEW LOGIC TO BOLD THE MATCH NAME ---
        // Match names contain " vs "
        if (text.contains(" vs ") || text.startsWith("Match ")) {
            textView.setTypeface(null, Typeface.BOLD);
        }
        // --- END NEW LOGIC ---

        return textView;
    }

    /**
     * Displays a message when no match history is available. (The final correct version)
     */
    private void addNoHistoryRow() {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 16, 0, 16);

        TextView messageView = new TextView(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 3f);
        messageView.setLayoutParams(params);
        messageView.setText("No match history found.");
        messageView.setTextSize(16);
        messageView.setGravity(Gravity.CENTER);
        messageView.setTextColor(Color.GRAY);
        messageView.setTypeface(null, Typeface.ITALIC);

        row.addView(messageView);
        historyTable.addView(row);
    }

    private String getShortTeamName(String fullTeamName) {
        if (fullTeamName == null || fullTeamName.trim().isEmpty()) {
            return "TBD";
        }
        String[] words = fullTeamName.trim().split("\\s+");

        if (words.length >= 2) {
            // Return the second word (index 1)
            return words[1];
        } else {
            // Return the single word
            return words[0];
        }
    }
}
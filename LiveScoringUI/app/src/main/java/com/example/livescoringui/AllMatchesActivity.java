package com.example.livescoringui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AllMatchesActivity extends AppCompatActivity implements MatchAdapter.OnMatchClickListener {

    private RecyclerView allMatchesRecyclerView;
    private MatchAdapter matchAdapter;
    private TextView emptyStateText;

    // Bottom navigation bar declaration
    private BottomNavigationView bottomNavigation;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_matches);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = FirebaseFirestore.getInstance();

        allMatchesRecyclerView = findViewById(R.id.allMatchesRecyclerView);

        // Removed backButton initialization (ImageButton backButton = findViewById(R.id.backButton);)

        // Initialize BottomNavigationView
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);

        // Set 'Score' tab as selected
        bottomNavigation.setSelectedItemId(R.id.nav_score);

        allMatchesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Removed backButton click listener (backButton.setOnClickListener(view -> finish());)

        // Set up Bottom Navigation Listener
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(AllMatchesActivity.this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_teams) {
                Intent intent = new Intent(AllMatchesActivity.this, teams_detail_page.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_score) {
                // Already on All Matches page
                return true;
            }
            return false;
        });

        loadMatchesFromFirebase();
    }

    private void loadMatchesFromFirebase() {
        db.collection("Matches")
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Match> matches = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MatchModel matchModel = doc.toObject(MatchModel.class);
                        Match match = new Match(
                                matchModel.getDate(),
                                matchModel.getTime(),
                                matchModel.getTeamAName() + " (" + matchModel.getTeamAScore() + ")",
                                matchModel.getTeamBName() + " (" + matchModel.getTeamBScore() + ")"
                        );
                        // Store matchId in the Match object
                        match.setMatchId(doc.getId());
                        matches.add(match);
                    }

                    if (matches.isEmpty()) {
                        // Show empty state
                        matches.add(new Match("No matches yet", "Create your first match", "Tap to create", ""));
                    }

                    matchAdapter = new MatchAdapter(matches, this);
                    allMatchesRecyclerView.setAdapter(matchAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading matches: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Show empty state on error
                    List<Match> matches = new ArrayList<>();
                    matches.add(new Match("Error loading matches", "Please try again", "", ""));
                    matchAdapter = new MatchAdapter(matches, this);
                    allMatchesRecyclerView.setAdapter(matchAdapter);
                });
    }

    @Override
    public void onMatchClick(Match match) {
        if (match.getDate().equals("No matches yet") || match.getDate().equals("Error loading matches")) {
            // Navigate to create match
            Intent intent = new Intent(AllMatchesActivity.this, CreateMatchActivity.class);
            startActivity(intent);
            return;
        }

        // Navigate to match summary with matchId
        Intent intent = new Intent(AllMatchesActivity.this, matchsummary.class);
        intent.putExtra("matchId", match.getMatchId());
        intent.putExtra("MATCH_DATE", match.getDate());
        intent.putExtra("MATCH_TIME", match.getTime());
        intent.putExtra("TEAM_A_NAME", match.getTeamA());
        intent.putExtra("TEAM_B_NAME", match.getTeamB());
        startActivity(intent);
    }
}

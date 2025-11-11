package com.example.livescoringui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.Button;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.LinearLayout; // Import for playersSection

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore; // Ensure this is imported
import com.google.firebase.firestore.Query; // Ensure this is imported
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// 1. FIX: Implement PlayerAdapter.OnPlayerClickListener along with MatchAdapter.OnMatchClickListener
public class MainActivity extends AppCompatActivity
        implements MatchAdapter.OnMatchClickListener, PlayerAdapter.OnPlayerClickListener {

    private RecyclerView matchesRecyclerView;
    private RecyclerView playersRecyclerView;
    private MatchAdapter matchAdapter;
    private PlayerAdapter playerAdapter;
    private ImageButton exitButton;
    private ImageButton viewAllPlayersButton;
    private Button addMatchButton;
    private Button viewAllButton;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // FIX: Remove the default Action Bar to solve the double header
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize UI components
        matchesRecyclerView = findViewById(R.id.matchesRecyclerView);
        playersRecyclerView = findViewById(R.id.playersRecyclerView);
        exitButton = findViewById(R.id.exitButton);
        viewAllPlayersButton = findViewById(R.id.viewAllPlayersButton);
        addMatchButton = findViewById(R.id.addMatchButton);
        viewAllButton = findViewById(R.id.viewAllButton);

        bottomNavigationView = findViewById(R.id.bottom_navigation_bar);

        // Set up Matches RecyclerView
        matchesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up Players RecyclerView (horizontal)
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Load data from Firebase
        loadMatchesFromFirebase();
        loadPlayersFromFirebase();

        // Handle button clicks - Logout
        exitButton.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, login_page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });

        // ... (rest of onCreate remains the same)
        viewAllButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AllMatchesActivity.class);
            startActivity(intent);
        });

        viewAllPlayersButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AllPlayersActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Navigating to All Players page", Toast.LENGTH_SHORT).show();
        });

        // Add Match button - Navigate to Create Match
        addMatchButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CreateMatchActivity.class);
            startActivity(intent);
        });

        // Set up Bottom Navigation Bar listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Already on home
                return true;
            } else if (itemId == R.id.nav_teams) {
                // Navigate to Teams page
                Intent intent = new Intent(MainActivity.this, teams_detail_page.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_score) {
                // Navigate to All Matches (scoring overview)
                Intent intent = new Intent(MainActivity.this, AllMatchesActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload matches when returning to this activity
        loadMatchesFromFirebase();
        loadPlayersFromFirebase();
    }

    private void loadMatchesFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Matches")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Match> matches = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Assuming MatchModel and Match classes are correctly defined elsewhere
                        MatchModel matchModel = doc.toObject(MatchModel.class);
                        Match match = new Match(
                                matchModel.getDate(),
                                matchModel.getTime(),
                                matchModel.getTeamAName() + " (" + matchModel.getTeamAScore() + ")",
                                matchModel.getTeamBName() + " (" + matchModel.getTeamBScore() + ")"
                        );
                        // Store matchId
                        match.setMatchId(doc.getId());
                        matches.add(match);
                    }

                    if (matches.isEmpty()) {
                        // Show empty state
                        matches.add(new Match("No matches yet", "Create your first match", "---", "---"));
                    }

                    matchAdapter = new MatchAdapter(matches, this);
                    matchesRecyclerView.setAdapter(matchAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading matches", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPlayersFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Players")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Player> players = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Assuming com.playersteams.details.Player is your Firebase model class
                        com.playersteams.details.Player fbPlayer =
                                doc.toObject(com.playersteams.details.Player.class);

                        // Assuming Player is your local model class for the RecyclerView
                        Player player = new Player(fbPlayer.getName());

                        // *** CRITICAL FIX 1: Capture the Firebase Document ID ***
                        player.setPlayerId(doc.getId());

                        players.add(player);
                    }

                    if (players.isEmpty()) {
                        // Show empty state
                        players.add(new Player("No players yet"));
                    }

                    playerAdapter = new PlayerAdapter(players, this);
                    playersRecyclerView.setAdapter(playerAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading players", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onMatchClick(Match match) {
        if (match.getDate().equals("No matches yet")) {
            // Navigate to create match
            Intent intent = new Intent(MainActivity.this, CreateMatchActivity.class);
            startActivity(intent);
            return;
        }

        // Navigate to match summary with matchId
        Intent intent = new Intent(MainActivity.this, matchsummary.class);
        intent.putExtra("matchId", match.getMatchId());
        intent.putExtra("MATCH_DATE", match.getDate());
        intent.putExtra("MATCH_TIME", match.getTime());
        intent.putExtra("TEAM_A_NAME", match.getTeamA());
        intent.putExtra("TEAM_B_NAME", match.getTeamB());
        startActivity(intent);
    }

    @Override
    public void onPlayerClick(Player player) {
        if (player.getName().equals("No players yet")) {
            // Navigate to teams page to add players
            Intent intent = new Intent(MainActivity.this, teams_detail_page.class);
            startActivity(intent);
            Toast.makeText(this, "Add teams and players first", Toast.LENGTH_SHORT).show();
            return;
        }

        // *** CRITICAL FIX 2: Send the Player ID using the correct key "playerId" ***
        Intent intent = new Intent(MainActivity.this, playerstats.class);

        // ----------------------------------------------------------------------
        // --- THIS LINE MUST BE UNCOMMENTED AND ACTIVE! ---
        intent.putExtra("playerId", player.getPlayerId());
        // ----------------------------------------------------------------------

        startActivity(intent);
    }

}

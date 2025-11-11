package com.example.livescoringui;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.annotation.NonNull; // Added import for @NonNull
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

// FIX 1: Implement the missing PlayerAdapter.OnPlayerClickListener interface
public class AllPlayersActivity extends AppCompatActivity
        implements PlayerAdapter.OnPlayerClickListener { // Interface added

    private RecyclerView allPlayersRecyclerView;
    private RecyclerView mostGoalsRecyclerView;
    private PlayerAdapter allPlayersAdapter;
    private PlayerAdapter mostGoalsAdapter;
    private ImageButton backButton;
    private SearchView searchView;
    private BottomNavigationView bottomNavigation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_players);

        // FIX: Hide the default Action Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Find views
        allPlayersRecyclerView = findViewById(R.id.allPlayersRecyclerView);
        mostGoalsRecyclerView = findViewById(R.id.mostGoalsRecyclerView);
        backButton = findViewById(R.id.backButton);

        // Find the new SearchView ID
        searchView = findViewById(R.id.searchView);
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);
        bottomNavigation.setSelectedItemId(R.id.nav_teams);
        // All Players grid with spacing
        // FIX 2: GridLayoutManager constructor with 3 arguments (Context, spanCount, orientation)
        allPlayersRecyclerView.setLayoutManager(
                new GridLayoutManager(this, 3, RecyclerView.VERTICAL, false)
        );

        // Ensure R.dimen.recycler_item_spacing exists in your dimen.xml
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.recycler_item_spacing);
        allPlayersRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(spacingInPixels));

        // Most Goals grid with spacing
        // FIX 2: GridLayoutManager constructor with 3 arguments (Context, spanCount, orientation)
        mostGoalsRecyclerView.setLayoutManager(
                new GridLayoutManager(this, 3, RecyclerView.VERTICAL, false)
        );
        mostGoalsRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(spacingInPixels));

        // Load players from Firebase
        loadPlayersFromFirebase();

        // Handle button clicks
        // The 'finish()' method resolves correctly because AllPlayersActivity extends AppCompatActivity.
        backButton.setOnClickListener(view -> finish());

        // =========================================================
        // NEW: Search View Listener Implementation (Uncommented and fixed)
        // =========================================================
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                allPlayersAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                allPlayersAdapter.filter(newText);
                return false;
            }
        });
        // Bottom navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(AllPlayersActivity.this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_teams) {
                // Already on teams page
                return true;
            } else if (itemId == R.id.nav_score) {
                Intent intent = new Intent(AllPlayersActivity.this, AllMatchesActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        // =========================================================
    }

    @Override
    public void onPlayerClick(Player player) {
        if (player.getName().contains("No players yet") || player.getName().equals("No data yet")) {
            // Navigate to teams page
            android.content.Intent intent = new android.content.Intent(this, teams_detail_page.class);
            startActivity(intent);
            android.widget.Toast.makeText(this, "Add teams and players first",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to player stats
        android.content.Intent intent = new android.content.Intent(this, playerstats.class);
        intent.putExtra("PLAYER_NAME", player.getName());
        startActivity(intent);
    }

    private void loadPlayersFromFirebase() {
        com.google.firebase.firestore.FirebaseFirestore db =
                com.google.firebase.firestore.FirebaseFirestore.getInstance();

        db.collection("Players")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Player> allPlayers = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        com.playersteams.details.Player fbPlayer =
                                doc.toObject(com.playersteams.details.Player.class);
                        Player player = new Player(fbPlayer.getName());
                        allPlayers.add(player);
                    }

                    if (allPlayers.isEmpty()) {
                        allPlayers.add(new Player("No players yet - Add teams and players"));
                    }

                    allPlayersAdapter = new PlayerAdapter(allPlayers, this);
                    allPlayersRecyclerView.setAdapter(allPlayersAdapter);

                    // For most goals, just show first 3 players for now
                    List<Player> topPlayers = new ArrayList<>();
                    int count = Math.min(3, allPlayers.size());
                    for (int i = 0; i < count; i++) {
                        topPlayers.add(allPlayers.get(i));
                    }

                    if (topPlayers.isEmpty()) {
                        topPlayers.add(new Player("No data yet"));
                    }

                    mostGoalsAdapter = new PlayerAdapter(topPlayers, this);
                    mostGoalsRecyclerView.setAdapter(mostGoalsAdapter);
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(this, "Error loading players",
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlayersFromFirebase();
    }

    private static class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int verticalSpaceHeight;

        VerticalSpaceItemDecoration(int verticalSpaceHeight) {
            this.verticalSpaceHeight = verticalSpaceHeight;
        }

        // Use @NonNull annotation for best practice
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.bottom = verticalSpaceHeight;
        }
    }
}
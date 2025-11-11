package com.example.livescoringui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
public class teams_detail_page extends AppCompatActivity {

    private LinearLayout teamContainer;
    private TextView noTeamsText;
    private Button addTeamBtn;
    private ActivityResultLauncher<Intent> teamActivityLauncher;
    private ActivityResultLauncher<Intent> teamActivity1Launcher;


    private BottomNavigationView bottomNavigation;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teams_detail_page);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // UI elements
        teamContainer = findViewById(R.id.teamContainer);
        noTeamsText = findViewById(R.id.noTeamsText);
        addTeamBtn = findViewById(R.id.addTeamBtn);
        Button startMatchBtn = findViewById(R.id.startMatchBtn);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // FIX: Explicitly set the 'Teams' tab as selected upon creation.
        // This ensures the visual state (like background color/tint) is applied.
        bottomNavigation.setSelectedItemId(R.id.nav_teams);



        // Start Match button
        startMatchBtn.setOnClickListener(v -> {
            Intent intent = new Intent(teams_detail_page.this, CreateMatchActivity.class);
            startActivity(intent);
        });

        // Bottom navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(teams_detail_page.this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_teams) {
                // Already on teams page
                return true;
            } else if (itemId == R.id.nav_score) {
                Intent intent = new Intent(teams_detail_page.this, AllMatchesActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Launcher for TeamActivity (adding or editing)
        teamActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String teamName = result.getData().getStringExtra("teamName");
                        String teamId = result.getData().getStringExtra("teamId");

                        if (teamName != null && !teamName.isEmpty() && teamId != null) {
                            // Directly add/update the UI card
                            addTeamCard(teamName, teamId);
                        }
                    }
                }
        );
        teamActivity1Launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String updatedName = result.getData().getStringExtra("teamName");
                        int position = result.getData().getIntExtra("position", -1);

                        if (updatedName != null && position != -1) {
                            LinearLayout teamCard = (LinearLayout) teamContainer.getChildAt(position);
                            TextView teamNameLabel = teamCard.findViewById(R.id.teamNameLabel);
                            teamNameLabel.setText(updatedName);
                        }
                    }
                }
        );


        addTeamBtn.setOnClickListener(v -> {
            Intent intent = new Intent(teams_detail_page.this, TeamActivity.class);
            teamActivityLauncher.launch(intent);
        });

        // Load all existing teams from Firestore
        getAllTeamsFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload teams when returning to this page
        getAllTeamsFromFirestore();
    }

    // -----------------------------
    // UI + Firestore Methods
    // -----------------------------

    private void addTeamCard(String teamName, String teamId) {
        noTeamsText.setVisibility(TextView.GONE);

        // Check if card already exists
        for (int i = 0; i < teamContainer.getChildCount(); i++) {
            LinearLayout existingCard = (LinearLayout) teamContainer.getChildAt(i);
            TextView existingName = existingCard.findViewById(R.id.teamNameLabel);
            if (existingName.getText().toString().equals(teamName)) {
                return; // Prevent duplicate cards
            }
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout teamCard = (LinearLayout) inflater.inflate(R.layout.item_team_card, teamContainer, false);

        TextView teamNameLabel = teamCard.findViewById(R.id.teamNameLabel);
        teamNameLabel.setText(teamName);

        ImageView editIcon = teamCard.findViewById(R.id.editIcon);
        ImageView deleteIcon = teamCard.findViewById(R.id.deleteIcon);

        final String finalTeamName = teamName;
        final String finalTeamId = teamId;

        editIcon.setOnClickListener(v -> {
            Intent intent = new Intent(teams_detail_page.this, TeamActivity1.class);
            intent.putExtra("teamName", finalTeamName);
            intent.putExtra("teamId", finalTeamId);
            intent.putExtra("position", teamContainer.indexOfChild(teamCard));
            intent.putExtra("launchFrom", "MainActivity");
            teamActivity1Launcher.launch(intent);
        });
        deleteIcon.setOnClickListener(v -> {
            teamContainer.removeView(teamCard);
            if (teamContainer.getChildCount() == 0) {
                noTeamsText.setVisibility(TextView.VISIBLE);
            }
            deleteTeamFromFirestore(teamId);
        });

        teamContainer.addView(teamCard);
    }

    private void getAllTeamsFromFirestore() {
        db.collection("Teams")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        teamContainer.removeAllViews();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String teamName = document.getString("TeamName");
                            String teamId = document.getId();
                            if (teamName != null && !teamName.isEmpty()) {
                                addTeamCard(teamName, teamId);
                            }
                        }

                        if (teamContainer.getChildCount() == 0) {
                            noTeamsText.setVisibility(TextView.VISIBLE);
                        }
                    } else {
                        Exception e = task.getException();
                        Log.e("Firestore", "Error loading teams", e);
                        Toast.makeText(this, "Error loading teams: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteTeamFromFirestore(String teamId) {
        db.collection("Players")
                .whereEqualTo("TeamId", teamId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();

                    // Delete all players
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        batch.delete(doc.getReference());
                    }

                    // Delete the team itself
                    batch.delete(db.collection("Teams").document(teamId));

                    // Commit all deletes at once
                    batch.commit()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Team and its players deleted", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error deleting team/players: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching players: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

}

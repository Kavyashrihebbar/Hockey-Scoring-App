package com.example.livescoringui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import com.playersteams.details.Player;
import com.playersteams.details.Team;

public class PlayerAdapter_players extends RecyclerView.Adapter<PlayerAdapter_players.PlayerViewHolder> {

private ArrayList<Player> playerList;
private OnPlayerDeleteListener deleteListener;
private FirebaseFirestore db;

// Listener interface for delete events
public interface OnPlayerDeleteListener {
    void onPlayerDeleted();
}

public void setOnPlayerDeleteListener(OnPlayerDeleteListener listener) {
    this.deleteListener = listener;
}

public PlayerAdapter_players(ArrayList<Player> playerList) {
    this.playerList = playerList;
}

@NonNull
@Override
public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.activity_playerdetails, parent, false);

    // Initialize Firebase
    if (db == null) {
        FirebaseApp.initializeApp(parent.getContext());
        db = FirebaseFirestore.getInstance();
    }

    return new PlayerViewHolder(view);
}

@Override
public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
    Player player = playerList.get(position);
    holder.nameText.setText("Name: " + player.getName());
    holder.jerseyText.setText("Jersey No: " + player.getJersey());
    holder.positionText.setText("Position: " + player.getPosition());

    // Click listener for entire player card
    holder.itemView.setOnClickListener(v ->
            Toast.makeText(v.getContext(),
                    "Clicked player: " + player.getName(),
                    Toast.LENGTH_SHORT).show()
    );

    // Delete button
    holder.deleteBtn.setOnClickListener(v -> {
        int pos = holder.getAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) {
            Player deletedPlayer = playerList.get(pos);

            // Delete from Firestore using TeamId and PlayerName
            deletePlayerFromFirestore(deletedPlayer, v);

            // Delete locally
            playerList.remove(pos);
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, playerList.size());

            if (deleteListener != null) {
                deleteListener.onPlayerDeleted();
            }
        }
    });
}

@Override
public int getItemCount() {
    return playerList.size();
}

// ----------------------------
// 🔥 Firestore Delete Function (TeamId included)
// ----------------------------
private void deletePlayerFromFirestore(Player player, View view) {
    if (db == null) {
        FirebaseApp.initializeApp(view.getContext());
        db = FirebaseFirestore.getInstance();
    }

    db.collection("Players")
            .whereEqualTo("PlayerName", player.getName())
            .whereEqualTo("JersyNO", player.getJersey())
            .whereEqualTo("Position", player.getPosition())
            .whereEqualTo("TeamId", player.getTeamId()) // ✅ Filter by TeamId
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        db.collection("Players").document(doc.getId())
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(view.getContext(),
                                                "Deleted from Firebase",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(view.getContext(),
                                                "Error deleting: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Toast.makeText(view.getContext(),
                            "Player not found in Firebase",
                            Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e ->
                    Toast.makeText(view.getContext(),
                            "Error fetching player: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
}

// ----------------------------
// ViewHolder Class
// ----------------------------
static class PlayerViewHolder extends RecyclerView.ViewHolder {
    TextView nameText, jerseyText, positionText;
    ImageButton deleteBtn;

    public PlayerViewHolder(@NonNull View itemView) {
        super(itemView);
        nameText = itemView.findViewById(R.id.nameText);
        jerseyText = itemView.findViewById(R.id.jerseyText);
        positionText = itemView.findViewById(R.id.positionText);
        deleteBtn = itemView.findViewById(R.id.deleteBtn);
    }
}
}
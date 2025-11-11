package com.example.livescoringui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// Change: Remove Button import
import android.widget.ImageView;
import android.widget.TextView; // <--- NEW: Import TextView

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.livescoringui.Player;
import com.example.livescoringui.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // Ensure Locale is imported

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {

    public interface OnPlayerClickListener {
        void onPlayerClick(Player player);
    }

    private final List<Player> originalPlayerList;
    private List<Player> filteredPlayerList;
    private final OnPlayerClickListener onPlayerClickListener;

    public PlayerAdapter(List<Player> playerList, OnPlayerClickListener listener) {
        this.originalPlayerList = playerList;
        this.filteredPlayerList = new ArrayList<>(playerList);
        this.onPlayerClickListener = listener;
    }


    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_player, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        Player player = filteredPlayerList.get(position);

        // Set name on the TextView (not Button)
        holder.playerNameTextView.setText(player.getName()); // <--- CHANGE: Use TextView reference

        // Set the click listener on the entire itemView (CardView)
        holder.itemView.setOnClickListener(v -> {
            if (onPlayerClickListener != null) {
                onPlayerClickListener.onPlayerClick(player);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredPlayerList.size();
    }

    // =========================================================
    // Filtering Method (Existing)
    // =========================================================
    public void filter(String query) {
        String prefix = query.trim().toLowerCase(Locale.ROOT);
        filteredPlayerList.clear();

        if (prefix.isEmpty()) {
            filteredPlayerList.addAll(originalPlayerList);
        } else {
            for (Player p : originalPlayerList) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    filteredPlayerList.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    // =========================================================

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        ImageView playerPhotoImageView;
        TextView playerNameTextView; // <--- CHANGE: Use TextView instead of Button

        PlayerViewHolder(@NonNull View itemView) {
            super(itemView);

            playerPhotoImageView = itemView.findViewById(R.id.playerPhotoImageView);

            // <--- CHANGE: Use the correct TextView ID from the XML
            playerNameTextView = itemView.findViewById(R.id.playerNameTextView);
        }

    }
}
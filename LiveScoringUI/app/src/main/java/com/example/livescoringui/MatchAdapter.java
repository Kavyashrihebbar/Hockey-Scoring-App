package com.example.livescoringui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    // Keep two lists: one for the original data, one for the filtered (visible) data
    private final List<Match> originalMatchList;
    private List<Match> filteredMatchList;
    private OnMatchClickListener listener;

    // Interface definition
    public interface OnMatchClickListener {
        void onMatchClick(Match match);
    }

    // Updated constructor
    public MatchAdapter(List<Match> matchList, OnMatchClickListener listener) {
        this.originalMatchList = matchList;
        this.filteredMatchList = new ArrayList<>(matchList); // Initialize filtered list with all data
        this.listener = listener;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = filteredMatchList.get(position); // Bind using the filtered list
        holder.matchDate.setText(match.getDate());
        holder.matchTime.setText("Time: " + match.getTime());
        holder.teamA.setText(match.getTeamA());
        holder.teamB.setText(match.getTeamB());

        // Make the entire card clickable
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMatchClick(match);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredMatchList.size(); // Use filtered list size
    }

    // =========================================================
    // NEW: Filtering Method
    // =========================================================
    public void filter(String query) {
        filteredMatchList.clear();
        if (query == null || query.isEmpty()) {
            // If query is empty, restore the full list
            filteredMatchList.addAll(originalMatchList);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault()).trim();
            for (Match match : originalMatchList) {
                // Filter logic: search for the query within the match date string
                if (match.getDate().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredMatchList.add(match);
                }
            }
        }
        notifyDataSetChanged();
    }
    // =========================================================

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView matchDate, matchTime, teamA, teamB;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            matchDate = itemView.findViewById(R.id.matchDateTextView);
            matchTime = itemView.findViewById(R.id.matchTimeTextView);
            teamA = itemView.findViewById(R.id.teamATextView);
            teamB = itemView.findViewById(R.id.teamBTextView);
        }
    }
}
package com.example.livescoringui;

import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchModel {
    private String matchId;
    private String teamAId;
    private String teamBId;
    private String teamAName;
    private String teamBName;
    private int teamAScore;
    private int teamBScore;
    private int teamASaves;
    private int teamBSaves;
    private String date;
    private String time;
    private String status; // "upcoming", "live", "completed"
    private long startTime;
    private long endTime;
    private String createdBy;
    private List<GoalEvent> goals;
    private List<PenaltyEvent> penalties;
    private List<SubstitutionEvent> substitutions;

    public MatchModel() {
        this.goals = new ArrayList<>();
        this.penalties = new ArrayList<>();
        this.substitutions = new ArrayList<>();
    }

    public MatchModel(String teamAId, String teamBId, String teamAName, String teamBName, 
                      String date, String time, String createdBy) {
        this.teamAId = teamAId;
        this.teamBId = teamBId;
        this.teamAName = teamAName;
        this.teamBName = teamBName;
        this.date = date;
        this.time = time;
        this.createdBy = createdBy;
        this.teamAScore = 0;
        this.teamBScore = 0;
        this.teamASaves = 0;
        this.teamBSaves = 0;
        this.status = "upcoming";
        this.goals = new ArrayList<>();
        this.penalties = new ArrayList<>();
        this.substitutions = new ArrayList<>();
    }

    // Getters and Setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getTeamAId() { return teamAId; }
    public void setTeamAId(String teamAId) { this.teamAId = teamAId; }

    public String getTeamBId() { return teamBId; }
    public void setTeamBId(String teamBId) { this.teamBId = teamBId; }

    public String getTeamAName() { return teamAName; }
    public void setTeamAName(String teamAName) { this.teamAName = teamAName; }

    public String getTeamBName() { return teamBName; }
    public void setTeamBName(String teamBName) { this.teamBName = teamBName; }

    public int getTeamAScore() { return teamAScore; }
    public void setTeamAScore(int teamAScore) { this.teamAScore = teamAScore; }

    public int getTeamBScore() { return teamBScore; }
    public void setTeamBScore(int teamBScore) { this.teamBScore = teamBScore; }

    public int getTeamASaves() { return teamASaves; }
    public void setTeamASaves(int teamASaves) { this.teamASaves = teamASaves; }

    public int getTeamBSaves() { return teamBSaves; }
    public void setTeamBSaves(int teamBSaves) { this.teamBSaves = teamBSaves; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<GoalEvent> getGoals() { return goals; }
    public void setGoals(List<GoalEvent> goals) { this.goals = goals; }

    public List<PenaltyEvent> getPenalties() { return penalties; }
    public void setPenalties(List<PenaltyEvent> penalties) { this.penalties = penalties; }

    public List<SubstitutionEvent> getSubstitutions() { return substitutions; }
    public void setSubstitutions(List<SubstitutionEvent> substitutions) { this.substitutions = substitutions; }

    // Helper methods
    public void addGoal(String teamId, String playerName, long timestamp) {
        GoalEvent goal = new GoalEvent(teamId, playerName, timestamp);
        goals.add(goal);
        if (teamId.equals(teamAId)) {
            teamAScore++;
        } else {
            teamBScore++;
        }
    }

    public void addPenalty(String teamId, String playerName, String type, long timestamp) {
        PenaltyEvent penalty = new PenaltyEvent(teamId, playerName, type, timestamp);
        penalties.add(penalty);
    }

    public void addSubstitution(String teamId, String playerIn, String playerOut, long timestamp) {
        SubstitutionEvent sub = new SubstitutionEvent(teamId, playerIn, playerOut, timestamp);
        substitutions.add(sub);
    }

    // Inner classes for events
    public static class GoalEvent {
        private String teamId;
        private String playerName;
        private long timestamp;

        public GoalEvent() {}

        public GoalEvent(String teamId, String playerName, long timestamp) {
            this.teamId = teamId;
            this.playerName = playerName;
            this.timestamp = timestamp;
        }

        public String getTeamId() { return teamId; }
        public void setTeamId(String teamId) { this.teamId = teamId; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class PenaltyEvent {
        private String teamId;
        private String playerName;
        private String type;
        private long timestamp;

        public PenaltyEvent() {}

        public PenaltyEvent(String teamId, String playerName, String type, long timestamp) {
            this.teamId = teamId;
            this.playerName = playerName;
            this.type = type;
            this.timestamp = timestamp;
        }

        public String getTeamId() { return teamId; }
        public void setTeamId(String teamId) { this.teamId = teamId; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class SubstitutionEvent {
        private String teamId;
        private String playerIn;
        private String playerOut;
        private long timestamp;

        public SubstitutionEvent() {}

        public SubstitutionEvent(String teamId, String playerIn, String playerOut, long timestamp) {
            this.teamId = teamId;
            this.playerIn = playerIn;
            this.playerOut = playerOut;
            this.timestamp = timestamp;
        }

        public String getTeamId() { return teamId; }
        public void setTeamId(String teamId) { this.teamId = teamId; }

        public String getPlayerIn() { return playerIn; }
        public void setPlayerIn(String playerIn) { this.playerIn = playerIn; }

        public String getPlayerOut() { return playerOut; }
        public void setPlayerOut(String playerOut) { this.playerOut = playerOut; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}

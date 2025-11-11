package com.playersteams.details;

import com.google.firebase.firestore.PropertyName;

public class Player {
    private String playerId; // <--- NEW: Field to hold the Firestore Document ID
    private String PlayerName;
    private String JersyNO;
    private String Position;
    private String TeamId;
    private String TeamName;

    // Default constructor required for Firestore
    public Player() {}

    // Constructor to create a new Player object (without ID initially)
    public Player(String playerName, String jersyNO, String position, String teamId, String teamName) {
        this.PlayerName = playerName;
        this.JersyNO = jersyNO;
        this.Position = position;
        this.TeamId = teamId;
        this.TeamName = teamName;
    }

    // --- NEW: Player ID Getter and Setter ---

    // IMPORTANT: @Exclude is sometimes used here, but since the ID is not stored
    // inside the document, we simply omit the @PropertyName annotation.
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    // --- Existing Getters and Setters ---

    @PropertyName("PlayerName")
    public String getName() { return PlayerName; }

    @PropertyName("PlayerName")
    public void setName(String playerName) { this.PlayerName = playerName; }

    @PropertyName("JersyNO")
    public String getJersey() { return JersyNO; }

    @PropertyName("JersyNO")
    public void setJersey(String jersyNO) { this.JersyNO = jersyNO; }

    @PropertyName("Position")
    public String getPosition() { return Position; }

    @PropertyName("Position")
    public void setPosition(String position) { this.Position = position; }

    @PropertyName("TeamId")
    public String getTeamId() { return TeamId; }

    @PropertyName("TeamId")
    public void setTeamId(String teamId) { this.TeamId = teamId; }

    @PropertyName("TeamName")
    public String getTeamName() { return TeamName; }

    @PropertyName("TeamName")
    public void setTeamName(String teamName) { this.TeamName = teamName; }
}
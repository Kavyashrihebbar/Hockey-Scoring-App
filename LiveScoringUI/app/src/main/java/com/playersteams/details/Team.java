package com.playersteams.details;

import com.google.firebase.firestore.PropertyName;

public class Team {
    private String teamId;
    private String teamName;

    public Team() {} // Required for Firestore

    public Team(String teamName, String teamId) {
        this.teamName = teamName;
        this.teamId = teamId;
    }

    // Firestore uses "TeamName" (capital T) in the database
    @PropertyName("TeamName")
    public String getTeamName() { return teamName; }
    
    @PropertyName("TeamName")
    public void setTeamName(String teamName) { this.teamName = teamName; }

    // Firestore uses "TeamId" (capital T) in the database
    @PropertyName("TeamId")
    public String getTeamId() { return teamId; }
    
    @PropertyName("TeamId")
    public void setTeamId(String teamId) { this.teamId = teamId; }
}

package com.example.livescoringui;

/**
 * Model class for a Player, used specifically for the RecyclerView in MainActivity.
 */
public class Player {
    private String name;
    private String playerId; // Stores the unique Firestore Document ID

    /**
     * Constructor used when loading players from Firebase for the RecyclerView.
     * @param name The name of the player.
     */
    public Player(String name) {
        this.name = name;
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public String getPlayerId() {
        return playerId;
    }

    // --- Setters ---

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the unique Firestore Document ID for the player.
     * @param playerId The unique ID of the player document.
     */
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
}

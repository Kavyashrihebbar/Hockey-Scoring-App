package com.example.livescoringui;

public class Match {
    private String date;
    private String time;
    private String teamA;
    private String teamB;
    private String matchId;

    public Match(String date, String time, String teamA, String teamB) {
        this.date = date;
        this.time = time;
        this.teamA = teamA;
        this.teamB = teamB;
    }

    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getTeamA() { return teamA; }
    public String getTeamB() { return teamB; }
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
}

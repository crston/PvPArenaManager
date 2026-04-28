package com.gmail.bobason01.pvparenamanager.data;

public class PlayerData {

    private int wins;
    private int losses;
    private int points;

    // 기본 생성자 : 브론즈 티어인 0점부터 시작하도록 수정
    public PlayerData() {
        this.wins = 0;
        this.losses = 0;
        this.points = 0;
    }

    public PlayerData(int wins, int losses, int points) {
        this.wins = wins;
        this.losses = losses;
        this.points = points;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    // 현재 점수에 기반한 티어 가져오기
    public Tier getTier() {
        return Tier.getTier(this.points);
    }

    // 전적 합산 및 업데이트
    public void addStats(int wins, int losses, int pointsChange) {
        this.wins += wins;
        this.losses += losses;
        this.points += pointsChange;

        // 점수가 0점 미만 브론즈 밑으로 내려가지 않도록 처리
        if (this.points < 0) {
            this.points = 0;
        }
    }
}
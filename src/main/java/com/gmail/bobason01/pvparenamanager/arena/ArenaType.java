package com.gmail.bobason01.pvparenamanager.arena;

public enum ArenaType {
    ONE_VS_ONE(1),
    TWO_VS_TWO(2),
    THREE_VS_THREE(3),
    FOUR_VS_FOUR(4),
    DEATHMATCH(0);

    private final int playersPerTeam;

    ArenaType(int playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }
}
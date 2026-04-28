package com.gmail.bobason01.pvparenamanager.match;

import java.util.UUID;

public class MatchQueueEntry {
    private final UUID uuid;
    private final int points;
    private final long joinTime;

    public MatchQueueEntry(UUID uuid, int points) {
        this.uuid = uuid;
        this.points = points;
        this.joinTime = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getPoints() {
        return points;
    }

    public int getSearchRange() {
        long secondsWaiting = (System.currentTimeMillis() - joinTime) / 1000;
        return (int) (100 + (secondsWaiting * 10)); // 대기 시간에 따라 범위 확장
    }
}
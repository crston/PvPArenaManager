package com.gmail.bobason01.pvparenamanager.match;

import java.util.UUID;

public class MatchQueueEntry {
    private final UUID uuid;
    private final int points;
    private final long startTime;
    private int searchRange;

    public MatchQueueEntry(UUID uuid, int points) {
        this.uuid = uuid;
        this.points = points;
        this.startTime = System.currentTimeMillis();
        this.searchRange = 100;
    }

    public UUID getUuid() { // 이 메서드가 누락되었거나 이름이 달랐을 것입니다.
        return uuid;
    }

    public int getPoints() {
        return points;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getSearchRange() {
        return searchRange;
    }

    public void expandSearchRange(int amount) {
        this.searchRange += amount;
    }
}
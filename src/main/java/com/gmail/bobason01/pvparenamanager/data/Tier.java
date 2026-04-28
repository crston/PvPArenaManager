package com.gmail.bobason01.pvparenamanager.data;

public enum Tier {
    BRONZE(0, "브론즈"),
    SILVER(1000, "실버"),
    GOLD(2000, "골드"),
    PLATINUM(3000, "플래티넘"),
    DIAMOND(4000, "다이아몬드");

    private final int minPoints;
    private final String displayName;

    Tier(int minPoints, String displayName) {
        this.minPoints = minPoints;
        this.displayName = displayName;
    }

    public static Tier getTier(int points) {
        Tier highest = BRONZE;
        for (Tier tier : values()) {
            if (points >= tier.minPoints) highest = tier;
        }
        return highest;
    }

    public String getDisplayName() {
        return displayName;
    }
}
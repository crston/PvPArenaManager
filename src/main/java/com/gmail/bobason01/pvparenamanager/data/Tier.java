package com.gmail.bobason01.pvparenamanager.data;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import org.bukkit.entity.Player;

public enum Tier {
    BRONZE(0, "tier_bronze"),
    SILVER(1000, "tier_silver"),
    GOLD(2000, "tier_gold"),
    PLATINUM(3000, "tier_platinum"),
    DIAMOND(4000, "tier_diamond");

    private final int minPoints;
    private final String langKey;

    Tier(int minPoints, String langKey) {
        this.minPoints = minPoints;
        this.langKey = langKey;
    }

    public static Tier getTier(int points) {
        Tier highest = BRONZE;
        for (Tier tier : values()) {
            if (points >= tier.minPoints) {
                highest = tier;
            }
        }
        return highest;
    }

    // 플레이어의 언어 설정에 맞는 티어 이름 반환
    public String getDisplayName(Player player) {
        return PvPArenaManager.getInstance().getLangManager().getMessage(player, this.langKey);
    }

    public int getMinPoints() {
        return minPoints;
    }
}
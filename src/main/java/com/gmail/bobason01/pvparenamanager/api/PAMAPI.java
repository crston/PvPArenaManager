package com.gmail.bobason01.pvparenamanager.api;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import com.gmail.bobason01.pvparenamanager.arena.Arena;
import com.gmail.bobason01.pvparenamanager.data.PlayerData;
import com.gmail.bobason01.pvparenamanager.data.Tier;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class PAMAPI {

    private static final PvPArenaManager plugin = PvPArenaManager.getInstance();

    public static PlayerData getPlayerData(Player player) {
        return plugin.getDataManager().getPlayerData(player.getUniqueId());
    }

    public static Tier getPlayerTier(Player player) {
        return getPlayerData(player).getTier();
    }

    public static boolean isIngame(Player player) {
        return plugin.getMatchManager().getPlayerArena(player.getUniqueId()) != null;
    }

    public static Optional<Arena> getPlayerArena(Player player) {
        return Optional.ofNullable(plugin.getMatchManager().getPlayerArena(player.getUniqueId()));
    }

    public static void updateStats(Player player, int wins, int losses, int points) {
        UUID uuid = player.getUniqueId();
        plugin.getDatabaseManager().updateStats(uuid, wins, losses, points);

        PlayerData cached = plugin.getDataManager().getPlayerData(uuid);
        if (cached != null) {
            cached.addStats(wins, losses, points);
        }
    }

    public static int getQueueCount() {
        return plugin.getMatchManager().getMatchQueues().values().stream()
                .mapToInt(java.util.List::size)
                .sum();
    }
}
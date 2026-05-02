package com.gmail.bobason01.pvparenamanager.match;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import com.gmail.bobason01.pvparenamanager.arena.Arena;
import com.gmail.bobason01.pvparenamanager.arena.ArenaState;
import com.gmail.bobason01.pvparenamanager.arena.ArenaType;
import com.gmail.bobason01.pvparenamanager.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MatchManager {

    private final PvPArenaManager plugin;
    private final Map<ArenaType, List<MatchQueueEntry>> matchQueues = new EnumMap<>(ArenaType.class);
    private final Map<UUID, Arena> playerInMatch = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> playerInQueueBar = new ConcurrentHashMap<>();

    // 플레이어의 원래 위치를 저장하기 위한 맵
    private final Map<UUID, Location> originalLocations = new ConcurrentHashMap<>();

    public MatchManager(PvPArenaManager plugin) {
        this.plugin = plugin;
        for (ArenaType type : ArenaType.values()) {
            matchQueues.put(type, new CopyOnWriteArrayList<>());
        }
        startMatchingTask();
    }

    public Map<ArenaType, List<MatchQueueEntry>> getMatchQueues() {
        return matchQueues;
    }

    public Arena getPlayerArena(UUID uuid) {
        return playerInMatch.get(uuid);
    }

    private void startMatchingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ArenaType type : ArenaType.values()) {
                    updateQueueBars(type);
                    processQueue(type);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void updateQueueBars(ArenaType type) {
        List<MatchQueueEntry> queue = matchQueues.get(type);
        long now = System.currentTimeMillis();

        for (MatchQueueEntry entry : queue) {
            Player p = Bukkit.getPlayer(entry.getUuid());
            if (p == null || !p.isOnline()) continue;

            BossBar bar = playerInQueueBar.get(p.getUniqueId());
            if (bar == null) continue;

            long elapsedSeconds = (now - entry.getStartTime()) / 1000;
            String timeStr = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60);

            // 다국어 적용
            String title = plugin.getLangManager().getMessage(p, "queue_waiting_bar").replace("%time%", timeStr);
            bar.setTitle(title);
            bar.setProgress(1.0);
            bar.setVisible(true);
        }
    }

    private void processQueue(ArenaType type) {
        List<MatchQueueEntry> queue = matchQueues.get(type);
        int required = (type == ArenaType.DEATHMATCH) ? 4 : type.getPlayersPerTeam() * 2;
        if (queue.size() < required) return;

        for (int i = 0; i < queue.size(); i++) {
            MatchQueueEntry entry1 = queue.get(i);
            List<MatchQueueEntry> matched = new ArrayList<>();
            matched.add(entry1);

            for (int j = 0; j < queue.size(); j++) {
                if (i == j) continue;

                MatchQueueEntry entry2 = queue.get(j);
                int scoreDiff = Math.abs(entry1.getPoints() - entry2.getPoints());

                if (scoreDiff <= entry1.getSearchRange() || scoreDiff <= entry2.getSearchRange()) {
                    matched.add(entry2);
                }

                if (matched.size() == required) {
                    executeMatchStart(type, matched);
                    return;
                }
            }
            entry1.expandSearchRange(50);
        }
    }

    private void executeMatchStart(ArenaType type, List<MatchQueueEntry> matchedEntries) {
        Arena arena = plugin.getArenaManager().findAvailableArena();
        if (arena == null) return;

        List<Player> players = new ArrayList<>();
        for (MatchQueueEntry entry : matchedEntries) {
            Player p = Bukkit.getPlayer(entry.getUuid());
            if (p != null && p.isOnline()) {
                players.add(p);
                // 매치 시작 직전 현재 위치 저장
                originalLocations.put(p.getUniqueId(), p.getLocation());
                removeQueueBar(p.getUniqueId());
            }
        }

        if (players.size() == matchedEntries.size()) {
            for (MatchQueueEntry entry : matchedEntries) {
                matchQueues.get(type).remove(entry);
            }
            startMatch(arena, players, type);
        }
    }

    public void addToQueue(Player player, ArenaType type) {
        UUID uuid = player.getUniqueId();
        removeFromQueue(player);

        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        matchQueues.get(type).add(new MatchQueueEntry(uuid, data.getPoints()));

        String initialTitle = plugin.getLangManager().getMessage(player, "queue_waiting_bar").replace("%time%", "00:00");
        BossBar bar = Bukkit.createBossBar(initialTitle, BarColor.WHITE, BarStyle.SOLID);
        bar.addPlayer(player);
        playerInQueueBar.put(uuid, bar);

        player.sendMessage(plugin.getLangManager().getMessage(player, "queue_join"));
    }

    public void removeFromQueue(Player player) {
        UUID uuid = player.getUniqueId();
        for (ArenaType type : ArenaType.values()) {
            matchQueues.get(type).removeIf(entry -> entry.getUuid().equals(uuid));
        }
        removeQueueBar(uuid);
        originalLocations.remove(uuid);
    }

    private void removeQueueBar(UUID uuid) {
        BossBar bar = playerInQueueBar.remove(uuid);
        if (bar != null) {
            bar.setVisible(false);
            bar.removeAll();
        }
    }

    public void handleQuit(Player player) {
        Arena arena = playerInMatch.get(player.getUniqueId());
        if (arena == null) return;

        Set<UUID> winners = new HashSet<>();
        Set<UUID> losers = new HashSet<>();
        losers.add(player.getUniqueId());

        if (arena.getRedTeam().contains(player.getUniqueId())) {
            winners.addAll(arena.getBlueTeam());
        } else if (arena.getBlueTeam().contains(player.getUniqueId())) {
            winners.addAll(arena.getRedTeam());
        }

        player.sendMessage(plugin.getLangManager().getMessage(player, "match_quit_penalty"));
        endMatch(arena, winners, losers);
    }

    public void startMatch(Arena arena, List<Player> players, ArenaType type) {
        arena.setState(ArenaState.COUNTDOWN);
        int totalTime = plugin.getConfigManager().getGameTime(type);
        arena.setTimeLeft(totalTime);

        BossBar bossBar = arena.getBossBar();
        bossBar.removeAll();
        bossBar.setProgress(1.0);
        bossBar.setColor(BarColor.YELLOW);
        bossBar.setVisible(true);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            playerInMatch.put(p.getUniqueId(), arena);
            bossBar.addPlayer(p);

            p.sendTitle(plugin.getLangManager().getMessage(p, "match_title_ready"), " ", 10, 40, 10);
            p.addScoreboardTag("PAM_INGAME");

            if (type == ArenaType.DEATHMATCH) {
                p.teleport(arena.getRedSpawn());
                p.addScoreboardTag("PAM_FREE");
            } else {
                if (i % 2 == 0) {
                    arena.getRedTeam().add(p.getUniqueId());
                    p.addScoreboardTag("PAM_RED");
                    p.teleport(arena.getRedSpawn());
                } else {
                    arena.getBlueTeam().add(p.getUniqueId());
                    p.addScoreboardTag("PAM_BLUE");
                    p.teleport(arena.getBlueSpawn());
                }
            }
        }

        new BukkitRunnable() {
            int count = plugin.getConfigManager().getCountdownSeconds();
            @Override
            public void run() {
                if (count > 0) {
                    broadcastToArena(arena, plugin.getLangManager().getMessage(null, "match_countdown").replace("%time%", String.valueOf(count)));
                    count--;
                } else {
                    arena.setState(ArenaState.PLAYING);
                    for (UUID uuid : getAllPlayers(arena)) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) p.sendTitle(plugin.getLangManager().getMessage(p, "match_title_start"), " ", 5, 20, 5);
                    }
                    startArenaTask(arena, totalTime);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startArenaTask(Arena arena, int totalTime) {
        arena.stopTask();
        int halfTime = totalTime / 2;
        BossBar bossBar = arena.getBossBar();

        arena.setGameTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.getState() != ArenaState.PLAYING) {
                    bossBar.setVisible(false);
                    this.cancel();
                    return;
                }
                int remaining = arena.getTimeLeft();
                double progress = (double) remaining / totalTime;
                if (progress >= 0 && progress <= 1) bossBar.setProgress(progress);

                String timeText = plugin.getLangManager().getMessage(null, "match_time_left").replace("%time%", String.valueOf(remaining));
                bossBar.setTitle(timeText);

                if (remaining == halfTime) {
                    applyGlow(arena);
                    bossBar.setColor(BarColor.RED);
                    broadcastToArena(arena, plugin.getLangManager().getMessage(null, "match_glow_start"));
                }
                if (remaining <= 0) {
                    endMatchDraw(arena);
                    this.cancel();
                    return;
                }
                arena.setTimeLeft(remaining - 1);
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    private void applyGlow(Arena arena) {
        for (UUID uuid : getAllPlayers(arena)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getGameMode() != GameMode.SPECTATOR) {
                p.setGlowing(true);
            }
        }
    }

    public void endMatchDraw(Arena arena) {
        if (arena.getState() == ArenaState.ENDING) return;
        arena.setState(ArenaState.ENDING);
        arena.stopTask();
        broadcastToArena(arena, plugin.getLangManager().getMessage(null, "match_draw"));
        finalizeMatch(arena);
    }

    public void endMatch(Arena arena, Set<UUID> winners, Set<UUID> losers) {
        if (arena.getState() == ArenaState.ENDING) return;
        arena.setState(ArenaState.ENDING);
        arena.stopTask();

        int pointWin = 20;
        int pointLoss = 10;

        for (UUID uuid : winners) {
            plugin.getDatabaseManager().updateStats(uuid, 1, 0, pointWin);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(plugin.getLangManager().getMessage(p, "match_win").replace("%points%", String.valueOf(pointWin)));
        }
        for (UUID uuid : losers) {
            plugin.getDatabaseManager().updateStats(uuid, 0, 1, -pointLoss);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(plugin.getLangManager().getMessage(p, "match_loss").replace("%points%", String.valueOf(pointLoss)));
        }
        finalizeMatch(arena);
    }

    private void finalizeMatch(Arena arena) {
        arena.getBossBar().setVisible(false);
        arena.getBossBar().removeAll();

        for (UUID uuid : getAllPlayers(arena)) {
            playerInMatch.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.removeScoreboardTag("PAM_RED");
                p.removeScoreboardTag("PAM_BLUE");
                p.removeScoreboardTag("PAM_FREE");
                p.removeScoreboardTag("PAM_INGAME");
                p.setGameMode(GameMode.SURVIVAL);
                p.setGlowing(false);

                // 원래 위치로 복구
                Location loc = originalLocations.remove(uuid);
                if (loc != null) {
                    p.teleport(loc);
                }
            }
        }
        arena.resetArena();
    }

    public Set<UUID> getAllPlayers(Arena arena) {
        Set<UUID> all = new HashSet<>(arena.getRedTeam());
        all.addAll(arena.getBlueTeam());
        all.addAll(arena.getSpectators());
        return all;
    }

    public void broadcastToArena(Arena arena, String message) {
        for (UUID uuid : getAllPlayers(arena)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }
}
package com.gmail.bobason01.pvparenamanager.match;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import com.gmail.bobason01.pvparenamanager.arena.Arena;
import com.gmail.bobason01.pvparenamanager.arena.ArenaState;
import com.gmail.bobason01.pvparenamanager.arena.ArenaType;
import com.gmail.bobason01.pvparenamanager.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class MatchManager {

    private final PvPArenaManager plugin;
    private final Map<ArenaType, List<MatchQueueEntry>> matchQueues = new EnumMap<>(ArenaType.class);
    private final Map<UUID, Arena> playerInMatch = new ConcurrentHashMap<>();

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
                    processQueue(type);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 40L);
    }

    private void processQueue(ArenaType type) {
        List<MatchQueueEntry> queue = matchQueues.get(type);
        int required = (type == ArenaType.DEATHMATCH) ? 4 : type.getPlayersPerTeam() * 2;
        if (queue.size() < required) return;

        List<MatchQueueEntry> matched = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            MatchQueueEntry entry1 = queue.get(i);
            matched.clear();
            matched.add(entry1);
            for (int j = 0; j < queue.size(); j++) {
                if (i == j) continue;
                MatchQueueEntry entry2 = queue.get(j);
                int scoreDiff = Math.abs(entry1.getPoints() - entry2.getPoints());
                if (scoreDiff <= entry1.getSearchRange() || scoreDiff <= entry2.getSearchRange()) {
                    matched.add(entry2);
                }
                if (matched.size() == required) {
                    executeMatchStart(type, new ArrayList<>(matched));
                    return;
                }
            }
        }
    }

    private void executeMatchStart(ArenaType type, List<MatchQueueEntry> matchedEntries) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Arena arena = plugin.getArenaManager().findAvailableArena();
            if (arena == null) return;

            List<Player> players = new ArrayList<>();
            for (MatchQueueEntry entry : matchedEntries) {
                Player p = Bukkit.getPlayer(entry.getUuid());
                if (p != null && p.isOnline()) players.add(p);
            }

            if (players.size() == matchedEntries.size()) {
                for (MatchQueueEntry entry : matchedEntries) {
                    matchQueues.get(type).remove(entry);
                }
                startMatch(arena, players, type);
            }
        });
    }

    public void addToQueue(Player player, ArenaType type) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        matchQueues.get(type).add(new MatchQueueEntry(player.getUniqueId(), data.getPoints()));
        player.sendMessage(plugin.getLangManager().getMessage(player, "queue_join"));
    }

    public void startMatch(Arena arena, List<Player> players, ArenaType type) {
        arena.setState(ArenaState.COUNTDOWN);
        int totalTime = plugin.getConfigManager().getGameTime(type);
        arena.setTimeLeft(totalTime);

        BossBar bossBar = arena.getBossBar();
        bossBar.removeAll();
        bossBar.setTitle("준비하세요!");
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
                p.teleport(getRandomSpawnInRegion(arena));
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

            for (String cmd : plugin.getConfigManager().getMatchStartCommandsBoth()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
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

    private Location getRandomSpawnInRegion(Arena arena) {
        Location red = arena.getRedSpawn();
        Location blue = arena.getBlueSpawn();
        double minX = Math.min(red.getX(), blue.getX());
        double maxX = Math.max(red.getX(), blue.getX());
        double minZ = Math.min(red.getZ(), blue.getZ());
        double maxZ = Math.max(red.getZ(), blue.getZ());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            double x = random.nextDouble(minX, maxX);
            double z = random.nextDouble(minZ, maxZ);
            int y = red.getWorld().getHighestBlockYAt((int)x, (int)z);
            Location loc = new Location(red.getWorld(), x + 0.5, y + 1, z + 0.5);
            if (loc.getBlock().getType() == Material.AIR && loc.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
                return loc;
            }
        }
        return red;
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
                bossBar.setTitle("남은 시간: " + remaining + "초");

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
            if (p != null && p.getGameMode() != GameMode.SPECTATOR) p.setGlowing(true);
        }
    }

    public void endMatch(Arena arena, Set<UUID> winners, Set<UUID> losers) {
        arena.setState(ArenaState.ENDING);
        arena.stopTask();
        int avgWinnerPoints = getAveragePoints(winners);
        int avgLoserPoints = getAveragePoints(losers);
        int diff = avgLoserPoints - avgWinnerPoints;
        int pointChange = Math.max(5, Math.min(50, 20 + (diff / 10)));
        for (UUID uuid : winners) {
            plugin.getDatabaseManager().updateStats(uuid, 1, 0, pointChange);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(plugin.getLangManager().getMessage(p, "match_win") + " (+" + pointChange + ")");
                for (String cmd : plugin.getConfigManager().getMatchEndCommandsWinner()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
                }
            }
        }
        for (UUID uuid : losers) {
            int lossPenalty = Math.max(2, pointChange / 2);
            plugin.getDatabaseManager().updateStats(uuid, 0, 1, -lossPenalty);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(plugin.getLangManager().getMessage(p, "match_loss") + " (-" + lossPenalty + ")");
                for (String cmd : plugin.getConfigManager().getMatchEndCommandsLoser()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
                }
            }
        }
        finalizeMatch(arena);
    }

    public void endMatchDraw(Arena arena) {
        arena.setState(ArenaState.ENDING);
        arena.stopTask();
        broadcastToArena(arena, plugin.getLangManager().getMessage(null, "match_draw"));
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
                for (String cmd : plugin.getConfigManager().getMatchEndCommandsBoth()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
                }
            }
        }
        arena.resetArena();
    }

    private int getAveragePoints(Set<UUID> players) {
        if (players.isEmpty()) return 0;
        int sum = 0;
        for (UUID uuid : players) {
            PlayerData data = plugin.getDataManager().getPlayerData(uuid);
            sum += data.getPoints();
        }
        return sum / players.size();
    }

    public void broadcastToArena(Arena arena, String message) {
        for (UUID uuid : getAllPlayers(arena)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    public Set<UUID> getAllPlayers(Arena arena) {
        Set<UUID> all = new HashSet<>(arena.getRedTeam());
        all.addAll(arena.getBlueTeam());
        all.addAll(arena.getSpectators());
        return all;
    }
}
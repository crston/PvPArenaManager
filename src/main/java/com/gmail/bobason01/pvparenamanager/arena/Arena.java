package com.gmail.bobason01.pvparenamanager.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Arena {

    private final String arenaName;
    private final String regionName;
    private ArenaState state;

    private Location redSpawn;
    private Location blueSpawn;

    private final Set<UUID> redTeam = new HashSet<>();
    private final Set<UUID> blueTeam = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();

    private int timeLeft;
    private BukkitTask gameTask;
    private BossBar bossBar;

    public Arena(String arenaName, String regionName) {
        this.arenaName = arenaName;
        this.regionName = regionName;
        this.state = ArenaState.WAITING;
        this.bossBar = Bukkit.createBossBar(arenaName, BarColor.YELLOW, BarStyle.SOLID);
        this.bossBar.setVisible(false);
    }

    public void resetArena() {
        this.state = ArenaState.WAITING;
        this.redTeam.clear();
        this.blueTeam.clear();
        this.spectators.clear();
        this.timeLeft = 0;
        if (this.gameTask != null) {
            this.gameTask.cancel();
            this.gameTask = null;
        }
        if (this.bossBar != null) {
            this.bossBar.setVisible(false);
            this.bossBar.removeAll();
        }
    }

    public String getArenaName() { return arenaName; }
    public String getRegionName() { return regionName; }
    public ArenaState getState() { return state; }
    public void setState(ArenaState state) { this.state = state; }
    public Location getRedSpawn() { return redSpawn; }
    public void setRedSpawn(Location redSpawn) { this.redSpawn = redSpawn; }
    public Location getBlueSpawn() { return blueSpawn; }
    public void setBlueSpawn(Location blueSpawn) { this.blueSpawn = blueSpawn; }
    public Set<UUID> getRedTeam() { return redTeam; }
    public Set<UUID> getBlueTeam() { return blueTeam; }
    public Set<UUID> getSpectators() { return spectators; }
    public int getTimeLeft() { return timeLeft; }
    public void setTimeLeft(int timeLeft) { this.timeLeft = timeLeft; }
    public void setGameTask(BukkitTask task) { this.gameTask = task; }
    public void stopTask() { if (this.gameTask != null) this.gameTask.cancel(); }
    public BossBar getBossBar() { return bossBar; }
}
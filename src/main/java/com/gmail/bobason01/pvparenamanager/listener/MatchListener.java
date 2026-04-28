package com.gmail.bobason01.pvparenamanager.listener;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import com.gmail.bobason01.pvparenamanager.arena.Arena;
import com.gmail.bobason01.pvparenamanager.arena.ArenaState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MatchListener implements Listener {

    private final PvPArenaManager plugin;

    public MatchListener(PvPArenaManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getMatchManager().getPlayerArena(player.getUniqueId());

        if (arena == null) return;

        // 카운트다운 상태일 때 움직임 제어
        if (arena.getState() == ArenaState.COUNTDOWN) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
                return;
            }
        }

        // 게임 중 지역 이탈 체크 (성능을 위해 블록 좌표가 바뀔 때만 실행)
        if (arena.getState() == ArenaState.PLAYING) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                    event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

                if (!plugin.getWgUtil().isPlayerInRegion(player, arena.getRegionName())) {
                    handleDefeat(player, arena);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Arena arena = plugin.getMatchManager().getPlayerArena(victim.getUniqueId());

        if (arena == null) return;

        // 아이템 소실 방지 및 경험치 드랍 차단
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        // 1틱 뒤에 리스폰 및 유령 모드 처리
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            victim.spigot().respawn();
            victim.setGameMode(GameMode.SPECTATOR);
            arena.getSpectators().add(victim.getUniqueId());
            checkMatchResult(arena);
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getMatchManager().getPlayerArena(player.getUniqueId());

        if (arena != null) {
            handleDefeat(player, arena);
        }
    }

    // 탈주나 지역 이탈 시 패배 처리 로직
    private void handleDefeat(Player loser, Arena arena) {
        Set<UUID> winners = new HashSet<>();
        Set<UUID> losers = new HashSet<>();

        if (arena.getRedTeam().contains(loser.getUniqueId())) {
            winners.addAll(arena.getBlueTeam());
            losers.addAll(arena.getRedTeam());
        } else if (arena.getBlueTeam().contains(loser.getUniqueId())) { // Arena 클래스의 Getter 명칭 확인 필요
            winners.addAll(arena.getRedTeam());
            losers.addAll(arena.getBlueTeam());
        } else {
            // 데스매치 등의 경우
            losers.add(loser.getUniqueId());
            for (UUID uuid : arena.getRedTeam()) winners.add(uuid);
            for (UUID uuid : arena.getBlueTeam()) winners.add(uuid);
            winners.remove(loser.getUniqueId());
        }

        plugin.getMatchManager().endMatch(arena, winners, losers);
    }

    // 생존 인원을 확인하여 승패 결정
    private void checkMatchResult(Arena arena) {
        if (arena.getState() != ArenaState.PLAYING) return;

        int redAlive = 0;
        int blueAlive = 0;

        for (UUID uuid : arena.getRedTeam()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getGameMode() != GameMode.SPECTATOR) {
                redAlive++;
            }
        }

        for (UUID uuid : arena.getBlueTeam()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getGameMode() != GameMode.SPECTATOR) {
                blueAlive++;
            }
        }

        // 어느 한 팀이 전멸했을 경우
        if (redAlive == 0 || blueAlive == 0) {
            Set<UUID> winners = (redAlive > 0) ? arena.getRedTeam() : arena.getBlueTeam();
            Set<UUID> losers = (redAlive > 0) ? arena.getBlueTeam() : arena.getRedTeam();
            plugin.getMatchManager().endMatch(arena, winners, losers);
        }
    }
}
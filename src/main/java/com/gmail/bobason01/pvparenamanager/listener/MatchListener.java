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

        // 게임 중 지역 이탈 체크 (성능 최적화: 블록 좌표가 바뀔 때만 실행)
        if (arena.getState() == ArenaState.PLAYING) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                    event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

                // 월드가드 지역을 벗어났는지 확인
                if (!plugin.getWgUtil().isPlayerInRegion(player, arena.getRegionName())) {
                    plugin.getMatchManager().handleQuit(player);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Arena arena = plugin.getMatchManager().getPlayerArena(victim.getUniqueId());

        if (arena == null) return;

        // 아이템 소실 방지 및 메시지 제거
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        // 1틱 뒤에 리스폰 및 관전 모드 처리
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline()) {
                victim.spigot().respawn();
                victim.setGameMode(GameMode.SPECTATOR);
                arena.getSpectators().add(victim.getUniqueId());

                // 남은 인원 체크하여 매치 종료 여부 결정
                checkMatchResult(arena);
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 매칭 대기열에서 제거
        plugin.getMatchManager().removeFromQueue(player);

        // 진행 중인 게임이 있다면 패배 처리 (탈주 방지)
        plugin.getMatchManager().handleQuit(player);
    }

    // 생존 인원을 확인하여 승패를 판정하는 내부 로직
    private void checkMatchResult(Arena arena) {
        if (arena.getState() != ArenaState.PLAYING) return;

        int redAlive = 0;
        int blueAlive = 0;

        for (java.util.UUID uuid : arena.getRedTeam()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getGameMode() != GameMode.SPECTATOR) {
                redAlive++;
            }
        }

        for (java.util.UUID uuid : arena.getBlueTeam()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getGameMode() != GameMode.SPECTATOR) {
                blueAlive++;
            }
        }

        // 한 팀이 전멸했을 경우 결과 처리
        if (redAlive == 0 || blueAlive == 0) {
            java.util.Set<java.util.UUID> winners = (redAlive > 0) ? arena.getRedTeam() : arena.getBlueTeam();
            java.util.Set<java.util.UUID> losers = (redAlive > 0) ? arena.getBlueTeam() : arena.getRedTeam();
            plugin.getMatchManager().endMatch(arena, winners, losers);
        }
    }
}
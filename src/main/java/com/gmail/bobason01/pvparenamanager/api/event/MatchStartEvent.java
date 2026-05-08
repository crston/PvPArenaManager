package com.gmail.bobason01.pvparenamanager.api.event;

import com.gmail.bobason01.pvparenamanager.arena.Arena;
import com.gmail.bobason01.pvparenamanager.arena.ArenaType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class MatchStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Arena arena;
    private final List<Player> players;
    private final ArenaType type;

    public MatchStartEvent(Arena arena, List<Player> players, ArenaType type) {
        this.arena = arena;
        this.players = players;
        this.type = type;
    }

    public Arena getArena() { return arena; }
    public List<Player> getPlayers() { return players; }
    public ArenaType getType() { return type; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
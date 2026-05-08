package com.gmail.bobason01.pvparenamanager.api.event;

import com.gmail.bobason01.pvparenamanager.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class MatchEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Arena arena;
    private final Set<Player> winners;
    private final Set<Player> losers;
    private final boolean isDraw;

    public MatchEndEvent(Arena arena, Set<Player> winners, Set<Player> losers, boolean isDraw) {
        this.arena = arena;
        this.winners = winners;
        this.losers = losers;
        this.isDraw = isDraw;
    }

    public Arena getArena() { return arena; }
    public Set<Player> getWinners() { return winners; }
    public Set<Player> getLosers() { return losers; }
    public boolean isDraw() { return isDraw; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
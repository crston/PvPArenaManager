package com.gmail.bobason01.pvparenamanager.command;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import com.gmail.bobason01.pvparenamanager.arena.Arena;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private final PvPArenaManager plugin;
    private final List<String> adminArgs = Arrays.asList("create", "delete", "list", "setred", "setblue", "reload");

    public ArenaCommand(PvPArenaManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) reloadSystem(sender);
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            plugin.getMenuManager().openMatchMenu(player);
            return true;
        }

        if (!player.hasPermission("pvparena.admin")) {
            player.sendMessage(plugin.getLangManager().getMessage(player, "no_permission"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                sendArenaList(player);
                break;
            case "create":
                if (args.length >= 3) {
                    plugin.getArenaManager().createArena(args[1], args[2]);
                    player.sendMessage(plugin.getLangManager().getMessage(player, "admin_arena_created")
                            .replace("%name%", args[1]).replace("%region%", args[2]));
                } else {
                    player.sendMessage(plugin.getLangManager().getMessage(player, "admin_usage_create"));
                }
                break;
            case "delete":
                if (args.length >= 2) {
                    plugin.getArenaManager().deleteArena(args[1]);
                    player.sendMessage(plugin.getLangManager().getMessage(player, "admin_arena_deleted").replace("%name%", args[1]));
                }
                break;
            case "setred":
                if (args.length >= 2) {
                    Arena a = plugin.getArenaManager().getArena(args[1]);
                    if (a != null) {
                        a.setRedSpawn(player.getLocation());
                        plugin.getArenaManager().saveArenas();
                        player.sendMessage(plugin.getLangManager().getMessage(player, "admin_spawn_set_red").replace("%name%", args[1]));
                    }
                }
                break;
            case "setblue":
                if (args.length >= 2) {
                    Arena a = plugin.getArenaManager().getArena(args[1]);
                    if (a != null) {
                        a.setBlueSpawn(player.getLocation());
                        plugin.getArenaManager().saveArenas();
                        player.sendMessage(plugin.getLangManager().getMessage(player, "admin_spawn_set_blue").replace("%name%", args[1]));
                    }
                }
                break;
            case "reload":
                reloadSystem(player);
                player.sendMessage(plugin.getLangManager().getMessage(player, "reload_success"));
                break;
            default:
                plugin.getMenuManager().openMatchMenu(player);
                break;
        }
        return true;
    }

    private void sendArenaList(Player player) {
        player.sendMessage("§8§m      §e [ PvPArena List ] §8§m      ");
        if (plugin.getArenaManager().getArenas().isEmpty()) {
            player.sendMessage(plugin.getLangManager().getMessage(player, "admin_arena_empty"));
            return;
        }

        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            String name = arena.getArenaName();
            String region = arena.getRegionName();
            boolean redSet = arena.getRedSpawn() != null;
            boolean blueSet = arena.getBlueSpawn() != null;

            String readyStr = (redSet && blueSet) ? plugin.getLangManager().getMessage(player, "admin_status_ready") : plugin.getLangManager().getMessage(player, "admin_status_setup");
            player.sendMessage("§f- " + name + " §7(" + region + ") " + readyStr);
            player.sendMessage("  §7Spawn: §cRed " + (redSet ? "✔" : "✘") + " §8| §bBlue " + (blueSet ? "✔" : "✘"));
        }
        player.sendMessage("§8§m                            ");
    }

    private void reloadSystem(CommandSender sender) {
        plugin.getConfigManager().loadConfig();
        plugin.getLangManager().loadLang();
        plugin.getArenaManager().loadArenas();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("pvparena.admin")) return null;
        if (args.length == 1) return adminArgs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("delete") || sub.equals("setred") || sub.equals("setblue")) {
                return plugin.getArenaManager().getArenas().keySet().stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
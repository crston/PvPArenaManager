package com.gmail.bobason01.pvparenamanager.arena;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final Map<String, Arena> arenas = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public void loadArenas() {
        arenas.clear();
        File folder = PvPArenaManager.getInstance().getDataFolder();
        file = new File(folder, "arenas.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.getConfigurationSection("arenas") != null) {
            for (String name : config.getConfigurationSection("arenas").getKeys(false)) {
                String path = "arenas." + name + ".";
                String region = config.getString(path + "region");
                Arena arena = new Arena(name, region);

                arena.setRedSpawn(config.getLocation(path + "redSpawn"));
                arena.setBlueSpawn(config.getLocation(path + "blueSpawn"));

                arenas.put(name, arena);
            }
        }
    }

    public void saveArenas() {
        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.getArenaName() + ".";
            config.set(path + "region", arena.getRegionName());
            config.set(path + "redSpawn", arena.getRedSpawn());
            config.set(path + "blueSpawn", arena.getBlueSpawn());
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void createArena(String name, String region) {
        arenas.put(name, new Arena(name, region));
        saveArenas();
    }

    public void deleteArena(String name) {
        arenas.remove(name);
        config.set("arenas." + name, null);
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Map<String, Arena> getArenas() {
        return arenas;
    }

    public Arena findAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.getState() == ArenaState.WAITING && arena.getRedSpawn() != null && arena.getBlueSpawn() != null) {
                return arena;
            }
        }
        return null;
    }
}
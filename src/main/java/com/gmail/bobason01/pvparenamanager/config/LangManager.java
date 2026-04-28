package com.gmail.bobason01.pvparenamanager.config;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LangManager {

    private final PvPArenaManager plugin;
    private final Map<String, Map<String, String>> messages = new HashMap<>();
    private final Map<UUID, String> playerLangCache = new HashMap<>();

    public LangManager(PvPArenaManager plugin) {
        this.plugin = plugin;
    }

    public void loadLang() {
        messages.clear();
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
            plugin.saveResource("lang/ko.yml", false);
            plugin.saveResource("lang/en.yml", false);
        }

        File[] files = langFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    String langCode = file.getName().replace(".yml", "");
                    FileConfiguration langConfig = YamlConfiguration.loadConfiguration(file);
                    Map<String, String> langMap = new HashMap<>();
                    for (String key : langConfig.getKeys(true)) {
                        if (langConfig.isString(key)) {
                            langMap.put(key, ChatColor.translateAlternateColorCodes('&', langConfig.getString(key)));
                        }
                    }
                    messages.put(langCode, langMap);
                }
            }
        }
    }

    public void setPlayerLanguage(Player player, String langCode) {
        playerLangCache.put(player.getUniqueId(), langCode);
    }

    public String getMessage(Player player, String key) {
        String langCode = plugin.getConfigManager().getDefaultLanguage();
        if (player != null && playerLangCache.containsKey(player.getUniqueId())) {
            langCode = playerLangCache.get(player.getUniqueId());
        }

        Map<String, String> langMap = messages.get(langCode);
        if (langMap == null || !langMap.containsKey(key)) {
            langMap = messages.get(plugin.getConfigManager().getDefaultLanguage());
            if (langMap == null || !langMap.containsKey(key)) {
                return "메시지 오류 키 " + key;
            }
        }
        return langMap.get(key);
    }
}
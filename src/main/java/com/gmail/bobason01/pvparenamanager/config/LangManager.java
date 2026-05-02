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
            // 기본 리소스 저장 (영어 기본 제공)
            plugin.saveResource("lang/en.yml", false);
            plugin.saveResource("lang/ko.yml", false);
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
        // 1. 플레이어 설정 언어 확인 -> 2. 시스템 기본 언어 확인 -> 3. 영어(en) 확인
        String defaultLang = plugin.getConfigManager().getDefaultLanguage();
        String langCode = (player != null) ? playerLangCache.getOrDefault(player.getUniqueId(), defaultLang) : defaultLang;

        String msg = getRawMessage(langCode, key);
        if (msg == null) msg = getRawMessage(defaultLang, key);
        if (msg == null) msg = getRawMessage("en", key);

        return (msg != null) ? msg : "Missing message: " + key;
    }

    private String getRawMessage(String langCode, String key) {
        Map<String, String> langMap = messages.get(langCode);
        return (langMap != null) ? langMap.get(key) : null;
    }
}
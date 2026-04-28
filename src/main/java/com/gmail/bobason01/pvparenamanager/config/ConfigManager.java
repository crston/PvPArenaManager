package com.gmail.bobason01.pvparenamanager.config;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import com.gmail.bobason01.pvparenamanager.arena.ArenaType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final PvPArenaManager plugin;
    private FileConfiguration config;

    // 캐싱을 위한 필드들
    private String defaultLanguage;
    private int countdownSeconds;
    private final Map<ArenaType, Integer> modeTimes = new EnumMap<>(ArenaType.class);

    private List<String> matchStartCommandsBoth;
    private List<String> matchEndCommandsWinner;
    private List<String> matchEndCommandsLoser;
    private List<String> matchEndCommandsBoth;

    public ConfigManager(PvPArenaManager plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // 시스템 설정 로드
        this.defaultLanguage = config.getString("system.default_language", "ko");
        this.countdownSeconds = config.getInt("system.countdown_seconds", 5);

        // 모드별 제한 시간 로드 (성능을 위해 미리 캐싱)
        for (ArenaType type : ArenaType.values()) {
            int time = config.getInt("times." + type.name(), 300);
            modeTimes.put(type, time);
        }

        // 실행 명령어 리스트 로드
        this.matchStartCommandsBoth = config.getStringList("commands.start.both");
        this.matchEndCommandsWinner = config.getStringList("commands.end.winner");
        this.matchEndCommandsLoser = config.getStringList("commands.end.loser");
        this.matchEndCommandsBoth = config.getStringList("commands.end.both");
    }

    // Getter 메서드들 (메모리에 로드된 값 즉시 반환)
    public int getGameTime(ArenaType type) {
        return modeTimes.getOrDefault(type, 300);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public List<String> getMatchStartCommandsBoth() {
        return matchStartCommandsBoth;
    }

    public List<String> getMatchEndCommandsWinner() {
        return matchEndCommandsWinner;
    }

    public List<String> getMatchEndCommandsLoser() {
        return matchEndCommandsLoser;
    }

    public List<String> getMatchEndCommandsBoth() {
        return matchEndCommandsBoth;
    }

    public String getStorageType() {
        return config.getString("storage.type", "YAML").toUpperCase();
    }
}
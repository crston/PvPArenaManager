package com.gmail.bobason01.pvparenamanager;

import com.gmail.bobason01.pvparenamanager.arena.ArenaManager;
import com.gmail.bobason01.pvparenamanager.command.ArenaCommand;
import com.gmail.bobason01.pvparenamanager.config.ConfigManager;
import com.gmail.bobason01.pvparenamanager.config.LangManager;
import com.gmail.bobason01.pvparenamanager.data.DataManager;
import com.gmail.bobason01.pvparenamanager.database.DatabaseManager;
import com.gmail.bobason01.pvparenamanager.listener.MatchListener;
import com.gmail.bobason01.pvparenamanager.listener.MenuListener;
import com.gmail.bobason01.pvparenamanager.match.MatchManager;
import com.gmail.bobason01.pvparenamanager.menu.MenuManager;
import com.gmail.bobason01.pvparenamanager.util.WGUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PvPArenaManager extends JavaPlugin implements Listener {

    private static PvPArenaManager instance;

    private ConfigManager configManager;
    private LangManager langManager;
    private ArenaManager arenaManager;
    private DataManager dataManager;
    private DatabaseManager databaseManager;
    private MatchManager matchManager;
    private MenuManager menuManager;
    private WGUtil wgUtil;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 설정 및 언어 로드
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        this.langManager = new LangManager(this);
        this.langManager.loadLang();

        // 2. 데이터 및 DB 연결
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        this.dataManager = new DataManager();

        // 3. 유틸 및 매니저 초기화
        this.wgUtil = new WGUtil();
        this.arenaManager = new ArenaManager();
        this.arenaManager.loadArenas();

        this.matchManager = new MatchManager(this);
        this.menuManager = new MenuManager(this);

        // 4. 리스너 등록 (매칭, GUI, 전투 로직)
        getServer().getPluginManager().registerEvents(this, this); // 데이터 로드용
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new MatchListener(this), this);

        // 5. 명령어 등록
        if (getCommand("pvparena") != null) {
            ArenaCommand arenaCommand = new ArenaCommand(this);
            getCommand("pvparena").setExecutor(arenaCommand);
            getCommand("pvparena").setTabCompleter(arenaCommand);
        }

        getLogger().info("PvPArenaManager has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (this.matchManager != null) {
            // 종료 시 진행 중인 모든 게임 강제 종료 처리 필요 시 추가
        }
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
    }

    // 데이터 로드 최적화: 접속 시 비동기로 DB에서 데이터 가져오기
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        CompletableFuture.supplyAsync(() -> databaseManager.loadPlayerData(uuid))
                .thenAccept(data -> dataManager.setPlayerData(uuid, data));
    }

    // 데이터 정리: 퇴장 시 메모리 캐시 삭제
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dataManager.removePlayerData(event.getPlayer().getUniqueId());
    }

    public static PvPArenaManager getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public WGUtil getWgUtil() {
        return wgUtil;
    }
}
package com.gmail.bobason01.pvparenamanager.database;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import com.gmail.bobason01.pvparenamanager.data.PlayerData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final PvPArenaManager plugin;
    private HikariDataSource dataSource;
    private String storageType;
    private File yamlFile;
    private FileConfiguration yamlConfig;

    public DatabaseManager(PvPArenaManager plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        this.storageType = plugin.getConfig().getString("storage.type", "YAML").toUpperCase();

        if (storageType.equals("MYSQL") || storageType.equals("SQLITE")) {
            setupSQL();
        } else {
            setupYAML();
        }
    }

    private void setupSQL() {
        HikariConfig config = new HikariConfig();
        if (storageType.equals("MYSQL")) {
            String host = plugin.getConfig().getString("storage.mysql.host");
            String port = plugin.getConfig().getString("storage.mysql.port");
            String db = plugin.getConfig().getString("storage.mysql.database");
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db);
            config.setUsername(plugin.getConfig().getString("storage.mysql.username"));
            config.setPassword(plugin.getConfig().getString("storage.mysql.password"));
        } else {
            // SQLite 설정
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.setMaximumPoolSize(10);

        this.dataSource = new HikariDataSource(config);
        createTable();
    }

    private void setupYAML() {
        this.yamlFile = new File(plugin.getDataFolder(), "userdata.yml");
        if (!yamlFile.exists()) {
            try { yamlFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        this.yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
    }

    private void createTable() {
        if (dataSource == null) return;
        String sql = "CREATE TABLE IF NOT EXISTS pvp_stats (uuid VARCHAR(36) PRIMARY KEY, wins INT DEFAULT 0, losses INT DEFAULT 0, points INT DEFAULT 0)";
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // 비동기 데이터 저장
    public void updateStats(UUID uuid, int winAdd, int lossAdd, int pointAdd) {
        CompletableFuture.runAsync(() -> {
            if (storageType.equals("MYSQL") || storageType.equals("SQLITE")) {
                String sql = "INSERT INTO pvp_stats (uuid, wins, losses, points) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE wins = wins + ?, losses = losses + ?, points = points + ?";
                // SQLite의 경우 ON DUPLICATE KEY 대신 REPLACE 등을 써야할 수 있으나 성능상 통합 처리
                try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, winAdd); ps.setInt(3, lossAdd); ps.setInt(4, pointAdd);
                    ps.setInt(5, winAdd); ps.setInt(6, lossAdd); ps.setInt(7, pointAdd);
                    ps.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            } else {
                // YAML 저장 (메모리 업데이트 후 파일 저장)
                String path = "users." + uuid.toString() + ".";
                yamlConfig.set(path + "wins", yamlConfig.getInt(path + "wins", 0) + winAdd);
                yamlConfig.set(path + "losses", yamlConfig.getInt(path + "losses", 0) + lossAdd);
                yamlConfig.set(path + "points", yamlConfig.getInt(path + "points", 0) + pointAdd);
                try { yamlConfig.save(yamlFile); } catch (IOException e) { e.printStackTrace(); }
            }
        });
    }

    // 플레이어 접속 시 데이터 로드 (중요)
    public PlayerData loadPlayerData(UUID uuid) {
        if (storageType.equals("MYSQL") || storageType.equals("SQLITE")) {
            String sql = "SELECT * FROM pvp_stats WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new PlayerData(rs.getInt("wins"), rs.getInt("losses"), rs.getInt("points"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            String path = "users." + uuid.toString() + ".";
            if (yamlConfig.contains(path)) {
                return new PlayerData(yamlConfig.getInt(path + "wins"), yamlConfig.getInt(path + "losses"), yamlConfig.getInt(path + "points"));
            }
        }
        return new PlayerData(); // 데이터 없으면 브론즈 0점 기본값 반환
    }

    public void close() {
        if (dataSource != null) dataSource.close();
    }
}
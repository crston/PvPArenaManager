package com.gmail.bobason01.pvparenamanager.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    // 스레드 안전성을 위해 ConcurrentHashMap 사용 (성능 최적화)
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();

    // 플레이어 데이터 가져오기 (없으면 기본값 생성)
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, k -> new PlayerData());
    }

    // DB에서 로드된 데이터를 캐시에 설정
    public void setPlayerData(UUID uuid, PlayerData data) {
        if (data != null) {
            playerDataCache.put(uuid, data);
        }
    }

    // 플레이어 퇴장 시 메모리 관리를 위해 제거
    public void removePlayerData(UUID uuid) {
        playerDataCache.remove(uuid);
    }

    // 전체 캐시 데이터 반환 (필요 시)
    public Map<UUID, PlayerData> getAllCachedData() {
        return playerDataCache;
    }
}
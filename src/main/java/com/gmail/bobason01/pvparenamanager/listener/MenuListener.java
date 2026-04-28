package com.gmail.bobason01.pvparenamanager.listener;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import com.gmail.bobason01.pvparenamanager.arena.ArenaType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {

    private final PvPArenaManager plugin;
    private final String mainMenuTitle;
    private final String langMenuTitle;

    public MenuListener(PvPArenaManager plugin) {
        this.plugin = plugin;
        this.mainMenuTitle = plugin.getLangManager().getMessage(null, "menu_title");
        this.langMenuTitle = plugin.getMenuManager().getLangMenuTitle();
    }

    // 아이템 클릭 및 이동 방지 (가장 높은 우선순위로 처리)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // 우리 플러그인의 GUI가 아니면 즉시 리턴 (성능 최적화)
        if (!title.equals(mainMenuTitle) && !title.equals(langMenuTitle)) return;

        // GUI 내의 모든 클릭 이벤트 취소 (아이템이 빠지는 것을 원천 봉쇄)
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // 빈 공간 클릭 시 무시
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        // 메인 매칭 메뉴 로직
        if (title.equals(mainMenuTitle)) {
            handleMainMenu(event.getCurrentItem().getType(), player);
        }
        // 언어 선택 메뉴 로직
        else if (title.equals(langMenuTitle)) {
            handleLangMenu(event.getRawSlot(), player);
        }
    }

    // 아이템 드래그를 통한 유출 방지
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(mainMenuTitle) || title.equals(langMenuTitle)) {
            event.setCancelled(true);
        }
    }

    private void handleMainMenu(Material mat, Player player) {
        switch (mat) {
            case IRON_SWORD: plugin.getMatchManager().addToQueue(player, ArenaType.ONE_VS_ONE); break;
            case GOLDEN_SWORD: plugin.getMatchManager().addToQueue(player, ArenaType.TWO_VS_TWO); break;
            case DIAMOND_SWORD: plugin.getMatchManager().addToQueue(player, ArenaType.THREE_VS_THREE); break;
            case NETHERITE_SWORD: plugin.getMatchManager().addToQueue(player, ArenaType.FOUR_VS_FOUR); break;
            case TNT: plugin.getMatchManager().addToQueue(player, ArenaType.DEATHMATCH); break;
            case BOOK: plugin.getMenuManager().openLanguageMenu(player); return; // 닫지 않고 언어 메뉴로 이동
            case BARRIER: removeFromAllQueues(player); break;
            default: return;
        }
        player.closeInventory();
    }

    private void handleLangMenu(int slot, Player player) {
        if (slot == 3) {
            plugin.getLangManager().setPlayerLanguage(player, "ko");
            player.sendMessage(plugin.getLangManager().getMessage(player, "lang_change_success"));
        } else if (slot == 5) {
            plugin.getLangManager().setPlayerLanguage(player, "en");
            player.sendMessage(plugin.getLangManager().getMessage(player, "lang_change_success"));
        }
        player.closeInventory();
    }

    private void removeFromAllQueues(Player player) {
        for (ArenaType type : ArenaType.values()) {
            plugin.getMatchManager().getMatchQueues().get(type).removeIf(entry -> entry.getUuid().equals(player.getUniqueId()));
        }
        player.sendMessage(plugin.getLangManager().getMessage(player, "queue_leave"));
    }
}
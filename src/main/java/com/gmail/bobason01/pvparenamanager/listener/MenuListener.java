package com.gmail.bobason01.pvparenamanager.listener;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import com.gmail.bobason01.pvparenamanager.arena.ArenaType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {

    private final PvPArenaManager plugin;

    public MenuListener(PvPArenaManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();

        // 해당 플레이어의 언어 설정에 따른 메뉴 타이틀 가져오기
        String mainMenuTitle = plugin.getLangManager().getMessage(player, "menu_title");
        String langMenuTitle = plugin.getLangManager().getMessage(player, "menu_lang_title");

        boolean isMainMenu = title.equals(mainMenuTitle);
        boolean isLangMenu = title.equals(langMenuTitle);

        // PAM 메뉴가 아니면 무시
        if (!isMainMenu && !isLangMenu) return;

        // 메뉴 조작 차단 (아이템 가져오기, 옮기기 등)
        event.setCancelled(true);

        // 강력한 조치: 클릭 액션이 아이템 수집 또는 이동일 경우 추가 차단
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR ||
                event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // 상단 인벤토리 영역 클릭 시에만 로직 수행
        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            if (isMainMenu) {
                handleMainMenu(clickedItem.getType(), player);
            } else if (isLangMenu) {
                handleLangMenu(event.getRawSlot(), player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        String mainMenuTitle = plugin.getLangManager().getMessage(player, "menu_title");
        String langMenuTitle = plugin.getLangManager().getMessage(player, "menu_lang_title");

        if (title.equals(mainMenuTitle) || title.equals(langMenuTitle)) {
            event.setCancelled(true);
        }
    }

    private void handleMainMenu(Material mat, Player player) {
        switch (mat) {
            case IRON_SWORD:
                plugin.getMatchManager().addToQueue(player, ArenaType.ONE_VS_ONE);
                break;
            case GOLDEN_SWORD:
                plugin.getMatchManager().addToQueue(player, ArenaType.TWO_VS_TWO);
                break;
            case DIAMOND_SWORD:
                plugin.getMatchManager().addToQueue(player, ArenaType.THREE_VS_THREE);
                break;
            case NETHERITE_SWORD:
                plugin.getMatchManager().addToQueue(player, ArenaType.FOUR_VS_FOUR);
                break;
            case TNT:
                plugin.getMatchManager().addToQueue(player, ArenaType.DEATHMATCH);
                break;
            case BOOK:
                plugin.getMenuManager().openLanguageMenu(player);
                return; // 언어 메뉴를 여는 경우 인벤토리를 닫지 않음
            case BARRIER:
                plugin.getMatchManager().removeFromQueue(player);
                break;
            default:
                return;
        }
        player.closeInventory();
    }

    private void handleLangMenu(int slot, Player player) {
        // 슬롯 번호 기반 언어 설정 처리
        if (slot == 3) {
            plugin.getLangManager().setPlayerLanguage(player, "ko");
            player.sendMessage(plugin.getLangManager().getMessage(player, "lang_change_success"));
        } else if (slot == 5) {
            plugin.getLangManager().setPlayerLanguage(player, "en");
            player.sendMessage(plugin.getLangManager().getMessage(player, "lang_change_success"));
        }
        player.closeInventory();
    }
}
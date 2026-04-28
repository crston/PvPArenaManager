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
    private final String mainMenuTitle;
    private final String langMenuTitle;

    public MenuListener(PvPArenaManager plugin) {
        this.plugin = plugin;
        this.mainMenuTitle = plugin.getLangManager().getMessage(null, "menu_title");
        this.langMenuTitle = plugin.getMenuManager().getLangMenuTitle();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (!title.equals(mainMenuTitle) && !title.equals(langMenuTitle)) return;

        // 아이템이 인벤토리 밖으로 빠지거나 마우스에 붙는 것을 방지하는 강력한 조치
        event.setCancelled(true);

        // 아이템을 수집하거나 옮기는 액션 자체를 차단
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR ||
                event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        }

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // 메뉴 영역(상단) 클릭 시에만 로직 작동
        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            if (title.equals(mainMenuTitle)) {
                handleMainMenu(clickedItem.getType(), player);
            } else if (title.equals(langMenuTitle)) {
                handleLangMenu(event.getRawSlot(), player);
            }
        }
    }

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
            case BOOK:
                plugin.getMenuManager().openLanguageMenu(player);
                return;
            case BARRIER:
                // [수정] 직접 삭제하지 말고 MatchManager의 메서드를 호출해야 보스바가 지워집니다.
                plugin.getMatchManager().removeFromQueue(player);
                break;
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
}
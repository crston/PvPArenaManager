package com.gmail.bobason01.pvparenamanager.menu;

import com.gmail.bobason01.pvparenamanager.PvPArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MenuManager {

    private final PvPArenaManager plugin;

    public MenuManager(PvPArenaManager plugin) {
        this.plugin = plugin;
    }

    public void openMatchMenu(Player player) {
        // 플레이어 언어별 메뉴 타이틀 로드
        String title = plugin.getLangManager().getMessage(player, "menu_title");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // 매칭 모드 아이템 설정
        inv.setItem(10, createItem(Material.IRON_SWORD,
                plugin.getLangManager().getMessage(player, "item_1v1_name"),
                plugin.getLangManager().getMessage(player, "item_1v1_lore")));

        inv.setItem(11, createItem(Material.GOLDEN_SWORD,
                plugin.getLangManager().getMessage(player, "item_2v2_name"),
                plugin.getLangManager().getMessage(player, "item_2v2_lore")));

        inv.setItem(12, createItem(Material.DIAMOND_SWORD,
                plugin.getLangManager().getMessage(player, "item_3v3_name"),
                plugin.getLangManager().getMessage(player, "item_3v3_lore")));

        inv.setItem(13, createItem(Material.NETHERITE_SWORD,
                plugin.getLangManager().getMessage(player, "item_4v4_name"),
                plugin.getLangManager().getMessage(player, "item_4v4_lore")));

        inv.setItem(15, createItem(Material.TNT,
                plugin.getLangManager().getMessage(player, "item_dm_name"),
                plugin.getLangManager().getMessage(player, "item_dm_lore")));

        // 유틸리티 버튼 (언어 설정 및 취소)
        inv.setItem(21, createItem(Material.BOOK,
                plugin.getLangManager().getMessage(player, "item_lang_name"),
                plugin.getLangManager().getMessage(player, "item_lang_lore")));

        inv.setItem(23, createItem(Material.BARRIER,
                plugin.getLangManager().getMessage(player, "item_cancel_name"),
                plugin.getLangManager().getMessage(player, "item_cancel_lore")));

        player.openInventory(inv);
    }

    public void openLanguageMenu(Player player) {
        // 플레이어 언어별 언어 선택 메뉴 타이틀 로드
        String title = plugin.getLangManager().getMessage(player, "menu_lang_title");
        Inventory inv = Bukkit.createInventory(null, 9, title);

        // 언어 선택 아이템 (한국어/영어 고정 명칭 사용)
        inv.setItem(3, createItem(Material.PAPER, "&f한국어 (Korean)", "&7클릭하여 한국어로 설정합니다."));
        inv.setItem(5, createItem(Material.PAPER, "&fEnglish (English)", "&7Click to set language to English."));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lores) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // LangManager에서 이미 ChatColor 처리가 완료된 문자열을 전달받음
            meta.setDisplayName(name);
            if (lores.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lores) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
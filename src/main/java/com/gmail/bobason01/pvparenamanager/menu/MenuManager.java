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
    private final String mainMenuTitle;
    private final String langMenuTitle;

    public MenuManager(PvPArenaManager plugin) {
        this.plugin = plugin;
        this.mainMenuTitle = plugin.getLangManager().getMessage(null, "menu_title");
        this.langMenuTitle = "Select Language / 언어 선택";
    }

    public void openMatchMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, mainMenuTitle);

        // 배경 유리판 생략 (성능 최적화 버전)
        inv.setItem(10, createItem(Material.IRON_SWORD, "&f[ 1vs1 ]", "&71vs1 Match"));
        inv.setItem(11, createItem(Material.GOLDEN_SWORD, "&f[ 2vs2 ]", "&72vs2 Match"));
        inv.setItem(12, createItem(Material.DIAMOND_SWORD, "&f[ 3vs3 ]", "&73vs3 Match"));
        inv.setItem(13, createItem(Material.NETHERITE_SWORD, "&f[ 4vs4 ]", "&74vs4 Match"));
        inv.setItem(15, createItem(Material.TNT, "&c[ Deathmatch ]", "&7Deathmatch"));

        // 언어 설정 버튼 (책 아이템)
        inv.setItem(21, createItem(Material.BOOK, "&e[ Language / 언어 ]", "&7Change your language."));
        inv.setItem(23, createItem(Material.BARRIER, "&c[ Cancel / 취소 ]", "&7Leave Queue"));

        player.openInventory(inv);
    }

    public void openLanguageMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, langMenuTitle);

        inv.setItem(3, createItem(Material.PAPER, "&f한국어 (Korean)", "&7클릭하여 한국어로 설정합니다."));
        inv.setItem(5, createItem(Material.PAPER, "&fEnglish (영어)", "&7Click to set language to English."));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lores) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
            if (lores.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lores) {
                    loreList.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getLangMenuTitle() {
        return langMenuTitle;
    }
}
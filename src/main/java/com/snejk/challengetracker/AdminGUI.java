package com.snejk.challengetracker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminGUI implements Listener {

    private final ChallengeTracker plugin;
    private final String GUI_NAME = "§8Event Settings";

    public AdminGUI(ChallengeTracker plugin) {
        this.plugin = plugin;
    }

    private Component format(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, format(GUI_NAME));

        // Adding Time
        inv.setItem(10, createItem(Material.GOLD_BLOCK, "§6+1 Week", "§7Click to add 7 days", "§7to the event timer."));
        inv.setItem(11, createItem(Material.CLOCK, "§a+1 Day", "§7Click to add 24 hours", "§7to the event timer."));

        // Status / Refresh
        long endTime = plugin.getConfig().getLong("active-event.end-timestamp", 0);
        long timeLeft = endTime - System.currentTimeMillis();
        String status = (timeLeft > 0) ? "§aActive" : "§cEnded";
        inv.setItem(13, createItem(Material.PAPER, "§eCurrent Event Status", "§7Status: " + status, "§7Click to refresh menu!"));

        // Removing Time
        inv.setItem(15, createItem(Material.REDSTONE_TORCH, "§c-1 Day", "§7Click to remove 24 hours", "§7from the event timer."));

        // Utilities
        inv.setItem(16, createItem(Material.NAME_TAG, "§bShow Leaderboard", "§7Closes menu and shows", "§7the leaderboard in chat."));
        inv.setItem(26, createItem(Material.BARRIER, "§cClose Menu", "§7Exit the settings."));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(format(name));
            List<Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(format(line));
            }
            meta.lore(componentLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().title().equals(format(GUI_NAME))) return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null) return;

        Component displayNameComponent = meta.displayName();
        if (displayNameComponent == null) return;

        Player player = (Player) e.getWhoClicked();
        String itemName = LegacyComponentSerializer.legacySection().serialize(displayNameComponent);

        long currentEnd = plugin.getConfig().getLong("active-event.end-timestamp", 0);
        long oneDayMillis = 1000L * 60 * 60 * 24;
        long oneWeekMillis = oneDayMillis * 7;

        switch (itemName) {
            case "§6+1 Week" -> {
                plugin.getConfig().set("active-event.end-timestamp", currentEnd + oneWeekMillis);
                plugin.saveConfig();
                player.sendMessage("§aAdded 7 days to the event!");
                open(player);
            }
            case "§a+1 Day" -> {
                plugin.getConfig().set("active-event.end-timestamp", currentEnd + oneDayMillis);
                plugin.saveConfig();
                player.sendMessage("§aAdded 24 hours to the event!");
                open(player);
            }
            case "§c-1 Day" -> {
                plugin.getConfig().set("active-event.end-timestamp", currentEnd - oneDayMillis);
                plugin.saveConfig();
                player.sendMessage("§cRemoved 24 hours from the event!");
                open(player);
            }
            case "§eCurrent Event Status" -> open(player); // No brackets needed!
            case "§bShow Leaderboard" -> {
                player.closeInventory();
                player.performCommand("challenge leaderboard");
            }
            case "§cClose Menu" -> player.closeInventory(); // No brackets needed!
        }
    }
}
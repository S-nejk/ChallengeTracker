package com.snejk.challengetracker;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class ChallengeCommand implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final ChallengeTracker plugin;

    public ChallengeCommand(ChallengeTracker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        // === 1. ADMIN RELOAD COMMAND ===
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("challengetracker.admin") && !player.isOp()) {
                player.sendMessage("§cYou don't have permission to do this.");
                return true;
            }
            plugin.reloadConfig();
            player.sendMessage("§a[ChallengeTracker] Config reloaded successfully!");
            return true;
        }

        // === 2. ADMIN GUI COMMAND ===
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!player.hasPermission("challengetracker.admin") && !player.isOp()) {
                player.sendMessage("§cYou don't have permission to do this.");
                return true;
            }
            new AdminGUI(plugin).open(player);
            return true;
        }

        // === 3. LEADERBOARD COMMAND ===
        if (args.length == 0 || args[0].equalsIgnoreCase("leaderboard")) {

            String title = plugin.getConfig().getString("active-event.title", "&6Event");
            int goal = plugin.getConfig().getInt("active-event.goal", 100000);
            long endTime = plugin.getConfig().getLong("active-event.end-timestamp", 0);
            boolean isCompleted = plugin.getConfig().getBoolean("active-event.is-completed", false);

            // --- THE SAFE SWITCH (FROZEN LEADERBOARD) ---
            if (isCompleted) {
                player.sendMessage("");
                player.sendMessage(title.replace("&", "§") + " §c[ENDED]");
                player.sendMessage("§eFinal Total: §6" + plugin.getConfig().getInt("active-event.final-total") + " §e/ §6" + goal);
                player.sendMessage("");

                List<String> snapshot = plugin.getConfig().getStringList("active-event.final-leaderboard");
                for (String line : snapshot) {
                    player.sendMessage(line.replace("&", "§"));
                }
                player.sendMessage("");
                player.sendMessage("§cThis event has concluded!");
                player.sendMessage("");
                return true;
            }

            // --- ACTIVE SCANNER ---
            player.sendMessage("§7[§6Challenge§7] §eScanning data, please wait...");
            String questId = plugin.getConfig().getString("active-event.quest-id", "Quest1");
            String taskId = plugin.getConfig().getString("active-event.task-id", "task1");

            new BukkitRunnable() {
                @Override
                public void run() {
                    File questsFolder = new File(plugin.getDataFolder().getParentFile(), "Quests/playerdata");
                    if (!questsFolder.exists() || !questsFolder.isDirectory()) {
                        player.sendMessage("§cError: Could not find the Quests data folder!");
                        return;
                    }

                    File[] files = questsFolder.listFiles();
                    if (files == null) return;

                    Map<String, Integer> playerScores = new HashMap<>();
                    int totalScore = 0;

                    for (File file : files) {
                        if (!file.getName().endsWith(".yml")) continue;
                        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);

                        if (data.getBoolean("quest-progress." + questId + ".started", false)) {
                            int score = data.getInt("quest-progress." + questId + ".task-progress." + taskId + ".progress", 0);
                            if (score > 0) {
                                String uuidString = file.getName().replace(".yml", "");
                                try {
                                    UUID uuid = UUID.fromString(uuidString);
                                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                                    String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
                                    playerScores.put(name, score);
                                    totalScore += score;
                                } catch (IllegalArgumentException ignored) {}
                            }
                        }
                    }

                    List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(playerScores.entrySet());
                    sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                    long timeLeft = endTime - System.currentTimeMillis();
                    boolean timeIsUp = timeLeft <= 0;
                    boolean goalReached = totalScore >= goal;

                    int finalTotal = totalScore;
                    double percentage = ((double) finalTotal / goal) * 100;

                    // Execute final messages and saves on the Main Thread safely
                    new BukkitRunnable() {
                        @Override
                        public void run() {

                            // 1. MILESTONE BROADCASTS
                            checkAndBroadcastMilestone(25, percentage, title);
                            checkAndBroadcastMilestone(50, percentage, title);
                            checkAndBroadcastMilestone(75, percentage, title);

                            // 2. CHECK IF WE NEED TO FREEZE THE LEADERBOARD
                            if (goalReached || timeIsUp) {
                                plugin.getConfig().set("active-event.is-completed", true);
                                plugin.getConfig().set("active-event.final-total", finalTotal);

                                List<String> savedBoard = new ArrayList<>();
                                int rank = 1;
                                for (Map.Entry<String, Integer> entry : sortedList) {
                                    if (rank > 10) break;
                                    savedBoard.add("&7#" + rank + " &a" + entry.getKey() + " &8- &f" + entry.getValue());
                                    rank++;
                                }
                                plugin.getConfig().set("active-event.final-leaderboard", savedBoard);
                                plugin.saveConfig();

// GLOBAL WIN ANNOUNCEMENT!
                                // 1. Convert Minecraft ticks to milliseconds (1 tick = 50ms)
                                // In, Stay, Out = 10 ticks (500ms), 70 ticks (3500ms), 20 ticks (1000ms)
                                java.time.Duration fadeIn = java.time.Duration.ofMillis(500);
                                java.time.Duration stay = java.time.Duration.ofMillis(3500);
                                java.time.Duration fadeOut = java.time.Duration.ofMillis(1000);
                                net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(fadeIn, stay, fadeOut);

                                // 2. Create the modern components
                                net.kyori.adventure.text.Component mainTitle = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("§6§lEVENT COMPLETE!");
                                net.kyori.adventure.text.Component subTitle = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("§eWe reached the goal!");
                                net.kyori.adventure.title.Title winTitle = net.kyori.adventure.title.Title.title(mainTitle, subTitle, times);

                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                                    p.showTitle(winTitle); // The modern replacement for sendTitle!
                                    p.sendMessage("§8=================================");
                                    p.sendMessage("§6§l" + title.replace("&", "§") + " §e§lCOMPLETED!");
                                    p.sendMessage("§7Type §a/ch leaderboard §7to see final results!");
                                    p.sendMessage("§8=================================");
                                }
                            }

                            // 3. SHOW THE REQUESTING PLAYER THE LIVE BOARD
                            player.sendMessage("");
                            player.sendMessage(title.replace("&", "§"));
                            player.sendMessage("§eTotal: §6" + finalTotal + " §e/ §6" + goal);
                            player.sendMessage("");

                            int rank = 1;
                            for (Map.Entry<String, Integer> entry : sortedList) {
                                if (rank > 10) break;
                                player.sendMessage("§7#" + rank + " §a" + entry.getKey() + " §8- §f" + entry.getValue());
                                rank++;
                            }

                            player.sendMessage("");
                            if (timeIsUp) {
                                player.sendMessage("§cEvent Ended!");
                            } else {
                                long days = timeLeft / (1000 * 60 * 60 * 24);
                                long hours = (timeLeft / (1000 * 60 * 60)) % 24;
                                player.sendMessage("§7Time Remaining: §a" + days + "d " + hours + "h");
                            }
                            player.sendMessage("");
                        }
                    }.runTask(plugin);
                }
            }.runTaskAsynchronously(plugin);
            return true;
        }

        return false;
    }

    // Helper method for playing sounds and broadcasting percentages
    private void checkAndBroadcastMilestone(int targetPercent, double currentPercent, String title) {
        if (currentPercent >= targetPercent) {
            String path = "active-event.milestones." + targetPercent;
            // If the milestone hasn't been hit yet in the config
            if (!plugin.getConfig().getBoolean(path, false)) {
                plugin.getConfig().set(path, true);
                plugin.saveConfig();

                // Play a cool chime and announce to everyone online
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    p.sendMessage("§8[§b" + title.replace("&", "§") + "§8] §eWe just reached §a§l" + targetPercent + "% §eof our goal!");
                }
            }
        }
    }
    // === TAB COMPLETION ===
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // We only want to auto-complete the first word they type (args.length == 1)
        if (args.length == 1) {

            // Everyone gets the leaderboard command
            completions.add("leaderboard");

            // Only admins get the secret commands
            if (sender.hasPermission("challengetracker.admin") || sender.isOp()) {
                completions.add("admin");
                completions.add("reload");
            }

            // This clever loop filters the list based on what they are currently typing!
            List<String> result = new ArrayList<>();
            for (String str : completions) {
                if (str.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(str);
                }
            }
            return result;
        }

        // Return an empty list if they are typing a second word so it doesn't suggest player names
        return new ArrayList<>();
    }
}
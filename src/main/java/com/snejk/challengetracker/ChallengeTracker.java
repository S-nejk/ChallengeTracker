package com.snejk.challengetracker;

import org.bukkit.plugin.java.JavaPlugin;

public class ChallengeTracker extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("ChallengeTracker has been enabled! Let the games begin. Created by Snejk_");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new AdminGUI(this), this);
        // Register the /challenge command safely
        if (getCommand("challenge") != null) {
            getCommand("challenge").setExecutor(new ChallengeCommand(this));
        }
    }

    @Override
    public void onDisable() {
        // This runs when the server stops
        getLogger().info("ChallengeTracker has been disabled.");
    }
}
package me.shawlaf.varlight.spigot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.function.Supplier;

public class DebugManager {

    private final VarLightPlugin plugin;
    private boolean debugEnabled;

    public DebugManager(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public void logDebugAction(CommandSender source, Supplier<String> message, boolean broadcastToOps) {
        if (!debugEnabled) {
            return;
        }

        String msg = String.format("[DEBUG] %s: %s", source.getName(), message.get());

        if (broadcastToOps) {
            Bukkit.broadcast(ChatColor.GRAY + "" + ChatColor.ITALIC + msg, "varlight.admin");
        }
    }
}

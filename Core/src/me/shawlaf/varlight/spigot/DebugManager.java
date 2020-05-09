package me.shawlaf.varlight.spigot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.function.Supplier;

public class DebugManager {

    private final VarLightPlugin plugin;

    public DebugManager(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    public void logDebugAction(CommandSender source, Supplier<String> message) {
        if (!plugin.getConfiguration().isLogDebug()) {
            return;
        }

        Bukkit.broadcast(ChatColor.GRAY + "" + ChatColor.ITALIC + String.format("[DEBUG] %s: %s", source.getName(), message.get()), "varlight.admin");
    }
}

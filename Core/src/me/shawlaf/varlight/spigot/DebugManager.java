package me.shawlaf.varlight.spigot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        String messageFormatted = ChatColor.GRAY + "" + ChatColor.ITALIC + String.format("[DEBUG] %s: %s", source.getName(), message.get());
        String messagePlain = ChatColor.stripColor(messageFormatted);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("varlight.admin")) {
                player.sendMessage(messageFormatted);
            }
        }

        plugin.getLogger().info(messagePlain);
    }
}

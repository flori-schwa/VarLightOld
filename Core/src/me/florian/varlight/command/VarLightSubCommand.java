package me.florian.varlight.command;

import me.florian.varlight.command.exception.VarLightCommandException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public interface VarLightSubCommand {

    String getName();

    boolean execute(CommandSender sender, ArgumentIterator args);

    default void sendHelp(CommandSender sender) {

    }

    static void assertPermission(CommandSender commandSender, String node) {
        if (!commandSender.hasPermission(node)) {
            throw new VarLightCommandException(ChatColor.RED + "You do not have permission to use this command");
        }
    }

    static boolean checkPerm(CommandSender commandSender, String node) {
        if (!commandSender.hasPermission(node)) {
            commandSender.sendMessage(ChatColor.RED + "You do not have permission to use this command");
            return false;
        }

        return true;
    }

    static void sendPrefixedMessage(CommandSender to, String message) {
        to.sendMessage(getPrefixedMessage(message));
    }

    static String getPrefixedMessage(String message) {
        return String.format("[VarLight] %s", message);
    }

    static void broadcastResult(CommandSender source, String message, String node) {
        String msg = String.format("%s: %s", source.getName(), getPrefixedMessage(message));
        String formatted = ChatColor.GRAY + "" + ChatColor.ITALIC + String.format("[%s]", msg);
        source.sendMessage(getPrefixedMessage(message));

        Bukkit.getPluginManager().getPermissionSubscriptions(node).stream().filter(p -> p != source && p instanceof CommandSender).forEach(p -> {
            if (p instanceof ConsoleCommandSender) {
                ((ConsoleCommandSender) p).sendMessage(msg);
            } else {
                ((CommandSender) p).sendMessage(formatted);
            }
        });
    }
}

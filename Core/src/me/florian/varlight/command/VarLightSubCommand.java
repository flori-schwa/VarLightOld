package me.florian.varlight.command;

import me.florian.varlight.command.exception.VarLightCommandException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

public interface VarLightSubCommand {

    String getName();

    /**
     * @return The remaining syntax after "/varlight [subcommand]"
     */
    default String getSyntax() {
        return null;
    }

    /**
     * @return A short description to be rendered after "/varlight [subcommand] [syntax]:"
     */
    default String getDescription() {
        return null;
    }

    default String getCommandHelp() {
        if (getSyntax() == null || getDescription() == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        builder.append(ChatColor.GOLD);
        builder.append("/varlight ");
        builder.append(getName());
        builder.append(getSyntax());
        builder.append(": ");
        builder.append(ChatColor.RESET);
        builder.append(getDescription());

        return builder.toString();
    }

    boolean execute(CommandSender sender, ArgumentIterator args);

    default List<String> tabComplete(CommandSender sender, ArgumentIterator args) {
        return null;
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

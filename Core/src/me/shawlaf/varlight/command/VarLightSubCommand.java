package me.shawlaf.varlight.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public abstract class VarLightSubCommand {

    public abstract String getName();

    /**
     * @return The remaining syntax after "/varlight [subcommand]"
     */
    public String getSyntax() {
        return null;
    }

    /**
     * @return A short description to be rendered after "/varlight [subcommand] [syntax]:"
     */
    public String getDescription() {
        return null;
    }

    public String getCommandHelp() {
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

    public abstract boolean execute(CommandSender sender, ArgumentIterator args);

    public void tabComplete(CommandSuggestions commandSuggestions) {

    }

}

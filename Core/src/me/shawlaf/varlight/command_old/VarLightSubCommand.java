package me.shawlaf.varlight.command_old;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@Deprecated
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

        return ChatColor.GOLD + "/varlight " + getName() + getSyntax() + ": " + ChatColor.RESET + getDescription();
    }

    public abstract boolean execute(CommandSender sender, ArgumentIterator args);

    public void tabComplete(CommandSuggestions commandSuggestions) {

    }

}

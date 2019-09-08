package me.shawlaf.varlight.command.commands;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.ArgumentIterator;
import me.shawlaf.varlight.command.CommandSuggestions;
import me.shawlaf.varlight.command.VarLightCommand;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

public class VarLightCommandPermission extends VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandPermission(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "perm";
    }

    @Override
    public String getSyntax() {
        return " get/set/unset [new permission]";
    }

    @Override
    public String getDescription() {
        return "Gets/Sets/Unsets the permission node that is required to use the plugin's functionality";
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        VarLightCommand.assertPermission(sender, "varlight.admin.perm");

        if (!args.hasNext()) {
            return false;
        }

        switch (args.next().toLowerCase()) {
            case "get": {
                VarLightCommand.sendPrefixedMessage(sender, String.format("The current required permission node is \"%s\".", plugin.getConfiguration().getRequiredPermissionNode()));
                return true;
            }

            case "set": {
                if (!args.hasNext()) {
                    return false;
                }

                final String newNode = args.next();
                final String oldNode = plugin.getConfiguration().getRequiredPermissionNode();

                plugin.getConfiguration().setRequiredPermissionNode(newNode);

                VarLightCommand.broadcastResult(sender, String.format("The required permission node has been updated from \"%s\" to \"%s\".", oldNode, newNode), "varlight.admin.perm");
                return true;
            }

            case "unset": {
                plugin.getConfiguration().setRequiredPermissionNode("");
                VarLightCommand.broadcastResult(sender, "The required permission node has been un-set.", "varlight.admin.perm");
                return true;
            }

            default: {
                return false;
            }
        }
    }

    @Override
    public void tabComplete(CommandSuggestions commandSuggestions) {
        if (commandSuggestions.getArgumentCount() != 1) {
            return;
        }

        commandSuggestions.suggestChoices("get", "set", "unset");
    }
}

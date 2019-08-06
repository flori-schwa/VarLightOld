package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightCommand;
import me.florian.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

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
    public List<String> tabComplete(CommandSender sender, ArgumentIterator args) {
        final int arguments = args.length;

        if (arguments != 1) {
            return new ArrayList<>();
        }

        return VarLightCommand.suggestChoice(args.get(0), "get", "set", "unset");
    }
}

package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

public class VarLightCommandDebug implements VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandDebug(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        VarLightSubCommand.assertPermission(sender, "varlight.admin");

        VarLightPlugin.DEBUG = !VarLightPlugin.DEBUG;
        VarLightSubCommand.broadcastResult(sender, String.format("Updated Varlight debug state to: %s", VarLightPlugin.DEBUG), "varlight.admin");

        return true;
    }
}

package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightCommand;
import me.florian.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class VarLightCommandDebug extends VarLightSubCommand {

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
        VarLightCommand.assertPermission(sender, "varlight.admin");

        VarLightPlugin.DEBUG = !VarLightPlugin.DEBUG;
        VarLightCommand.broadcastResult(sender, String.format("Updated Varlight debug state to: %s", VarLightPlugin.DEBUG), "varlight.admin");

        return true;
    }
}

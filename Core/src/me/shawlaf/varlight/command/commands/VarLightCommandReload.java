package me.shawlaf.varlight.command.commands;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.ArgumentIterator;
import me.shawlaf.varlight.command.VarLightCommand;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

public class VarLightCommandReload extends VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandReload(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getSyntax() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Reload the configuration file";
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        VarLightCommand.assertPermission(sender, "varlight.admin");

        plugin.reloadConfig();
        plugin.loadLightUpdateItem();

        VarLightCommand.broadcastResult(sender, "Configuration Reloaded", "varlight.admin");
        return true;
    }
}

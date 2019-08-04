package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

public class VarLightCommandReload implements VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandReload(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public void sendHelp(CommandSender sender) {
        sender.sendMessage("/varlight reload: Reload the configuration file");
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        VarLightSubCommand.assertPermission(sender, "varlight.admin");

        plugin.reloadConfig();
        VarLightSubCommand.broadcastResult(sender, "Configuration Reloaded", "varlight.admin");
        return true;
    }
}

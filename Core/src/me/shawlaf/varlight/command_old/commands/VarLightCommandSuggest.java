package me.shawlaf.varlight.command_old.commands;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command_old.ArgumentIterator;
import me.shawlaf.varlight.command_old.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Deprecated
public class VarLightCommandSuggest extends VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandSuggest(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "suggest";
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        plugin.getNmsAdapter().suggestCommand((Player) sender, args.join());
        return true;
    }
}

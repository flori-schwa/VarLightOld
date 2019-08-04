package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VarLightCommandSuggest implements VarLightSubCommand {

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

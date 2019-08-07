package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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

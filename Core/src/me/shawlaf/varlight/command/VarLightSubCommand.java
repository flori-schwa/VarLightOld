package me.shawlaf.varlight.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.florian.command.brigadier.BrigadierCommand;
import me.shawlaf.varlight.VarLightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public abstract class VarLightSubCommand extends BrigadierCommand<CommandSender, VarLightPlugin> {

    public VarLightSubCommand(VarLightPlugin plugin, String s, boolean lateBuild) {
        super(plugin, s, CommandSender.class, lateBuild);
    }

    public VarLightSubCommand(VarLightPlugin varLightPlugin, String s) {
        this(varLightPlugin, s, false);
    }

    protected final LiteralArgumentBuilder<CommandSender> buildFrom(LiteralArgumentBuilder<CommandSender> node) {
        return buildCommand(node);
    }

    public abstract String getSubCommandName();

    public String getCommandHelp() {
        if (getSyntax() == null || getDescription() == null) {
            return null;
        }

        return ChatColor.GOLD + "/varlight " + getName() + getSyntax() + ": " + ChatColor.RESET + getDescription();
    }
}

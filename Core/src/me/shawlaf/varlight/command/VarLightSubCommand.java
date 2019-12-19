package me.shawlaf.varlight.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.florian.command.brigadier.BrigadierCommand;
import me.shawlaf.varlight.VarLightPlugin;
import org.bukkit.command.CommandSender;

public abstract class VarLightSubCommand extends BrigadierCommand<CommandSender, VarLightPlugin> {
    public VarLightSubCommand(VarLightPlugin varLightPlugin, String s) {
        super(varLightPlugin, s, CommandSender.class);
    }

    protected final LiteralArgumentBuilder<CommandSender> buildFrom(LiteralArgumentBuilder<CommandSender> node) {
        return buildCommand(node);
    }

    public abstract String getSubCommandName();
}

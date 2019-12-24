package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class VarLightCommandSave extends VarLightSubCommand {
    public VarLightCommandSave(VarLightPlugin plugin) {
        super(plugin, "save");
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " [all/<world>]";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Save all custom light sources in the current world, the specified world or all worlds";
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.save";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        return node;
    }
}

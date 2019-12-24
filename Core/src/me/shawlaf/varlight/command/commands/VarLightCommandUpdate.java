package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class VarLightCommandUpdate extends VarLightSubCommand {
    public VarLightCommandUpdate(VarLightPlugin plugin) {
        super(plugin, "update");
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " <position> <light level> [world (only if using console)]";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Update the light level at the given position";
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.update";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        return node;
    }
}

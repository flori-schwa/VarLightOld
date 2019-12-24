package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class VarLightCommandStepSize extends VarLightSubCommand {
    public VarLightCommandStepSize(VarLightPlugin plugin) {
        super(plugin, "stepsize");
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Edit the Step size when using " + plugin.getLightUpdateItem().name();
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " <step size>";
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.stepsize";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        return node;
    }
}

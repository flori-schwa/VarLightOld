package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.success;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class VarLightCommandStepSize extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_STEPSIZE = integerArgument("stepsize", 1, 15);

    public VarLightCommandStepSize(VarLightCommand command) {
        super(command, "stepsize");
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Edit the Step size when using " + plugin.getLightUpdateItem().getKey().toString() + ".";
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.stepsize";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.then(ARG_STEPSIZE.executes(this::run));

        return node;
    }

    private int run(CommandContext<CommandSender> context) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "Only players may use this command!");

            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int newStepSize = context.getArgument(ARG_STEPSIZE.getName(), int.class);

        plugin.setStepSize(player, newStepSize); // Brigadier automatically filters all inputs < 1 and > 15

        success(this, player, String.format("Set your step size to %d", newStepSize));

        return SUCCESS;
    }
}

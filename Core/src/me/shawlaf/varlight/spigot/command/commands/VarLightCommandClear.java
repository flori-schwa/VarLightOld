package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class VarLightCommandClear extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_REGION_X = integerArgument("regionX");
    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_REGION_Z = integerArgument("regionZ");

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_CHUNK_X = integerArgument("chunkX");
    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_CHUNK_Z = integerArgument("chunkZ");

    public VarLightCommandClear(VarLightCommand rootCommand) {
        super(rootCommand, "clear");
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.clear";
    }

    @Override
    public @NotNull String getDescription() {
        return "Remove Custom Light sources in a certain chunk or region";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        suggestCoordinate(ARG_CHUNK_X, e -> e.getLocation().getBlockX() >> 4);
        suggestCoordinate(ARG_CHUNK_Z, e -> e.getLocation().getBlockZ() >> 4);

        suggestCoordinate(ARG_REGION_X, e -> (e.getLocation().getBlockX() >> 4) >> 5);
        suggestCoordinate(ARG_REGION_Z, e -> (e.getLocation().getBlockZ() >> 4) >> 5);

        node.then(
                literalArgument("chunk")
                        .executes(this::executeChunkImplicit)
                        .then(
                                ARG_CHUNK_X
                                        .then(
                                                ARG_CHUNK_Z.executes(this::executeChunkExplicit)
                                        )
                        )
        );

        node.then(
                literalArgument("region")
                        .executes(this::executeRegionImplicit)
                        .then(
                                ARG_REGION_X
                                        .then(
                                                ARG_REGION_Z.executes(this::executeRegionExplicit)
                                        )
                        )
        );

        return node;
    }

    private int executeChunkImplicit(CommandContext<CommandSender> context) {

        // TODO Implement

        return SUCCESS;
    }

    private int executeChunkExplicit(CommandContext<CommandSender> context) {

        // TODO Implement

        return SUCCESS;
    }

    private int executeRegionImplicit(CommandContext<CommandSender> context) {

        // TODO Implement

        return SUCCESS;
    }

    private int executeRegionExplicit(CommandContext<CommandSender> context) {

        // TODO Implement

        return SUCCESS;
    }
}

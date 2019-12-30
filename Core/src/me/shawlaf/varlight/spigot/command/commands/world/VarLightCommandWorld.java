package me.shawlaf.varlight.spigot.command.commands.world;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.spigot.VarLightConfiguration;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.command.brigadier.argument.WorldArgumentType.world;
import static me.shawlaf.command.result.CommandResult.*;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class VarLightCommandWorld extends VarLightSubCommand {

    private static final String PARAM_WORLD = "world";

    private final VarLightConfiguration.WorldListType worldListType;

    public VarLightCommandWorld(VarLightPlugin plugin, VarLightConfiguration.WorldListType listType) {
        super(plugin, listType.getConfigPath());

        this.worldListType = listType;
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " add/remove/list [world]";
    }

    @NotNull
    @Override
    public String getDescription() {
        return String.format("Add/Remove worlds from the %s or list all worlds on the %s", worldListType.getName(), worldListType.getName());
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.world";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        node.then(
                LiteralArgumentBuilder.<CommandSender>literal("add")
                        .then(RequiredArgumentBuilder.<CommandSender, World>argument(PARAM_WORLD, world())
                                .executes(this::add))
        );

        node.then(
                LiteralArgumentBuilder.<CommandSender>literal("remove")
                        .then(RequiredArgumentBuilder.<CommandSender, World>argument(PARAM_WORLD, world())
                                .executes(this::remove))
        );

        node.then(
                LiteralArgumentBuilder.<CommandSender>literal("list").executes(this::list)
        );

        return node;
    }

    private int add(CommandContext<CommandSender> context) {
        World toAdd = context.getArgument(PARAM_WORLD, World.class);

        if (plugin.getConfiguration().addWorldToList(toAdd, worldListType)) {
            successBroadcast(this, context.getSource(), String.format("Added world \"%s\" to the VarLight %s", toAdd.getName(), worldListType.getName()));

            return SUCCESS;
        } else {
            failure(this, context.getSource(), String.format("World \"%s\" is already on the VarLight %s", toAdd.getName(), worldListType.getName()));

            return FAILURE;
        }
    }

    private int remove(CommandContext<CommandSender> context) {
        World toRemove = context.getArgument(PARAM_WORLD, World.class);

        if (plugin.getConfiguration().removeWorldFromList(toRemove, worldListType)) {
            successBroadcast(this, context.getSource(), String.format("Removed world \"%s\" from the VarLight %s", toRemove.getName(), worldListType.getName()));

            return SUCCESS;
        } else {
            failure(this, context.getSource(), String.format("World \"%s\" is not on the VarLight %s", toRemove.getName(), worldListType.getName()));

            return FAILURE;
        }
    }

    private int list(CommandContext<CommandSender> context) {
        info(this, context.getSource(), String.format("Worlds on the VarLight %s:", worldListType.getName()));

        for (World world : plugin.getConfiguration().getWorlds(worldListType)) {
            context.getSource().sendMessage(String.format("   - \"%s\"", world.getName()));
        }

        return SUCCESS;
    }
}

package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.brigadier.argument.PositionArgumentType;
import me.shawlaf.command.brigadier.argument.WorldArgumentType;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import me.shawlaf.varlight.event.LightUpdateEvent;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static me.shawlaf.command.brigadier.argument.PositionArgumentType.position;
import static me.shawlaf.command.brigadier.argument.WorldArgumentType.world;
import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.command.VarLightCommand.SUCCESS;

public class VarLightCommandUpdate extends VarLightSubCommand {

    private static final String PARAM_POSITION = "position";
    private static final String PARAM_LIGHT_LEVEL = "light level";
    private static final String PARAM_WORLD = "world";

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

        node.then(
                RequiredArgumentBuilder.<CommandSender, ICoordinates>argument(PARAM_POSITION, position())
                .then(
                        RequiredArgumentBuilder.<CommandSender, Integer>argument(PARAM_LIGHT_LEVEL, integer(0, 15))
                        .executes(this::updateImplicit)
                        .then(
                                RequiredArgumentBuilder.<CommandSender, World>argument(PARAM_WORLD, world())
                                .executes(this::updateExplicit)
                        )
                )
        );

        return node;
    }

    private int updateImplicit(CommandContext<CommandSender> context) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "Only players may use the implicit variant of this command");

            return FAILURE;
        }

        Location position = context.getArgument(PARAM_POSITION, ICoordinates.class).toLocation(context.getSource());
        int lightLevel = context.getArgument(PARAM_LIGHT_LEVEL, int.class);

        position.setWorld(((Player) context.getSource()).getWorld());

        return update(context.getSource(), position, lightLevel);
    }

    private int updateExplicit(CommandContext<CommandSender> context) throws CommandSyntaxException {
        Location position = context.getArgument(PARAM_POSITION, ICoordinates.class).toLocation(context.getSource());
        int lightLevel = context.getArgument(PARAM_LIGHT_LEVEL, int.class);
        World world = context.getArgument(PARAM_WORLD, World.class);

        position.setWorld(world);

        return update(context.getSource(), position, lightLevel);
    }

    private int update(CommandSender source, Location location, int toLight) {
        World world = location.getWorld();
        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            failure(this, source, String.format("VarLight is not active in world \"%s\"", world.getName()));

            return FAILURE;
        }

        int fromLight = manager.getCustomLuminance(new IntPosition(location), 0);

        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            failure(this, source, "The target chunk is not loaded!");

            return FAILURE;
        }

        if (plugin.getNmsAdapter().isIllegalBlock(location.getBlock())) {
            failure(this, source, String.format("%s cannot be used as a custom light source!", location.getBlock().getType().name()));

            return FAILURE;
        }

        LightUpdateEvent lightUpdateEvent = new LightUpdateEvent(location.getBlock(), fromLight, toLight);
        Bukkit.getPluginManager().callEvent(lightUpdateEvent);

        if (lightUpdateEvent.isCancelled()) {
            failure(this, source, "The Light update event was cancelled!");

            return FAILURE;
        }

        manager.setCustomLuminance(location, lightUpdateEvent.getToLight());
        plugin.getNmsAdapter().updateBlockLight(location, lightUpdateEvent.getToLight());

        successBroadcast(this, source, String.format("Updated Light level at [%d, %d, %d] in world \"%s\" from %d to %d",
                location.getBlockX(), location.getBlockY(), location.getBlockZ(), world.getName(), fromLight, lightUpdateEvent.getToLight()));

        return SUCCESS;
    }
}

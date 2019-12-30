package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.LightUpdateResult;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.util.LightSourceUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static me.shawlaf.command.brigadier.argument.PositionArgumentType.position;
import static me.shawlaf.command.brigadier.argument.WorldArgumentType.world;
import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;
import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

public class VarLightCommandUpdate extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POSITION = argument("position", position());
    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_LIGHT_LEVEL = argument("light level", integer(0, 15));
    private static final RequiredArgumentBuilder<CommandSender, World> ARG_WORLD = argument("world", world());

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

        node.then(ARG_POSITION.then(
                ARG_LIGHT_LEVEL
                        .executes(this::updateImplicit)
                        .then(ARG_WORLD.executes(this::updateExplicit))
                )
        );

        return node;
    }

    private int updateImplicit(CommandContext<CommandSender> context) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "Only players may use the implicit variant of this command");

            return FAILURE;
        }

        Location position = context.getArgument(ARG_POSITION.getName(), ICoordinates.class).toLocation(context.getSource());
        int lightLevel = context.getArgument(ARG_LIGHT_LEVEL.getName(), int.class);

        position.setWorld(((Player) context.getSource()).getWorld());

        return update(context.getSource(), position, lightLevel);
    }

    private int updateExplicit(CommandContext<CommandSender> context) throws CommandSyntaxException {
        Location position = context.getArgument(ARG_POSITION.getName(), ICoordinates.class).toLocation(context.getSource());
        int lightLevel = context.getArgument(ARG_LIGHT_LEVEL.getName(), int.class);
        World world = context.getArgument(ARG_WORLD.getName(), World.class);

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

        int fromLight = manager.getCustomLuminance(toIntPosition(location), 0);

        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            failure(this, source, "The target chunk is not loaded!");

            return FAILURE;
        }

        if (plugin.getNmsAdapter().isIllegalBlock(location.getBlock())) {
            failure(this, source, String.format("%s cannot be used as a custom light source!", location.getBlock().getType().name()));

            return FAILURE;
        }

        LightUpdateResult result = LightSourceUtil.placeNewLightSource(plugin, location, toLight);

        if (!result.successful()) {
            failure(this, source, result.getMessage());

            return FAILURE;
        } else {
            successBroadcast(this, source, String.format("Updated Light level at [%d, %d, %d] in world \"%s\" from %d to %d",
                    location.getBlockX(), location.getBlockY(), location.getBlockZ(), world.getName(), fromLight, result.getToLight()));

            return SUCCESS;
        }
    }
}

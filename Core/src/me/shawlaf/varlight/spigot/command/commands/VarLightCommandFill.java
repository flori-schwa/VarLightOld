package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.LightUpdateResult;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.util.LightSourceUtil;
import me.shawlaf.varlight.spigot.util.RegionIterator;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static me.shawlaf.command.brigadier.argument.PositionArgumentType.position;
import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;
import static me.shawlaf.varlight.spigot.command.commands.arguments.CollectionArgumentType.collection;
import static me.shawlaf.varlight.spigot.command.commands.arguments.MinecraftTypeArgumentType.minecraftType;
import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toBlock;
import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

@SuppressWarnings("DuplicatedCode")
public class VarLightCommandFill extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_1 = argument("pos1", position());
    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_2 = argument("pos2", position());

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_LIGHT_LEVEL = argument("light level", integer(0, 15));

    public VarLightCommandFill(VarLightPlugin plugin) {
        super(plugin, "fill");
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.fill";
    }

//    @Override
//    public boolean meetsRequirement(CommandSender commandSender) {
//        return commandSender.hasPermission(getRequiredPermission()) && commandSender.hasPermission("varlight.admin.update");
//    }

    @Override
    public @NotNull String getSyntax() {
        return " <pos 1> <pos 2> <light level> [include|exclude] [<filters>...]";
    }

    @Override
    public @NotNull String getDescription() {
        return "Fills a larger area with custom light sources";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.then(
                ARG_POS_1.then(
                        ARG_POS_2.then(
                                ARG_LIGHT_LEVEL
                                        .executes(this::fillNoFilter)
                                        .then(
                                                LiteralArgumentBuilder.<CommandSender>literal("include")
                                                        .then(
                                                                RequiredArgumentBuilder.<CommandSender, Collection<Material>>argument("posFilter", collection(minecraftType(plugin, MaterialType.BLOCK)))
                                                                        .executes(this::fillPosFilter)
                                                        )
                                        )
                                        .then(
                                                LiteralArgumentBuilder.<CommandSender>literal("exclude")
                                                        .then(
                                                                RequiredArgumentBuilder.<CommandSender, Collection<Material>>argument("negFilter", collection(minecraftType(plugin, MaterialType.BLOCK)))
                                                                        .executes(this::fillNegFilter)
                                                        )
                                        )
                        )
                )
        );

        return node;
    }

    private int fillNoFilter(CommandContext<CommandSender> context) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command");
        }

        Location a = context.getArgument(ARG_POS_1.getName(), ICoordinates.class).toLocation(context.getSource());
        Location b = context.getArgument(ARG_POS_2.getName(), ICoordinates.class).toLocation(context.getSource());

        int lightLevel = context.getArgument(ARG_LIGHT_LEVEL.getName(), int.class);

        return fill((Player) context.getSource(), a, b, lightLevel, x -> true);
    }

    private int fillPosFilter(CommandContext<CommandSender> context) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command");
        }

        Location a = context.getArgument(ARG_POS_1.getName(), ICoordinates.class).toLocation(context.getSource());
        Location b = context.getArgument(ARG_POS_2.getName(), ICoordinates.class).toLocation(context.getSource());

        int lightLevel = context.getArgument(ARG_LIGHT_LEVEL.getName(), int.class);

        Collection<Material> positiveFilter = context.getArgument("posFilter", Collection.class);

        return fill((Player) context.getSource(), a, b, lightLevel, positiveFilter::contains);
    }

    private int fillNegFilter(CommandContext<CommandSender> context) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command");
        }

        Location a = context.getArgument(ARG_POS_1.getName(), ICoordinates.class).toLocation(context.getSource());
        Location b = context.getArgument(ARG_POS_2.getName(), ICoordinates.class).toLocation(context.getSource());

        int lightLevel = context.getArgument(ARG_LIGHT_LEVEL.getName(), int.class);

        Collection<Material> negativeFilter = context.getArgument("negFilter", Collection.class);

        return fill((Player) context.getSource(), a, b, lightLevel, o -> !negativeFilter.contains(o));
    }

    private int fill(Player source, Location pos1, Location pos2, int lightLevel, Predicate<Material> filter) {
        World world = source.getWorld();

        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            failure(this, source, "VarLight is not active in World \"" + world.getName() + "\"");

            return FAILURE;
        }

        IntPosition a = toIntPosition(pos1);
        IntPosition b = toIntPosition(pos2);

        RegionIterator regionIterator = new RegionIterator(a, b);

        if (!regionIterator.isRegionLoaded(world)) {
            failure(this, source, "Not all chunks in the specified region are loaded!");

            return FAILURE;
        }

        int total = 0, illegal = 0, updated = 0, skipped = 0, failed = 0;

        regionIterator.reset();

        IntPosition next;

        while (regionIterator.hasNext()) {
            next = regionIterator.next();
            Block block = toBlock(next, world);

            ++total;

            if (!filter.test(block.getType())) {
                ++skipped;
                continue;
            }

            if (plugin.getNmsAdapter().isIllegalBlock(block)) {
                ++illegal;
                continue;
            }

            LightUpdateResult result = LightSourceUtil.placeNewLightSource(plugin, block.getLocation(), lightLevel);

            if (!result.successful()) {
                ++failed;
            } else {
                ++updated;
            }
        }

        successBroadcast(this, source, String.format("Successfully updated %d Light sources in Region [%d, %d, %d] to [%d, %d, %d]. (Total blocks: %d, Invalid Blocks: %d, Skipped Blocks: %d, Failed Blocks: %d)",
                updated,
                regionIterator.pos1.x,
                regionIterator.pos1.y,
                regionIterator.pos1.z,
                regionIterator.pos2.x,
                regionIterator.pos2.y,
                regionIterator.pos2.z,
                total,
                illegal,
                skipped,
                failed
        ));

        return SUCCESS;
    }
}

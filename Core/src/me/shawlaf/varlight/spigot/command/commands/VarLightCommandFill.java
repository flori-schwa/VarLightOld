package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.LightUpdateResult;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.util.LightSourceUtil;
import me.shawlaf.varlight.spigot.util.ProgressReport;
import me.shawlaf.varlight.spigot.util.RegionIterator;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;
import static me.shawlaf.varlight.spigot.command.commands.arguments.MinecraftTypeArgumentType.minecraftType;
import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toBlock;
import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

@SuppressWarnings("DuplicatedCode")
public class VarLightCommandFill extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_1 = positionArgument("pos1");
    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_2 = positionArgument("pos2");

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_LIGHT_LEVEL = integerArgument("light level", 0, 15);

    public VarLightCommandFill(VarLightCommand command) {
        super(command, "fill");
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
    public @NotNull String getDescription() {
        return "Fills a large area with custom light sources.";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.then(
                ARG_POS_1.then(
                        ARG_POS_2.then(
                                ARG_LIGHT_LEVEL
                                        .executes(this::fillNoFilter)
                                        .then(
                                                literalArgument("include")
                                                        .then(
                                                                collectionArgument("posFilter", minecraftType(plugin, MaterialType.BLOCK)))
                                                        .executes(this::fillPosFilter)
                                        )
                                        .then(
                                                literalArgument("exclude")
                                                        .then(
                                                                collectionArgument("negFilter", minecraftType(plugin, MaterialType.BLOCK))
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

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int totalSize = regionIterator.getSize();
            ProgressReport progressReport = totalSize < 1_000_000 ? ProgressReport.EMPTY : new ProgressReport(plugin, source, String.format("Fill from %s to %s", a.toShortString(), b.toShortString()), totalSize);

            int total = 0, illegal = 0, updated = 0, skipped = 0, failed = 0;

            IntPosition next;

            Set<ChunkCoords> chunksToUpdate = new HashSet<>();

            while (regionIterator.hasNext()) {
                next = regionIterator.next();
                Block block = toBlock(next, world);
                progressReport.reportProgress(++total);

                if (!filter.test(block.getType())) {
                    ++skipped;
                    continue;
                }

                if (plugin.getNmsAdapter().isIllegalBlock(block)) {
                    ++illegal;
                    continue;
                }

                LightUpdateResult result = LightSourceUtil.placeNewLightSource(plugin, block.getLocation(), lightLevel, false);

                if (!result.successful()) {
                    ++failed;
                } else {
                    ++updated;
                }

                chunksToUpdate.addAll(plugin.getNmsAdapter().collectChunkPositionsToUpdate(new ChunkCoords(block.getX() >> 4, block.getZ() >> 4)));
            }

            plugin.getLightUpdateScheduler().enqueueChunks(true, true, world, chunksToUpdate);

            progressReport.finish();

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
        });

        return SUCCESS;
    }
}

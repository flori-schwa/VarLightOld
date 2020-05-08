package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.LightUpdateResult;
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

    public static final String ARG_NAME_POS1 = "position 1";
    public static final String ARG_NAME_POS2 = "position 2";
    public static final String ARG_NAME_LIGHT_LEVEL = "light level";
    public static final String ARG_NAME_POSITIVE_FILTER = "posFilter";
    public static final String ARG_NAME_NEGATIVE_FILTER = "negFilter";

    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_1 = positionArgument(ARG_NAME_POS1);
    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_2 = positionArgument(ARG_NAME_POS2);

    private WorldEditUtil worldEditUtil;

    public VarLightCommandFill(VarLightCommand command) {
        super(command, "fill");
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.fill";
    }

    @Override
    public @NotNull String getDescription() {
        return "Fills a large area with custom light sources.";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.then(
                ARG_POS_1.then(
                        ARG_POS_2.then(
                                integerArgument(ARG_NAME_LIGHT_LEVEL, 0, 15)
                                        .executes(c -> fillNoFilter(c, false))
                                        .then(
                                                literalArgument("include")
                                                        .then(
                                                                collectionArgument(ARG_NAME_POSITIVE_FILTER, minecraftType(plugin, MaterialType.BLOCK))
                                                                        .executes(c -> fillPosFilter(c, false))
                                                        )
                                        )
                                        .then(
                                                literalArgument("exclude")
                                                        .then(
                                                                collectionArgument(ARG_NAME_NEGATIVE_FILTER, minecraftType(plugin, MaterialType.BLOCK))
                                                                        .executes(c -> fillNegFilter(c, false))
                                                        )
                                        )
                        )
                )
        );

        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            this.worldEditUtil = new WorldEditUtil(plugin);

            node.then(
                    integerArgument(ARG_NAME_LIGHT_LEVEL, 0, 15)
                            .executes(c -> fillNoFilter(c, true))
                            .then(
                                    literalArgument("include")
                                            .then(
                                                    collectionArgument(ARG_NAME_POSITIVE_FILTER, minecraftType(plugin, MaterialType.BLOCK))
                                                            .executes(c -> fillPosFilter(c, true))
                                            )
                            )
                            .then(
                                    literalArgument("exclude")
                                            .then(
                                                    collectionArgument(ARG_NAME_NEGATIVE_FILTER, minecraftType(plugin, MaterialType.BLOCK))
                                                            .executes(c -> fillNegFilter(c, true))
                                            )
                            )
            );
        }

        return node;
    }

    private Location[] getSelection(CommandContext<CommandSender> context, boolean worldEdit) throws CommandSyntaxException {
        Player player = (Player) context.getSource();

        if (worldEdit) {
            return worldEditUtil.getSelection(player, player.getWorld());
        }

        Location a, b;

        a = context.getArgument(ARG_POS_1.getName(), ICoordinates.class).toLocation(context.getSource());
        b = context.getArgument(ARG_POS_2.getName(), ICoordinates.class).toLocation(context.getSource());

        return new Location[]{a, b};
    }

    private int fillNoFilter(CommandContext<CommandSender> context, boolean worldedit) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command");
        }

        Location[] selection = getSelection(context, worldedit);

        if (selection == null) {
            failure(this, context.getSource(), "You do not have a WorldEdit selection in that world");
            return FAILURE;
        }

        int lightLevel = context.getArgument(ARG_NAME_LIGHT_LEVEL, int.class);

        return fill((Player) context.getSource(), selection[0], selection[1], lightLevel, x -> true);
    }

    private int fillPosFilter(CommandContext<CommandSender> context, boolean worldedit) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command");
        }

        Location[] selection = getSelection(context, worldedit);

        if (selection == null) {
            failure(this, context.getSource(), "You do not have a WorldEdit selection in that world");
            return FAILURE;
        }

        int lightLevel = context.getArgument(ARG_NAME_LIGHT_LEVEL, int.class);

        Collection<Material> positiveFilter = context.getArgument(ARG_NAME_POSITIVE_FILTER, Collection.class);

        return fill((Player) context.getSource(), selection[0], selection[1], lightLevel, positiveFilter::contains);
    }

    private int fillNegFilter(CommandContext<CommandSender> context, boolean worldedit) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command");
        }

        Location[] selection = getSelection(context, worldedit);

        if (selection == null) {
            failure(this, context.getSource(), "You do not have a WorldEdit selection in that world");
            return FAILURE;
        }

        int lightLevel = context.getArgument(ARG_NAME_LIGHT_LEVEL, int.class);

        Collection<Material> negativeFilter = context.getArgument(ARG_NAME_NEGATIVE_FILTER, Collection.class);

        return fill((Player) context.getSource(), selection[0], selection[1], lightLevel, o -> !negativeFilter.contains(o));
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

                LightUpdateResult result = LightSourceUtil.placeNewLightSource(plugin, source, block.getLocation(), lightLevel, false);

                if (!result.successful()) {
                    ++failed;
                } else {
                    ++updated;
                }

                chunksToUpdate.addAll(plugin.getNmsAdapter().collectChunkPositionsToUpdate(new ChunkCoords(block.getX() >> 4, block.getZ() >> 4)));
            }

            for (ChunkCoords chunkCoords : chunksToUpdate) {
                plugin.getNmsAdapter().updateBlocks(world, chunkCoords).thenRun(
                        () -> {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getNmsAdapter().updateChunk(world, chunkCoords);
                            });
                        }
                );
            }

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

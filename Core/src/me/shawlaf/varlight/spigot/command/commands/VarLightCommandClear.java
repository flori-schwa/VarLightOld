package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.command.result.CommandResult;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.ChunkCoords;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.FAILURE;
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

        node.requires(cs -> cs instanceof LivingEntity);

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

    private int startPrompt(LivingEntity source, Set<ChunkCoords> chunks) {
        World world = source.getWorld();

        plugin.getChatPromptManager().startPrompt(
                source,
                new ComponentBuilder("[VarLight] Are you sure, you want to ")
                        .append("delete Light sources in " + chunks.size() + " chunks? ").color(ChatColor.RED)
                        .append("This action cannot be undone.").color(ChatColor.RED).underlined(true).create(),
                () -> clear(source, world, chunks),
                30,
                TimeUnit.SECONDS
        );

        return SUCCESS;
    }

    private Set<ChunkCoords> collectionRegionChunks(int regionX, int regionZ, Predicate<ChunkCoords> filter) {
        Set<ChunkCoords> chunks = new HashSet<>();

        for (int cx = 0; cx < 32; cx++) {
            for (int cz = 0; cz < 32; cz++) {
                ChunkCoords coords = new ChunkCoords(32 * regionX + cx, 32 * regionZ + cz);

                if (!filter.test(coords)) {
                    continue;
                }

                chunks.add(coords);
            }
        }

        return chunks;
    }

    private int executeChunkImplicit(CommandContext<CommandSender> context) {
        LivingEntity source = (LivingEntity) context.getSource();
        Set<ChunkCoords> chunks = new HashSet<>();
        chunks.add(new ChunkCoords(source.getLocation().getBlockX() >> 4, source.getLocation().getBlockZ() >> 4));
        return startPrompt(source, chunks);
    }

    private int executeChunkExplicit(CommandContext<CommandSender> context) {
        LivingEntity source = (LivingEntity) context.getSource();
        Set<ChunkCoords> chunks = new HashSet<>();
        chunks.add(new ChunkCoords(context.getArgument(ARG_CHUNK_X.getName(), int.class), context.getArgument(ARG_CHUNK_Z.getName(), int.class)));
        return startPrompt(source, chunks);
    }

    private int executeRegionImplicit(CommandContext<CommandSender> context) {
        LivingEntity source = (LivingEntity) context.getSource();

        int regionX = (source.getLocation().getBlockX() >> 4) >> 5;
        int regionZ = (source.getLocation().getBlockZ() >> 4) >> 5;

        WorldLightSourceManager manager = plugin.getManager(source.getWorld());

        if (manager == null) {
            failure(this, source, "VarLight is not enabled in your world");
            return FAILURE;
        }

        return startPrompt(source, collectionRegionChunks(regionX, regionZ, manager::hasChunkCustomLightData));
    }

    private int executeRegionExplicit(CommandContext<CommandSender> context) {
        LivingEntity source = (LivingEntity) context.getSource();

        int regionX = context.getArgument(ARG_REGION_X.getName(), int.class);
        int regionZ = context.getArgument(ARG_REGION_Z.getName(), int.class);

        WorldLightSourceManager manager = plugin.getManager(source.getWorld());

        if (manager == null) {
            failure(this, source, "VarLight is not enabled in your world");
            return FAILURE;
        }

        return startPrompt(source, collectionRegionChunks(regionX, regionZ, manager::hasChunkCustomLightData));
    }

    private void clear(CommandSender source, World world, Set<ChunkCoords> chunks) {
        if (Bukkit.isPrimaryThread()) {
            // Ensure this is running on a different thread
            plugin.getBukkitAsyncExecutorService().submit(() -> clear(source, world, chunks));
            return;
        }

        info(this, source, "Clearing Custom Light data in " + chunks.size() + " chunks...");

        createTickets(world, chunks).join();

        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            failure(this, source, "VarLight is not enabled in your world");
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>(chunks.size());
        int i = 0;

        for (ChunkCoords chunk : chunks) {
            manager.getNLSFile(chunk.toRegionCoords()).clearChunk(chunk);
            futures.add(plugin.getNmsAdapter().recalculateChunk(world, chunk));
//            futures.add(plugin.getNmsAdapter().resetBlocks(world, chunk));
        }

        futures.forEach(CompletableFuture::join);

        plugin.getBukkitMainThreadExecutorService().submit(() -> {
            futures.clear();

            for (ChunkCoords chunkCoords : chunks) {
                CompletableFuture<Void> future = new CompletableFuture<>();

                plugin.getNmsAdapter().updateChunk(world, chunkCoords).thenRun(() -> {
                    try {
                        plugin.getNmsAdapter().sendLightUpdates(world, chunkCoords);
                    } finally {
                        future.complete(null);
                    }
                });

                futures.add(future);
            }

            plugin.getBukkitAsyncExecutorService().submit(() -> {
                futures.forEach(CompletableFuture::join);

                releaseTickets(world, chunks).join();
                CommandResult.successBroadcast(this, source, "Cleared Custom Light sources in " + chunks.size() + " chunks");
            });
        });
    }
}
